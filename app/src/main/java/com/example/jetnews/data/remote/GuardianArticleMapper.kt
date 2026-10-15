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

package com.example.jetnews.data.remote

import com.example.jetnews.R
import com.example.jetnews.data.remote.model.GuardianArticleDto
import com.example.jetnews.model.Metadata
import com.example.jetnews.model.Paragraph
import com.example.jetnews.model.ParagraphType
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostAuthor
import com.example.jetnews.model.Publication
import kotlin.math.ceil

private const val DEFAULT_GUARDIAN_SOURCE = "The Guardian"
private const val DEFAULT_SUMMARY_FALLBACK = "请在原网站阅读完整报道。"

private val scriptOrStyleRegex = Regex("<(script|style)\\b[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val htmlCommentRegex = Regex("<!--.*?-->", setOf(RegexOption.DOT_MATCHES_ALL))
private val blockRegex = Regex("<(p|h2|h3|li|blockquote)\\b[^>]*>(.*?)</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val breakRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val tagRegex = Regex("<[^>]+>")
private val entityRegex = Regex("&(#x?[0-9A-Fa-f]+|[A-Za-z][A-Za-z0-9]+);")
private val guardianNamedEntities = mapOf(
    "amp" to "&",
    "lt" to "<",
    "gt" to ">",
    "quot" to "\"",
    "apos" to "'",
    "nbsp" to " ",
    "ndash" to "–",
    "mdash" to "—",
    "hellip" to "…",
    "lsquo" to "‘",
    "rsquo" to "’",
    "ldquo" to "“",
    "rdquo" to "”",
)

fun GuardianArticleDto.toPostOrNull(): Post? {
    val safeId = id.trim()
    val safeTitle = webTitle.trim()
    val safeUrl = webUrl.takeIf(::isHttpUrl) ?: return null
    if (safeId.isEmpty() || safeTitle.isEmpty() || (type != null && type != "article")) return null

    val plainTrailText = guardianHtmlToPlainText(fields?.trailText)
    val bodyParagraphs = guardianHtmlToParagraphs(fields?.body)
    val isSummaryContent = bodyParagraphs.isEmpty()
    val contentParagraphs = if (bodyParagraphs.isNotEmpty()) {
        bodyParagraphs
    } else {
        listOf(
            Paragraph(
                type = ParagraphType.Text,
                text = plainTrailText.ifBlank { DEFAULT_SUMMARY_FALLBACK },
            ),
        )
    }

    val authorName = guardianHtmlToPlainText(fields?.byline).ifBlank {
        sectionName?.trim().orEmpty().ifBlank { DEFAULT_GUARDIAN_SOURCE }
    }

    return Post(
        id = safeId,
        title = safeTitle,
        subtitle = plainTrailText.takeIf { it.isNotBlank() },
        url = safeUrl,
        imageUrl = fields?.thumbnail?.takeIf(::isHttpUrl),
        publication = Publication(DEFAULT_GUARDIAN_SOURCE, ""),
        metadata = Metadata(
            author = PostAuthor(authorName),
            date = webPublicationDate?.substringBefore('T')?.ifBlank { "日期未知" } ?: "日期未知",
            readTimeMinutes = estimateReadTimeMinutes(
                wordCount = fields?.wordcount?.toIntOrNull(),
                paragraphs = contentParagraphs,
            ),
        ),
        paragraphs = contentParagraphs,
        isSummaryContent = isSummaryContent,
        imageId = R.drawable.placeholder_4_3,
        imageThumbId = R.drawable.placeholder_1_1,
    )
}

internal fun guardianHtmlToParagraphs(html: String?): List<Paragraph> {
    val sanitized = html
        ?.replace(scriptOrStyleRegex, "")
        ?.replace(htmlCommentRegex, "")
        ?.trim()
        .orEmpty()
    if (sanitized.isBlank()) return emptyList()

    val paragraphs = blockRegex.findAll(sanitized).mapNotNull { match ->
        val tag = match.groupValues[1].lowercase()
        val text = guardianHtmlToPlainText(match.groupValues[2])
        text.takeIf { it.isNotBlank() }?.let {
            Paragraph(
                type = when (tag) {
                    "h2" -> ParagraphType.Header
                    "h3" -> ParagraphType.Subhead
                    "li" -> ParagraphType.Bullet
                    "blockquote" -> ParagraphType.Quote
                    else -> ParagraphType.Text
                },
                text = it,
            )
        }
    }.toList()

    if (paragraphs.isNotEmpty()) return paragraphs

    return guardianHtmlToPlainText(sanitized)
        .split(Regex("\\n{2,}"))
        .mapNotNull { block ->
            block.trim().takeIf { it.isNotEmpty() }?.let { text ->
                Paragraph(type = ParagraphType.Text, text = text)
            }
        }
}

internal fun guardianHtmlToPlainText(html: String?): String {
    if (html.isNullOrBlank()) return ""

    return decodeHtmlEntities(
        html.replace(scriptOrStyleRegex, "")
            .replace(htmlCommentRegex, "")
            .replace(breakRegex, "\n")
            .replace(tagRegex, " ")
            .replace('\u00A0', ' '),
    )
        .lines()
        .joinToString("\n") { line ->
            line.replace(Regex("\\s+"), " ").trim()
        }
        .replace(Regex("\\s+([.,;:!?])"), "$1")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

private fun estimateReadTimeMinutes(wordCount: Int?, paragraphs: List<Paragraph>): Int {
    val words = wordCount ?: paragraphs.sumOf { paragraph ->
        paragraph.text.split(Regex("\\s+")).count { it.isNotBlank() }
    }
    return ceil(words.coerceAtLeast(1) / 200.0).toInt().coerceAtLeast(1)
}

private fun decodeHtmlEntities(text: String): String = entityRegex.replace(text) { match ->
    val entity = match.groupValues[1]
    when {
        entity.startsWith("#x", ignoreCase = true) -> {
            entity.substring(2).toIntOrNull(16)?.let(::codePointToString) ?: match.value
        }

        entity.startsWith("#") -> {
            entity.substring(1).toIntOrNull()?.let(::codePointToString) ?: match.value
        }

        else -> guardianNamedEntities[entity.lowercase()] ?: match.value
    }
}

private fun codePointToString(codePoint: Int): String =
    runCatching { String(Character.toChars(codePoint)) }.getOrDefault("")

private fun isHttpUrl(value: String): Boolean =
    value.startsWith("https://") || value.startsWith("http://")
