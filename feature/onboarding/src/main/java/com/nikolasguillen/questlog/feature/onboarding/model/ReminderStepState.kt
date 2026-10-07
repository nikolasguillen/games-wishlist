package com.nikolasguillen.questlog.feature.onboarding.model

import androidx.compose.runtime.Immutable

/**
 * Where the reminders page is. Not persisted: the flow starts over on a new process, so the step starts
 * [Undecided] again — which is right, because nothing was decided that outlives the flow.
 */
@Immutable
internal sealed interface ReminderStepState {
    /** Nothing asked yet: the page explains why reminders are useful and offers to allow them. */
    data object Undecided : ReminderStepState

    /** The system permission was granted. */
    data object Granted : ReminderStepState

    /** Declined with "Not now", or the system request was denied — including the case where it cannot be shown. */
    data object Declined : ReminderStepState
}
