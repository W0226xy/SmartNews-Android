package com.example.jetnews.ui.saved

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.jetnews.R
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.deeplink.util.DeepLinkPattern
import com.example.jetnews.model.Post
import com.example.jetnews.ui.home.AuthorAndReadTime
import com.example.jetnews.ui.home.PostImage
import com.example.jetnews.ui.home.PostTitle
import com.example.jetnews.ui.navigation.ListDetailScene
import com.example.jetnews.ui.theme.JetnewsTheme
import com.example.jetnews.ui.utils.BookmarkButton
import kotlinx.serialization.Serializable

enum class SavedSection(@param:StringRes val titleResId: Int) {
    Favorites(R.string.saved_tab_favorites),
    History(R.string.saved_tab_history),
}

@Serializable
data object SavedKey : NavKey

val SavedDeepLinkPattern = DeepLinkPattern(
    SavedKey.serializer(),
    uriPattern = "https://developer.android.com/jetnews/saved".toUri(),
)

fun EntryProviderScope<NavKey>.savedEntry(
    postsRepository: PostsRepository,
    isExpandedScreen: () -> Boolean,
    openDrawer: () -> Unit,
    navigateToPost: (String) -> Unit,
) {
    entry<SavedKey>(
        metadata = ListDetailScene.list(
            ListDetailScene.ListConfiguration(
                modifier = Modifier.width(334.dp),
                detailPlaceholder = {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.saved_detail_placeholder),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                },
            ),
        ),
    ) {
        val savedViewModel: SavedViewModel =
            viewModel(factory = SavedViewModel.provideFactory(postsRepository))

        SavedScreen(
            savedViewModel = savedViewModel,
            isExpandedScreen = isExpandedScreen(),
            openDrawer = openDrawer,
            navigateToPost = navigateToPost,
        )
    }
}

@Composable
fun SavedScreen(
    savedViewModel: SavedViewModel,
    isExpandedScreen: Boolean,
    openDrawer: () -> Unit,
    navigateToPost: (String) -> Unit,
) {
    val uiState by savedViewModel.uiState.collectAsStateWithLifecycle()
    var currentSection by rememberSaveable { mutableStateOf(SavedSection.Favorites) }

    SavedScreen(
        uiState = uiState,
        currentSection = currentSection,
        isExpandedScreen = isExpandedScreen,
        onSectionChange = { currentSection = it },
        onToggleFavorite = savedViewModel::toggleFavorite,
        onSelectPost = navigateToPost,
        openDrawer = openDrawer,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    uiState: SavedUiState,
    currentSection: SavedSection,
    isExpandedScreen: Boolean,
    onSectionChange: (SavedSection) -> Unit,
    onToggleFavorite: (Post) -> Unit,
    onSelectPost: (String) -> Unit,
    openDrawer: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.saved_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                navigationIcon = {
                    if (!isExpandedScreen) {
                        IconButton(onClick = openDrawer) {
                            Icon(
                                painter = painterResource(R.drawable.ic_jetnews_logo),
                                contentDescription = stringResource(R.string.cd_open_navigation_drawer),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            PrimaryTabRow(selectedTabIndex = currentSection.ordinal) {
                SavedSection.entries.forEach { section ->
                    Tab(
                        selected = currentSection == section,
                        onClick = { onSectionChange(section) },
                        text = { Text(stringResource(section.titleResId)) },
                    )
                }
            }

            when (currentSection) {
                SavedSection.Favorites -> {
                    SavedPostsContent(
                        posts = uiState.favoritePosts,
                        favoriteIds = uiState.favoriteIds,
                        emptyTitle = R.string.saved_empty_favorites_title,
                        emptySubtitle = R.string.saved_empty_favorites_subtitle,
                        sectionLabel = null,
                        onToggleFavorite = onToggleFavorite,
                        onSelectPost = onSelectPost,
                    )
                }

                SavedSection.History -> {
                    SavedPostsContent(
                        posts = uiState.readingHistory,
                        favoriteIds = uiState.favoriteIds,
                        emptyTitle = R.string.saved_empty_history_title,
                        emptySubtitle = R.string.saved_empty_history_subtitle,
                        sectionLabel = R.string.saved_recently_read,
                        onToggleFavorite = onToggleFavorite,
                        onSelectPost = onSelectPost,
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedPostsContent(
    posts: List<Post>,
    favoriteIds: Set<String>,
    @StringRes emptyTitle: Int,
    @StringRes emptySubtitle: Int,
    @StringRes sectionLabel: Int?,
    onToggleFavorite: (Post) -> Unit,
    onSelectPost: (String) -> Unit,
) {
    if (posts.isEmpty()) {
        SavedEmptyState(
            title = stringResource(emptyTitle),
            subtitle = stringResource(emptySubtitle),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(posts, key = { it.id }) { post ->
            SavedPostCard(
                post = post,
                isFavorite = favoriteIds.contains(post.id),
                sectionLabel = sectionLabel?.let { stringResource(it) },
                onToggleFavorite = { onToggleFavorite(post) },
                onSelectPost = { onSelectPost(post.id) },
            )
        }
    }
}

@Composable
private fun SavedPostCard(
    post: Post,
    isFavorite: Boolean,
    sectionLabel: String?,
    onToggleFavorite: () -> Unit,
    onSelectPost: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectPost)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PostImage(post = post, modifier = Modifier.padding(end = 16.dp))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            if (sectionLabel != null) {
                Text(
                    text = sectionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            PostTitle(post)
            AuthorAndReadTime(
                post = post,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = post.metadata.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BookmarkButton(
            isBookmarked = isFavorite,
            onClick = onToggleFavorite,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SavedEmptyState(title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark_outline),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview("Saved screen")
@Preview("Saved screen dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSavedScreen() {
    JetnewsTheme {
        SavedScreen(
            uiState = SavedUiState(),
            currentSection = SavedSection.Favorites,
            isExpandedScreen = false,
            onSectionChange = {},
            onToggleFavorite = {},
            onSelectPost = {},
            openDrawer = {},
        )
    }
}
