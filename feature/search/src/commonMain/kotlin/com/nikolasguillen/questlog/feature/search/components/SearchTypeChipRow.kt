package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.component.CustomChipRow
import com.nikolasguillen.questlog.core.ui.component.CustomFilterChip
import com.nikolasguillen.questlog.feature.search.mapper.toLabelUiText
import com.nikolasguillen.questlog.feature.search.model.SearchType

/**
 * The row of chips that picks what a search looks up, one chip per [SearchType]. The selected chip shows a check
 * mark; the others show the icon of their type. It is always a single row; if the space is narrower than the chips,
 * the row scrolls rather than wraps.
 *
 * @param horizontalArrangement How the chips sit in the row: spread across it by default, as the full-screen overlay
 * has them; the wide panel starts them at the leading edge.
 * @param chipModifier Applied to a box around each chip, given its type, so a caller can mark or track it.
 */
@Composable
internal fun SearchTypeChipRow(
    selected: SearchType,
    onSelected: (SearchType) -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceEvenly,
    chipModifier: (SearchType) -> Modifier = { Modifier }
) {
    CustomChipRow(
        items = SearchType.entries,
        key = { it.name },
        horizontalArrangement = horizontalArrangement,
        modifier = modifier
    ) { type, bringIntoView ->
        Box(modifier = chipModifier(type)) {
            SearchTypeChip(type = type, selected = selected, onSelected = onSelected, bringIntoView = bringIntoView)
        }
    }
}

@Composable
private fun SearchTypeChip(
    type: SearchType,
    selected: SearchType,
    onSelected: (SearchType) -> Unit,
    bringIntoView: () -> Unit
) {
    CustomFilterChip(
        label = type.toLabelUiText().asString(),
        selected = type == selected,
        onFilterClick = {
            onSelected(type)
            bringIntoView()
        },
        leadingIcon = {
            Icon(
                imageVector = if (type == selected) Icons.Default.Check else type.toIcon(),
                contentDescription = null
            )
        }
    )
}

private fun SearchType.toIcon(): ImageVector = when (this) {
    SearchType.GAMES -> Icons.Outlined.SportsEsports
    SearchType.CHARACTERS -> Icons.Outlined.Person
    SearchType.DEVELOPERS -> Icons.Outlined.Business
}

@QuestLogPreviews
@Composable
private fun SearchTypeChipRowGamesPreview() {
    QuestLogTheme {
        SearchTypeChipRow(selected = SearchType.GAMES, onSelected = {})
    }
}

@QuestLogPreviews
@Composable
private fun SearchTypeChipRowCharactersPreview() {
    QuestLogTheme {
        SearchTypeChipRow(selected = SearchType.CHARACTERS, onSelected = {})
    }
}

@QuestLogPreviews
@Composable
private fun SearchTypeChipRowDevelopersPreview() {
    QuestLogTheme {
        SearchTypeChipRow(selected = SearchType.DEVELOPERS, onSelected = {})
    }
}
