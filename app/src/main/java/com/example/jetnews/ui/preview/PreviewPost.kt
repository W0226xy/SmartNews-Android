package com.example.jetnews.ui.preview

import com.example.jetnews.R
import com.example.jetnews.model.Metadata
import com.example.jetnews.model.Paragraph
import com.example.jetnews.model.ParagraphType
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostAuthor
import com.example.jetnews.model.PostsFeed
import com.example.jetnews.model.Publication

fun previewPost(
    id: String = "preview-post",
    title: String = "Guardian 新闻预览",
): Post = Post(
    id = id,
    title = title,
    subtitle = "用于 Compose Preview 的最小展示数据。",
    url = "https://www.theguardian.com",
    publication = Publication("The Guardian", ""),
    metadata = Metadata(PostAuthor("The Guardian"), "2026-10-15", 3),
    paragraphs = listOf(Paragraph(ParagraphType.Text, "这是一段仅供界面预览使用的新闻正文。")),
    imageId = R.drawable.placeholder_4_3,
    imageThumbId = R.drawable.placeholder_1_1,
)

val previewPosts: List<Post> = List(12) { index ->
    previewPost(id = "preview-post-$index", title = "Guardian 新闻预览 ${index + 1}")
}

fun previewPostsFeed(): PostsFeed = PostsFeed(
    highlightedPost = previewPosts.first(),
    recommendedPosts = previewPosts.drop(1).take(4),
    popularPosts = previewPosts.drop(5).take(5),
    recentPosts = previewPosts.drop(10),
)
