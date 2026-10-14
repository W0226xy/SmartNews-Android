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
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostsFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    fun reload_withFakePostsRepository_emitsSuccess() = runTest {
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
}

private class ThrowingPostsRepository : PostsRepository {
    override suspend fun getPost(postId: String?): Result<Post> =
        throw UnsupportedOperationException()

    override suspend fun getPostsFeed(): Result<PostsFeed> =
        throw IllegalStateException("测试加载失败")

    override fun observeFavorites(): Flow<Set<String>> = flowOf(emptySet())

    override fun observePostsFeed(): Flow<PostsFeed?> = flowOf(null)

    override suspend fun toggleFavorite(postId: String) = Unit
}
