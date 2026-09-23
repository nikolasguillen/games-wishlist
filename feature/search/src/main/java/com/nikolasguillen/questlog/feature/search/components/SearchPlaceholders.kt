package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.component.EmptyPage
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.R

@Composable
internal fun DiscoverPlaceholder(modifier: Modifier = Modifier) {
    EmptyPage(
        message = stringResource(R.string.search_initial_message),
        icon = Icons.Outlined.SportsEsports,
        modifier = modifier
    )
}

@Composable
internal fun EmptySearchPlaceholder(onClearSearchClick: () -> Unit, modifier: Modifier = Modifier) {
    EmptyPage(
        message = stringResource(R.string.search_no_results),
        icon = Icons.Outlined.SmartToy,
        actionLabel = UiText.StringResource(R.string.clear_search_action),
        onActionClick = onClearSearchClick,
        modifier = modifier
    )
}

@Composable
internal fun NoFilteredResultsPlaceholder(onClearFiltersClick: () -> Unit, modifier: Modifier = Modifier) {
    EmptyPage(
        message = stringResource(R.string.search_no_filtered_results),
        icon = Icons.Outlined.SearchOff,
        actionLabel = UiText.StringResource(R.string.clear_filters_action),
        onActionClick = onClearFiltersClick,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun DiscoverPlaceholderPreview() {
    QuestLogTheme {
        DiscoverPlaceholder()
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptySearchPlaceholderPreview() {
    QuestLogTheme {
        EmptySearchPlaceholder(onClearSearchClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun NoFilteredResultsPlaceholderPreview() {
    QuestLogTheme {
        NoFilteredResultsPlaceholder(onClearFiltersClick = {})
    }
}
