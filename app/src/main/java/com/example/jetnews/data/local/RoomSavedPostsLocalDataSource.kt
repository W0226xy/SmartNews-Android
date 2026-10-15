package com.example.jetnews.data.local

import com.example.jetnews.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RoomSavedPostsLocalDataSource(
    private val savedPostsDao: SavedPostsDao,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) : SavedPostsLocalDataSource {

    override fun observeFavoriteIds(): Flow<Set<String>> =
        savedPostsDao.observeFavoriteIds().map { it.toSet() }

    override fun observeFavoritePosts(): Flow<List<Post>> =
        savedPostsDao.observeFavoritePosts().map { entities -> entities.map(FavoritePostEntity::toPost) }

    override fun observeReadingHistory(): Flow<List<Post>> =
        savedPostsDao.observeReadingHistory().map { entities -> entities.map(ReadingHistoryEntity::toPost) }

    override suspend fun findSavedPost(postId: String): Post? =
        observeFavoritePosts().first().find { it.id == postId }
            ?: observeReadingHistory().first().find { it.id == postId }

    override suspend fun isFavorite(postId: String): Boolean = savedPostsDao.isFavorite(postId)

    override suspend fun upsertFavorite(post: Post) {
        savedPostsDao.upsertFavorite(post.toFavoriteEntity(favoritedAt = nowProvider()))
    }

    override suspend fun removeFavorite(postId: String) {
        savedPostsDao.deleteFavorite(postId)
    }

    override suspend fun recordPostRead(post: Post) {
        savedPostsDao.upsertReadingHistory(post.toReadingHistoryEntity(lastReadAt = nowProvider()))
    }
}
