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

package com.example.jetnews.data.posts.impl

import com.example.jetnews.data.Result
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostsFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Implementation of PostsRepository that returns a hardcoded list of
 * posts with resources synchronously.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BlockingFakePostsRepository : PostsRepository {

    private val postsFeed = MutableStateFlow<PostsFeed?>(null)
    private val favoriteTimestamps = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val historyTimestamps = MutableStateFlow<Map<String, Long>>(emptyMap())
    private var nextTimestamp = 0L

    override suspend fun getPost(postId: String?): Result<Post> {
        return withContext(Dispatchers.IO) {
            val post = posts.allPosts.find { it.id == postId }
            if (post == null) {
                Result.Error(IllegalArgumentException("Unable to find post"))
            } else {
                Result.Success(post)
            }
        }
    }

    override suspend fun getPostsFeed(): Result<PostsFeed> {
        postsFeed.update { posts }
        return Result.Success(posts)
    }

    override fun observeFavoriteIds(): Flow<Set<String>> = favoriteTimestamps.map { it.keys }
    override fun observeFavoritePosts(): Flow<List<Post>> = favoriteTimestamps.map(::toOrderedPosts)
    override fun observeReadingHistory(): Flow<List<Post>> = historyTimestamps.map(::toOrderedPosts)
    override fun observePostsFeed(): Flow<PostsFeed?> = postsFeed

    override suspend fun toggleFavorite(postId: String) {
        favoriteTimestamps.update {
            if (postId in it) it - postId else it + (postId to nextStamp())
        }
    }

    override suspend fun toggleFavorite(post: Post) {
        toggleFavorite(post.id)
    }

    override suspend fun recordPostRead(post: Post) {
        historyTimestamps.update { it + (post.id to nextStamp()) }
    }

    private fun toOrderedPosts(timestamps: Map<String, Long>): List<Post> =
        timestamps.entries
            .sortedByDescending { it.value }
            .mapNotNull { (postId, _) -> posts.allPosts.find { it.id == postId } }

    private fun nextStamp(): Long = ++nextTimestamp
}
