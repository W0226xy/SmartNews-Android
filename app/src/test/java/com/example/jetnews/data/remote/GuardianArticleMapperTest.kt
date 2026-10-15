package com.example.jetnews.data.remote

import com.example.jetnews.data.remote.model.GuardianArticleDto
import com.example.jetnews.data.remote.model.GuardianArticleFieldsDto
import com.example.jetnews.model.ParagraphType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianArticleMapperTest {

    @Test
    fun mapper_mapsGuardianBodyToFullPost() {
        val post = sampleGuardianArticle(
            body = """
                <p>First <strong>paragraph</strong>.</p>
                <h2>Orbit update</h2>
                <p>Second &amp; third paragraph.</p>
            """.trimIndent(),
        ).toPostOrNull()

        requireNotNull(post)
        assertEquals("world/2026/oct/guardian-story", post.id)
        assertEquals("Guardian launch update", post.title)
        assertEquals("Jane Smith", post.metadata.author.name)
        assertFalse(post.isSummaryContent)
        assertEquals(3, post.paragraphs.size)
        assertEquals(ParagraphType.Text, post.paragraphs[0].type)
        assertEquals("First paragraph.", post.paragraphs[0].text)
        assertEquals(ParagraphType.Header, post.paragraphs[1].type)
        assertEquals("Orbit update", post.paragraphs[1].text)
        assertEquals("Second & third paragraph.", post.paragraphs[2].text)
    }

    @Test
    fun htmlConversion_stripsTagsAndKeepsMultipleParagraphs() {
        val paragraphs = guardianHtmlToParagraphs(
            """
            <div>
              <p>Alpha <em>beta</em>.</p>
              <script>alert('x')</script>
              <blockquote>Quoted&nbsp;text</blockquote>
              <p>Gamma<br/>Delta</p>
            </div>
            """.trimIndent(),
        )

        assertEquals(listOf("Alpha beta.", "Quoted text", "Gamma\nDelta"), paragraphs.map { it.text })
        assertTrue(paragraphs.none { it.text.contains("<") || it.text.contains(">") })
    }

    @Test
    fun mapper_whenBodyMissing_fallsBackToSummary() {
        val post = sampleGuardianArticle(body = null, trailText = "<p>Only summary.</p>").toPostOrNull()

        requireNotNull(post)
        assertTrue(post.isSummaryContent)
        assertEquals("Only summary.", post.subtitle)
        assertEquals(1, post.paragraphs.size)
        assertEquals("Only summary.", post.paragraphs.single().text)
    }
}

internal fun sampleGuardianArticle(
    id: String = "world/2026/oct/guardian-story",
    title: String = "Guardian launch update",
    body: String? = "<p>Full story body.</p>",
    trailText: String? = "<p>Short summary.</p>",
    byline: String? = "<a href=\"/profile/jane\">Jane Smith</a>",
    thumbnail: String? = "https://media.guim.co.uk/image.jpg",
    wordcount: String? = "420",
    publicationDate: String? = "2026-10-15T01:02:03Z",
) = GuardianArticleDto(
    id = id,
    type = "article",
    sectionName = "World news",
    webPublicationDate = publicationDate,
    webTitle = title,
    webUrl = "https://www.theguardian.com/$id",
    fields = GuardianArticleFieldsDto(
        trailText = trailText,
        byline = byline,
        thumbnail = thumbnail,
        body = body,
        wordcount = wordcount,
    ),
)
