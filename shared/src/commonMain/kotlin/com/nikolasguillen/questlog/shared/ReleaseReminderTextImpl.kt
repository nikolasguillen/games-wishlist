package com.nikolasguillen.questlog.shared

import com.nikolasguillen.questlog.core.data.notification.ReleaseReminderText
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes
import com.nikolasguillen.questlog.core.ui.resources.release_notification_body
import com.nikolasguillen.questlog.core.ui.resources.release_notification_title_format
import org.jetbrains.compose.resources.getString

/**
 * The release reminder's words, read from `:core:ui`'s Compose resources. Lives here because `:core:data` has no
 * Compose and `:shared` already depends on every module.
 */
internal class ReleaseReminderTextImpl : ReleaseReminderText {
    override suspend fun title(gameName: String): String =
        getString(CoreUiRes.string.release_notification_title_format, gameName)

    override suspend fun body(): String = getString(CoreUiRes.string.release_notification_body)
}
