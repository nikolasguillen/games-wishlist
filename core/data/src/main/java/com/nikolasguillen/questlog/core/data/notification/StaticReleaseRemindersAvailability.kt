package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.domain.notification.ReleaseRemindersAvailability

/** A [ReleaseRemindersAvailability] decided once, by whichever platform module binds it. */
class StaticReleaseRemindersAvailability(override val isAvailable: Boolean) : ReleaseRemindersAvailability
