package com.nikolasguillen.questlog.feature.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.PlatformPickerList
import com.nikolasguillen.questlog.core.ui.component.PlatformSearchField
import com.nikolasguillen.questlog.core.ui.model.PlatformPickerContentState
import com.nikolasguillen.questlog.core.ui.model.PlatformPickerItemUiModel
import com.nikolasguillen.questlog.feature.onboarding.resources.Res
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_platforms_body
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_platforms_filtering
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_platforms_headline
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_platforms_none_selected
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The owned-platforms step. The headline and the search field are pinned, in that order; the body and the
 * caption are the list's first items in `Success`, so at a large font size they scroll away instead of
 * leaving the rows no room. The other states have nothing long to scroll, so the same text is drawn
 * statically above their message.
 *
 * The body, which says the choice can be made later in Settings, is on screen in every state — including
 * the offline one, where it is what tells the user they are not stuck.
 */
@Composable
internal fun OnboardingPlatformsPage(
    pickerState: PlatformPickerContentState,
    selectedCount: Int,
    searchFieldState: TextFieldState,
    onToggle: (platformId: Int) -> Unit,
    onClearQuery: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val intro: @Composable () -> Unit = { PlatformsIntro(selectedCount = selectedCount) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(Res.string.onboarding_platforms_headline),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = spacing.extraLarge)
                .padding(top = spacing.large, bottom = spacing.medium)
                .semantics { heading() }
        )
        PlatformSearchField(
            state = searchFieldState,
            onClearQuery = onClearQuery,
            modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.medium)
        )
        if (pickerState is PlatformPickerContentState.Success) {
            PlatformPickerList(
                state = pickerState,
                onToggle = onToggle,
                onClearQuery = onClearQuery,
                onRetry = onRetry,
                header = intro,
                modifier = Modifier.weight(1f)
            )
        } else {
            intro()
            PlatformPickerList(
                state = pickerState,
                onToggle = onToggle,
                onClearQuery = onClearQuery,
                onRetry = onRetry,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PlatformsIntro(selectedCount: Int, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.extraLarge, vertical = spacing.large)
    ) {
        Text(
            text = stringResource(Res.string.onboarding_platforms_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (selectedCount == 0) {
                stringResource(Res.string.onboarding_platforms_none_selected)
            } else {
                pluralStringResource(Res.plurals.onboarding_platforms_filtering, selectedCount, selectedCount)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val previewPlatforms = listOf(
    PlatformPickerItemUiModel(id = 167, name = "PlayStation 5", abbreviation = "PS5", isSelected = true),
    PlatformPickerItemUiModel(id = 6, name = "PC (Microsoft Windows)", abbreviation = "PC", isSelected = false),
    PlatformPickerItemUiModel(id = 130, name = "Nintendo Switch", abbreviation = "Switch", isSelected = false)
)

@QuestLogPreviews
@Composable
private fun OnboardingPlatformsPageSuccessPreview() {
    QuestLogTheme {
        OnboardingPlatformsPage(
            pickerState = PlatformPickerContentState.Success(previewPlatforms),
            selectedCount = 1,
            searchFieldState = TextFieldState(),
            onToggle = {},
            onClearQuery = {},
            onRetry = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun OnboardingPlatformsPageEmptyPreview() {
    QuestLogTheme {
        OnboardingPlatformsPage(
            pickerState = PlatformPickerContentState.Empty,
            selectedCount = 0,
            searchFieldState = TextFieldState(),
            onToggle = {},
            onClearQuery = {},
            onRetry = {}
        )
    }
}
