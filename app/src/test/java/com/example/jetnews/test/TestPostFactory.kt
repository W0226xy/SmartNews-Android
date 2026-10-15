package com.example.jetnews.test

import com.example.jetnews.R
import com.example.jetnews.model.Metadata
import com.example.jetnews.model.Paragraph
import com.example.jetnews.model.ParagraphType
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostAuthor
import com.example.jetnews.model.PostsFeed
import com.example.jetnews.model.Publication

object TestPostFactory {
    fun create(id: String = "test-post", title: String = "Test Guardian article"): Post = Post(
        id = id,
        title = title,
        subtitle = "Test summary",
        url = "https://www.theguardian.com/test",
        publication = Publication("The Guardian", ""),
        metadata = Metadata(PostAuthor("The Guardian"), "2026-10-15", 2),
        paragraphs = listOf(Paragraph(ParagraphType.Text, "Test article body")),
        imageId = R.drawable.placeholder_4_3,
        imageThumbId = R.drawable.placeholder_1_1,
    )
}

fun testPostsFeed(): PostsFeed {
    val posts = List(12) { index -> TestPostFactory.create("test-post-$index", "Test article ${index + 1}") }
    return PostsFeed(
        highlightedPost = posts.first(),
        recommendedPosts = posts.drop(1).take(4),
        popularPosts = posts.drop(5).take(5),
        recentPosts = posts.drop(10),
    )
}
