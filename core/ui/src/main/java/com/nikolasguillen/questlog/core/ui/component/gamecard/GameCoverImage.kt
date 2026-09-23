package com.nikolasguillen.questlog.core.ui.component.gamecard

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.R as CoreUiR

/**
 * Bare cover art -- placeholder on error, cropped, corner-clipped -- with no fixed size so callers size it
 * through [modifier]. [MiniGameCard] wraps this in a bordered [androidx.compose.material3.OutlinedCard]
 * for the 48x48 case; use this directly for a different aspect ratio, such as a portrait cover.
 */
@Composable
fun GameCoverImage(
    coverImage: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small
) {
    AsyncImage(
        model = coverImage,
        contentDescription = null,
        error = painterResource(CoreUiR.drawable.placeholder),
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(shape)
    )
}

@Preview
@Composable
private fun GameCoverImagePreview() {
    QuestLogTheme {
        GameCoverImage(coverImage = null, modifier = Modifier.size(48.dp))
    }
}
