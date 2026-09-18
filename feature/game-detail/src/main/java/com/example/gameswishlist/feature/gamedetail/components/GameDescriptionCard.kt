package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomContentCard
import com.example.gameswishlist.core.ui.util.modifiers.lineShimmer
import com.example.gameswishlist.core.ui.util.modifiers.rainbowMetallicBorder
import com.example.gameswishlist.core.ui.util.modifiers.rememberLineShimmerState
import com.example.gameswishlist.feature.gamedetail.R
import com.example.gameswishlist.feature.gamedetail.model.DescriptionTranslationState
import com.example.gameswishlist.core.ui.R as CoreUiR

private val TranslationProgressIndicatorTouchTarget = 48.dp
private val TranslationProgressIndicatorStrokeWidth = 2.dp

/**
 * A card displaying the game description.
 *
 * [translation] drives the translate action shown next to the card title — hidden while
 * [DescriptionTranslationState.Unavailable], a translate icon while
 * [DescriptionTranslationState.Available], a progress indicator while
 * [DescriptionTranslationState.InProgress] (also swapping the body for a shimmering skeleton, since
 * there is no partial description to show meanwhile), a highlighted "show original" icon plus a
 * disclosure label under the text once [DescriptionTranslationState.Ready], or a retry icon on
 * [DescriptionTranslationState.Failed] — same action as [DescriptionTranslationState.Available], since
 * retrying is just asking for a translation again.
 */
@Composable
internal fun GameDescriptionCard(
    description: String,
    translation: DescriptionTranslationState,
    onTranslateClick: () -> Unit,
    onShowOriginalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading = translation is DescriptionTranslationState.InProgress
    val displayedText = (translation as? DescriptionTranslationState.Ready)?.text ?: description
    val shimmerState = rememberLineShimmerState()

    CustomContentCard(
        title = stringResource(CoreUiR.string.description_title),
        titleAction = {
            DescriptionTitleAction(
                translation = translation,
                onTranslateClick = onTranslateClick,
                onShowOriginalClick = onShowOriginalClick
            )
        },
        modifier = modifier
    ) {
        Column {
            Text(
                text = displayedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                onTextLayout = { shimmerState.updateLayout(it) },
                modifier = Modifier
                    .lineShimmer(state = shimmerState, visible = isLoading)
                    .then(if (isLoading) Modifier.clearAndSetSemantics { } else Modifier)
            )

            if (translation is DescriptionTranslationState.Ready) {
                TranslatedOnDeviceBadge(
                    modifier = Modifier
                        .padding(top = MaterialTheme.spacing.mediumLarge)
                        .align(Alignment.Start)
                )
            }
        }
    }
}

/**
 * The icon shown next to the card title for the current [translation] state, or nothing while
 * [DescriptionTranslationState.Unavailable]. Retrying a [DescriptionTranslationState.Failed]
 * attempt reuses [onTranslateClick] — from the caller's side a retry is just another translate
 * request, the failure is only a display concern here.
 */
@Composable
private fun DescriptionTitleAction(
    translation: DescriptionTranslationState,
    onTranslateClick: () -> Unit,
    onShowOriginalClick: () -> Unit
) {
    when (translation) {
        DescriptionTranslationState.Unavailable -> Unit

        DescriptionTranslationState.Available -> {
            DescriptionTitleIconButton(
                icon = Icons.Outlined.Translate,
                contentDescription = stringResource(R.string.description_translate_action),
                onClick = onTranslateClick
            )
        }

        DescriptionTranslationState.InProgress -> {
            val translatingDescription = stringResource(R.string.description_translating)
            CircularProgressIndicator(
                modifier = Modifier
                    .size(TranslationProgressIndicatorTouchTarget)
                    .padding(MaterialTheme.spacing.medium)
                    .semantics { contentDescription = translatingDescription },
                strokeWidth = TranslationProgressIndicatorStrokeWidth
            )
        }

        is DescriptionTranslationState.Ready -> {
            DescriptionTitleIconButton(
                icon = Icons.Filled.Translate,
                contentDescription = stringResource(R.string.description_show_original),
                tint = MaterialTheme.colorScheme.primary,
                onClick = onShowOriginalClick
            )
        }

        DescriptionTranslationState.Failed -> {
            DescriptionTitleIconButton(
                icon = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.description_retry_translation_action),
                onClick = onTranslateClick
            )
        }
    }
}

@Composable
private fun TranslatedOnDeviceBadge(modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = modifier
            .clip(shape)
            .rainbowMetallicBorder(width = 2.dp, shape = shape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Text(
            text = stringResource(R.string.description_translated_on_device),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Composable
private fun DescriptionTitleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDescriptionCardAvailablePreview() {
    GamesWishlistTheme {
        GameDescriptionCard(
            description = "This is a long description that showcases how the card grows to fit its " +
                    "full content. There is no expand/collapse here — the whole text is always shown, " +
                    "however long it is.",
            translation = DescriptionTranslationState.Available,
            onTranslateClick = {},
            onShowOriginalClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDescriptionCardLoadingPreview() {
    GamesWishlistTheme {
        GameDescriptionCard(
            description = "This is a long description that showcases how the card grows to fit its " +
                    "full content.",
            translation = DescriptionTranslationState.InProgress,
            onTranslateClick = {},
            onShowOriginalClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDescriptionCardTranslatedPreview() {
    GamesWishlistTheme {
        GameDescriptionCard(
            description = "This is a long description that showcases how the card grows to fit its " +
                    "full content.",
            translation = DescriptionTranslationState.Ready(
                "Questa è una descrizione lunga che mostra come la card cresca per adattarsi al " +
                        "contenuto completo."
            ),
            onTranslateClick = {},
            onShowOriginalClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDescriptionCardFailedPreview() {
    GamesWishlistTheme {
        GameDescriptionCard(
            description = "This is a long description that showcases how the card grows to fit its " +
                    "full content.",
            translation = DescriptionTranslationState.Failed,
            onTranslateClick = {},
            onShowOriginalClick = {}
        )
    }
}
