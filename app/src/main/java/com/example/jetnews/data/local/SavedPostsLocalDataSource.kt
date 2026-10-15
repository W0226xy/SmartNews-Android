package com.example.jetnews.data.local

import com.example.jetnews.model.Post
import kotlinx.coroutines.flow.Flow

interface SavedPostsLocalDataSource {
    fun observeFavoriteIds(): Flow<Set<String>>

    fun observeFavoritePosts(): Flow<List<Post>>

    fun observeReadingHistory(): Flow<List<Post>>

    suspend fun findSavedPost(postId: String): Post?

    suspend fun isFavorite(postId: String): Boolean

    suspend fun upsertFavorite(post: Post)

    suspend fun removeFavorite(postId: String)

    suspend fun recordPostRead(post: Post)
}
