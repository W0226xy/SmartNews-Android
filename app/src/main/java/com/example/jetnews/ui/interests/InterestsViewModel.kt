package com.example.jetnews.ui.interests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jetnews.data.preferences.NewsInterest
import com.example.jetnews.data.preferences.NewsPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InterestsUiState(
    val selected: Set<NewsInterest> = NewsInterest.default.toSet(),
)

class InterestsViewModel(
    private val preferencesRepository: NewsPreferencesRepository,
) : ViewModel() {
    private val _lastInterestWarning = MutableStateFlow(false)
    val lastInterestWarning: StateFlow<Boolean> = _lastInterestWarning.asStateFlow()
    val uiState: StateFlow<InterestsUiState> = preferencesRepository.observeSelectedInterests()
        .map { InterestsUiState(selected = it.toSet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InterestsUiState())

    fun toggleInterest(interest: NewsInterest) {
        val current = uiState.value.selected
        if (current.size == 1 && interest in current) {
            _lastInterestWarning.value = true
            return
        }
        _lastInterestWarning.value = false
        val updated = if (interest in current) current - interest else current + interest
        viewModelScope.launch {
            // DataStore 拒绝空集合，保证至少保留一个真实新闻兴趣。
            preferencesRepository.updateSelectedInterests(updated)
        }
    }

    companion object {
        fun provideFactory(repository: NewsPreferencesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    InterestsViewModel(repository) as T
            }
    }
}
