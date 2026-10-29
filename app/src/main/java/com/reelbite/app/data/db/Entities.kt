package com.reelbite.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Lifecycle of reading a reel's caption and sorting it into a group. */
enum class ReelStatus {
    /** Caption is being fetched / classified. */
    PROCESSING,

    /** Sorted into a recipe group. */
    READY,

    /** Caption could not be read; the user should name the dish. */
    NEEDS_INFO,
}

/** State of the optional "save video to phone" feature for a reel. */
enum class DownloadState { NONE, QUEUED, DOWNLOADING, DONE, FAILED }

@Entity(
    tableName = "recipe_groups",
    indices = [Index(value = ["groupKey"], unique = true)],
)
data class RecipeGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /**
     * Stable identity produced by the classifier (e.g. "biryani|chicken").
     * Renaming a group keeps the key, so future reels of the same dish still land here.
     */
    val groupKey: String,
    val name: String,
    val emoji: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "reels",
    indices = [Index(value = ["shortcode"], unique = true), Index(value = ["groupId"])],
    foreignKeys = [
        ForeignKey(
            entity = RecipeGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class ReelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shortcode: String,
    val url: String,
    val caption: String? = null,
    /** Dish name typed by the user; takes priority over the caption for sorting and display. */
    val userTitle: String? = null,
    val author: String? = null,
    /** Local file path of the cached thumbnail (Instagram CDN links expire). */
    val thumbnailPath: String? = null,
    val groupId: Long? = null,
    val status: ReelStatus = ReelStatus.PROCESSING,
    val manuallyGrouped: Boolean = false,
    val downloadRequested: Boolean = false,
    val downloadState: DownloadState = DownloadState.NONE,
    /** MediaStore content URI of the saved video. */
    val localVideoUri: String? = null,
    val downloadError: String? = null,
    val errorMessage: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
)

/** A group plus aggregate info for the home grid. */
data class GroupSummary(
    @Embedded val group: RecipeGroupEntity,
    @ColumnInfo(name = "reelCount") val reelCount: Int,
    @ColumnInfo(name = "savedCount") val savedCount: Int,
    @ColumnInfo(name = "coverPath") val coverPath: String?,
    @ColumnInfo(name = "lastAdded") val lastAdded: Long,
)
