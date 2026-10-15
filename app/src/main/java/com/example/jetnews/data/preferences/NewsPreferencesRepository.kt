package com.example.jetnews.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class RecommendationTarget(val interest: NewsInterest, val page: Int)

sealed interface UpdateInterestsResult {
    data object Saved : UpdateInterestsResult
    data object LastInterestRequired : UpdateInterestsResult
}

interface NewsPreferencesRepository {
    fun observeSelectedInterests(): Flow<List<NewsInterest>>
    suspend fun updateSelectedInterests(interests: Set<NewsInterest>): UpdateInterestsResult
    suspend fun nextRecommendationTarget(): RecommendationTarget
    suspend fun recentArticleIds(): Set<String>
    suspend fun recordShownArticleIds(articleIds: Collection<String>)
}

private val Context.newsPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "news_preferences",
)

class DataStoreNewsPreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) : NewsPreferencesRepository {
    constructor(context: Context) : this(context.newsPreferencesDataStore)

    override fun observeSelectedInterests(): Flow<List<NewsInterest>> = dataStore.data.map(::interestsFrom)

    override suspend fun updateSelectedInterests(interests: Set<NewsInterest>): UpdateInterestsResult {
        if (interests.isEmpty()) return UpdateInterestsResult.LastInterestRequired
        dataStore.edit { preferences ->
            preferences[SELECTED_INTERESTS] = interests.mapTo(linkedSetOf()) { it.sectionId }
            preferences[LAST_INTEREST_INDEX] = -1
            NewsInterest.entries.forEach { preferences[pageKey(it)] = 1 }
        }
        return UpdateInterestsResult.Saved
    }

    override suspend fun nextRecommendationTarget(): RecommendationTarget {
        var target: RecommendationTarget? = null
        dataStore.edit { preferences ->
            val interests = interestsFrom(preferences)
            val nextIndex = (preferences[LAST_INTEREST_INDEX] ?: -1).plus(1).mod(interests.size)
            val interest = interests[nextIndex]
            val page = preferences[pageKey(interest)] ?: 1
            preferences[LAST_INTEREST_INDEX] = nextIndex
            preferences[pageKey(interest)] = page + 1
            target = RecommendationTarget(interest, page)
        }
        return checkNotNull(target)
    }

    override suspend fun recentArticleIds(): Set<String> =
        dataStore.data.first()[RECENT_ARTICLE_IDS].orEmpty()

    override suspend fun recordShownArticleIds(articleIds: Collection<String>) {
        dataStore.edit { preferences ->
            val ordered = (articleIds.toList() + preferences[RECENT_ARTICLE_IDS].orEmpty())
                .distinct()
                .take(MAX_RECENT_ARTICLES)
            preferences[RECENT_ARTICLE_IDS] = ordered.toSet()
        }
    }

    private fun interestsFrom(preferences: Preferences): List<NewsInterest> {
        val saved = preferences[SELECTED_INTERESTS].orEmpty()
        val interests = NewsInterest.entries.filter { it.sectionId in saved }
        return interests.ifEmpty { NewsInterest.default }
    }

    private companion object {
        val SELECTED_INTERESTS = stringSetPreferencesKey("selected_interests")
        val LAST_INTEREST_INDEX = intPreferencesKey("last_interest_index")
        val RECENT_ARTICLE_IDS = stringSetPreferencesKey("recent_article_ids")
        const val MAX_RECENT_ARTICLES = 100
        fun pageKey(interest: NewsInterest) = intPreferencesKey("page_${interest.sectionId}")
    }
}
