package com.reelbite.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query(
        """
        SELECT g.*,
               COUNT(r.id) AS reelCount,
               SUM(CASE WHEN r.downloadState = 'DONE' THEN 1 ELSE 0 END) AS savedCount,
               (SELECT r2.thumbnailPath FROM reels r2
                 WHERE r2.groupId = g.id AND r2.thumbnailPath IS NOT NULL
                 ORDER BY r2.addedAt DESC LIMIT 1) AS coverPath,
               MAX(r.addedAt) AS lastAdded
        FROM recipe_groups g
        INNER JOIN reels r ON r.groupId = g.id
        GROUP BY g.id
        ORDER BY lastAdded DESC
        """,
    )
    fun observeSummaries(): Flow<List<GroupSummary>>

    @Query("SELECT * FROM recipe_groups WHERE id = :id")
    fun observe(id: Long): Flow<RecipeGroupEntity?>

    @Query("SELECT * FROM recipe_groups WHERE id = :id")
    suspend fun get(id: Long): RecipeGroupEntity?

    @Query("SELECT * FROM recipe_groups WHERE groupKey = :key")
    suspend fun getByKey(key: String): RecipeGroupEntity?

    @Query("SELECT * FROM recipe_groups WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): RecipeGroupEntity?

    @Query("SELECT * FROM recipe_groups ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<RecipeGroupEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(group: RecipeGroupEntity): Long

    @Update
    suspend fun update(group: RecipeGroupEntity)

    @Query("DELETE FROM recipe_groups WHERE id = :id")
    suspend fun delete(id: Long)

    /** Groups are implicit containers; drop them once they hold no reels. */
    @Query("DELETE FROM recipe_groups WHERE id NOT IN (SELECT DISTINCT groupId FROM reels WHERE groupId IS NOT NULL)")
    suspend fun deleteEmpty()
}

@Dao
interface ReelDao {

    @Query("SELECT * FROM reels ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<ReelEntity>>

    @Query("SELECT * FROM reels WHERE groupId = :groupId ORDER BY addedAt DESC")
    fun observeByGroup(groupId: Long): Flow<List<ReelEntity>>

    @Query("SELECT * FROM reels WHERE groupId = :groupId")
    suspend fun getByGroup(groupId: Long): List<ReelEntity>

    @Query("SELECT * FROM reels WHERE id = :id")
    suspend fun get(id: Long): ReelEntity?

    @Query("SELECT * FROM reels WHERE shortcode = :shortcode")
    suspend fun getByShortcode(shortcode: String): ReelEntity?

    @Query("SELECT * FROM reels WHERE status = 'PROCESSING'")
    suspend fun getProcessing(): List<ReelEntity>

    @Query("SELECT * FROM reels WHERE downloadState IN ('QUEUED', 'DOWNLOADING')")
    suspend fun getPendingDownloads(): List<ReelEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reel: ReelEntity): Long

    @Update
    suspend fun update(reel: ReelEntity)

    @Query("UPDATE reels SET groupId = :toGroupId WHERE groupId = :fromGroupId")
    suspend fun moveAll(fromGroupId: Long, toGroupId: Long)

    @Query("DELETE FROM reels WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM reels WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: Long)
}
