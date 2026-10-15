package com.example.jetnews.ui.interests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.jetnews.R
import com.example.jetnews.data.preferences.NewsInterest
import com.example.jetnews.data.preferences.NewsPreferencesRepository
import com.example.jetnews.deeplink.util.DeepLinkPattern
import kotlinx.serialization.Serializable

@Serializable
data object InterestsKey : NavKey

val InterestsDeepLinkPattern = DeepLinkPattern(
    InterestsKey.serializer(),
    uriPattern = "https://developer.android.com/jetnews/interests".toUri(),
)

fun EntryProviderScope<NavKey>.interestsEntry(
    newsPreferencesRepository: NewsPreferencesRepository,
    isExpandedScreen: () -> Boolean,
    openDrawer: () -> Unit,
) {
    entry<InterestsKey> {
        val viewModel: InterestsViewModel = viewModel(
            factory = InterestsViewModel.provideFactory(newsPreferencesRepository),
        )
        InterestsScreen(viewModel, isExpandedScreen(), openDrawer)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestsScreen(
    interestsViewModel: InterestsViewModel,
    isExpandedScreen: Boolean,
    openDrawer: () -> Unit,
) {
    val state by interestsViewModel.uiState.collectAsStateWithLifecycle()
    val showLastInterestWarning by interestsViewModel.lastInterestWarning.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.interests_title)) },
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
    ) { padding ->
        // 宽屏内容居中且限制阅读宽度，手机端仍保留舒适边距。
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 900.dp)
                    .padding(horizontal = 12.dp, vertical = 24.dp),
            ) {
                InterestsHeader(selectedCount = state.selected.size)
                if (showLastInterestWarning) {
                    LastInterestWarning()
                }
                Spacer(Modifier.height(20.dp))
                InterestsGrid(
                    selected = state.selected,
                    onInterestClick = interestsViewModel::toggleInterest,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun InterestsHeader(selectedCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.interests_heading),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.interests_subheading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = stringResource(R.string.interests_selected_count, selectedCount),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                text = stringResource(R.string.interests_auto_save),
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LastInterestWarning() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = stringResource(R.string.interests_last_interest_warning),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun InterestsGrid(
    selected: Set<NewsInterest>,
    onInterestClick: (NewsInterest) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 限制网格自身宽度：手机能放两列，平板最多四列，避免内容向右无限延展。
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
            state = rememberLazyGridState(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(NewsInterest.entries, key = { it.sectionId }) { interest ->
                InterestCard(
                    interest = interest,
                    selected = interest in selected,
                    onClick = { onInterestClick(interest) },
                )
            }
        }
    }
}

@Composable
private fun InterestCard(
    interest: NewsInterest,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = if (selected) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
    val stateText = stringResource(if (selected) R.string.interests_selected else R.string.interests_not_selected)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(144.dp)
            .semantics { contentDescription = "${interest.displayName}，$stateText" }
            .clickable(onClick = onClick),
        colors = colors,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = InterestIcons.iconFor(interest),
                    contentDescription = interest.displayName,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.weight(1f))
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.interests_selected),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(interest.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    interest.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

/** 集中维护新闻类别图标，避免将映射散落在卡片 Composable 中。 */
private object InterestIcons {
    fun iconFor(interest: NewsInterest): ImageVector = when (interest) {
        NewsInterest.TECHNOLOGY -> Icons.Outlined.Computer
        NewsInterest.SCIENCE -> Icons.Outlined.Science
        NewsInterest.WORLD -> Icons.Outlined.Language
        NewsInterest.BUSINESS -> Icons.Outlined.BusinessCenter
        NewsInterest.ENVIRONMENT -> Icons.Outlined.Park
        NewsInterest.SPORT -> Icons.Outlined.SportsSoccer
        NewsInterest.CULTURE -> Icons.Outlined.TheaterComedy
        NewsInterest.POLITICS -> Icons.Outlined.AccountBalance
        NewsInterest.LIFE_AND_STYLE -> Icons.Outlined.Restaurant
    }
}
