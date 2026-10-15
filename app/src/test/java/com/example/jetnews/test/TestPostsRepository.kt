package com.example.jetnews.test

import com.example.jetnews.data.Result
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostsFeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class TestPostsRepository(
    private val feed: PostsFeed = testPostsFeed(),
) : PostsRepository {
    private val favorites = MutableStateFlow<Set<String>>(emptySet())
    private val postsFeed = MutableStateFlow<PostsFeed?>(null)

    override suspend fun getPost(postId: String?): Result<Post> =
        feed.allPosts.firstOrNull { it.id == postId }
            ?.let { Result.Success(it) }
            ?: Result.Error(IllegalArgumentException("Test post not found"))

    override suspend fun getPostsFeed(): Result<PostsFeed> {
        postsFeed.value = feed
        return Result.Success(feed)
    }

    override fun observeFavoriteIds(): Flow<Set<String>> = favorites
    override fun observeFavoritePosts(): Flow<List<Post>> = favorites.map { ids -> feed.allPosts.filter { it.id in ids } }
    override fun observeReadingHistory(): Flow<List<Post>> = MutableStateFlow(emptyList())
    override fun observePostsFeed(): Flow<PostsFeed?> = postsFeed

    override suspend fun toggleFavorite(postId: String) {
        favorites.value = favorites.value.toggle(postId)
    }

    override suspend fun toggleFavorite(post: Post) = toggleFavorite(post.id)
    override suspend fun recordPostRead(post: Post) = Unit

    private fun Set<String>.toggle(postId: String): Set<String> =
        if (postId in this) this - postId else this + postId
}
