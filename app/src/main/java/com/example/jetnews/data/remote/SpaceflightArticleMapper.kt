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
import com.example.jetnews.data.remote.model.SpaceflightArticleDto
import com.example.jetnews.model.Metadata
import com.example.jetnews.model.Paragraph
import com.example.jetnews.model.ParagraphType
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostAuthor
import com.example.jetnews.model.Publication

/** 将网络 DTO 转为 UI 只依赖的领域模型。 */
fun SpaceflightArticleDto.toPostOrNull(): Post? {
    val safeTitle = title.trim()
    val safeUrl = url.takeIf(::isHttpUrl) ?: return null
    if (safeTitle.isEmpty()) return null

    val safeSummary = summary?.trim().orEmpty()
    val sourceName = newsSite?.trim().orEmpty().ifBlank { "Spaceflight News" }
    val authorName = authors.firstOrNull { it.name.isNotBlank() }?.name ?: sourceName

    return Post(
        id = id.toString(),
        title = safeTitle,
        subtitle = safeSummary.ifBlank { "请在原网站阅读完整报道。" },
        url = safeUrl,
        imageUrl = imageUrl?.takeIf(::isHttpUrl),
        publication = Publication(sourceName, ""),
        metadata = Metadata(
            author = PostAuthor(authorName),
            date = publishedAt?.substringBefore('T')?.ifBlank { "日期未知" } ?: "日期未知",
            readTimeMinutes = estimateReadTime(safeSummary),
        ),
        paragraphs = listOf(
            Paragraph(
                type = ParagraphType.Text,
                text = safeSummary.ifBlank { "该来源未提供文章摘要，请在原网站阅读。" },
            ),
        ),
        isSummaryContent = true,
        imageId = R.drawable.placeholder_4_3,
        imageThumbId = R.drawable.placeholder_1_1,
    )
}

private fun estimateReadTime(summary: String): Int =
    (summary.trim().split(Regex("\\s+")).size / 80).coerceIn(1, 5)

private fun isHttpUrl(value: String): Boolean =
    value.startsWith("https://") || value.startsWith("http://")
