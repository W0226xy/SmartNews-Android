package com.example.jetnews.data.posts.impl

import com.example.jetnews.data.Result
import com.example.jetnews.data.remote.GuardianNewsService
import com.example.jetnews.data.remote.model.GuardianItemResponseDto
import com.example.jetnews.data.remote.model.GuardianItemResultDto
import com.example.jetnews.data.remote.model.GuardianSearchResponseDto
import com.example.jetnews.data.remote.model.GuardianSearchResultDto
import com.example.jetnews.data.remote.sampleGuardianArticle
import com.example.jetnews.data.remote.toPostOrNull
import com.example.jetnews.model.Post
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class GuardianPostsRepositorySavedStateTest {

    @Test
    fun favoriteAfterToggle_canReadFavoritePosts() = runTest {
        val repository = createRepository()
        val post = loadSamplePost(repository)

        repository.toggleFavorite(post)

        val favorites = repository.observeFavoritePosts().first()

        assertEquals(listOf(post.id), favorites.map { it.id })
        assertEquals(post.paragraphs, favorites.single().paragraphs)
    }

    @Test
    fun recordPostReadTwice_keepsSingleEntryAndRefreshesOrder() = runTest {
        val repository = createRepository()
        val firstPost = loadSamplePost(repository)
        val secondPost = sampleGuardianArticle(
            id = "science/2026/oct/guardian-second-story",
            title = "Second Guardian update",
            trailText = "<p>Second summary.</p>",
            body = "<p>Second full story.</p>",
        ).toPostOrNull()!!

        repository.recordPostRead(firstPost)
        repository.recordPostRead(secondPost)
        repository.recordPostRead(firstPost)

        val history = repository.observeReadingHistory().first()

        assertEquals(listOf(firstPost.id, secondPost.id), history.map { it.id })
    }

    @Test
    fun missingKey_stillReturnsSavedSnapshotForDetail() = runTest {
        val localDataSource = GuardianInMemorySavedPostsLocalDataSource()
        val repository = GuardianPostsRepository(
            service = SavedStateGuardianNewsService(),
            savedPostsLocalDataSource = localDataSource,
            apiKey = "",
        )
        val post = sampleGuardianArticle().toPostOrNull()!!
        localDataSource.upsertFavorite(post)

        val result = repository.getPost(post.id)

        assertTrue(result is Result.Success)
        assertEquals(post.id, (result as Result.Success).data.id)
    }

    private fun createRepository(): GuardianPostsRepository =
        GuardianPostsRepository(
            service = SavedStateGuardianNewsService(),
            savedPostsLocalDataSource = GuardianInMemorySavedPostsLocalDataSource(),
            apiKey = "demo-key",
        )

    private suspend fun loadSamplePost(repository: GuardianPostsRepository): Post {
        repository.getPostsFeed()
        return sampleGuardianArticle().toPostOrNull()!!
    }
}

private class SavedStateGuardianNewsService : GuardianNewsService {
    override suspend fun getLatestArticles(
        section: String?,
        orderBy: String,
        page: Int,
        pageSize: Int,
        showFields: String,
        apiKey: String,
    ): Response<GuardianSearchResponseDto> = Response.success(
        GuardianSearchResponseDto(
            response = GuardianSearchResultDto(
                status = "ok",
                total = 1,
                results = listOf(sampleGuardianArticle()),
            ),
        ),
    )

    override suspend fun getArticle(
        contentId: String,
        showFields: String,
        apiKey: String,
    ): Response<GuardianItemResponseDto> = Response.success(
        GuardianItemResponseDto(
            response = GuardianItemResultDto(
                status = "ok",
                total = 1,
                content = sampleGuardianArticle(id = contentId),
            ),
        ),
    )
}
