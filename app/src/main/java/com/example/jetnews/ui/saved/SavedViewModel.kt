package com.example.jetnews.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.model.Post
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedUiState(
    val favoritePosts: List<Post> = emptyList(),
    val readingHistory: List<Post> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
)

class SavedViewModel(private val postsRepository: PostsRepository) : ViewModel() {
    val uiState: StateFlow<SavedUiState> = combine(
        postsRepository.observeFavoritePosts(),
        postsRepository.observeReadingHistory(),
        postsRepository.observeFavoriteIds(),
    ) { favoritePosts, readingHistory, favoriteIds ->
        SavedUiState(
            favoritePosts = favoritePosts,
            readingHistory = readingHistory,
            favoriteIds = favoriteIds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavedUiState(),
    )

    fun toggleFavorite(post: Post) {
        viewModelScope.launch {
            postsRepository.toggleFavorite(post)
        }
    }

    companion object {
        fun provideFactory(postsRepository: PostsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SavedViewModel(postsRepository) as T
            }
    }
}
