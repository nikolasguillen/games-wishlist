package com.example.gameswishlist.core.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.gameswishlist.core.designsystem.theme.appColors

@Composable
fun CustomFilterChip(
    label: String,
    selected: Boolean,
    onFilterClick: () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
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
            selectedContainerColor = MaterialTheme.appColors.filterChipSelectedContainerColor,
            selectedLabelColor = MaterialTheme.appColors.filterChipSelectedContentColor,
            selectedTrailingIconColor = MaterialTheme.appColors.filterChipSelectedContentColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = Color.White.copy(alpha = 0.3f)
        ),
        modifier = Modifier.animateContentSize()
    )
}