package com.nikolasguillen.questlog.core.ui.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenNotificationSettingsURLString

/** Opens the app's own page in the system's notification settings (iOS 16 and later, the deployment target). */
internal fun openAppNotificationSettings() {
    val url = NSURL.URLWithString(UIApplicationOpenNotificationSettingsURLString) ?: return
    UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
}
