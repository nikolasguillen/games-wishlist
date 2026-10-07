package com.nikolasguillen.questlog.feature.onboarding.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.feature.onboarding.R
import com.nikolasguillen.questlog.feature.onboarding.mapper.toInfoUiModel
import com.nikolasguillen.questlog.feature.onboarding.model.ReminderStepState

/**
 * The reminders page. It explains why before it asks: the two actions exist only while the step is
 * [ReminderStepState.Undecided], and once it is decided the page is just text, because the bottom bar's
 * "Get started" is what ends the flow. Declining is therefore never a dead end.
 */
@Composable
internal fun OnboardingRemindersPage(
    step: ReminderStepState,
    onAllowClick: () -> Unit,
    onNotNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingInfoPage(page = step.toInfoUiModel(), modifier = modifier) {
        if (step == ReminderStepState.Undecided) {
            Button(onClick = onAllowClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.onboarding_reminders_allow))
            }
            TextButton(
                onClick = onNotNowClick,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.appColors.textOnSurface)
            ) {
                Text(text = stringResource(R.string.onboarding_reminders_not_now))
            }
        }
    }
}

@QuestLogPreviews
@Composable
private fun OnboardingRemindersPageUndecidedPreview() {
    QuestLogTheme {
        OnboardingRemindersPage(ReminderStepState.Undecided, onAllowClick = {}, onNotNowClick = {})
    }
}

@QuestLogPreviews
@Composable
private fun OnboardingRemindersPageGrantedPreview() {
    QuestLogTheme {
        OnboardingRemindersPage(ReminderStepState.Granted, onAllowClick = {}, onNotNowClick = {})
    }
}

@QuestLogPreviews
@Composable
private fun OnboardingRemindersPageDeclinedPreview() {
    QuestLogTheme {
        OnboardingRemindersPage(ReminderStepState.Declined, onAllowClick = {}, onNotNowClick = {})
    }
}
