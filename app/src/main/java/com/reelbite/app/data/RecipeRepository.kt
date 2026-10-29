package com.reelbite.app.data

import androidx.room.withTransaction
import com.reelbite.app.data.db.AppDatabase
import com.reelbite.app.data.db.DownloadState
import com.reelbite.app.data.db.GroupSummary
import com.reelbite.app.data.db.RecipeGroupEntity
import com.reelbite.app.data.db.ReelEntity
import com.reelbite.app.data.db.ReelStatus
import com.reelbite.app.data.remote.EmbedScraper
import com.reelbite.app.data.remote.InstagramClient
import com.reelbite.app.data.remote.ReelMetadata
import com.reelbite.app.domain.Classification
import com.reelbite.app.domain.InstagramLink
import com.reelbite.app.domain.RecipeClassifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Single source of truth for reels and recipe groups.
 *
 * Adding a reel stores it immediately (status PROCESSING) and then, in the background:
 * reads its caption → caches the thumbnail → classifies it → files it into a group
 * (creating the group when needed) → optionally saves the video to the phone.
 */
class RecipeRepository(
    private val db: AppDatabase,
    private val client: InstagramClient,
    private val scraper: EmbedScraper,
    private val saver: VideoSaver,
    private val thumbnailDir: File,
    private val scope: CoroutineScope,
) {
    private val reelDao = db.reelDao()
    private val groupDao = db.groupDao()

    private val processLimiter = Semaphore(3)
    private val downloadLimiter = Semaphore(2)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** One-off user-facing messages (download finished/failed…). */
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _downloadProgress = MutableStateFlow<Map<Long, Float>>(emptyMap())

    /** Download progress (0..1) per reel id while a download is running. */
    val downloadProgress: StateFlow<Map<Long, Float>> = _downloadProgress.asStateFlow()

    val groupSummaries: Flow<List<GroupSummary>> = groupDao.observeSummaries()
    val allReels: Flow<List<ReelEntity>> = reelDao.observeAll()

    fun observeGroup(groupId: Long): Flow<RecipeGroupEntity?> = groupDao.observe(groupId)
    fun observeReels(groupId: Long): Flow<List<ReelEntity>> = reelDao.observeByGroup(groupId)
    suspend fun allGroups(): List<RecipeGroupEntity> = groupDao.getAll()

    data class AddResult(val added: Int, val duplicates: Int)

    // ------------------------------------------------------------------ adding & sorting

    suspend fun addLinks(links: List<InstagramLink>, saveVideos: Boolean): AddResult {
        var added = 0
        var duplicates = 0
        for (link in links) {
            val existing = reelDao.getByShortcode(link.shortcode)
            if (existing != null) {
                duplicates++
                if (saveVideos && existing.downloadState in RETRYABLE) requestDownload(existing.id)
                continue
            }
            val id = reelDao.insert(
                ReelEntity(
                    shortcode = link.shortcode,
                    url = link.url,
                    downloadRequested = saveVideos,
                    downloadState = if (saveVideos) DownloadState.QUEUED else DownloadState.NONE,
                ),
            )
            if (id > 0) {
                added++
                scope.launch { process(id) }
            } else {
                duplicates++
            }
        }
        return AddResult(added, duplicates)
    }

    /** Restarts work interrupted by the app being closed. */
    fun resumePendingWork() {
        scope.launch {
            reelDao.getProcessing().forEach { reel -> launch { process(reel.id) } }
            reelDao.getPendingDownloads()
                .filter { it.status != ReelStatus.PROCESSING } // those download after processing
                .forEach { reel -> launch { runDownload(reel.id) } }
        }
    }

    private suspend fun process(reelId: Long) {
        processLimiter.withPermit {
            val reel = reelDao.get(reelId) ?: return
            var meta: ReelMetadata? = client.fetchOEmbed(reel.shortcode)
            if (meta?.caption == null) {
                val scraped = scraper.scrape(reel.shortcode, wantVideo = false)
                meta = merge(meta, scraped)
            }
            val thumbnail = meta?.thumbnailUrl?.let { cacheThumbnail(reel.shortcode, it) }

            fileIntoGroup(reelId) { fresh ->
                fresh.copy(
                    caption = meta?.caption ?: fresh.caption,
                    author = meta?.author ?: fresh.author,
                    thumbnailPath = thumbnail ?: fresh.thumbnailPath,
                )
            }
            val latest = reelDao.get(reelId) ?: return // deleted while we were working
            if (latest.downloadState == DownloadState.QUEUED) scope.launch { runDownload(reelId) }
        }
    }

    private fun merge(primary: ReelMetadata?, secondary: ReelMetadata?): ReelMetadata? {
        if (primary == null) return secondary
        if (secondary == null) return primary
        return ReelMetadata(
            caption = primary.caption ?: secondary.caption,
            author = primary.author ?: secondary.author,
            thumbnailUrl = primary.thumbnailUrl ?: secondary.thumbnailUrl,
            videoUrl = primary.videoUrl ?: secondary.videoUrl,
        )
    }

    private suspend fun cacheThumbnail(shortcode: String, url: String): String? {
        val file = File(thumbnailDir, "$shortcode.jpg")
        return if (client.downloadToFile(url, file)) file.absolutePath else null
    }

    private fun classify(reel: ReelEntity): Classification {
        val title = reel.userTitle?.trim().orEmpty()
        if (title.isNotEmpty()) {
            val fromTitle = RecipeClassifier.classify(title)
            if (fromTitle.recognised) return fromTitle
            // Unknown dish typed by the user: trust it and make a group with that exact name.
            return Classification(
                key = "custom|${RecipeClassifier.normalize(title)}",
                name = title,
                emoji = DEFAULT_EMOJI,
                recognised = true,
            )
        }
        return RecipeClassifier.classify(reel.caption)
    }

    /**
     * Applies [edit] to the latest stored copy of the reel, classifies it, puts it in the
     * matching group and saves it — atomically, so concurrent updates aren't lost.
     */
    private suspend fun fileIntoGroup(reelId: Long, edit: (ReelEntity) -> ReelEntity = { it }) {
        db.withTransaction {
            val reel = edit(reelDao.get(reelId) ?: return@withTransaction)
            val classification = classify(reel)
            val group = obtainGroup(classification)
            reelDao.update(
                reel.copy(
                    groupId = group.id,
                    status = if (classification.recognised) ReelStatus.READY else ReelStatus.NEEDS_INFO,
                    manuallyGrouped = false,
                    errorMessage = when {
                        classification.recognised -> null
                        reel.caption == null -> "Couldn't read this reel's caption. Name the dish to sort it."
                        else -> "Couldn't recognise the dish. Name it to sort it."
                    },
                ),
            )
            groupDao.deleteEmpty()
        }
    }

    /** Must run inside a transaction so a concurrent [GroupDao.deleteEmpty] can't drop it. */
    private suspend fun obtainGroup(c: Classification): RecipeGroupEntity {
        groupDao.getByKey(c.key)?.let { return it }
        val id = groupDao.insert(RecipeGroupEntity(groupKey = c.key, name = c.name, emoji = c.emoji))
        return groupDao.get(id) ?: groupDao.getByKey(c.key)
            ?: error("Could not create group ${c.key}")
    }

    // ------------------------------------------------------------------ user edits

    /** Sets (or clears) the dish name for a reel and re-sorts it. */
    suspend fun setDishName(reelId: Long, name: String) {
        fileIntoGroup(reelId) { it.copy(userTitle = name.trim().ifEmpty { null }) }
    }

    suspend fun moveReel(reelId: Long, groupId: Long) {
        db.withTransaction {
            val reel = reelDao.get(reelId) ?: return@withTransaction
            groupDao.get(groupId) ?: return@withTransaction
            reelDao.update(
                reel.copy(groupId = groupId, manuallyGrouped = true, status = ReelStatus.READY, errorMessage = null),
            )
            groupDao.deleteEmpty()
        }
    }

    suspend fun moveReelToNewGroup(reelId: Long, groupName: String) {
        val name = groupName.trim()
        if (name.isEmpty()) return
        db.withTransaction {
            val group = findOrCreateGroupByName(name)
            moveReel(reelId, group.id)
        }
    }

    private suspend fun findOrCreateGroupByName(name: String): RecipeGroupEntity {
        groupDao.getByName(name)?.let { return it }
        val guess = RecipeClassifier.classify(name)
        // If the name is a known dish, reuse the classifier key so future reels of it land here.
        val key = if (guess.recognised && groupDao.getByKey(guess.key) == null) {
            guess.key
        } else {
            "custom|${RecipeClassifier.normalize(name)}"
        }
        groupDao.getByKey(key)?.let { return it }
        val emoji = if (guess.recognised) guess.emoji else DEFAULT_EMOJI
        val id = groupDao.insert(RecipeGroupEntity(groupKey = key, name = name, emoji = emoji))
        return groupDao.get(id) ?: error("Could not create group $name")
    }

    /** Renames a group. Renaming to an existing group's name merges the two. */
    suspend fun renameGroup(groupId: Long, newName: String) {
        val name = newName.trim()
        if (name.isEmpty()) return
        db.withTransaction {
            val group = groupDao.get(groupId) ?: return@withTransaction
            val other = groupDao.getByName(name)
            if (other != null && other.id != groupId) {
                reelDao.moveAll(groupId, other.id)
                groupDao.delete(groupId)
            } else {
                groupDao.update(group.copy(name = name))
            }
        }
    }

    suspend fun deleteGroup(groupId: Long, deleteSavedVideos: Boolean) {
        val reels = reelDao.getByGroup(groupId)
        reels.forEach { cleanUpFiles(it, deleteSavedVideos) }
        db.withTransaction {
            reelDao.deleteByGroup(groupId)
            groupDao.delete(groupId)
        }
    }

    suspend fun deleteReel(reelId: Long, deleteSavedVideo: Boolean) {
        val reel = reelDao.get(reelId) ?: return
        cleanUpFiles(reel, deleteSavedVideo)
        db.withTransaction {
            reelDao.delete(reelId)
            groupDao.deleteEmpty()
        }
    }

    private suspend fun cleanUpFiles(reel: ReelEntity, deleteSavedVideo: Boolean) {
        withContext(Dispatchers.IO) { reel.thumbnailPath?.let { File(it).delete() } }
        if (deleteSavedVideo) reel.localVideoUri?.let { saver.delete(it) }
    }

    // ------------------------------------------------------------------ saving videos

    fun requestDownload(reelId: Long) {
        scope.launch {
            val reel = reelDao.get(reelId) ?: return@launch
            if (reel.downloadState == DownloadState.DOWNLOADING) return@launch
            reelDao.update(
                reel.copy(downloadRequested = true, downloadState = DownloadState.QUEUED, downloadError = null),
            )
            if (reel.status != ReelStatus.PROCESSING) runDownload(reelId)
        }
    }

    fun downloadAll(groupId: Long) {
        scope.launch {
            reelDao.getByGroup(groupId)
                .filter { it.downloadState in RETRYABLE }
                .forEach { requestDownload(it.id) }
        }
    }

    /** Returns true if the saved video still exists; resets the reel's state if it was deleted. */
    suspend fun verifySavedVideo(reel: ReelEntity): Boolean {
        val uri = reel.localVideoUri ?: return false
        if (saver.exists(uri)) return true
        reelDao.get(reel.id)?.let {
            reelDao.update(it.copy(downloadState = DownloadState.NONE, localVideoUri = null))
        }
        return false
    }

    private suspend fun runDownload(reelId: Long) {
        downloadLimiter.withPermit {
            val reel = reelDao.get(reelId) ?: return
            if (reel.downloadState == DownloadState.DONE) return
            reelDao.update(reel.copy(downloadState = DownloadState.DOWNLOADING, downloadError = null))
            _downloadProgress.update { it + (reelId to 0f) }
            try {
                val meta = scraper.scrape(reel.shortcode, wantVideo = true)
                val videoUrl = meta?.videoUrl
                    ?: throw IOException(
                        if (meta?.videoBlocked == true) {
                            "Instagram only plays this reel inside its app (licensed music), so it can't be saved"
                        } else {
                            "Instagram didn't share a downloadable video for this reel"
                        },
                    )

                val current = reelDao.get(reelId) ?: return
                val groupName = current.groupId?.let { groupDao.get(it)?.name }
                val uri = saver.save(videoUrl, "${groupName ?: "reel"}_${reel.shortcode}") { fraction ->
                    _downloadProgress.update { it + (reelId to fraction) }
                }

                val latest = reelDao.get(reelId)
                if (latest == null) { // deleted mid-download: don't leave an orphan file
                    saver.delete(uri.toString())
                    return
                }
                reelDao.update(latest.copy(downloadState = DownloadState.DONE, localVideoUri = uri.toString()))
                // The embed page may have given us a caption oEmbed couldn't.
                if (latest.caption == null && meta.caption != null && !latest.manuallyGrouped) {
                    fileIntoGroup(reelId) {
                        it.copy(caption = meta.caption, author = it.author ?: meta.author)
                    }
                }
                _events.tryEmit("Reel saved to Movies/${VideoSaver.FOLDER}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "Download failed"
                reelDao.get(reelId)?.let {
                    reelDao.update(it.copy(downloadState = DownloadState.FAILED, downloadError = message))
                }
                _events.tryEmit("Couldn't save reel: $message")
            } finally {
                _downloadProgress.update { it - reelId }
            }
        }
    }

    private companion object {
        const val DEFAULT_EMOJI = "🍽️"
        val RETRYABLE = setOf(DownloadState.NONE, DownloadState.FAILED)
    }
}
