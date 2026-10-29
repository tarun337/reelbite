package com.reelbite.app.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

/**
 * Reads a reel through Instagram's public embed page (the same page websites use to embed
 * posts) inside an off-screen WebView. The page is rendered with JavaScript, so a real browser
 * engine is needed. Used for:
 *  - the caption, when oEmbed doesn't answer;
 *  - the video file URL, needed for the optional "save to phone" feature.
 *
 * This is best effort: Instagram can change this page at any time.
 */
class EmbedScraper(context: Context) {

    private val appContext = context.applicationContext
    private val mutex = Mutex()

    suspend fun scrape(shortcode: String, wantVideo: Boolean, timeoutMs: Long = 20_000): ReelMetadata? =
        mutex.withLock {
            withContext(Dispatchers.Main) { runScrape(shortcode, wantVideo, timeoutMs) }
        }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun runScrape(shortcode: String, wantVideo: Boolean, timeoutMs: Long): ReelMetadata? {
        val interceptedVideo = AtomicReference<String?>(null)
        lastIsVideo = null
        var rendererGone = false
        val webView = WebView(appContext)
        var best: ReelMetadata? = null
        try {
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
            }
            // Give the page a real viewport so lazy media loading behaves like on screen.
            webView.layout(0, 0, 1080, 1920)
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    // Stay on the embed page; ignore redirects to the app / login.
                    !request.url.path.orEmpty().contains("/embed")

                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    videoFileUrl(request.url)?.let { interceptedVideo.compareAndSet(null, it) }
                    return null
                }

                // Without this, a crashed/killed renderer would take the whole app down with it.
                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    rendererGone = true
                    return true
                }
            }
            webView.loadUrl("https://www.instagram.com/p/$shortcode/embed/captioned/")

            val deadline = SystemClock.uptimeMillis() + timeoutMs
            var polls = 0
            while (SystemClock.uptimeMillis() < deadline && !rendererGone) {
                delay(POLL_MS)
                polls++
                val result = parse(webView.eval(EXTRACT_JS))
                if (result != null) {
                    best = ReelMetadata(
                        caption = result.caption ?: best?.caption,
                        author = result.author ?: best?.author,
                        thumbnailUrl = result.thumbnailUrl ?: best?.thumbnailUrl,
                        videoUrl = result.videoUrl ?: best?.videoUrl,
                        videoBlocked = result.videoBlocked || best?.videoBlocked == true,
                    )
                }
                val current = best
                val hasCaption = current?.caption != null
                if (!wantVideo && hasCaption) break
                if (wantVideo) {
                    if (current?.videoUrl != null || current?.videoBlocked == true) break
                    if (interceptedVideo.get() != null && polls >= 4) break
                    // Nudge the player so the video source gets requested.
                    if (polls % 3 == 0) webView.eval(PLAY_JS)
                    // Photo post: nothing to download.
                    if (hasCaption && lastIsVideo == false && polls >= 8) break
                }
            }
        } finally {
            webView.stopLoading()
            webView.destroy()
        }
        val intercepted = interceptedVideo.get()
        return when {
            best == null && intercepted == null -> null
            best?.videoUrl == null && intercepted != null -> (best ?: ReelMetadata()).copy(videoUrl = intercepted)
            else -> best
        }
    }

    private var lastIsVideo: Boolean? = null

    private fun parse(raw: String?): ReelMetadata? {
        if (raw.isNullOrBlank() || raw == "null") return null
        return runCatching {
            // evaluateJavascript returns the JS value JSON-encoded; our script returns a JSON string.
            val decoded = JSONTokener(raw).nextValue() as? String ?: return null
            val json = JSONObject(decoded)
            lastIsVideo = if (json.isNull("isVideo")) null else json.optBoolean("isVideo")
            ReelMetadata(
                caption = json.stringOrNull("caption"),
                author = json.stringOrNull("author"),
                thumbnailUrl = json.stringOrNull("image"),
                videoUrl = json.stringOrNull("video")?.takeIf { it.startsWith("http") },
                videoBlocked = json.optBoolean("blocked", false),
            )
        }.getOrNull()
    }

    private fun JSONObject.stringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private suspend fun WebView.eval(script: String): String? =
        suspendCancellableCoroutine { cont -> evaluateJavascript(script) { cont.resume(it) } }

    /** Recognises progressive MP4 requests from Instagram's CDN and strips byte-range params. */
    private fun videoFileUrl(uri: Uri): String? {
        val host = uri.host.orEmpty()
        val isCdn = host.endsWith("cdninstagram.com") || host.endsWith("fbcdn.net")
        if (!isCdn || !uri.path.orEmpty().endsWith(".mp4")) return null
        val builder = uri.buildUpon().clearQuery()
        uri.queryParameterNames
            .filterNot { it == "bytestart" || it == "byteend" }
            .forEach { name -> uri.getQueryParameters(name).forEach { builder.appendQueryParameter(name, it) } }
        return builder.build().toString()
    }

    private companion object {
        const val POLL_MS = 700L

        val EXTRACT_JS = """
            (function () {
              // The embed page ships the post data as a double-encoded JSON string "contextJSON".
              function context() {
                var sources = [];
                document.querySelectorAll('script').forEach(function (s) {
                  if (s.textContent.indexOf('contextJSON') >= 0) sources.push(s.textContent);
                });
                if (!sources.length) sources.push(document.documentElement.innerHTML);
                for (var i = 0; i < sources.length; i++) {
                  var m = sources[i].match(/"contextJSON":"((?:[^"\\]|\\.)*)"/);
                  if (!m) continue;
                  try { return JSON.parse(JSON.parse('"' + m[1] + '"')); } catch (e) {}
                }
                return null;
              }
              function captionText(el) {
                if (!el) return null;
                var c = el.cloneNode(true);
                c.querySelectorAll('br').forEach(function (b) { b.replaceWith('\n'); });
                var u = c.querySelector('.CaptionUsername'); if (u) u.remove();
                var cc = c.querySelector('.CaptionComments'); if (cc) cc.remove();
                var t = (c.textContent || '').trim();
                return t.length ? t : null;
              }
              var ctx = context();
              var media = ctx && ctx.gql_data && ctx.gql_data.shortcode_media;
              var blocked = !!(ctx && ctx.context && ctx.context.copyright_blocked);

              var caption = null, author = null, image = null, src = null, isVideo = null;
              if (media) {
                var edges = media.edge_media_to_caption && media.edge_media_to_caption.edges;
                if (edges && edges.length && edges[0].node) caption = edges[0].node.text || null;
                author = media.owner ? media.owner.username : null;
                image = media.display_url || null;
                src = media.video_url || null;
                isVideo = !!media.is_video;
              }
              // Fall back to what's rendered on the page.
              if (!caption) caption = captionText(document.querySelector('.Caption'));
              if (!author) {
                var user = document.querySelector('.CaptionUsername') || document.querySelector('.UsernameText');
                author = user ? user.textContent.trim() : null;
              }
              if (!image) {
                var img = document.querySelector('img.EmbeddedMediaImage');
                image = img ? (img.currentSrc || img.src) : null;
              }
              var vid = document.querySelector('video');
              if (!src && vid) {
                src = vid.currentSrc || vid.src || null;
                if (!src) { var s = vid.querySelector('source'); if (s) src = s.src; }
              }
              if (src && src.indexOf('http') !== 0) src = null; // blob: URLs can't be downloaded
              if (isVideo === null && vid) isVideo = true;
              return JSON.stringify({
                caption: caption, author: author, image: image,
                video: src, isVideo: isVideo, blocked: blocked
              });
            })();
        """.trimIndent()

        val PLAY_JS = """
            (function () {
              var v = document.querySelector('video');
              if (v) { v.muted = true; var p = v.play(); if (p && p.catch) p.catch(function () {}); }
              var b = document.querySelector('[aria-label*="Play" i], .PlayButton, [class*="playButton"]');
              if (b && b.tagName !== 'A') b.click();
              return true;
            })();
        """.trimIndent()
    }
}
