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
import com.example.jetnews.model.PostsFeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 首页渲染所需的最小状态集合。 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val postsFeed: PostsFeed,
        val favorites: Set<String>,
        val isRefreshing: Boolean = false,
        val searchInput: String = "",
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

/** 负责首页文章数据和用户事件，不持有 Compose 或 Android View 对象。 */
class HomeViewModel(private val postsRepository: PostsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val favorites = MutableStateFlow<Set<String>>(emptySet())

    init {
        observeFavorites()
        reload()
    }

    /** 从仓库重新加载文章；阶段三替换远程实现后页面无需改动。 */
    fun reload() {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            _uiState.value = currentState.copy(isRefreshing = true)
        } else {
            _uiState.value = HomeUiState.Loading
        }

        viewModelScope.launch {
            try {
                when (val result = postsRepository.getPostsFeed()) {
                    is Result.Success -> {
                        _uiState.value = HomeUiState.Success(
                            postsFeed = result.data,
                            favorites = favorites.value,
                            searchInput = (currentState as? HomeUiState.Success)?.searchInput.orEmpty(),
                        )
                    }

                    is Result.Error -> {
                        _uiState.value = HomeUiState.Error(
                            result.exception.message ?: "暂时无法更新资讯",
                        )
                    }
                }
            } catch (exception: Exception) {
                _uiState.value = HomeUiState.Error(
                    exception.message ?: "暂时无法更新资讯",
                )
            }
        }
    }

    fun toggleFavourite(postId: String) {
        viewModelScope.launch {
            postsRepository.toggleFavorite(postId)
        }
    }

    fun onSearchInputChanged(searchInput: String) {
        _uiState.update { state ->
            if (state is HomeUiState.Success) state.copy(searchInput = searchInput) else state
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            postsRepository.observeFavorites().collect { updatedFavorites ->
                favorites.value = updatedFavorites
                _uiState.update { state ->
                    if (state is HomeUiState.Success) state.copy(favorites = updatedFavorites) else state
                }
            }
        }
    }

    companion object {
        /** 用已有 AppContainer 提供的仓库创建 ViewModel。 */
        fun provideFactory(postsRepository: PostsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(postsRepository) as T
            }
    }
}
