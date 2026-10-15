package com.example.jetnews.data.posts.impl

import com.example.jetnews.data.Result
import com.example.jetnews.data.remote.GuardianNewsService
import com.example.jetnews.data.remote.model.GuardianItemResponseDto
import com.example.jetnews.data.remote.model.GuardianItemResultDto
import com.example.jetnews.data.remote.model.GuardianSearchResponseDto
import com.example.jetnews.data.remote.model.GuardianSearchResultDto
import com.example.jetnews.data.remote.sampleGuardianArticle
import com.example.jetnews.data.remote.toPostOrNull
import com.example.jetnews.data.preferences.NewsInterest
import com.example.jetnews.data.preferences.NewsPreferencesRepository
import com.example.jetnews.data.preferences.RecommendationTarget
import com.example.jetnews.data.preferences.UpdateInterestsResult
import java.io.IOException
import java.util.ArrayDeque
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class GuardianPostsRepositoryTest {

    @Test
    fun getPostsFeed_whenServiceSucceeds_returnsPostsFeed() = runTest {
        val repository = GuardianPostsRepository(
            service = FakeGuardianNewsService(
                searchResponse = Response.success(
                    GuardianSearchResponseDto(
                        response = GuardianSearchResultDto(
                            status = "ok",
                            total = 1,
                            results = listOf(sampleGuardianArticle()),
                        ),
                    ),
                ),
            ),
            savedPostsLocalDataSource = GuardianEmptySavedPostsLocalDataSource,
            apiKey = "demo-key",
        )

        val result = repository.getPostsFeed()

        assertTrue(result is Result.Success)
        assertEquals(
            "world/2026/oct/guardian-story",
            (result as Result.Success).data.highlightedPost.id,
        )
    }

    @Test
    fun getPostsFeed_whenCalledTwice_executesTwoGuardianRequests() = runTest {
        val service = FakeGuardianNewsService()
        val repository = GuardianPostsRepository(
            service = service,
            savedPostsLocalDataSource = GuardianEmptySavedPostsLocalDataSource,
            apiKey = "demo-key",
        )

        repository.getPostsFeed()
        repository.getPostsFeed()

        assertEquals(2, service.latestArticlesCallCount)
    }

    @Test
    fun getPostsFeed_whenKeyMissing_returnsDisplayableError() = runTest {
        val repository = GuardianPostsRepository(
            service = FakeGuardianNewsService(),
            savedPostsLocalDataSource = GuardianEmptySavedPostsLocalDataSource,
            apiKey = "",
        )

        val result = repository.getPostsFeed()

        assertTrue(result is Result.Error)
        assertEquals(
            "尚未配置 Guardian API Key，无法加载资讯。",
            (result as Result.Error).exception.message,
        )
    }

    @Test
    fun recommendations_rotateGuardianSectionsAndUseNewPages() = runTest {
        val service = FakeGuardianNewsService()
        val repository = GuardianPostsRepository(
            service = service,
            savedPostsLocalDataSource = GuardianEmptySavedPostsLocalDataSource,
            apiKey = "demo-key",
            newsPreferencesRepository = TestNewsPreferencesRepository(
                listOf(NewsInterest.TECHNOLOGY, NewsInterest.SCIENCE),
            ),
        )

        repository.getRecommendations()
        repository.getRecommendations()
        repository.getRecommendations()

        assertEquals(
            listOf("technology" to 1, "science" to 1, "technology" to 2),
            service.recommendationRequests,
        )
    }

    @Test
    fun recommendations_prioritizeArticlesNotRecentlyShown() = runTest {
        val first = sampleGuardianArticle().copy(id = "technology/first")
        val second = sampleGuardianArticle().copy(id = "technology/second")
        val service = FakeGuardianNewsService(
            searchResponses = ArrayDeque(
                listOf(searchResponse(first), searchResponse(first, second)),
            ),
        )
        val preferences = TestNewsPreferencesRepository(listOf(NewsInterest.TECHNOLOGY))
        val repository = GuardianPostsRepository(
            service = service,
            savedPostsLocalDataSource = GuardianEmptySavedPostsLocalDataSource,
            apiKey = "demo-key",
            newsPreferencesRepository = preferences,
        )

        repository.getRecommendations()
        val secondResult = repository.getRecommendations()

        assertTrue(secondResult is Result.Success)
        assertEquals("technology/second", (secondResult as Result.Success).data.postsFeed.highlightedPost.id)
        assertEquals(1, secondResult.data.reusedArticleCount)
    }

    @Test
    fun getPostsFeed_whenServiceReturnsHttpError_returnsError() = runTest {
        val repository = GuardianPostsRepository(
            service = FakeGuardianNewsService(
                searchResponse = Response.error(
                    503,
                    "temporary failure".toResponseBody("application/json".toMediaType()),
                ),
            ),
            savedPostsLocalDataSource = GuardianEmptySavedPostsLocalDataSource,
            apiKey = "demo-key",
        )

        val result = repository.getPostsFeed()

        assertTrue(result is Result.Error)
        assertEquals("Guardian 资讯服务暂时不可用", (result as Result.Error).exception.message)
    }

    @Test
    fun getPost_whenServiceThrows_returnsSavedSnapshotFallback() = runTest {
        val localDataSource = GuardianInMemorySavedPostsLocalDataSource()
        val repository = GuardianPostsRepository(
            service = FakeGuardianNewsService(detailException = IOException("boom")),
            savedPostsLocalDataSource = localDataSource,
            apiKey = "demo-key",
        )
        val savedPost = sampleGuardianArticle().toPostOrNull()!!
        localDataSource.upsertFavorite(savedPost)

        val result = repository.getPost(savedPost.id)

        assertTrue(result is Result.Success)
        assertEquals(savedPost.id, (result as Result.Success).data.id)
    }
}

private class FakeGuardianNewsService(
    private val searchResponse: Response<GuardianSearchResponseDto> = Response.success(
        GuardianSearchResponseDto(
            response = GuardianSearchResultDto(
                status = "ok",
                total = 1,
                results = listOf(sampleGuardianArticle()),
            ),
        ),
    ),
    private val detailResponse: Response<GuardianItemResponseDto> = Response.success(
        GuardianItemResponseDto(
            response = GuardianItemResultDto(
                status = "ok",
                total = 1,
                content = sampleGuardianArticle(),
            ),
        ),
    ),
    private val searchException: Exception? = null,
    private val detailException: Exception? = null,
    private val searchResponses: ArrayDeque<Response<GuardianSearchResponseDto>>? = null,
) : GuardianNewsService {
    var latestArticlesCallCount: Int = 0
        private set
    val recommendationRequests = mutableListOf<Pair<String?, Int>>()

    override suspend fun getLatestArticles(
        section: String?,
        orderBy: String,
        page: Int,
        pageSize: Int,
        showFields: String,
        apiKey: String,
    ): Response<GuardianSearchResponseDto> {
        latestArticlesCallCount += 1
        recommendationRequests += section to page
        searchException?.let { throw it }
        return if (searchResponses.isNullOrEmpty()) searchResponse else searchResponses.removeFirst()
    }

    override suspend fun getArticle(
        contentId: String,
        showFields: String,
        apiKey: String,
    ): Response<GuardianItemResponseDto> {
        detailException?.let { throw it }
        return detailResponse
    }
}

private fun searchResponse(vararg articles: com.example.jetnews.data.remote.model.GuardianArticleDto) =
    Response.success(
        GuardianSearchResponseDto(
            response = GuardianSearchResultDto(status = "ok", total = articles.size, results = articles.toList()),
        ),
    )

private class TestNewsPreferencesRepository(
    initial: List<NewsInterest> = NewsInterest.default,
) : NewsPreferencesRepository {
    private val interests = MutableStateFlow(initial)
    private val pages = mutableMapOf<String, Int>()
    private var lastIndex = -1
    private val recent = linkedSetOf<String>()

    override fun observeSelectedInterests(): Flow<List<NewsInterest>> = interests

    override suspend fun updateSelectedInterests(interests: Set<NewsInterest>): UpdateInterestsResult {
        if (interests.isEmpty()) return UpdateInterestsResult.LastInterestRequired
        this.interests.value = NewsInterest.entries.filter { it in interests }
        pages.clear()
        lastIndex = -1
        return UpdateInterestsResult.Saved
    }

    override suspend fun nextRecommendationTarget(): RecommendationTarget {
        val selected = interests.value
        lastIndex = (lastIndex + 1) % selected.size
        val interest = selected[lastIndex]
        val page = pages.getOrDefault(interest.sectionId, 1)
        pages[interest.sectionId] = page + 1
        return RecommendationTarget(interest, page)
    }

    override suspend fun recentArticleIds(): Set<String> = recent

    override suspend fun recordShownArticleIds(articleIds: Collection<String>) {
        recent += articleIds
    }
}
