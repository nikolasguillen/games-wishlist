package com.nikolasguillen.questlog.core.ui.util

/**
 * Where the app stands with the system notification permission on iOS. Public so that `:shared` can tell when it
 * becomes [AUTHORIZED].
 */
enum class NotificationAuthorizationStatus {
    /** The system has not asked yet, so the prompt can still be shown. */
    NOT_DETERMINED,

    /** The user said no, or turned notifications off in Settings. Only Settings can undo it. */
    DENIED,

    /** Notifications can be delivered (including provisional and ephemeral authorization). */
    AUTHORIZED
}
