package com.example.jetnews.data.local

import com.example.jetnews.R
import com.example.jetnews.model.Metadata
import com.example.jetnews.model.Paragraph
import com.example.jetnews.model.ParagraphType
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostAuthor
import com.example.jetnews.model.Publication
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DEFAULT_SUMMARY = "请在原网站阅读完整报道。"
private val savedPostJson = Json { ignoreUnknownKeys = true }

private fun Post.savedSourceName(): String =
    publication?.name?.takeIf { it.isNotBlank() }
        ?: metadata.author.name.ifBlank { "Spaceflight News" }

@Serializable
private data class SavedParagraphSnapshot(
    val type: String,
    val text: String,
)

// 统一把远程文章裁成可离线展示的最小快照，避免 UI 直接依赖 Room Entity。
fun Post.toFavoriteEntity(favoritedAt: Long): FavoritePostEntity = FavoritePostEntity(
    id = id,
    title = title,
    summary = subtitle.orEmpty(),
    imageUrl = imageUrl,
    sourceName = savedSourceName(),
    authorName = metadata.author.name,
    publishedDate = metadata.date,
    url = url,
    readTimeMinutes = metadata.readTimeMinutes,
    paragraphsJson = paragraphs.toSavedParagraphsJson(),
    isSummaryContent = isSummaryContent,
    favoritedAt = favoritedAt,
)

fun Post.toReadingHistoryEntity(lastReadAt: Long): ReadingHistoryEntity = ReadingHistoryEntity(
    id = id,
    title = title,
    summary = subtitle.orEmpty(),
    imageUrl = imageUrl,
    sourceName = savedSourceName(),
    authorName = metadata.author.name,
    publishedDate = metadata.date,
    url = url,
    readTimeMinutes = metadata.readTimeMinutes,
    paragraphsJson = paragraphs.toSavedParagraphsJson(),
    isSummaryContent = isSummaryContent,
    lastReadAt = lastReadAt,
)

fun FavoritePostEntity.toPost(): Post = savedPost(
    id = id,
    title = title,
    summary = summary,
    imageUrl = imageUrl,
    sourceName = sourceName,
    authorName = authorName,
    publishedDate = publishedDate,
    url = url,
    readTimeMinutes = readTimeMinutes,
    paragraphsJson = paragraphsJson,
    isSummaryContent = isSummaryContent,
)

fun ReadingHistoryEntity.toPost(): Post = savedPost(
    id = id,
    title = title,
    summary = summary,
    imageUrl = imageUrl,
    sourceName = sourceName,
    authorName = authorName,
    publishedDate = publishedDate,
    url = url,
    readTimeMinutes = readTimeMinutes,
    paragraphsJson = paragraphsJson,
    isSummaryContent = isSummaryContent,
)

private fun savedPost(
    id: String,
    title: String,
    summary: String,
    imageUrl: String?,
    sourceName: String,
    authorName: String,
    publishedDate: String,
    url: String,
    readTimeMinutes: Int,
    paragraphsJson: String,
    isSummaryContent: Boolean,
): Post {
    val safeSummary = summary.ifBlank { DEFAULT_SUMMARY }
    val paragraphs = paragraphsJson.toSavedParagraphs().ifEmpty {
        listOf(
            Paragraph(
                type = ParagraphType.Text,
                text = safeSummary,
            ),
        )
    }
    return Post(
        id = id,
        title = title,
        subtitle = safeSummary,
        url = url,
        imageUrl = imageUrl,
        publication = Publication(sourceName, ""),
        metadata = Metadata(
            author = PostAuthor(authorName.ifBlank { sourceName }),
            date = publishedDate,
            readTimeMinutes = readTimeMinutes.coerceAtLeast(1),
        ),
        paragraphs = paragraphs,
        isSummaryContent = isSummaryContent,
        imageId = R.drawable.placeholder_4_3,
        imageThumbId = R.drawable.placeholder_1_1,
    )
}

private fun List<Paragraph>.toSavedParagraphsJson(): String = savedPostJson.encodeToString(
    map { paragraph ->
        SavedParagraphSnapshot(
            type = paragraph.type.name,
            text = paragraph.text,
        )
    },
)

private fun String.toSavedParagraphs(): List<Paragraph> = runCatching {
    savedPostJson.decodeFromString<List<SavedParagraphSnapshot>>(this)
}.getOrDefault(emptyList()).mapNotNull { snapshot ->
    snapshot.text.trim().takeIf { it.isNotEmpty() }?.let { text ->
        Paragraph(
            type = runCatching { ParagraphType.valueOf(snapshot.type) }.getOrDefault(ParagraphType.Text),
            text = text,
        )
    }
}
