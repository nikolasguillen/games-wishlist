package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.isDarkTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.CustomChipRow
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.mapper.defaultBrowseGenres
import com.nikolasguillen.questlog.feature.search.model.BrowseGenreUiModel
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.browse_by_genre
import org.jetbrains.compose.resources.stringResource

/**
 * The "Browse by genre" row of the expanded search overlay: a heading and one chip per genre.
 *
 * A tap hands the genre to [onGenreClick] and nothing else -- the overlay closes and the browse starts from
 * there, so no chip has to be scrolled into view afterwards.
 *
 * @param wrap Lay the chips out in a wrapping grid instead of one row that scrolls sideways.
 * @param chipModifier Applied to a box around each chip, given its genre, so a caller can mark or track it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GenreBrowseSection(
    genres: List<BrowseGenreUiModel>,
    onGenreClick: (BrowseGenreUiModel) -> Unit,
    modifier: Modifier = Modifier,
    wrap: Boolean = false,
    chipModifier: (BrowseGenreUiModel) -> Modifier = { Modifier }
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        modifier = modifier
    ) {
        Text(
            text = stringResource(Res.string.browse_by_genre),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
        )
        if (wrap) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.large)
            ) {
                genres.forEach { genre ->
                    Box(modifier = chipModifier(genre)) {
                        GenreChip(
                            label = genre.label.asString(),
                            onClick = { onGenreClick(genre) }
                        )
                    }
                }
            }
        } else {
            CustomChipRow(
                items = genres,
                key = { it.id }
            ) { genre, _ ->
                Box(modifier = chipModifier(genre)) {
                    GenreChip(
                        label = genre.label.asString(),
                        onClick = { onGenreClick(genre) }
                    )
                }
            }
        }
    }
}

/**
 * A genre chip. It wears the colours of a selected filter chip so it reads as part of the same family, but it
 * is an [AssistChip]: a chip that runs an action, which Compose exposes as a button with no selected state.
 * A screen reader must announce "RPG, button" and never "selected" -- tapping it starts something, it does
 * not toggle anything.
 */
@Composable
private fun GenreChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.isDarkTheme
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                maxLines = 1
            )
        },
        shape = MaterialTheme.shapes.small,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.appColors.chipSelectedContainerColor,
            labelColor = MaterialTheme.appColors.chipSelectedContentColor
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = MaterialTheme.appColors.chipSelectedBorderColor,
            borderWidth = if (isDarkTheme) 0.dp else 1.dp
        ),
        modifier = modifier
    )
}

@QuestLogPreviews
@Composable
private fun GenreBrowseSectionPreview() {
    QuestLogTheme {
        GenreBrowseSection(
            genres = defaultBrowseGenres(),
            onGenreClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun GenreBrowseSectionLongLabelPreview() {
    QuestLogTheme {
        GenreBrowseSection(
            genres = listOf(
                BrowseGenreUiModel(1, UiText.DynamicString("Hack and slash")),
                BrowseGenreUiModel(2, UiText.DynamicString("Turn-based strategy")),
                BrowseGenreUiModel(3, UiText.DynamicString("A genre IGDB added after this release"))
            ),
            onGenreClick = {}
        )
    }
}

@Preview(widthDp = 720)
@Composable
private fun GenreBrowseSectionWidePreview() {
    QuestLogTheme {
        GenreBrowseSection(
            genres = defaultBrowseGenres(),
            onGenreClick = {},
            wrap = true
        )
    }
}
