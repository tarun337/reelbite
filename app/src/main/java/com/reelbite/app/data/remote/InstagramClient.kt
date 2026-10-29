package com.reelbite.app.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.OutputStream

/** What we could learn about a reel. Any field may be missing. */
data class ReelMetadata(
    val caption: String? = null,
    val author: String? = null,
    val thumbnailUrl: String? = null,
    val videoUrl: String? = null,
    /** Instagram won't play this reel outside its app (usually licensed music), so it can't be saved. */
    val videoBlocked: Boolean = false,
)

/**
 * Talks to Instagram's public, login-free endpoints.
 *
 * Primary source is the oEmbed endpoint, which returns the caption, author and thumbnail for
 * public posts. Video URLs are not part of oEmbed; see [EmbedScraper] for those.
 */
class InstagramClient(private val http: OkHttpClient) {

    suspend fun fetchOEmbed(shortcode: String): ReelMetadata? = withContext(Dispatchers.IO) {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("www.instagram.com")
            .addPathSegments("api/v1/oembed/")
            .addQueryParameter("url", "https://www.instagram.com/p/$shortcode/")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", BROWSER_UA)
            .build()
        runCatching {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "oEmbed $shortcode: HTTP ${response.code}")
                    return@use null
                }
                val body = response.body?.string() ?: return@use null
                val json = JSONObject(body)
                ReelMetadata(
                    caption = json.stringOrNull("title"),
                    author = json.stringOrNull("author_name"),
                    thumbnailUrl = json.stringOrNull("thumbnail_url"),
                )
            }
        }.onFailure { Log.w(TAG, "oEmbed $shortcode failed", it) }.getOrNull()
    }

    /** Downloads [url] into [target]. Returns false on any failure. */
    suspend fun downloadToFile(url: String, target: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            target.parentFile?.mkdirs()
            val tmp = File(target.parentFile, target.name + ".part")
            http.newCall(mediaRequest(url)).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val body = response.body ?: return@use false
                tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                tmp.renameTo(target)
            }
        }.onFailure { Log.w(TAG, "Thumbnail download failed", it) }.getOrDefault(false)
    }

    /**
     * Streams a video into [out]. Throws [IOException] with a user-friendly message on failure.
     * [onProgress] receives a 0..1 fraction when the size is known.
     *
     * Mobile networks and the CDN regularly drop long transfers, so an interrupted download
     * is resumed from the last byte with an HTTP Range request instead of failing.
     */
    suspend fun downloadVideo(url: String, out: OutputStream, onProgress: (Float) -> Unit = {}) =
        withContext(Dispatchers.IO) {
            var copied = 0L
            var total = -1L
            var attempt = 0
            val buffer = ByteArray(64 * 1024)
            while (true) {
                try {
                    http.newCall(mediaRequest(url, rangeFrom = copied.takeIf { it > 0 })).execute().use { response ->
                        if (copied > 0 && response.code != 206) {
                            throw FatalDownloadException("Download was interrupted and couldn't be resumed")
                        }
                        if (!response.isSuccessful) {
                            throw FatalDownloadException("Instagram refused the download (HTTP ${response.code})")
                        }
                        val body = response.body ?: throw IOException("Empty response from Instagram")
                        val type = body.contentType()?.toString().orEmpty()
                        if (type.isNotEmpty() && !type.startsWith("video") && !type.contains("octet-stream")) {
                            throw FatalDownloadException("Instagram didn't return a video file")
                        }
                        if (total < 0) {
                            total = response.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
                                ?: body.contentLength().takeIf { it > 0 }?.plus(copied)
                                ?: -1L
                        }
                        body.byteStream().use { input ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                out.write(buffer, 0, read)
                                copied += read
                                if (total > 0) onProgress(copied.toFloat() / total)
                            }
                        }
                    }
                    if (total > 0 && copied < total) throw IOException("Connection closed early")
                    break
                } catch (e: FatalDownloadException) {
                    throw e
                } catch (e: IOException) {
                    if (++attempt > MAX_RESUME_ATTEMPTS) {
                        throw IOException("Network kept dropping — try again on a better connection", e)
                    }
                    Log.w(TAG, "Video download interrupted at $copied/$total bytes, resuming (attempt $attempt)", e)
                    delay(1_000L * attempt)
                }
            }
            if (copied == 0L) throw IOException("Downloaded file was empty")
        }

    private class FatalDownloadException(message: String) : IOException(message)

    private fun mediaRequest(url: String, rangeFrom: Long? = null) = Request.Builder()
        .url(url)
        .header("User-Agent", BROWSER_UA)
        .header("Referer", "https://www.instagram.com/")
        .apply { if (rangeFrom != null) header("Range", "bytes=$rangeFrom-") }
        .build()

    private fun JSONObject.stringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    companion object {
        private const val TAG = "InstagramClient"
        private const val MAX_RESUME_ATTEMPTS = 6
        const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/130.0.0.0 Mobile Safari/537.36"
    }
}
