package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.ui.model.UiText

/**
 * A small colored tile showing a platform's short code (e.g. "PS5", "PC"), for use wherever a game
 * needs a compact per-platform visual marker — the release info card's platform strip, Radar's
 * per-platform rows.
 */
@Composable
fun PlatformTile(
    code: UiText,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .clip(MaterialTheme.shapes.small)
            .background(color)
    ) {
        Text(
            text = code.asString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlatformTilePreview() {
    GamesWishlistTheme {
        Surface {
            PlatformTile(code = UiText.DynamicString("PS5"), color = Color(0xFF2E4EA6))
        }
    }
}
