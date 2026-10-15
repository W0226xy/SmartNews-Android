package com.example.jetnews.ui.post

import com.example.jetnews.model.Paragraph
import com.example.jetnews.model.Post
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** 翻译过程的细分阶段，供 ViewModel 将状态暴露给 UI。 */
enum class TranslationStage { DownloadingModel, Translating }

/** 可替换接口让 ViewModel 在 JVM 测试中不依赖 Android/ML Kit 运行时。 */
interface ArticleTranslator : AutoCloseable {
    suspend fun translate(post: Post, onStageChanged: (TranslationStage) -> Unit): Post
    override fun close() = Unit
}

/** 使用 Google ML Kit 设备端 English → Chinese 模型翻译一篇文章。 */
class MlKitArticleTranslator : ArticleTranslator {
    override suspend fun translate(post: Post, onStageChanged: (TranslationStage) -> Unit): Post {
        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.CHINESE)
                .build(),
        )
        try {
            onStageChanged(TranslationStage.DownloadingModel)
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitResult()
            onStageChanged(TranslationStage.Translating)
            return post.copy(
                title = translator.translateLongText(post.title),
                subtitle = post.subtitle?.let { translator.translateLongText(it) },
                // 保留段落类型和顺序；译文长度不同，原富文本下标不再可靠，故不复用 markups。
                paragraphs = post.paragraphs.map { paragraph ->
                    paragraph.copy(text = translator.translateLongText(paragraph.text), markups = emptyList())
                },
            )
        } finally {
            translator.close()
        }
    }

    /** ML Kit 单次请求有长度约束，按安全边界分段后顺序重新拼接，绝不截断正文。 */
    private suspend fun Translator.translateLongText(text: String): String {
        if (text.isBlank()) return text
        val translatedChunks = StringBuilder()
        for (chunk in splitForTranslation(text)) {
            translatedChunks.append(translate(chunk).awaitResult())
        }
        return translatedChunks.toString()
    }

    private fun splitForTranslation(text: String): List<String> {
        if (text.length <= MAX_CHARS_PER_REQUEST) return listOf(text)
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val limit = minOf(start + MAX_CHARS_PER_REQUEST, text.length)
            if (limit == text.length) {
                chunks += text.substring(start)
                break
            }
            val boundary = text.lastIndexOfAny(
                charArrayOf('.', '!', '?', ';', ',', ' ', '\n'),
                limit - 1,
            )
                .takeIf { it > start }
                ?.plus(1)
                ?: limit
            chunks += text.substring(start, boundary)
            start = boundary
        }
        return chunks
    }

    private companion object {
        const val MAX_CHARS_PER_REQUEST = 4_000
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) continuation.resume(task.result)
        else continuation.resumeWithException(task.exception ?: IllegalStateException("翻译任务失败"))
    }
}
