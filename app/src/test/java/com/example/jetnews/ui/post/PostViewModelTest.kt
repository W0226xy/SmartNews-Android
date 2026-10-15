package com.example.jetnews.ui.post

import com.example.jetnews.data.Result
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.data.posts.impl.post3
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostsFeed
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun translate_successExposesTranslatedPost_andToggleDoesNotReloadDetails() = runTest {
        val repository = CountingPostRepository(post3)
        val translated = post3.copy(title = "中文标题", paragraphs = post3.paragraphs.map { it.copy(text = "中文正文") })
        val translator = FakeArticleTranslator(Result.Success(translated))
        val viewModel = PostViewModel(repository, post3.id, translator)
        advanceUntilIdle()

        viewModel.translateOrToggleOriginal()
        advanceUntilIdle()

        val success = viewModel.uiState.value.translationState as ArticleTranslationState.Success
        assertEquals("中文标题", success.translatedPost.title)
        assertTrue(viewModel.uiState.value.showingTranslation)
        assertEquals(1, repository.getPostCalls)
        assertEquals(1, translator.calls)

        viewModel.translateOrToggleOriginal()
        viewModel.translateOrToggleOriginal()
        assertTrue(viewModel.uiState.value.showingTranslation)
        assertEquals(1, repository.getPostCalls)
        assertEquals(1, translator.calls)
    }

    @Test
    fun translate_failureExposesError_andKeepsOriginalVisible() = runTest {
        val viewModel = PostViewModel(
            CountingPostRepository(post3),
            post3.id,
            FakeArticleTranslator(Result.Error(IllegalStateException("无法下载翻译模型"))),
        )
        advanceUntilIdle()

        viewModel.translateOrToggleOriginal()
        advanceUntilIdle()

        val error = viewModel.uiState.value.translationState as ArticleTranslationState.Error
        assertEquals("翻译失败，请检查网络连接后重试", error.message)
        assertFalse(viewModel.uiState.value.showingTranslation)
        assertEquals(post3, viewModel.uiState.value.post)
    }

    @Test
    fun repeatedTranslateClick_doesNotStartConcurrentWork() = runTest {
        val result = CompletableDeferred<Result<Post>>()
        val translator = FakeArticleTranslator(deferredResult = result)
        val viewModel = PostViewModel(CountingPostRepository(post3), post3.id, translator)
        advanceUntilIdle()

        viewModel.translateOrToggleOriginal()
        viewModel.translateOrToggleOriginal()

        assertEquals(1, translator.calls)
        assertTrue(viewModel.uiState.value.translationState == ArticleTranslationState.Translating)
        result.complete(Result.Success(post3.copy(title = "译文")))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.translationState is ArticleTranslationState.Success)
    }
}

private class FakeArticleTranslator(
    private val result: Result<Post>? = null,
    private val deferredResult: CompletableDeferred<Result<Post>>? = null,
) : ArticleTranslator {
    var calls = 0
        private set

    override suspend fun translate(post: Post, onStageChanged: (TranslationStage) -> Unit): Post {
        calls += 1
        onStageChanged(TranslationStage.DownloadingModel)
        onStageChanged(TranslationStage.Translating)
        return when (val response = deferredResult?.await() ?: checkNotNull(result)) {
            is Result.Success -> response.data
            is Result.Error -> throw response.exception
        }
    }
}

private class CountingPostRepository(private val post: Post) : PostsRepository {
    var getPostCalls = 0
        private set

    override suspend fun getPost(postId: String?): Result<Post> {
        getPostCalls += 1
        return Result.Success(post)
    }

    override suspend fun getPostsFeed(): Result<PostsFeed> = Result.Error(UnsupportedOperationException())
    override fun observeFavoriteIds(): Flow<Set<String>> = flowOf(emptySet())
    override fun observeFavoritePosts(): Flow<List<Post>> = flowOf(emptyList())
    override fun observeReadingHistory(): Flow<List<Post>> = flowOf(emptyList())
    override fun observePostsFeed(): Flow<PostsFeed?> = flowOf(null)
    override suspend fun toggleFavorite(postId: String) = Unit
    override suspend fun toggleFavorite(post: Post) = Unit
    override suspend fun recordPostRead(post: Post) = Unit
}
