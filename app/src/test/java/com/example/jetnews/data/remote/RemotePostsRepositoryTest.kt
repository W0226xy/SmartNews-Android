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

package com.example.jetnews.data.remote

import com.example.jetnews.data.Result
import com.example.jetnews.data.posts.impl.RemotePostsRepository
import com.example.jetnews.data.remote.model.SpaceflightArticleDto
import com.example.jetnews.data.remote.model.SpaceflightAuthorDto
import com.example.jetnews.data.remote.model.SpaceflightArticlesResponseDto
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RemotePostsRepositoryTest {

    @Test
    fun mapper_mapsNormalArticleToDomainPost() {
        val post = sampleArticle().toPostOrNull()

        assertNotNull(post)
        assertEquals("42", post?.id)
        assertEquals("Mars mission update", post?.title)
        assertEquals("https://example.com/image.jpg", post?.imageUrl)
        assertEquals("NASA", post?.metadata?.author?.name)
    }

    @Test
    fun mapper_usesFallbacksForEmptyOptionalFields() {
        val post = sampleArticle(
            authors = emptyList(),
            imageUrl = "",
            newsSite = "",
            summary = "",
            publishedAt = null,
        ).toPostOrNull()

        assertNotNull(post)
        assertEquals("Spaceflight News", post?.metadata?.author?.name)
        assertEquals("日期未知", post?.metadata?.date)
        assertEquals(null, post?.imageUrl)
        assertTrue(post?.paragraphs?.first()?.text?.isNotBlank() == true)
    }

    @Test
    fun getPostsFeed_whenServiceSucceeds_returnsPostsFeed() = runTest {
        val repository = RemotePostsRepository(
            FakeSpaceflightNewsService(
                listResponse = Response.success(
                    SpaceflightArticlesResponseDto(results = listOf(sampleArticle())),
                ),
            ),
        )

        val result = repository.getPostsFeed()

        assertTrue(result is Result.Success)
        assertEquals("42", (result as Result.Success).data.highlightedPost.id)
    }

    @Test
    fun getPostsFeed_whenServiceReturnsHttpError_returnsError() = runTest {
        val repository = RemotePostsRepository(
            FakeSpaceflightNewsService(
                listResponse = Response.error(
                    503,
                    "temporary failure".toResponseBody("application/json".toMediaType()),
                ),
            ),
        )

        val result = repository.getPostsFeed()

        assertTrue(result is Result.Error)
    }
}

private class FakeSpaceflightNewsService(
    private val listResponse: Response<SpaceflightArticlesResponseDto>,
    private val articleResponse: Response<SpaceflightArticleDto> = Response.success(sampleArticle()),
) : SpaceflightNewsService {
    override suspend fun getArticles(limit: Int): Response<SpaceflightArticlesResponseDto> = listResponse

    override suspend fun getArticle(id: Long): Response<SpaceflightArticleDto> = articleResponse
}

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
