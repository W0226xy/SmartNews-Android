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
import com.example.jetnews.data.posts.RecommendationFeed
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.data.preferences.NewsPreferencesRepository
import com.example.jetnews.data.remote.GuardianNewsService
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

/** 正式运行时的 Guardian 网络文章仓库，本地收藏和阅读历史交给 Room 持久化。 */
class GuardianPostsRepository(
    private val service: GuardianNewsService,
    private val savedPostsLocalDataSource: SavedPostsLocalDataSource,
    private val apiKey: String,
    private val newsPreferencesRepository: NewsPreferencesRepository? = null,
) : PostsRepository {

    private val postsFeed = MutableStateFlow<PostsFeed?>(null)
    private val cachedPosts = MutableStateFlow<Map<String, Post>>(emptyMap())

    override suspend fun getPostsFeed(): Result<PostsFeed> = withContext(Dispatchers.IO) {
        val safeApiKey = apiKey.takeIf { it.isNotBlank() }
            ?: return@withContext Result.Error(IOException(MISSING_KEY_MESSAGE))

        try {
            logger.info("开始请求 Guardian 最新资讯")
            val response = service.getLatestArticles(apiKey = safeApiKey)
            if (!response.isSuccessful) return@withContext httpError(response.code())

            val body = response.body()?.response
                ?: return@withContext Result.Error(IOException("Guardian 返回了空响应"))
            if (body.status != "ok") return@withContext Result.Error(IOException("Guardian 资讯服务暂时不可用"))

            val posts = body.results.mapNotNull { it.toPostOrNull() }
            if (posts.isEmpty()) return@withContext Result.Error(IOException("Guardian 暂无可显示的资讯"))

            val feed = posts.toPostsFeed()
            cachePosts(posts)
            postsFeed.value = feed
            logger.info("Guardian 最新资讯请求成功，返回 ${posts.size} 篇")
            Result.Success(feed)
        } catch (exception: Exception) {
            logger.log(Level.WARNING, "加载 Guardian 资讯失败", exception)
            Result.Error(IOException("暂时无法加载 Guardian 资讯"))
        }
    }

    override suspend fun getRecommendations(): Result<RecommendationFeed> = withContext(Dispatchers.IO) {
        val preferences = newsPreferencesRepository
            ?: return@withContext when (val feed = getPostsFeed()) {
                is Result.Success -> Result.Success(RecommendationFeed(feed.data))
                is Result.Error -> feed
            }
        val safeApiKey = apiKey.takeIf { it.isNotBlank() }
            ?: return@withContext Result.Error(IOException(MISSING_KEY_MESSAGE))
        val target = preferences.nextRecommendationTarget()
        try {
            logger.info("请求 Guardian 推荐：section=${target.interest.sectionId}, page=${target.page}")
            val response = service.getLatestArticles(
                section = target.interest.sectionId,
                page = target.page,
                apiKey = safeApiKey,
            )
            if (!response.isSuccessful) return@withContext httpError(response.code())
            val body = response.body()?.response
                ?: return@withContext Result.Error(IOException("Guardian 返回了空响应"))
            if (body.status != "ok") return@withContext Result.Error(IOException("Guardian 资讯服务暂时不可用"))

            val posts = body.results.mapNotNull { it.toPostOrNull() }
            if (posts.isEmpty()) return@withContext Result.Error(IOException("该兴趣暂无可显示的资讯"))
            val recentIds = preferences.recentArticleIds()
            val freshPosts = posts.filterNot { it.id in recentIds }
            // API 结果不足时才回退到已展示文章，绝不本地随机重排来伪造新资讯。
            val displayedPosts = (freshPosts + posts.filter { it.id in recentIds }).take(20)
            val reusedCount = displayedPosts.count { it.id in recentIds }
            val feed = displayedPosts.toPostsFeed()
            cachePosts(displayedPosts)
            postsFeed.value = feed
            preferences.recordShownArticleIds(displayedPosts.map { it.id })
            logger.info(
                "Guardian 推荐请求成功：section=${target.interest.sectionId}, " +
                    "page=${target.page}, fresh=${displayedPosts.size - reusedCount}, reused=$reusedCount",
            )
            Result.Success(
                RecommendationFeed(
                    postsFeed = feed,
                    interest = target.interest,
                    page = target.page,
                    reusedArticleCount = reusedCount,
                ),
            )
        } catch (exception: Exception) {
            logger.log(Level.WARNING, "加载 Guardian 推荐失败", exception)
            Result.Error(IOException("暂时无法加载 Guardian 推荐"))
        }
    }

    override suspend fun getPost(postId: String?): Result<Post> = withContext(Dispatchers.IO) {
        val articleId = postId?.takeIf { it.isNotBlank() }
            ?: return@withContext Result.Error(IllegalArgumentException("文章不存在"))
        val safeApiKey = apiKey.takeIf { it.isNotBlank() }
            ?: return@withContext savedPostOrError(articleId, MISSING_KEY_MESSAGE)

        try {
            val response = service.getArticle(articleId, apiKey = safeApiKey)
            if (!response.isSuccessful) {
                return@withContext savedPostOrError(articleId, httpErrorMessage(response.code()))
            }

            val post = response.body()?.response?.content?.toPostOrNull()
                ?: return@withContext savedPostOrError(articleId, "该文章暂时无法显示")
            cachePost(post)
            Result.Success(post)
        } catch (exception: Exception) {
            logger.log(Level.WARNING, "加载 Guardian 文章详情失败", exception)
            savedPostOrError(articleId, "暂时无法加载 Guardian 文章")
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
        logger.warning("Guardian 接口返回 HTTP $code")
        return Result.Error(IOException(httpErrorMessage(code)))
    }

    private fun httpErrorMessage(code: Int): String =
        when (code) {
            401, 403 -> "Guardian API Key 无效或无权限访问。"
            else -> "Guardian 资讯服务暂时不可用"
        }

    private suspend fun savedPostOrError(postId: String, errorMessage: String): Result<Post> {
        savedPostsLocalDataSource.findSavedPost(postId)?.let { return Result.Success(it) }
        cachedPosts.value[postId]?.let { return Result.Success(it) }
        return Result.Error(IOException(errorMessage))
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
        const val MISSING_KEY_MESSAGE = "尚未配置 Guardian API Key，无法加载资讯。"
        val logger: Logger = Logger.getLogger(GuardianPostsRepository::class.java.name)
    }
}
