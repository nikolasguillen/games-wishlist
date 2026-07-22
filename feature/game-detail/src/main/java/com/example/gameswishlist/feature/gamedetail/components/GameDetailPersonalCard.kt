package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlin.time.Duration.Companion.milliseconds

private val NOTES_CHANGE_DEBOUNCE = 500.milliseconds

@Composable
internal fun GameDetailPersonalCard(
    uiModel: GameDetailPersonalUiModel,
    onStatusChange: (id: Int) -> Unit,
    onPriorityChange: (id: Int) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevronRotation"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(MaterialTheme.shapes.medium)
            .height(IntrinsicSize.Min)
            // Only merge when collapsed: expanded content holds filter chips and a text
            // field that must stay individually focusable for TalkBack.
            .then(if (!expanded) Modifier.semantics(mergeDescendants = true) {} else Modifier)
            .clickable(
                onClick = { expanded = !expanded },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        if (expanded) {
            PersonalCardExpandedContent(
                uiModel = uiModel,
                rotationState = rotationState,
                onStatusChange = onStatusChange,
                onPriorityChange = onPriorityChange,
                onNotesChange = onNotesChange
            )
        } else {
            PersonalCardCollapsedContent(
                uiModel = uiModel,
                rotationState = rotationState
            )
        }
    }
}

@Composable
private fun PersonalCardCollapsedContent(
    uiModel: GameDetailPersonalUiModel,
    rotationState: Float
) {
    Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.personal_progress_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.extraSmall)
                ) {
                    uiModel.availableStatuses.find { it.selected }?.let { status ->
                        SummaryBadge(text = status.label.asString())
                    }
                    uiModel.availablePriorities.find { it.selected }?.let { priority ->
                        SummaryBadge(
                            text = priority.label.asString(),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.expand_content_description),
                modifier = Modifier.rotate(rotationState)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
private fun PersonalCardExpandedContent(
    uiModel: GameDetailPersonalUiModel,
    rotationState: Float,
    onStatusChange: (id: Int) -> Unit,
    onPriorityChange: (id: Int) -> Unit,
    onNotesChange: (String) -> Unit
) {
    val notesFieldState = rememberTextFieldState(initialText = uiModel.notes.asString())

    LaunchedEffect(notesFieldState) {
        snapshotFlow { notesFieldState.text.toString() }
            .drop(1) // the initial emission just mirrors initialText, not a user edit
            .debounce(NOTES_CHANGE_DEBOUNCE)
            .collect { onNotesChange(it) }
    }

    Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.personal_progress_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.collapse_content_description),
                modifier = Modifier.rotate(rotationState)
            )
        }

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

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            uiModel.availablePriorities.forEach { priorityUi ->
                CustomFilterChip(
                    label = priorityUi.label.asString(),
                    selected = priorityUi.selected,
                    onFilterClick = { onPriorityChange(priorityUi.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        // Notes
        OutlinedTextField(
            state = notesFieldState,
            label = { Text(stringResource(R.string.personal_notes_label)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SummaryBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        color = containerColor,
        shape = CircleShape,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDetailPersonalCardPreview() {
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
                notes = UiText.DynamicString("Loving the open world so far!")
            ),
            onStatusChange = {},
            onPriorityChange = {},
            onNotesChange = {}
        )
    }
}
