package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.component.EmptyPage
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.clear_filters_action
import com.nikolasguillen.questlog.feature.search.resources.clear_search_action
import com.nikolasguillen.questlog.feature.search.resources.search_initial_message
import com.nikolasguillen.questlog.feature.search.resources.search_no_filtered_results
import com.nikolasguillen.questlog.feature.search.resources.search_no_results
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DiscoverPlaceholder(modifier: Modifier = Modifier) {
    EmptyPage(
        message = stringResource(Res.string.search_initial_message),
        icon = Icons.Outlined.SportsEsports,
        modifier = modifier
    )
}

@Composable
internal fun EmptySearchPlaceholder(onClearSearchClick: () -> Unit, modifier: Modifier = Modifier) {
    EmptyPage(
        message = stringResource(Res.string.search_no_results),
        icon = Icons.Outlined.SmartToy,
        actionLabel = UiText.StringResource(Res.string.clear_search_action),
        onActionClick = onClearSearchClick,
        modifier = modifier
    )
}

@Composable
internal fun NoFilteredResultsPlaceholder(onClearFiltersClick: () -> Unit, modifier: Modifier = Modifier) {
    EmptyPage(
        message = stringResource(Res.string.search_no_filtered_results),
        icon = Icons.Outlined.SearchOff,
        actionLabel = UiText.StringResource(Res.string.clear_filters_action),
        onActionClick = onClearFiltersClick,
        modifier = modifier
    )
}

@QuestLogPreviews
@Composable
private fun DiscoverPlaceholderPreview() {
    QuestLogTheme {
        DiscoverPlaceholder()
    }
}

@QuestLogPreviews
@Composable
private fun EmptySearchPlaceholderPreview() {
    QuestLogTheme {
        EmptySearchPlaceholder(onClearSearchClick = {})
    }
}

@QuestLogPreviews
@Composable
private fun NoFilteredResultsPlaceholderPreview() {
    QuestLogTheme {
        NoFilteredResultsPlaceholder(onClearFiltersClick = {})
    }
}
