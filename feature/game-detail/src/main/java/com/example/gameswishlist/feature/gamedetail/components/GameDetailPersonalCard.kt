package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.component.CustomFilterChip
import com.example.gameswishlist.core.ui.component.CustomSegmentedButton
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.core.ui.util.rememberAnimatedMetallicGradient
import com.example.gameswishlist.feature.gamedetail.model.GameDetailPersonalUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameStatusUiModel
import com.example.gameswishlist.feature.gamedetail.model.PriorityUiModel

@Composable
fun GameDetailPersonalCard(
    uiModel: GameDetailPersonalUiModel,
    onStatusChange: (id: Int) -> Unit,
    onPriorityChange: (id: Int) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(2.dp, rememberAnimatedMetallicGradient())
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
            Text(
                stringResource(R.string.personal_progress_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Status
            Text(
                text = stringResource(R.string.status_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                uiModel.availableStatuses.forEach { statusUi ->
                    CustomFilterChip(
                        label = statusUi.label.asString(),
                        selected = statusUi.selected,
                        onFilterClick = { onStatusChange(statusUi.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Priority
            Text(
                text = stringResource(R.string.priority_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            val selectedPriorityIndex = uiModel.availablePriorities.indexOfFirst { it.selected }

            CustomSegmentedButton(
                options = uiModel.availablePriorities,
                selectedIndex = selectedPriorityIndex,
                onOptionSelected = { index ->
                    onPriorityChange(uiModel.availablePriorities[index].id)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { priorityUi ->
                    Text(
                        text = priorityUi.label.asString(),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Notes
            OutlinedTextField(
                value = uiModel.notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.personal_notes_label)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameDetailPersonalCardPreview() {
    GamesWishlistTheme {
        GameDetailPersonalCard(
            uiModel = GameDetailPersonalUiModel(
                availableStatuses = listOf(
                    GameStatusUiModel(
                        GameStatus.WANT_TO_BUY.id,
                        UiText.DynamicString("Want to buy"),
                        false
                    ),
                    GameStatusUiModel(
                        GameStatus.PLAYING.id,
                        UiText.DynamicString("Playing"),
                        true
                    )
                ),
                availablePriorities = listOf(
                    PriorityUiModel(
                        Priority.LOW.id,
                        UiText.DynamicString("Low"),
                        false
                    ),
                    PriorityUiModel(
                        Priority.MEDIUM.id,
                        UiText.DynamicString("Medium"),
                        true
                    ),
                    PriorityUiModel(
                        Priority.HIGH.id,
                        UiText.DynamicString("High"),
                        false
                    )
                ),
                notes = "Loving the open world so far!"
            ),
            onStatusChange = {},
            onPriorityChange = {},
            onNotesChange = {}
        )
    }
}
