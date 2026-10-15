package com.example.jetnews.data.posts.impl

import com.example.jetnews.data.local.SavedPostsLocalDataSource
import com.example.jetnews.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal object GuardianEmptySavedPostsLocalDataSource : SavedPostsLocalDataSource {
    override fun observeFavoriteIds(): Flow<Set<String>> = flowOf(emptySet())
    override fun observeFavoritePosts(): Flow<List<Post>> = flowOf(emptyList())
    override fun observeReadingHistory(): Flow<List<Post>> = flowOf(emptyList())
    override suspend fun findSavedPost(postId: String): Post? = null
    override suspend fun isFavorite(postId: String): Boolean = false
    override suspend fun upsertFavorite(post: Post) = Unit
    override suspend fun removeFavorite(postId: String) = Unit
    override suspend fun recordPostRead(post: Post) = Unit
}

internal class GuardianInMemorySavedPostsLocalDataSource : SavedPostsLocalDataSource {
    private val favoritePosts = MutableStateFlow<Map<String, GuardianStoredPost>>(emptyMap())
    private val readingHistory = MutableStateFlow<Map<String, GuardianStoredPost>>(emptyMap())
    private var now = 0L

    override fun observeFavoriteIds(): Flow<Set<String>> = favoritePosts.map { it.keys }

    override fun observeFavoritePosts(): Flow<List<Post>> =
        favoritePosts.map { posts -> posts.values.sortedByDescending { it.timestamp }.map { it.post } }

    override fun observeReadingHistory(): Flow<List<Post>> =
        readingHistory.map { posts -> posts.values.sortedByDescending { it.timestamp }.map { it.post } }

    override suspend fun findSavedPost(postId: String): Post? =
        favoritePosts.value[postId]?.post ?: readingHistory.value[postId]?.post

    override suspend fun isFavorite(postId: String): Boolean = favoritePosts.value.containsKey(postId)

    override suspend fun upsertFavorite(post: Post) {
        favoritePosts.value = favoritePosts.value + (post.id to GuardianStoredPost(post, nextStamp()))
    }

    override suspend fun removeFavorite(postId: String) {
        favoritePosts.value = favoritePosts.value - postId
    }

    override suspend fun recordPostRead(post: Post) {
        readingHistory.value = readingHistory.value + (post.id to GuardianStoredPost(post, nextStamp()))
    }

    private fun nextStamp(): Long = ++now
}

private data class GuardianStoredPost(val post: Post, val timestamp: Long)
