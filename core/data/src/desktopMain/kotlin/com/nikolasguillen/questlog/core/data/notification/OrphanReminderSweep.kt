package com.nikolasguillen.questlog.core.data.notification

/**
 * Removes every pending reminder the [ledger] does not know. A reminder survives an uninstall (the operating system
 * keeps it, and nothing of the app runs to remove it), while the ledger does not, so after a reinstall the pending
 * set can hold reminders for games the app no longer tracks. The launch sync re-arms the ones that still count.
 */
internal suspend fun sweepOrphanReminders(center: LocalNotificationCenter, ledger: ArmedReminderLedger) {
    val armed = ledger.entries().keys
    val orphans = center.pendingReminders().map { it.gameId }.filter { it !in armed }
    if (orphans.isNotEmpty()) center.remove(orphans)
}
