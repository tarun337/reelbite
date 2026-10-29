package com.reelbite.app.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.reelbite.app.data.remote.InstagramClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Saves reel videos into the shared "Movies/ReelBite" folder via MediaStore, so they show up
 * in the phone's gallery. No storage permission is needed on Android 10+.
 */
class VideoSaver(context: Context, private val client: InstagramClient) {

    private val resolver = context.applicationContext.contentResolver

    suspend fun save(videoUrl: String, baseName: String, onProgress: (Float) -> Unit): Uri =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "${sanitize(baseName)}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$FOLDER")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values) ?: throw IOException("Couldn't create a file in Movies")
            try {
                val out = resolver.openOutputStream(uri) ?: throw IOException("Couldn't write to Movies")
                out.use { client.downloadVideo(videoUrl, it, onProgress) }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } catch (t: Throwable) {
                runCatching { resolver.delete(uri, null, null) }
                throw t
            }
        }

    /** True if the saved video is still on the device (the user may delete it from the gallery). */
    suspend fun exists(uri: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            resolver.query(Uri.parse(uri), arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                ?.use { it.moveToFirst() } ?: false
        }.getOrDefault(false)
    }

    suspend fun delete(uri: String): Boolean = withContext(Dispatchers.IO) {
        // Only works for files this app created; anything else is left untouched.
        runCatching { resolver.delete(Uri.parse(uri), null, null) > 0 }.getOrDefault(false)
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("""[^\p{L}\p{N} _-]"""), "")
            .trim()
            .replace(Regex("""\s+"""), "_")
            .take(80)
            .ifEmpty { "reel" }

    companion object {
        const val FOLDER = "ReelBite"
    }
}
