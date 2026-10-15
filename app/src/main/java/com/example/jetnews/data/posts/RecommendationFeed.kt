package com.example.jetnews.data.posts

import com.example.jetnews.data.preferences.NewsInterest
import com.example.jetnews.model.PostsFeed

/** 一次真实 Guardian 推荐请求的结果及其请求上下文。 */
data class RecommendationFeed(
    val postsFeed: PostsFeed,
    val interest: NewsInterest? = null,
    val page: Int? = null,
    val reusedArticleCount: Int = 0,
)
