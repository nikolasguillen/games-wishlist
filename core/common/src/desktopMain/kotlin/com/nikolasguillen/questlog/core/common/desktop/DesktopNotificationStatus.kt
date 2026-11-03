package com.nikolasguillen.questlog.core.common.desktop

/** Whether the operating system lets QuestLog show notifications. What each value means per OS is in data-model §2. */
enum class DesktopNotificationStatus {
    /** The system has not been asked yet (macOS only). */
    NOT_DETERMINED,
    AUTHORIZED,
    DENIED,

    /** There is no way to know or to notify: not running from an app bundle on macOS, or a failed native call. */
    UNSUPPORTED
}
