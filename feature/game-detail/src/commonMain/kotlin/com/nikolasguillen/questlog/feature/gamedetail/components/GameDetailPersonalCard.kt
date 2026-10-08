package com.nikolasguillen.questlog.feature.gamedetail.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.Priority
import com.nikolasguillen.questlog.core.ui.component.CustomContentCard
import com.nikolasguillen.questlog.core.ui.component.CustomFilterChip
import com.nikolasguillen.questlog.core.ui.component.CustomSummaryBadge
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.collapse_content_description
import com.nikolasguillen.questlog.core.ui.resources.expand_content_description
import com.nikolasguillen.questlog.core.ui.resources.personal_notes_label
import com.nikolasguillen.questlog.core.ui.resources.personal_progress_title
import com.nikolasguillen.questlog.core.ui.resources.priority_label
import com.nikolasguillen.questlog.core.ui.resources.status_label
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailPersonalUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.GameStatusUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.PriorityUiModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

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

    CustomContentCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .height(IntrinsicSize.Min)
    ) {
        if (expanded) {
            PersonalCardExpandedContent(
                uiModel = uiModel,
                rotationState = rotationState,
                onStatusChange = onStatusChange,
                onPriorityChange = onPriorityChange,
                onNotesChange = onNotesChange,
                onCollapse = { expanded = false }
            )
        } else {
            PersonalCardCollapsedContent(
                uiModel = uiModel,
                rotationState = rotationState,
                onExpand = { expanded = true }
            )
        }
    }
}

@Composable
private fun PersonalCardCollapsedContent(
    uiModel: GameDetailPersonalUiModel,
    rotationState: Float,
    onExpand: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(CoreUiRes.string.personal_progress_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.extraSmall)
                ) {
                    uiModel.availableStatuses.find { it.selected }?.let { status ->
                        CustomSummaryBadge(text = status.label.asString())
                    }
                    uiModel.availablePriorities.find { it.selected }?.let { priority ->
                        CustomSummaryBadge(text = priority.label.asString())
                    }
                }
            }

            IconButton(onClick = onExpand) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(CoreUiRes.string.expand_content_description),
                    modifier = Modifier.rotate(rotationState)
                )
            }
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
    onNotesChange: (String) -> Unit,
    onCollapse: () -> Unit
) {
    val notesFieldState = rememberTextFieldState(initialText = uiModel.notes.asString())

    LaunchedEffect(notesFieldState) {
        snapshotFlow { notesFieldState.text.toString() }
            .drop(1) // the initial emission just mirrors initialText, not a user edit
            .debounce(NOTES_CHANGE_DEBOUNCE)
            .collect { onNotesChange(it) }
    }

    Column {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(CoreUiRes.string.personal_progress_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(CoreUiRes.string.collapse_content_description),
                    modifier = Modifier.rotate(rotationState)
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        // Status
        Text(
            text = stringResource(CoreUiRes.string.status_label),
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
                    onFilterClick = { onStatusChange(statusUi.id) },
                    enabled = statusUi.enabled
                )
            }
        }
        uiModel.lockedStatusesHint?.let { hint ->
            Text(
                text = hint.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        // Priority
        Text(
            text = stringResource(CoreUiRes.string.priority_label),
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
            label = { Text(stringResource(CoreUiRes.string.personal_notes_label)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

@QuestLogPreviews
@Composable
private fun GameDetailPersonalCardPreview() {
    QuestLogTheme {
        GameDetailPersonalCard(
            uiModel = GameDetailPersonalUiModel(
                availableStatuses = listOf(
                    GameStatusUiModel(
                        GameStatus.WANT_TO_BUY.id,
                        UiText.DynamicString("Want to buy"),
                        selected = false,
                        enabled = true
                    ),
                    GameStatusUiModel(
                        GameStatus.PLAYING.id,
                        UiText.DynamicString("Playing"),
                        selected = true,
                        enabled = true
                    )
                ),
                lockedStatusesHint = null,
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

@QuestLogPreviews
@Composable
private fun PersonalCardExpandedContentUnreleasedPreview() {
    QuestLogTheme {
        CustomContentCard {
            PersonalCardExpandedContent(
                uiModel = GameDetailPersonalUiModel(
                    availableStatuses = listOf(
                        GameStatusUiModel(
                            GameStatus.WANT_TO_BUY.id,
                            UiText.DynamicString("Want to buy"),
                            selected = false,
                            enabled = true
                        ),
                        GameStatusUiModel(
                            GameStatus.BOUGHT.id,
                            UiText.DynamicString("Bought"),
                            selected = true,
                            enabled = true
                        ),
                        GameStatusUiModel(
                            GameStatus.PLAYING.id,
                            UiText.DynamicString("Playing"),
                            selected = false,
                            enabled = false
                        )
                    ),
                    lockedStatusesHint = UiText.DynamicString("More statuses unlock once the game is released"),
                    availablePriorities = listOf(
                        PriorityUiModel(
                            Priority.HIGH.id,
                            UiText.DynamicString("High"),
                            true
                        )
                    ),
                    notes = UiText.DynamicString("Pre-ordered the collector's edition")
                ),
                rotationState = 180f,
                onStatusChange = {},
                onPriorityChange = {},
                onNotesChange = {},
                onCollapse = {}
            )
        }
    }
}
