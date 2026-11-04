package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.ScrollToTopFabDefaults
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfUiModel
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.discover_release_date_disclaimer
import org.jetbrains.compose.resources.stringResource

/**
 * The Discover feed for a window at least 840dp wide: the same content as [DiscoverFeed] in a layout of its own,
 * a tall hero banner with its own actions, then one full-width row of cards per shelf.
 *
 * It emits the same items, in the same order and under the same keys as [DiscoverFeed] (see
 * `DiscoverFeedItemKeys.kt`) and takes the same [state], so a resize across the wide-window threshold swaps the
 * layout without a reload and keeps the first visible shelf. With nothing to show it falls back to the placeholder,
 * as [DiscoverFeed] does. [isStale] draws [DiscoverRefreshPrompt] in the same top overlay.
 */
@Composable
internal fun WideDiscoverFeed(
    hero: GameItemUiModel?,
    popular: DiscoverShelfUiModel?,
    upcoming: DiscoverShelfUiModel?,
    onGameClick: (Int) -> Unit,
    onSaveClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    onSeeAllClick: (DiscoverShelfUiModel) -> Unit,
    modifier: Modifier = Modifier,
    recommended: List<DiscoverShelfUiModel> = emptyList(),
    isStale: Boolean = false,
    isRefreshing: Boolean = false,
    onRefreshClick: () -> Unit = {},
    onDismissRefreshClick: () -> Unit = {},
    state: LazyListState = rememberLazyListState()
) {
    if (hero == null && popular == null && upcoming == null && recommended.isEmpty()) {
        DiscoverPlaceholder(modifier = modifier)
        return
    }

    Box {
        LazyColumn(
            state = state,
            contentPadding = PaddingValues(
                top = MaterialTheme.spacing.large,
                bottom = ScrollToTopFabDefaults.ContentBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
            modifier = modifier
        ) {
            if (hero != null) {
                item(key = DISCOVER_HERO_KEY) {
                    Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)) {
                        WideDiscoverHero(
                            game = hero,
                            onDetailsClick = { onGameClick(hero.id) },
                            onSaveClick = { onSaveClick(hero.id) },
                            onSaveLongClick = { onLongClick(hero.id) }
                        )
                        Text(
                            text = stringResource(Res.string.discover_release_date_disclaimer),
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(
                                top = MaterialTheme.spacing.small,
                                start = MaterialTheme.spacing.small
                            )
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isStale,
            enter = fadeIn() + slideInVertically { -it },
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            DiscoverRefreshPrompt(
                isRefreshing = isRefreshing,
                onClick = onRefreshClick,
                onDismissClick = onDismissRefreshClick,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.large)
                    .padding(top = MaterialTheme.spacing.extraLarge)
            )
        }
    }
}

private val previewHero = GameItemUiModel.getDummy().copy(name = "Hollow Knight: Silksong")

@QuestLogPreviews
@Composable
private fun WideDiscoverFeedPreview() {
    QuestLogTheme {
        WideDiscoverFeed(
            hero = previewHero,
            popular = null,
            upcoming = null,
            onGameClick = {},
            onSaveClick = {},
            onLongClick = {},
            onSeeAllClick = {}
        )
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun WideDiscoverFeedDesktopPreview() {
    QuestLogTheme {
        WideDiscoverFeed(
            hero = previewHero,
            popular = null,
            upcoming = null,
            onGameClick = {},
            onSaveClick = {},
            onLongClick = {},
            onSeeAllClick = {}
        )
    }
}
