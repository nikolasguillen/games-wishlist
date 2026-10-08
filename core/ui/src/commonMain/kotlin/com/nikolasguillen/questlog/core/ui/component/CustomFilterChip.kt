package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.isDarkTheme

@Composable
fun CustomFilterChip(
    label: String,
    selected: Boolean,
    onFilterClick: () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    val isDarkTheme = MaterialTheme.isDarkTheme
    FilterChip(
        selected = selected,
        onClick = onFilterClick,
        enabled = enabled,
        label = {
            Text(
                text = label,
                maxLines = 1
            )
        },
        trailingIcon = trailingIcon,
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.appColors.chipSelectedContainerColor,
            selectedLabelColor = MaterialTheme.appColors.chipSelectedContentColor,
            selectedTrailingIconColor = MaterialTheme.appColors.chipSelectedContentColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = MaterialTheme.appColors.chipBorderColor,
            selectedBorderColor = MaterialTheme.appColors.chipSelectedBorderColor,
            selectedBorderWidth = if (isDarkTheme) 0.dp else 1.dp
        ),
        modifier = Modifier.animateContentSize()
    )
}

@QuestLogPreviews
@Composable
private fun CustomFilterChipUnselectedPreview() {
    QuestLogTheme {
        CustomFilterChip(
            label = "Action",
            selected = false,
            onFilterClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun CustomFilterChipSelectedPreview() {
    QuestLogTheme {
        CustomFilterChip(
            label = "RPG",
            selected = true,
            onFilterClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun CustomFilterChipWithTrailingIconPreview() {
    QuestLogTheme {
        CustomFilterChip(
            label = "Platform: PC",
            selected = true,
            onFilterClick = {},
            trailingIcon = {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
            }
        )
    }
}

@QuestLogPreviews
@Composable
private fun CustomFilterChipDisabledPreview() {
    QuestLogTheme {
        CustomFilterChip(
            label = "Status: Playing",
            selected = false,
            onFilterClick = {},
            enabled = false
        )
    }
}