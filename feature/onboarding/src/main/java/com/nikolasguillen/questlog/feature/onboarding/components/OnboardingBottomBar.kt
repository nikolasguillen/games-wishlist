package com.nikolasguillen.questlog.feature.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.CustomPagerIndicator
import com.nikolasguillen.questlog.feature.onboarding.R

/**
 * Back, the position indicator and Next — which becomes "Get started" on the last page.
 *
 * The side slots share the width equally, so the indicator stays centred whether or not Back is shown.
 */
@Composable
internal fun OnboardingBottomBar(
    pagerState: PagerState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val isFirstPage = pagerState.currentPage == 0
    val isLastPage = pagerState.currentPage == pagerState.pageCount - 1
    val positionDescription = stringResource(
        R.string.onboarding_page_position,
        pagerState.currentPage + 1,
        pagerState.pageCount
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.large, vertical = spacing.large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (!isFirstPage) {
                TextButton(onClick = onBack) {
                    Text(text = stringResource(R.string.onboarding_back))
                }
            }
        }
        CustomPagerIndicator(
            pagerState = pagerState,
            currentIndicatorColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.semantics { contentDescription = positionDescription }
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Button(onClick = if (isLastPage) onFinish else onNext) {
                Text(
                    text = stringResource(
                        if (isLastPage) R.string.onboarding_get_started else R.string.onboarding_next
                    )
                )
            }
        }
    }
}

@QuestLogPreviews
@Composable
private fun OnboardingBottomBarPreview() {
    QuestLogTheme {
        OnboardingBottomBar(
            pagerState = rememberPagerState(initialPage = 1) { 4 },
            onBack = {},
            onNext = {},
            onFinish = {}
        )
    }
}
