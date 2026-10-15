/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetnews.ui.home

import com.example.jetnews.data.Result
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.data.posts.impl.FakePostsRepository
import com.example.jetnews.data.posts.impl.post1
import com.example.jetnews.data.posts.impl.posts
import com.example.jetnews.data.preferences.NewsInterest
import com.example.jetnews.data.preferences.NewsPreferencesRepository
import com.example.jetnews.data.preferences.RecommendationTarget
import com.example.jetnews.data.preferences.UpdateInterestsResult
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostsFeed
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_withFakePostsRepository_emitsSuccess() = runTest {
        val viewModel = HomeViewModel(FakePostsRepository())

        val state = viewModel.uiState.filterIsInstance<HomeUiState.Success>().first()

        assertTrue(state.postsFeed.allPosts.isNotEmpty())
        assertEquals(emptySet<String>(), state.favorites)
    }

    @Test
    fun reload_whenRepositoryThrows_emitsError() = runTest {
        val viewModel = HomeViewModel(ThrowingPostsRepository())

        val state = viewModel.uiState.filterIsInstance<HomeUiState.Error>().first()

        assertEquals("测试加载失败", state.message)
    }

    @Test
    fun refresh_callsRepositoryAgain_andUpdatesFeedAfterSuccess() = runTest {
        val updatedFeed = posts.copy(highlightedPost = post1.copy(id = "updated-post"))
        val refreshResult = CompletableDeferred<Result<PostsFeed>>()
        val repository = ControlledRefreshPostsRepository(
            initialResult = Result.Success(posts),
            refreshResult = refreshResult,
        )
        val viewModel = HomeViewModel(repository)
        viewModel.uiState.filterIsInstance<HomeUiState.Success>().first()

        viewModel.refresh()

        val refreshingState = viewModel.uiState.value as HomeUiState.Success
        assertEquals(2, repository.getPostsFeedCalls)
        assertTrue(refreshingState.isRefreshing)
        assertEquals(posts, refreshingState.postsFeed)

        refreshResult.complete(Result.Success(updatedFeed))
        advanceUntilIdle()

        val refreshedState = viewModel.uiState.value as HomeUiState.Success
        assertEquals(updatedFeed, refreshedState.postsFeed)
        assertTrue(!refreshedState.isRefreshing)
        assertEquals(null, refreshedState.refreshErrorMessage)
    }

    @Test
    fun refresh_whenRepositoryFails_keepsOldFeedAndShowsError() = runTest {
        val refreshResult = CompletableDeferred<Result<PostsFeed>>()
        val repository = ControlledRefreshPostsRepository(
            initialResult = Result.Success(posts),
            refreshResult = refreshResult,
        )
        val viewModel = HomeViewModel(repository)
        viewModel.uiState.filterIsInstance<HomeUiState.Success>().first()

        viewModel.refresh()
        refreshResult.complete(Result.Error(IOException("刷新失败，请检查网络")))
        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Success
        assertEquals(2, repository.getPostsFeedCalls)
        assertEquals(posts, state.postsFeed)
        assertTrue(!state.isRefreshing)
        assertEquals("刷新失败，请检查网络", state.refreshErrorMessage)
    }

    @Test
    fun changingInterests_requestsRecommendationsAgain() = runTest {
        val refreshResult = CompletableDeferred<Result<PostsFeed>>()
        val repository = ControlledRefreshPostsRepository(Result.Success(posts), refreshResult)
        val preferences = MutableNewsPreferencesRepository()
        val viewModel = HomeViewModel(repository, preferences)
        viewModel.uiState.filterIsInstance<HomeUiState.Success>().first()

        preferences.setInterests(listOf(NewsInterest.SCIENCE))

        assertEquals(2, repository.getPostsFeedCalls)
        refreshResult.complete(Result.Success(posts))
        advanceUntilIdle()
    }
}

private class ControlledRefreshPostsRepository(
    private val initialResult: Result<PostsFeed>,
    private val refreshResult: CompletableDeferred<Result<PostsFeed>>,
) : PostsRepository {
    var getPostsFeedCalls: Int = 0
        private set

    override suspend fun getPost(postId: String?): Result<Post> =
        Result.Error(UnsupportedOperationException())

    override suspend fun getPostsFeed(): Result<PostsFeed> {
        getPostsFeedCalls += 1
        return if (getPostsFeedCalls == 1) initialResult else refreshResult.await()
    }

    override fun observeFavoriteIds(): Flow<Set<String>> = flowOf(emptySet())

    override fun observeFavoritePosts(): Flow<List<Post>> = flowOf(emptyList())

    override fun observeReadingHistory(): Flow<List<Post>> = flowOf(emptyList())

    override fun observePostsFeed(): Flow<PostsFeed?> = flowOf(null)

    override suspend fun toggleFavorite(postId: String) = Unit

    override suspend fun toggleFavorite(post: Post) = Unit

    override suspend fun recordPostRead(post: Post) = Unit
}

private class ThrowingPostsRepository : PostsRepository {
    override suspend fun getPost(postId: String?): Result<Post> =
        throw UnsupportedOperationException()

    override suspend fun getPostsFeed(): Result<PostsFeed> =
        throw IllegalStateException("测试加载失败")

    override fun observeFavoriteIds(): Flow<Set<String>> = flowOf(emptySet())

    override fun observeFavoritePosts(): Flow<List<Post>> = flowOf(emptyList())

    override fun observeReadingHistory(): Flow<List<Post>> = flowOf(emptyList())

    override fun observePostsFeed(): Flow<PostsFeed?> = flowOf(null)

    override suspend fun toggleFavorite(postId: String) = Unit

    override suspend fun toggleFavorite(post: Post) = Unit

    override suspend fun recordPostRead(post: Post) = Unit
}

private class MutableNewsPreferencesRepository : NewsPreferencesRepository {
    private val interests = MutableStateFlow(NewsInterest.default)

    override fun observeSelectedInterests(): Flow<List<NewsInterest>> = interests

    fun setInterests(value: List<NewsInterest>) {
        interests.value = value
    }

    override suspend fun updateSelectedInterests(interests: Set<NewsInterest>): UpdateInterestsResult =
        UpdateInterestsResult.Saved

    override suspend fun nextRecommendationTarget(): RecommendationTarget =
        RecommendationTarget(interests.value.first(), 1)

    override suspend fun recentArticleIds(): Set<String> = emptySet()

    override suspend fun recordShownArticleIds(articleIds: Collection<String>) = Unit
}
