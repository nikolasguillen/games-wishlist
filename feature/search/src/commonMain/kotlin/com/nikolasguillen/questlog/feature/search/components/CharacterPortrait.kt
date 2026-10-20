package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme

/** The icon takes this share of the circle's width, so it scales with the circle instead of using a fixed size. */
private const val PLACEHOLDER_ICON_SHARE = 0.5f

/**
 * A character's portrait, cropped into a circle. The size comes from [modifier] and is meant to be square.
 *
 * The person icon sits under the image, so it shows when there is no [imageUrl], while the image loads and when
 * it fails to load. Decorative: the card or header that holds it says who the character is.
 */
@Composable
internal fun CharacterPortrait(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(PLACEHOLDER_ICON_SHARE)
        )
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@QuestLogPreviews
@Composable
private fun CharacterPortraitWithoutImagePreview() {
    QuestLogTheme {
        CharacterPortrait(imageUrl = null, modifier = Modifier.size(96.dp))
    }
}

@QuestLogPreviews
@Composable
private fun CharacterPortraitWithImagePreview() {
    QuestLogTheme {
        // No network in a preview, so this shows the placeholder under a pending image.
        CharacterPortrait(imageUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/example.jpg", modifier = Modifier.size(96.dp))
    }
}
