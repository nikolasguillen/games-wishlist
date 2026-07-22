package com.example.gameswishlist.core.ui.component

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
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

@Preview(showBackground = true)
@Composable
private fun CustomFilterChipUnselectedPreview() {
    GamesWishlistTheme {
        CustomFilterChip(
            label = "Action",
            selected = false,
            onFilterClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomFilterChipSelectedPreview() {
    GamesWishlistTheme {
        CustomFilterChip(
            label = "RPG",
            selected = true,
            onFilterClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomFilterChipWithTrailingIconPreview() {
    GamesWishlistTheme {
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

@Preview(showBackground = true)
@Composable
private fun CustomFilterChipDisabledPreview() {
    GamesWishlistTheme {
        CustomFilterChip(
            label = "Status: Playing",
            selected = false,
            onFilterClick = {},
            enabled = false
        )
    }
}