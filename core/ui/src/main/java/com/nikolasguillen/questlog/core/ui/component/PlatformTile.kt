package com.nikolasguillen.questlog.core.ui.component

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
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.model.UiText

/**
 * A small colored tile showing a platform's short code (e.g. "PS5", "PC"), for use wherever a game
 * needs a compact per-platform visual marker — the release info card's platform strip, Radar's
 * per-platform rows.
 */
@Composable
fun PlatformTile(
    code: UiText,
    containerColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .clip(MaterialTheme.shapes.small)
            .background(containerColor)
    ) {
        Text(
            text = code.asString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1
        )
    }
}

@QuestLogPreviews
@Composable
private fun PlatformTilePreview() {
    QuestLogTheme {
        Surface {
            PlatformTile(code = UiText.DynamicString("PS5"), containerColor = Color(0xFF2E4EA6))
        }
    }
}
