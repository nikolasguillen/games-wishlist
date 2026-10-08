package com.nikolasguillen.questlog.core.domain.notification

/**
 * Whether the platform offers release reminders at all. ViewModels read it to leave every reminder entry
 * point out of their state on a platform without them, so a screen never shows a control that cannot work.
 * It is fixed for the life of the process.
 */
interface ReleaseRemindersAvailability {
    val isAvailable: Boolean
}
