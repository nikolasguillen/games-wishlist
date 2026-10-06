package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing

/**
 * A reusable segmented button component for single-choice selections.
 *
 * @param options List of options to display.
 * @param selectedIndex The index of the currently selected option.
 * @param onOptionSelected Callback invoked when an option is selected.
 * @param label Composable to display as the label for each option.
 * @param modifier The modifier to be applied to the row.
 */
@Composable
fun <T> CustomSegmentedButton(
    options: List<T>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    label: @Composable (T) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onOptionSelected(index) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                    baseShape = MaterialTheme.shapes.medium
                ),
                contentPadding = PaddingValues(
                    horizontal = MaterialTheme.spacing.mediumLarge,
                    vertical = MaterialTheme.spacing.large
                ),
                label = { label(option) },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.appColors.segmentedButtonSelectedColor,
                    activeContentColor = MaterialTheme.appColors.segmentedButtonSelectedContentColor,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@QuestLogPreviews
@Composable
private fun CustomSegmentedButtonPreview() {
    QuestLogTheme {
        CustomSegmentedButton(
            options = listOf("Option 1", "Option 2", "Option 3"),
            selectedIndex = 1,
            onOptionSelected = {},
            label = { Text(it) }
        )
    }
}
