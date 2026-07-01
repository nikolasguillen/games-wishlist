package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.designsystem.theme.spacing

@Composable
fun CustomInfoChip(
    text: String,
    modifier: Modifier = Modifier,
    isLarge: Boolean = true,
    containerColor: Color = MaterialTheme.appColors.filterChipSelectedContainerColor,
    contentColor: Color = MaterialTheme.appColors.filterChipSelectedContentColor,
    borderColor: Color? = null
) {
    val style =
        if (isLarge) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium

    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
        border = if (borderColor != null) BorderStroke(1.dp, borderColor) else null
    ) {
        Text(
            text = text,
            style = style,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            )
        )
    }
}
