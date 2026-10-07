package com.nikolasguillen.questlog.feature.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.ControllerLoadingAnimation
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.util.modifiers.metallicBorder
import com.nikolasguillen.questlog.core.ui.util.modifiers.rememberCoverBrush
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingInfoPageUiModel

/**
 * One informational page: an illustration, a headline and a short body.
 *
 * The column scrolls, so a large font size or a short screen never hides the text; the illustration is
 * sized as a fraction of the width rather than in dp, so it is the thing that gives way first.
 *
 * @param actions Optional content below the body — the reminders page puts its two buttons here.
 */
@Composable
internal fun OnboardingInfoPage(
    page: OnboardingInfoPageUiModel,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    val shape = MaterialTheme.shapes.extraLarge

    // fillMaxSize before verticalScroll keeps the column at least as tall as the page, which is what lets
    // Arrangement.Center centre short content while still scrolling tall content.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.extraLarge, vertical = spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.extraLarge, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(ILLUSTRATION_WIDTH_FRACTION)
                .aspectRatio(1f)
                .clip(shape)
                .background(rememberCoverBrush())
                .metallicBorder(shape = shape),
            contentAlignment = Alignment.Center
        ) {
            val icon = page.icon
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize(ICON_FRACTION)
                )
            } else {
                // Purely decorative here: nothing is loading, so it carries no description.
                ControllerLoadingAnimation(modifier = Modifier.fillMaxSize(ICON_FRACTION))
            }
        }
        Text(
            text = page.headline.asString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = page.body.asString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        actions()
    }
}

private const val ILLUSTRATION_WIDTH_FRACTION = 0.55f
private const val ICON_FRACTION = 0.5f

@QuestLogPreviews
@Composable
private fun OnboardingInfoPagePreview() {
    QuestLogTheme {
        OnboardingInfoPage(
            page = OnboardingInfoPageUiModel(
                headline = UiText.DynamicString("Find your next game"),
                body = UiText.DynamicString("Search the whole catalogue, or let Discover suggest games."),
                icon = Icons.Default.Search
            )
        )
    }
}
