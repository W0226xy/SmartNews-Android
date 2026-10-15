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

package com.example.jetnews.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.data.successOr
import com.example.jetnews.model.Post
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ArticleTranslationState {
    data object Idle : ArticleTranslationState
    data object DownloadingModel : ArticleTranslationState
    data object Translating : ArticleTranslationState
    data class Success(val translatedPost: Post) : ArticleTranslationState
    data class Error(val message: String) : ArticleTranslationState
}

data class PostUiState(
    val post: Post? = null,
    val initialFirstVisibleItemIndex: Int = 0,
    val initialFirstVisibleItemScrollOffset: Int = 0,
    val isFavorite: Boolean = false,
    val loading: Boolean = false,
    val translationState: ArticleTranslationState = ArticleTranslationState.Idle,
    val showingTranslation: Boolean = false,
)

class PostViewModel(
    private val postsRepository: PostsRepository,
    private val postId: String,
    private val articleTranslator: ArticleTranslator = MlKitArticleTranslator(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(PostUiState(loading = true))
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()
    private var hasRecordedRead = false

    init {
        _uiState.update { it.copy(loading = true) }

        viewModelScope.launch {
            val post = postsRepository.getPost(postId).successOr(null)

            _uiState.update {
                it.copy(
                    post = post,
                    loading = false,
                )
            }
        }

        viewModelScope.launch {
            postsRepository.observeFavoriteIds().collect { favorites ->
                _uiState.update { it.copy(isFavorite = favorites.contains(postId)) }
            }
        }
    }

    fun toggleFavorite() {
        val post = requireNotNull(_uiState.value.post) { "文章尚未加载，无法收藏" }
        viewModelScope.launch { postsRepository.toggleFavorite(post) }
    }

    fun onPostShown(post: Post) {
        if (hasRecordedRead) return
        hasRecordedRead = true
        viewModelScope.launch { postsRepository.recordPostRead(post) }
    }

    fun onScroll(index: Int, offset: Int) {
        _uiState.update {
            it.copy(
                initialFirstVisibleItemIndex = index,
                initialFirstVisibleItemScrollOffset = offset,
            )
        }
    }

    /** 首次请求翻译；成功后只在内存中的原文和译文副本之间切换。 */
    fun translateOrToggleOriginal() {
        when (_uiState.value.translationState) {
            is ArticleTranslationState.Success -> {
                _uiState.update { it.copy(showingTranslation = !it.showingTranslation) }
            }

            ArticleTranslationState.DownloadingModel,
            ArticleTranslationState.Translating,
            -> return // 禁止重复点击并发创建 ML Kit Translator。

            ArticleTranslationState.Idle,
            is ArticleTranslationState.Error,
            -> translateCurrentPost()
        }
    }

    private fun translateCurrentPost() {
        val originalPost = _uiState.value.post ?: return
        _uiState.update {
            it.copy(
                translationState = ArticleTranslationState.DownloadingModel,
                showingTranslation = false,
            )
        }
        viewModelScope.launch {
            try {
                val translatedPost = articleTranslator.translate(originalPost) { stage ->
                    _uiState.update {
                        it.copy(
                            translationState = when (stage) {
                                TranslationStage.DownloadingModel -> ArticleTranslationState.DownloadingModel
                                TranslationStage.Translating -> ArticleTranslationState.Translating
                            },
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        translationState = ArticleTranslationState.Success(translatedPost),
                        showingTranslation = true,
                    )
                }
            } catch (exception: CancellationException) {
                _uiState.update {
                    it.copy(
                        translationState = ArticleTranslationState.Idle,
                        showingTranslation = false,
                    )
                }
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        translationState = ArticleTranslationState.Error(
                            "翻译失败，请检查网络连接后重试",
                        ),
                        showingTranslation = false,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        articleTranslator.close()
        super.onCleared()
    }

    /**
     * Factory for PostViewModel that takes PostsRepository as a dependency
     */
    companion object {
        fun provideFactory(postsRepository: PostsRepository, postId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PostViewModel(postsRepository, postId) as T
                }
            }
    }
}
