package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomContentCard
import com.example.gameswishlist.core.ui.R as CoreUiR

/**
 * A card displaying the game description with expand/collapse functionality.
 */
@Composable
internal fun GameDescriptionCard(
    description: String,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var hasOverflow by rememberSaveable { mutableStateOf(false) }

    CustomContentCard(
        title = stringResource(CoreUiR.string.description_title),
        modifier = modifier.clickable(
            enabled = hasOverflow || expanded,
            onClick = { expanded = !expanded },
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        )
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    hasOverflow = textLayoutResult.hasVisualOverflow
                }
            )

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

@Preview(showBackground = true)
@Composable
private fun GameDescriptionCardPreview() {
    GamesWishlistTheme {
        GameDescriptionCard(
            description = "This is a long description that should overflow after three lines. " +
                    "It is designed to test the expand and collapse functionality of the card. " +
                    "When the user clicks on it, the full content will be revealed with a smooth animation."
        )
    }
}
