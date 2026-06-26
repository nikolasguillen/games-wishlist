package com.example.gameswishlist.feature.gamedetail.components

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
import androidx.compose.material3.Slider
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
import com.example.gameswishlist.core.ui.model.UiText
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
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
            Text(
                stringResource(R.string.personal_progress_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Status
            Text(stringResource(R.string.status_label), style = MaterialTheme.typography.bodyMedium)
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

            // Priority
            Text(
                stringResource(R.string.priority_label),
                style = MaterialTheme.typography.bodyMedium
            )
            val selectedPriorityIndex = uiModel.availablePriorities.indexOfFirst { it.selected }
                .coerceAtLeast(0)

            Slider(
                value = selectedPriorityIndex.toFloat(),
                onValueChange = { index ->
                    onPriorityChange(uiModel.availablePriorities[index.toInt()].id)
                },
                valueRange = 0f..(uiModel.availablePriorities.size - 1).toFloat(),
                steps = if (uiModel.availablePriorities.size > 1) uiModel.availablePriorities.size - 2 else 0
            )

            uiModel.availablePriorities.find { it.selected }?.let { selectedPriority ->
                Text(
                    text = selectedPriority.label.asString(),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Notes
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiModel.notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.personal_notes_label)) },
                modifier = Modifier.fillMaxWidth()
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
                        UiText.DynamicString("Playing"),
                        true
                    )
                ),
                availablePriorities = listOf(
                    PriorityUiModel(
                        Priority.MEDIUM.id,
                        UiText.DynamicString("Medium"),
                        true
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
