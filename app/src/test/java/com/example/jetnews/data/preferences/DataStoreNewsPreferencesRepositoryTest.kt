package com.example.jetnews.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DataStoreNewsPreferencesRepositoryTest {
    @Test
    fun defaults_arePersistedAndLastInterestCannotBeRemoved() = runTest {
        val repository = repository()

        assertEquals(NewsInterest.default, repository.observeSelectedInterests().first())
        assertEquals(
            UpdateInterestsResult.Saved,
            repository.updateSelectedInterests(setOf(NewsInterest.SCIENCE)),
        )
        assertEquals(listOf(NewsInterest.SCIENCE), repository.observeSelectedInterests().first())
        assertEquals(
            UpdateInterestsResult.LastInterestRequired,
            repository.updateSelectedInterests(emptySet()),
        )
        assertEquals(listOf(NewsInterest.SCIENCE), repository.observeSelectedInterests().first())
    }

    @Test
    fun multipleInterests_rotateAndSingleInterestAdvancesPage() = runTest {
        val repository = repository()
        repository.updateSelectedInterests(
            setOf(NewsInterest.TECHNOLOGY, NewsInterest.SCIENCE, NewsInterest.WORLD),
        )

        assertEquals(RecommendationTarget(NewsInterest.TECHNOLOGY, 1), repository.nextRecommendationTarget())
        assertEquals(RecommendationTarget(NewsInterest.SCIENCE, 1), repository.nextRecommendationTarget())
        assertEquals(RecommendationTarget(NewsInterest.WORLD, 1), repository.nextRecommendationTarget())
        assertEquals(RecommendationTarget(NewsInterest.TECHNOLOGY, 2), repository.nextRecommendationTarget())
    }

    @Test
    fun recentlyShownIds_areRemembered() = runTest {
        val repository = repository()
        repository.recordShownArticleIds(listOf("a", "b", "a"))

        assertEquals(setOf("a", "b"), repository.recentArticleIds())
    }

    private fun repository(): DataStoreNewsPreferencesRepository {
        val file = File.createTempFile("news-preferences", ".preferences_pb")
        file.delete()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
            produceFile = { file },
        )
        return DataStoreNewsPreferencesRepository(store)
    }
}
