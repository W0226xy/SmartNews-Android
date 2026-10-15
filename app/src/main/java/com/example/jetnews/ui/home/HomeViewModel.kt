/*
 * Copyright 2021 The Android Open Source Project
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

package com.example.jetnews.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jetnews.data.Result
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.data.preferences.NewsPreferencesRepository
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostsFeed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 首页渲染所需的最小状态集合。 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val postsFeed: PostsFeed,
        val favorites: Set<String>,
        val isRefreshing: Boolean = false,
        val refreshErrorMessage: String? = null,
        val recommendationLabel: String = "为你推荐",
        val recommendationMessage: String? = null,
        val searchInput: String = "",
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

/** 负责首页文章数据和用户事件，不持有 Compose 或 Android View 对象。 */
class HomeViewModel(
    private val postsRepository: PostsRepository,
    private val newsPreferencesRepository: NewsPreferencesRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val favorites = MutableStateFlow<Set<String>>(emptySet())

    init {
        observeFavorites()
        observePreferencesOrLoadInitial()
    }

    /** 保留现有列表并明确发起一次新的远程刷新。 */
    fun refresh() {
        val currentState = _uiState.value
        if (currentState !is HomeUiState.Success) {
            if (currentState is HomeUiState.Error) loadInitial()
            return
        }
        if (currentState.isRefreshing) return

        _uiState.value = currentState.copy(
            isRefreshing = true,
            refreshErrorMessage = null,
        )

        viewModelScope.launch {
            try {
                when (val result = postsRepository.getRecommendations()) {
                    is Result.Success -> {
                        _uiState.update { state ->
                            val success = state as? HomeUiState.Success ?: return@update state
                            success.copy(
                                postsFeed = result.data.postsFeed,
                                favorites = favorites.value,
                                isRefreshing = false,
                                refreshErrorMessage = null,
                                recommendationLabel = result.data.interest?.let { "为你推荐 · ${it.displayName}" }
                                    ?: success.recommendationLabel,
                                recommendationMessage = recommendationMessage(result.data, isRefresh = true),
                            )
                        }
                    }

                    is Result.Error -> {
                        finishRefreshWithError(result.exception.message)
                    }
                }
            } catch (exception: CancellationException) {
                _uiState.update { state ->
                    if (state is HomeUiState.Success) state.copy(isRefreshing = false) else state
                }
                throw exception
            } catch (exception: Exception) {
                finishRefreshWithError(exception.message)
            }
        }
    }

    private fun loadInitial() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                when (val result = postsRepository.getRecommendations()) {
                    is Result.Success -> {
                        _uiState.value = HomeUiState.Success(
                            postsFeed = result.data.postsFeed,
                            favorites = favorites.value,
                            recommendationLabel = result.data.interest?.let { "为你推荐 · ${it.displayName}" }
                                ?: "为你推荐",
                            recommendationMessage = recommendationMessage(result.data, isRefresh = false),
                        )
                    }

                    is Result.Error -> {
                        _uiState.value = HomeUiState.Error(
                            result.exception.message ?: DEFAULT_LOAD_ERROR,
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = HomeUiState.Error(
                    exception.message ?: DEFAULT_LOAD_ERROR,
                )
            }
        }
    }

    private fun observePreferencesOrLoadInitial() {
        val preferences = newsPreferencesRepository
        if (preferences == null) {
            loadInitial()
            return
        }
        viewModelScope.launch {
            preferences.observeSelectedInterests().distinctUntilChanged().collect {
                if (_uiState.value is HomeUiState.Success) refreshForChangedPreferences() else loadInitial()
            }
        }
    }

    /** 偏好变化时保留当前内容，同时获取新偏好的第一批推荐。 */
    private fun refreshForChangedPreferences() {
        val current = _uiState.value as? HomeUiState.Success ?: return
        if (current.isRefreshing) return
        _uiState.value = current.copy(isRefreshing = true, refreshErrorMessage = null)
        viewModelScope.launch {
            try {
                when (val result = postsRepository.getRecommendations()) {
                    is Result.Success -> _uiState.update { state ->
                        val success = state as? HomeUiState.Success ?: return@update state
                        success.copy(
                            postsFeed = result.data.postsFeed,
                            favorites = favorites.value,
                            isRefreshing = false,
                            recommendationLabel = result.data.interest?.let { "为你推荐 · ${it.displayName}" }
                                ?: success.recommendationLabel,
                            recommendationMessage = "已按你的兴趣更新推荐",
                        )
                    }
                    is Result.Error -> finishRefreshWithError(result.exception.message)
                }
            } catch (exception: CancellationException) {
                _uiState.update { state -> if (state is HomeUiState.Success) state.copy(isRefreshing = false) else state }
                throw exception
            } catch (exception: Exception) {
                finishRefreshWithError(exception.message)
            }
        }
    }

    private fun recommendationMessage(
        recommendation: com.example.jetnews.data.posts.RecommendationFeed,
        isRefresh: Boolean,
    ): String? {
        val interest = recommendation.interest ?: return null
        val prefix = if (isRefresh) "已为你换一批${interest.displayName}资讯" else "已为你推荐${interest.displayName}资讯"
        return if (recommendation.reusedArticleCount > 0) "$prefix；当前可用新资讯较少" else prefix
    }

    private fun finishRefreshWithError(message: String?) {
        _uiState.update { state ->
            if (state is HomeUiState.Success) {
                state.copy(
                    isRefreshing = false,
                    refreshErrorMessage = message ?: DEFAULT_LOAD_ERROR,
                )
            } else {
                state
            }
        }
    }

    fun toggleFavourite(post: Post) {
        viewModelScope.launch {
            postsRepository.toggleFavorite(post)
        }
    }

    fun onSearchInputChanged(searchInput: String) {
        _uiState.update { state ->
            if (state is HomeUiState.Success) state.copy(searchInput = searchInput) else state
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            postsRepository.observeFavoriteIds().collect { updatedFavorites ->
                favorites.value = updatedFavorites
                _uiState.update { state ->
                    if (state is HomeUiState.Success) state.copy(favorites = updatedFavorites) else state
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_LOAD_ERROR = "暂时无法更新资讯"

        /** 用已有 AppContainer 提供的仓库创建 ViewModel。 */
        fun provideFactory(
            postsRepository: PostsRepository,
            newsPreferencesRepository: NewsPreferencesRepository? = null,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(postsRepository, newsPreferencesRepository) as T
            }
    }
}
