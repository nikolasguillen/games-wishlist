package com.example.gameswishlist.core.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gameswishlist.core.designsystem.theme.appColors

/**
 * A reusable segmented button component for single-choice selections.
 *
 * @param options List of options to display.
 * @param selectedIndex The index of the currently selected option.
 * @param onOptionSelected Callback invoked when an option is selected.
 * @param label Composable to display as the label for each option.
 * @param modifier The modifier to be applied to the row.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                    count = options.size
                ),
                label = { label(option) },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.appColors.segmentedButtonSelectedColor,
                    activeContentColor = MaterialTheme.appColors.segmentedButtonSelectedContentColor,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
