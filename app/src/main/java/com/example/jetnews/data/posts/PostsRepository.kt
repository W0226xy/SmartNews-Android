/*
 * Copyright 2020 The Android Open Source Project
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

package com.example.jetnews.data.posts

import com.example.jetnews.data.Result
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostsFeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Interface to the Posts data layer.
 */
// TODO：阶段二由 ViewModel 通过此接口接入远程数据源。
interface PostsRepository {

    /**
     * Get a specific JetNews post.
     */
    suspend fun getPost(postId: String?): Result<Post>

    /**
     * Get JetNews posts.
     */
    suspend fun getPostsFeed(): Result<PostsFeed>

    /**
     * 获取一批推荐资讯。默认实现保留旧数据源与测试仓库的兼容性；Guardian 实现会附带
     * 实际请求的 section、page 和去重降级信息。
     */
    suspend fun getRecommendations(): Result<RecommendationFeed> = when (val result = getPostsFeed()) {
        is Result.Success -> Result.Success(RecommendationFeed(result.data))
        is Result.Error -> result
    }

    /**
     * Observe the current favorite ids.
     */
    fun observeFavoriteIds(): Flow<Set<String>>

    /**
     * Keep compatibility with existing callers that still observe favorites.
     */
    fun observeFavorites(): Flow<Set<String>> = observeFavoriteIds()

    /**
     * Observe the saved favorite post snapshots.
     */
    fun observeFavoritePosts(): Flow<List<Post>>

    /**
     * Observe the saved reading history snapshots.
     */
    fun observeReadingHistory(): Flow<List<Post>>

    /**
     * Observe the posts feed.
     */
    fun observePostsFeed(): Flow<PostsFeed?>

    /**
     * Toggle a postId to be a favorite or not.
     */
    suspend fun toggleFavorite(postId: String)

    /**
     * Toggle a post using its current snapshot.
     */
    suspend fun toggleFavorite(post: Post)

    /**
     * Record that a post has been opened successfully.
     */
    suspend fun recordPostRead(post: Post)
}
