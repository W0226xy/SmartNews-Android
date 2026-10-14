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

package com.example.jetnews.data.posts.impl

import com.example.jetnews.data.Result
import com.example.jetnews.data.remote.SpaceflightNewsService
import com.example.jetnews.data.remote.toPostOrNull
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostsFeed
import com.example.jetnews.utils.addOrRemove
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** 正式运行时的网络文章仓库，收藏仅保存在内存中。 */
class RemotePostsRepository(
    private val service: SpaceflightNewsService,
) : PostsRepository {

    private val favorites = MutableStateFlow<Set<String>>(emptySet())
    private val postsFeed = MutableStateFlow<PostsFeed?>(null)

    override suspend fun getPostsFeed(): Result<PostsFeed> = withContext(Dispatchers.IO) {
        try {
            val response = service.getArticles(limit = ARTICLE_LIMIT)
            if (!response.isSuccessful) return@withContext httpError(response.code())

            val posts = response.body()?.results.orEmpty().mapNotNull { it.toPostOrNull() }
            if (posts.isEmpty()) return@withContext Result.Error(IOException("暂无可显示的资讯"))

            val feed = posts.toPostsFeed()
            postsFeed.value = feed
            Result.Success(feed)
        } catch (exception: Exception) {
            logger.log(Level.WARNING, "加载资讯失败", exception)
            Result.Error(IOException("暂时无法加载资讯"))
        }
    }

    override suspend fun getPost(postId: String?): Result<Post> = withContext(Dispatchers.IO) {
        val articleId = postId?.toLongOrNull()
            ?: return@withContext Result.Error(IllegalArgumentException("文章不存在"))

        try {
            val response = service.getArticle(articleId)
            if (!response.isSuccessful) return@withContext httpError(response.code())

            val post = response.body()?.toPostOrNull()
                ?: return@withContext Result.Error(IOException("文章内容不可用"))
            Result.Success(post)
        } catch (exception: Exception) {
            logger.log(Level.WARNING, "加载文章详情失败", exception)
            Result.Error(IOException("暂时无法加载文章"))
        }
    }

    override fun observeFavorites(): Flow<Set<String>> = favorites

    override fun observePostsFeed(): Flow<PostsFeed?> = postsFeed

    override suspend fun toggleFavorite(postId: String) {
        // 阶段四接入 Room 后，此内存收藏会在应用重启后持久化。
        favorites.update { it.addOrRemove(postId) }
    }

    private fun List<Post>.toPostsFeed(): PostsFeed = PostsFeed(
        highlightedPost = first(),
        recommendedPosts = drop(1).take(4),
        popularPosts = drop(5).take(5),
        recentPosts = drop(10),
    )

    private fun httpError(code: Int): Result.Error {
        logger.warning("资讯接口返回 HTTP $code")
        return Result.Error(IOException("资讯服务暂时不可用"))
    }

    private companion object {
        const val ARTICLE_LIMIT = 20
        val logger: Logger = Logger.getLogger(RemotePostsRepository::class.java.name)
    }
}
