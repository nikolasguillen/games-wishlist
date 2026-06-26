package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.appColors

@Composable
fun CustomInfoChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.appColors.filterChipSelectedContainerColor,
    contentColor: Color = MaterialTheme.appColors.filterChipSelectedContentColor
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
