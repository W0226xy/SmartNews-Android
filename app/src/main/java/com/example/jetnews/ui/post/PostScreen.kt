/*
 * Copyright 2020 The Android Open Source Project
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

import android.content.Context
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.jetnews.R
import com.example.jetnews.data.Result
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.data.posts.impl.BlockingFakePostsRepository
import com.example.jetnews.data.posts.impl.post3
import com.example.jetnews.deeplink.util.DeepLinkPattern
import com.example.jetnews.model.Post
import com.example.jetnews.ui.home.HomeKey
import com.example.jetnews.ui.navigation.DeepLinkKey
import com.example.jetnews.ui.navigation.ListDetailScene
import com.example.jetnews.ui.theme.JetnewsTheme
import com.example.jetnews.ui.utils.BookmarkButton
import com.example.jetnews.ui.utils.FavoriteButton
import com.example.jetnews.ui.utils.ShareButton
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

@Serializable
data class PostKey(val postId: String) : DeepLinkKey {
    override val parent = HomeKey
}

val PostDeepLinkPattern = DeepLinkPattern(
    PostKey.serializer(),
    uriPattern = "https://developer.android.com/jetnews/posts/{postId}".toUri(),
)

fun EntryProviderScope<NavKey>.postEntry(postsRepository: PostsRepository, isExpandedScreen: () -> Boolean, onBack: () -> Unit) {
    entry<PostKey>(
        metadata = ListDetailScene.detail(),
    ) {
        val postViewModel: PostViewModel =
            viewModel(factory = PostViewModel.provideFactory(postsRepository, it.postId), key = it.postId)

        val uiState by postViewModel.uiState.collectAsStateWithLifecycle()

        PostScreen(
            uiState = uiState,
            isExpandedScreen = isExpandedScreen(),
            onBack = onBack,
            onToggleFavorite = postViewModel::toggleFavorite,
            onPostShown = postViewModel::onPostShown,
            onScroll = postViewModel::onScroll,
            onTranslateClick = postViewModel::translateOrToggleOriginal,
        )
    }
}

@OptIn(FlowPreview::class)
@Composable
fun PostScreen(
    uiState: PostUiState,
    isExpandedScreen: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPostShown: (Post) -> Unit,
    onScroll: (index: Int, offset: Int) -> Unit,
    onTranslateClick: () -> Unit,
) {
    if (uiState.loading) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.width(64.dp),
                )
            }
        }
    } else {
        val post = if (uiState.showingTranslation) {
            (uiState.translationState as? ArticleTranslationState.Success)?.translatedPost ?: uiState.post
        } else {
            uiState.post
        }
        if (post != null) {
            val lazyListState = rememberLazyListState(
                initialFirstVisibleItemIndex = uiState.initialFirstVisibleItemIndex,
                initialFirstVisibleItemScrollOffset = uiState.initialFirstVisibleItemScrollOffset,
            )

            LaunchedEffect(post.id) {
                // 只在详情真正显示成功后记一次历史，避免重组重复写入。
                onPostShown(post)
            }

            LaunchedEffect(lazyListState) {
                snapshotFlow {
                    Pair(
                        lazyListState.firstVisibleItemIndex,
                        lazyListState.firstVisibleItemScrollOffset,
                    )
                }
                    .debounce(50)
                    .collectLatest { (index, offset) ->
                        onScroll(index, offset)
                    }
            }

            PostScreen(
                post = post,
                isExpandedScreen = isExpandedScreen,
                onBack = onBack,
                isFavorite = uiState.isFavorite,
                onToggleFavorite = onToggleFavorite,
                lazyListState = lazyListState,
                translationState = uiState.translationState,
                showingTranslation = uiState.showingTranslation,
                onTranslateClick = onTranslateClick,
            )
        } else {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.post_not_found))
                }
            }
        }
    }
}

/**
 * Stateless Post Screen that displays a single post adapting the UI to different screen sizes.
 *
 * @param post (state) item to display
 * @param isExpandedScreen (state) whether the screen is expanded
 * @param onBack (event) request navigate back
 * @param isFavorite (state) is this item currently a favorite
 * @param onToggleFavorite (event) request that this post toggle its favorite state
 * @param modifier modifier for the root element
 * @param lazyListState (state) the [LazyListState] for the post content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    post: Post,
    isExpandedScreen: Boolean,
    onBack: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    translationState: ArticleTranslationState = ArticleTranslationState.Idle,
    showingTranslation: Boolean = false,
    onTranslateClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    var showUnimplementedActionDialog by rememberSaveable { mutableStateOf(false) }
    if (showUnimplementedActionDialog) {
        FunctionalityNotAvailablePopup { showUnimplementedActionDialog = false }
    }

    val context = LocalContext.current
    Box(modifier) {
        PostScreenContent(
            post = post,
            translationState = translationState,
            showingTranslation = showingTranslation,
            onTranslateClick = onTranslateClick,
            // Allow opening the Drawer if the screen is not expanded
            navigationIconContent = {
                if (!isExpandedScreen) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.cd_navigate_up),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            // Show the bottom bar if the screen is not expanded
            bottomBarContent = {
                if (!isExpandedScreen) {
                    BottomAppBar(
                        actions = {
                            FavoriteButton(onClick = { showUnimplementedActionDialog = true })
                            BookmarkButton(isBookmarked = isFavorite, onClick = onToggleFavorite)
                            ShareButton(onClick = { sharePost(post, context) })
                            TranslationAction(translationState, showingTranslation, onTranslateClick)
                        },
                    )
                }
            },
            lazyListState = lazyListState,
            showTopAppBar = !isExpandedScreen,
        )

        if (isExpandedScreen) {
            // Floating toolbar
            PostTopBar(
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onSharePost = { sharePost(post, context) },
                translationState = translationState,
                showingTranslation = showingTranslation,
                onTranslateClick = onTranslateClick,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.End),
            )
        }
    }
}

/**
 * Stateless Post Screen that displays a single post.
 *
 * @param post (state) item to display
 * @param navigationIconContent (UI) content to show for the navigation icon
 * @param bottomBarContent (UI) content to show for the bottom bar
 * @param lazyListState (state) the [LazyListState] for the post content
 * @param showTopAppBar (state) if the top app bar should be shown
 */
@ExperimentalMaterial3Api
@Composable
private fun PostScreenContent(
    post: Post,
    translationState: ArticleTranslationState,
    showingTranslation: Boolean,
    onTranslateClick: () -> Unit,
    navigationIconContent: @Composable () -> Unit = { },
    bottomBarContent: @Composable () -> Unit = { },
    lazyListState: LazyListState = rememberLazyListState(),
    showTopAppBar: Boolean = true,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
    Scaffold(
        topBar = {
            if (showTopAppBar) {
                TopAppBar(
                    title = post.publication?.name.orEmpty(),
                    navigationIconContent = navigationIconContent,
                    scrollBehavior = scrollBehavior,
                    actions = { TranslationAction(translationState, showingTranslation, onTranslateClick) },
                )
            }
        },
        bottomBar = bottomBarContent,
    ) { innerPadding ->
        PostContent(
            post = post,
            translationBanner = { PostTranslationBanner(translationState, showingTranslation, onTranslateClick) },
            contentPadding = innerPadding,
            modifier = if (showTopAppBar) {
                Modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            } else Modifier,
            state = lazyListState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    title: String,
    navigationIconContent: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        title = {
            Row {
                Image(
                    painter = painterResource(id = R.drawable.icon_post_background),
                    contentDescription = null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(36.dp),
                )
                Text(
                    text = stringResource(R.string.published_in, title),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        },
        navigationIcon = navigationIconContent,
        actions = { actions() },
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    )
}

/**
 * Top bar for a Post when displayed next to the Home feed
 *
 * @param isFavorite (state) is this item currently a favorite
 * @param onToggleFavorite (event) request that this post toggle its favorite state
 * @param onSharePost (event) request sharing the post
 * @param modifier modifier for the root element
 */
@Composable
private fun PostTopBar(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onSharePost: () -> Unit,
    translationState: ArticleTranslationState,
    showingTranslation: Boolean,
    onTranslateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(Dp.Hairline, MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)),
        modifier = modifier.padding(end = 16.dp),
    ) {
        Row(Modifier.padding(horizontal = 8.dp)) {
            FavoriteButton(onClick = { /* Functionality not available */ })
            BookmarkButton(isBookmarked = isFavorite, onClick = onToggleFavorite)
            ShareButton(onClick = onSharePost)
            TranslationAction(translationState, showingTranslation, onTranslateClick)
        }
    }
}

@Composable
private fun TranslationAction(
    translationState: ArticleTranslationState,
    showingTranslation: Boolean,
    onClick: () -> Unit,
) {
    val inProgress = translationState == ArticleTranslationState.DownloadingModel ||
        translationState == ArticleTranslationState.Translating
    val hasTranslation = translationState is ArticleTranslationState.Success
    if (hasTranslation) {
        TextButton(onClick = onClick) {
            Text(stringResource(if (showingTranslation) R.string.post_show_original else R.string.post_show_translation))
        }
    } else {
        IconButton(onClick = onClick, enabled = !inProgress) {
            if (inProgress) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = stringResource(R.string.post_translate_article),
                )
            }
        }
    }
}

@Composable
private fun PostTranslationBanner(
    translationState: ArticleTranslationState,
    showingTranslation: Boolean,
    onRetry: () -> Unit,
) {
    when (translationState) {
        ArticleTranslationState.Idle -> Unit
        ArticleTranslationState.DownloadingModel,
        ArticleTranslationState.Translating,
        -> Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(
                        if (translationState == ArticleTranslationState.DownloadingModel) {
                            R.string.post_downloading_translation_model
                        } else {
                            R.string.post_translating
                        },
                    ),
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        is ArticleTranslationState.Success -> if (showingTranslation) Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.post_translation_disclaimer),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is ArticleTranslationState.Error -> Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = translationState.message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onRetry) { Text(stringResource(R.string.post_translation_retry)) }
            }
        }
    }
}

/**
 * Display a popup explaining functionality not available.
 *
 * @param onDismiss (event) request the popup be dismissed
 */
@Composable
private fun FunctionalityNotAvailablePopup(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(
                text = stringResource(id = R.string.post_functionality_not_available),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.close))
            }
        },
    )
}

/**
 * Show a share sheet for a post
 *
 * @param post to share
 * @param context Android context to show the share sheet in
 */
fun sharePost(post: Post, context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, post.title)
        putExtra(Intent.EXTRA_TEXT, post.url)
    }
    context.startActivity(
        Intent.createChooser(
            intent,
            context.getString(R.string.post_share_post),
        ),
    )
}

@Preview("Post screen")
@Preview("Post screen (dark)", uiMode = UI_MODE_NIGHT_YES)
@Preview("Post screen (big font)", fontScale = 1.5f)
@Composable
fun PreviewPostDrawer() {
    JetnewsTheme {
        val post = runBlocking {
            (BlockingFakePostsRepository().getPost(post3.id) as Result.Success).data
        }
        PostScreen(post, false, {}, false, {})
    }
}

@Preview("Post screen navrail", device = Devices.PIXEL_C)
@Preview(
    "Post screen navrail (dark)",
    uiMode = UI_MODE_NIGHT_YES,
    device = Devices.PIXEL_C,
)
@Preview("Post screen navrail (big font)", fontScale = 1.5f, device = Devices.PIXEL_C)
@Composable
fun PreviewPostNavRail() {
    JetnewsTheme {
        val post = runBlocking {
            (BlockingFakePostsRepository().getPost(post3.id) as Result.Success).data
        }
        PostScreen(post, true, {}, false, {})
    }
}
