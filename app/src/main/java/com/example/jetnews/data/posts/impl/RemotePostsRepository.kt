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
import com.example.jetnews.data.local.SavedPostsLocalDataSource
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.data.remote.SpaceflightNewsService
import com.example.jetnews.data.remote.toPostOrNull
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostsFeed
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** 正式运行时的网络文章仓库，本地收藏和阅读历史交给 Room 持久化。 */
class RemotePostsRepository(
    private val service: SpaceflightNewsService,
    private val savedPostsLocalDataSource: SavedPostsLocalDataSource,
) : PostsRepository {

    private val postsFeed = MutableStateFlow<PostsFeed?>(null)
    private val cachedPosts = MutableStateFlow<Map<String, Post>>(emptyMap())

    override suspend fun getPostsFeed(): Result<PostsFeed> = withContext(Dispatchers.IO) {
        try {
            val response = service.getArticles(limit = ARTICLE_LIMIT)
            if (!response.isSuccessful) return@withContext httpError(response.code())

            val posts = response.body()?.results.orEmpty().mapNotNull { it.toPostOrNull() }
            if (posts.isEmpty()) return@withContext Result.Error(IOException("暂无可显示的资讯"))

            val feed = posts.toPostsFeed()
            cachePosts(posts)
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
            if (!response.isSuccessful) {
                return@withContext savedPostOrError(postId, response.code())
            }

            val post = response.body()?.toPostOrNull()
                ?: return@withContext savedPostOrError(postId)
            cachePost(post)
            Result.Success(post)
        } catch (exception: Exception) {
            logger.log(Level.WARNING, "加载文章详情失败", exception)
            savedPostOrError(postId)
        }
    }

    override fun observeFavoriteIds(): Flow<Set<String>> = savedPostsLocalDataSource.observeFavoriteIds()

    override fun observeFavoritePosts(): Flow<List<Post>> = savedPostsLocalDataSource.observeFavoritePosts()

    override fun observeReadingHistory(): Flow<List<Post>> = savedPostsLocalDataSource.observeReadingHistory()

    override fun observePostsFeed(): Flow<PostsFeed?> = postsFeed

    override suspend fun toggleFavorite(postId: String) {
        withContext(Dispatchers.IO) {
            toggleFavoriteInternal(postId)
        }
    }

    override suspend fun toggleFavorite(post: Post) {
        withContext(Dispatchers.IO) {
            cachePost(post)
            toggleFavoriteInternal(post.id)
        }
    }

    override suspend fun recordPostRead(post: Post) {
        withContext(Dispatchers.IO) {
            cachePost(post)
            savedPostsLocalDataSource.recordPostRead(post)
        }
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

    private suspend fun savedPostOrError(postId: String, responseCode: Int? = null): Result<Post> {
        // 断网或服务异常时仍可用 Room 中的文章快照打开已保存详情。
        savedPostsLocalDataSource.findSavedPost(postId)?.let { return Result.Success(it) }
        return if (responseCode != null) {
            httpError(responseCode)
        } else {
            Result.Error(IOException("暂时无法加载文章"))
        }
    }

    private suspend fun toggleFavoriteInternal(postId: String) {
        if (savedPostsLocalDataSource.isFavorite(postId)) {
            savedPostsLocalDataSource.removeFavorite(postId)
            return
        }

        val cachedPost = cachedPosts.value[postId]
            ?: throw IllegalStateException("文章尚未加载，无法收藏")
        savedPostsLocalDataSource.upsertFavorite(cachedPost)
    }

    private fun cachePosts(posts: List<Post>) {
        cachedPosts.update { previous ->
            buildMap {
                putAll(previous)
                posts.forEach { put(it.id, it) }
            }
        }
    }

    private fun cachePost(post: Post) {
        cachedPosts.update { previous -> previous + (post.id to post) }
    }

    private companion object {
        const val ARTICLE_LIMIT = 20
        val logger: Logger = Logger.getLogger(RemotePostsRepository::class.java.name)
    }
}
