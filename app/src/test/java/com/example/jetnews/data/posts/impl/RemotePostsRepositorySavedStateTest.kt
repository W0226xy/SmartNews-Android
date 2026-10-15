package com.example.jetnews.data.posts.impl

import com.example.jetnews.data.local.SavedPostsLocalDataSource
import com.example.jetnews.data.remote.SpaceflightNewsService
import com.example.jetnews.data.remote.model.SpaceflightArticleDto
import com.example.jetnews.data.remote.model.SpaceflightArticlesResponseDto
import com.example.jetnews.data.remote.model.SpaceflightAuthorDto
import com.example.jetnews.data.remote.toPostOrNull
import com.example.jetnews.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RemotePostsRepositorySavedStateTest {

    @Test
    fun favoriteAfterToggle_canReadFavoritePosts() = runTest {
        val repository = createRepository()
        val post = loadSamplePost(repository)

        repository.toggleFavorite(post)

        val favorites = repository.observeFavoritePosts().first()

        assertEquals(listOf(post.id), favorites.map { it.id })
    }

    @Test
    fun unfavoriteAfterToggle_updatesFavoriteList() = runTest {
        val repository = createRepository()
        val post = loadSamplePost(repository)
        repository.toggleFavorite(post)

        repository.toggleFavorite(post)

        assertTrue(repository.observeFavoritePosts().first().isEmpty())
    }

    @Test
    fun recordPostReadTwice_keepsSingleEntryAndRefreshesOrder() = runTest {
        val repository = createRepository()
        val firstPost = loadSamplePost(repository)
        val secondPost = samplePost(id = 99, title = "Lunar gateway update")

        repository.recordPostRead(firstPost)
        repository.recordPostRead(secondPost)
        repository.recordPostRead(firstPost)

        val history = repository.observeReadingHistory().first()

        assertEquals(listOf(firstPost.id, secondPost.id), history.map { it.id })
        assertEquals(2, history.size)
    }

    @Test
    fun readingHistory_isSortedByMostRecentRead() = runTest {
        val repository = createRepository()
        val firstPost = loadSamplePost(repository)
        val secondPost = samplePost(id = 100, title = "Crew mission update")

        repository.recordPostRead(firstPost)
        repository.recordPostRead(secondPost)

        val history = repository.observeReadingHistory().first()

        assertEquals(listOf(secondPost.id, firstPost.id), history.map { it.id })
    }

    @Test
    fun favoriteIds_mapBackToFavoritedPost() = runTest {
        val repository = createRepository()
        val post = loadSamplePost(repository)

        repository.toggleFavorite(post)

        val favoriteIds = repository.observeFavoriteIds().first()
        val favorites = repository.observeFavoritePosts().first()

        assertTrue(favoriteIds.contains(post.id))
        assertEquals(post.id, favorites.single().id)
        assertEquals(post.title, favorites.single().title)
    }

    private fun createRepository(): RemotePostsRepository =
        RemotePostsRepository(
            service = TestSpaceflightNewsService(
                listResponse = Response.success(
                    SpaceflightArticlesResponseDto(
                        results = listOf(sampleArticle()),
                    ),
                ),
            ),
            savedPostsLocalDataSource = InMemorySavedPostsLocalDataSource(),
        )

    private suspend fun loadSamplePost(repository: RemotePostsRepository): Post {
        repository.getPostsFeed()
        return sampleArticle().toPostOrNull()!!
    }

    private fun samplePost(id: Int, title: String): Post =
        sampleArticle(
            publishedAt = "2026-02-03T04:05:06Z",
            summary = "$title summary",
        ).copy(id = id.toLong(), title = title).toPostOrNull()!!
}

private class TestSpaceflightNewsService(
    private val listResponse: Response<SpaceflightArticlesResponseDto>,
    private val articleResponse: Response<SpaceflightArticleDto> = Response.success(sampleArticle()),
) : SpaceflightNewsService {
    override suspend fun getArticles(limit: Int): Response<SpaceflightArticlesResponseDto> = listResponse

    override suspend fun getArticle(id: Long): Response<SpaceflightArticleDto> = articleResponse
}

private class InMemorySavedPostsLocalDataSource : SavedPostsLocalDataSource {
    private val favoritePosts = MutableStateFlow<Map<String, StoredPost>>(emptyMap())
    private val readingHistory = MutableStateFlow<Map<String, StoredPost>>(emptyMap())
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
        favoritePosts.value = favoritePosts.value + (post.id to StoredPost(post, nextStamp()))
    }

    override suspend fun removeFavorite(postId: String) {
        favoritePosts.value = favoritePosts.value - postId
    }

    override suspend fun recordPostRead(post: Post) {
        readingHistory.value = readingHistory.value + (post.id to StoredPost(post, nextStamp()))
    }

    private fun nextStamp(): Long = ++now
}

private data class StoredPost(val post: Post, val timestamp: Long)

private fun sampleArticle(
    authors: List<SpaceflightAuthorDto> = listOf(SpaceflightAuthorDto("NASA")),
    imageUrl: String? = "https://example.com/image.jpg",
    newsSite: String? = "NASA",
    summary: String? = "A concise summary of the Mars mission update.",
    publishedAt: String? = "2026-01-02T03:04:05Z",
) = SpaceflightArticleDto(
    id = 42,
    title = "Mars mission update",
    authors = authors,
    url = "https://example.com/article",
    imageUrl = imageUrl,
    newsSite = newsSite,
    summary = summary,
    publishedAt = publishedAt,
)
