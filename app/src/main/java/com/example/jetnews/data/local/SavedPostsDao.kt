package com.example.jetnews.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPostsDao {
    @Query("SELECT id FROM favorite_posts")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT * FROM favorite_posts ORDER BY favoritedAt DESC")
    fun observeFavoritePosts(): Flow<List<FavoritePostEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_posts WHERE id = :postId)")
    suspend fun isFavorite(postId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(entity: FavoritePostEntity)

    @Query("DELETE FROM favorite_posts WHERE id = :postId")
    suspend fun deleteFavorite(postId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingHistory(entity: ReadingHistoryEntity)

    @Query("SELECT * FROM reading_history ORDER BY lastReadAt DESC LIMIT 50")
    fun observeReadingHistory(): Flow<List<ReadingHistoryEntity>>
}
