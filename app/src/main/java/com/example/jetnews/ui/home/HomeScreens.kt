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

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.jetnews.R
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.data.preferences.NewsPreferencesRepository
import com.example.jetnews.deeplink.util.DeepLinkPattern
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostsFeed
import com.example.jetnews.ui.modifiers.interceptKey
import com.example.jetnews.ui.navigation.ListDetailScene
import com.example.jetnews.ui.theme.JetnewsTheme
import com.example.jetnews.ui.preview.previewPostsFeed
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

val HomeDeepLinkPattern = DeepLinkPattern(
    HomeKey.serializer(),
    uriPattern = "https://developer.android.com/jetnews".toUri(),
)

fun EntryProviderScope<NavKey>.homeEntry(
    postsRepository: PostsRepository,
    newsPreferencesRepository: NewsPreferencesRepository,
    isExpandedScreen: () -> Boolean,
    openDrawer: () -> Unit,
    navigateToPost: (String) -> Unit,
) {
    entry<HomeKey>(
        metadata = ListDetailScene.list(
            ListDetailScene.ListConfiguration(
                modifier = Modifier.width(334.dp),
                detailPlaceholder = {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.home_detail_placeholder),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                },
            ),
        ),
    ) {
        val homeViewModel: HomeViewModel =
            viewModel(factory = HomeViewModel.provideFactory(postsRepository, newsPreferencesRepository))

        HomeScreen(
            homeViewModel = homeViewModel,
            isExpandedScreen = isExpandedScreen(),
            openDrawer = openDrawer,
            navigateToPost = navigateToPost,
        )
    }
}

/**
 * Displays the Home screen.
 *
 * Note: AAC ViewModels don't work with Compose Previews currently.
 *
 * @param homeViewModel ViewModel that handles the business logic of this screen
 * @param isExpandedScreen (state) whether the screen is expanded
 * @param openDrawer (event) request opening the app drawer
 * @param navigateToPost (event) request navigation to Post screen
 */
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    isExpandedScreen: Boolean,
    openDrawer: () -> Unit,
    navigateToPost: (String) -> Unit,
) {
    // UiState of the HomeScreen
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    HomeFeedScreen(
        uiState = uiState,
        showTopAppBar = !isExpandedScreen,
        onToggleFavorite = { homeViewModel.toggleFavourite(it) },
        onSelectPost = { navigateToPost(it) },
        onRefreshPosts = homeViewModel::refresh,
        openDrawer = openDrawer,
        homeListLazyListState = rememberLazyListState(),
        onSearchInputChanged = { homeViewModel.onSearchInputChanged(it) },
    )
}

/**
 * The home screen displaying just the post feed.
 */
@Composable
fun HomeFeedScreen(
    uiState: HomeUiState,
    showTopAppBar: Boolean,
    onToggleFavorite: (Post) -> Unit,
    onSelectPost: (String) -> Unit,
    onRefreshPosts: () -> Unit,
    openDrawer: () -> Unit,
    homeListLazyListState: LazyListState,
    modifier: Modifier = Modifier,
    onSearchInputChanged: (String) -> Unit,
) {
    HomeScreenWithList(
        uiState = uiState,
        showTopAppBar = showTopAppBar,
        onRefreshPosts = onRefreshPosts,
        openDrawer = openDrawer,
        modifier = modifier,
    ) { hasPostsUiState, contentPadding, contentModifier ->
        PostList(
            postsFeed = hasPostsUiState.postsFeed,
            favorites = hasPostsUiState.favorites,
            showExpandedSearch = !showTopAppBar,
            onPostTapped = onSelectPost,
            onToggleFavorite = onToggleFavorite,
            contentPadding = contentPadding,
            modifier = contentModifier,
            state = homeListLazyListState,
            searchInput = hasPostsUiState.searchInput,
            onSearchInputChanged = onSearchInputChanged,
            refreshErrorMessage = hasPostsUiState.refreshErrorMessage,
            recommendationLabel = hasPostsUiState.recommendationLabel,
            recommendationMessage = hasPostsUiState.recommendationMessage,
        )
    }
}

/**
 * A display of the home screen that has the list.
 *
 * This sets up the scaffold with the top app bar, and surrounds the [hasPostsContent] with refresh,
 * loading and error handling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenWithList(
    uiState: HomeUiState,
    showTopAppBar: Boolean,
    onRefreshPosts: () -> Unit,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    hasPostsContent: @Composable (
        uiState: HomeUiState.Success,
        contentPadding: PaddingValues,
        modifier: Modifier,
    ) -> Unit,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    Scaffold(
        topBar = {
            if (showTopAppBar) {
                HomeTopAppBar(
                    openDrawer = openDrawer,
                    topAppBarState = topAppBarState,
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)

        when (uiState) {
            HomeUiState.Loading -> FullScreenLoading()

            is HomeUiState.Error -> ErrorContent(
                message = uiState.message,
                onRetry = onRefreshPosts,
                modifier = Modifier.padding(innerPadding),
            )

            is HomeUiState.Success -> LoadingContent(
                loading = uiState.isRefreshing,
                onRefresh = onRefreshPosts,
                modifier = Modifier.padding(innerPadding),
                content = { hasPostsContent(uiState, innerPadding, contentModifier) },
            )
        }
    }
}

/**
 * Display content with pull-to-refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingContent(
    loading: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val refreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        content = { content() },
        state = refreshState,
        indicator = {
            Indicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(),
                isRefreshing = loading,
                state = refreshState,
            )
        },
    )
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

/**
 * Display a feed of posts.
 *
 * When a post is clicked on, [onPostTapped] will be called.
 *
 * @param postsFeed (state) the feed to display
 * @param favorites (state) a set of favorite posts
 * @param showExpandedSearch (state) whether the expanded search is shown
 * @param onPostTapped (event) request navigation to Post screen
 * @param onToggleFavorite (event) request that this post toggle its favorite state
 * @param modifier modifier for the root element
 * @param contentPadding the padding to apply to the content
 * @param state the state object to be used to control or observe the list's state
 * @param searchInput (state) the search input
 * @param onSearchInputChanged (event) request that the search input changed
 */
@Composable
private fun PostList(
    postsFeed: PostsFeed,
    favorites: Set<String>,
    showExpandedSearch: Boolean,
    onPostTapped: (postId: String) -> Unit,
    onToggleFavorite: (Post) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    state: LazyListState = rememberLazyListState(),
    searchInput: String = "",
    onSearchInputChanged: (String) -> Unit,
    refreshErrorMessage: String? = null,
    recommendationLabel: String = "为你推荐",
    recommendationMessage: String? = null,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        state = state,
    ) {
        if (refreshErrorMessage != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = refreshErrorMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (recommendationMessage != null) {
            item {
                Text(
                    text = recommendationMessage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (showExpandedSearch) {
            item {
                HomeSearch(
                    Modifier.padding(horizontal = 16.dp),
                    searchInput = searchInput,
                    onSearchInputChanged = onSearchInputChanged,
                )
            }
        }
        item { PostListTopSection(postsFeed.highlightedPost, onPostTapped, recommendationLabel) }
        if (postsFeed.recommendedPosts.isNotEmpty()) {
            item {
                PostListSimpleSection(
                    postsFeed.recommendedPosts,
                    onPostTapped,
                    favorites,
                    onToggleFavorite,
                )
            }
        }
        if (postsFeed.popularPosts.isNotEmpty() && !showExpandedSearch) {
            item {
                PostListPopularSection(
                    postsFeed.popularPosts, onPostTapped,
                )
            }
        }
        if (postsFeed.recentPosts.isNotEmpty()) {
            item { PostListHistorySection(postsFeed.recentPosts, onPostTapped) }
        }
    }
}

/**
 * Full screen circular progress indicator
 */
@Composable
private fun FullScreenLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Top section of [PostList]
 *
 * @param post (state) highlighted post to display
 * @param navigateToPost (event) request navigation to Post screen
 */
@Composable
private fun PostListTopSection(
    post: Post,
    navigateToPost: (String) -> Unit,
    recommendationLabel: String,
) {
    Text(
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        text = recommendationLabel,
        style = MaterialTheme.typography.titleMedium,
    )
    PostCardTop(
        post = post,
        modifier = Modifier.clickable(onClick = { navigateToPost(post.id) }),
    )
    PostListDivider()
}

/**
 * Full-width list items for [PostList]
 *
 * @param posts (state) to display
 * @param navigateToPost (event) request navigation to Post screen
 * @param favorites (state) a set of favorite posts
 * @param onToggleFavorite (event) request that this post toggle its favorite state
 */
@Composable
private fun PostListSimpleSection(
    posts: List<Post>,
    navigateToPost: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (Post) -> Unit,
) {
    Column {
        posts.forEach { post ->
            PostCardSimple(
                post = post,
                navigateToPost = navigateToPost,
                isFavorite = favorites.contains(post.id),
                onToggleFavorite = { onToggleFavorite(post) },
            )
            PostListDivider()
        }
    }
}

/**
 * Horizontal scrolling cards for [PostList]
 *
 * @param posts (state) to display
 * @param navigateToPost (event) request navigation to Post screen
 */
@Composable
private fun PostListPopularSection(posts: List<Post>, navigateToPost: (String) -> Unit) {
    Column {
        Text(
            modifier = Modifier.padding(16.dp),
            text = stringResource(id = R.string.home_popular_section_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .height(IntrinsicSize.Max)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (post in posts) {
                PostCardPopular(
                    post,
                    navigateToPost,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        PostListDivider()
    }
}

/**
 * Full-width list items that display "based on your history" for [PostList]
 *
 * @param posts (state) to display
 * @param navigateToPost (event) request navigation to Post screen
 */
@Composable
private fun PostListHistorySection(posts: List<Post>, navigateToPost: (String) -> Unit) {
    Column {
        posts.forEach { post ->
            PostCardHistory(post, navigateToPost)
            PostListDivider()
        }
    }
}

/**
 * Full-width divider with padding for [PostList]
 */
@Composable
private fun PostListDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )
}

/**
 * Expanded search UI - includes support for enter-to-send on the search field
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun HomeSearch(modifier: Modifier = Modifier, searchInput: String = "", onSearchInputChanged: (String) -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = searchInput,
        onValueChange = onSearchInputChanged,
        placeholder = { Text(stringResource(R.string.home_search)) },
        leadingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null) },
        modifier = modifier
            .fillMaxWidth()
            .interceptKey(Key.Enter) {
                // submit a search query when Enter is pressed
                submitSearch(onSearchInputChanged, context)
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            },
        singleLine = true,
        // keyboardOptions change the newline key to a search key on the soft keyboard
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        // keyboardActions submits the search query when the search key is pressed
        keyboardActions = KeyboardActions(
            onSearch = {
                submitSearch(onSearchInputChanged, context)
                keyboardController?.hide()
            },
        ),
    )
}

/**
 * Stub helper function to submit a user's search query
 */
private fun submitSearch(onSearchInputChanged: (String) -> Unit, context: Context) {
    onSearchInputChanged("")
    Toast.makeText(
        context,
        context.getString(R.string.search_not_available),
        Toast.LENGTH_SHORT,
    ).show()
}

/**
 * TopAppBar for the Home screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopAppBar(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    topAppBarState: TopAppBarState = rememberTopAppBarState(),
    scrollBehavior: TopAppBarScrollBehavior? =
        TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState),
) {
    val context = LocalContext.current
    val title = stringResource(id = R.string.app_name)
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(
                    painter = painterResource(R.drawable.ic_jetnews_logo),
                    contentDescription = stringResource(R.string.cd_open_navigation_drawer),
                )
            }
        },
        actions = {
            IconButton(onClick = {
                Toast.makeText(
                    context,
                    context.getString(R.string.search_not_available),
                    Toast.LENGTH_LONG,
                ).show()
            }) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = stringResource(R.string.cd_search),
                )
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    )
}

@Preview("Home list drawer screen")
@Preview("Home list drawer screen (dark)", uiMode = UI_MODE_NIGHT_YES)
@Preview("Home list drawer screen (big font)", fontScale = 1.5f)
@Composable
fun PreviewHomeListDrawerScreen() {
    JetnewsTheme {
        HomeFeedScreen(
            uiState = HomeUiState.Success(
                postsFeed = previewPostsFeed(),
                favorites = emptySet(),
                searchInput = "",
            ),
            showTopAppBar = false,
            onToggleFavorite = {},
            onSelectPost = {},
            onRefreshPosts = {},
            openDrawer = {},
            homeListLazyListState = rememberLazyListState(),
            onSearchInputChanged = {},
        )
    }
}

@Preview("Home list navrail screen", device = Devices.NEXUS_7_2013)
@Preview(
    "Home list navrail screen (dark)",
    uiMode = UI_MODE_NIGHT_YES,
    device = Devices.NEXUS_7_2013,
)
@Preview("Home list navrail screen (big font)", fontScale = 1.5f, device = Devices.NEXUS_7_2013)
@Composable
fun PreviewHomeListNavRailScreen() {
    JetnewsTheme {
        HomeFeedScreen(
            uiState = HomeUiState.Success(
                postsFeed = previewPostsFeed(),
                favorites = emptySet(),
                searchInput = "",
            ),
            showTopAppBar = true,
            onToggleFavorite = {},
            onSelectPost = {},
            onRefreshPosts = {},
            openDrawer = {},
            homeListLazyListState = rememberLazyListState(),
            onSearchInputChanged = {},
        )
    }
}
