package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomContentCard
import com.example.gameswishlist.core.ui.util.shimmerEffect
import com.example.gameswishlist.feature.gamedetail.R
import com.example.gameswishlist.feature.gamedetail.model.DescriptionTranslationState
import com.example.gameswishlist.core.ui.R as CoreUiR

/**
 * A card displaying the game description with expand/collapse functionality.
 *
 * [translation] drives what is actually shown: the original [description] while [DescriptionTranslationState.Off],
 * a shimmering skeleton while [DescriptionTranslationState.InProgress] (there is no partial description to
 * show meanwhile), or the translated text plus a disclosure label once [DescriptionTranslationState.Ready].
 * Expand/collapse state survives the swap from original to translated text: [hasOverflow] is recomputed by
 * `onTextLayout` against whichever text is currently displayed.
 */
@Composable
internal fun GameDescriptionCard(
    description: String,
    translation: DescriptionTranslationState,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var hasOverflow by rememberSaveable { mutableStateOf(false) }
    val isLoading = translation is DescriptionTranslationState.InProgress
    val displayedText = (translation as? DescriptionTranslationState.Ready)?.text ?: description

    CustomContentCard(
        title = stringResource(CoreUiR.string.description_title),
        modifier = modifier.clickable(
            enabled = !isLoading && (hasOverflow || expanded),
            onClick = { expanded = !expanded },
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        )
    ) {
        if (isLoading) {
            DescriptionLoadingSkeleton()
        } else {
            Column(modifier = Modifier.animateContentSize()) {
                Text(
                    text = displayedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        hasOverflow = textLayoutResult.hasVisualOverflow
                    }
                )

                if (translation is DescriptionTranslationState.Ready) {
                    Text(
                        text = stringResource(R.string.description_translated_on_device),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = MaterialTheme.spacing.small)
                    )
                }

                if (hasOverflow || expanded) {
                    Text(
                        text = stringResource(if (expanded) CoreUiR.string.show_less else CoreUiR.string.show_more),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .padding(top = MaterialTheme.spacing.small)
                            .align(Alignment.End)
                    )
                }
            }
        }
    }
}

/** Three shimmering lines standing in for the collapsed description while it is being translated. */
@Composable
private fun DescriptionLoadingSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val lineWidthFractions = listOf(1f, 1f, 0.6f)
        lineWidthFractions.forEachIndexed { index, widthFraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(16.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .shimmerEffect()
            )
            if (index != lineWidthFractions.lastIndex) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDescriptionCardPreview() {
    GamesWishlistTheme {
        GameDescriptionCard(
            description = "This is a long description that should overflow after three lines. " +
                    "It is designed to test the expand and collapse functionality of the card. " +
                    "When the user clicks on it, the full content will be revealed with a smooth animation.",
            translation = DescriptionTranslationState.Off
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDescriptionCardLoadingPreview() {
    GamesWishlistTheme {
        GameDescriptionCard(
            description = "This is a long description that should overflow after three lines.",
            translation = DescriptionTranslationState.InProgress
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDescriptionCardTranslatedPreview() {
    GamesWishlistTheme {
        GameDescriptionCard(
            description = "This is a long description that should overflow after three lines.",
            translation = DescriptionTranslationState.Ready(
                "Questa è una descrizione lunga che dovrebbe traboccare dopo tre righe."
            )
        )
    }
}
