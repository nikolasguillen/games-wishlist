package com.nikolasguillen.questlog.shared

import platform.Foundation.NSNumber
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

private const val GAME_ID_KEY = "gameId"

/**
 * The notification centre's delegate: it shows a reminder that arrives while the app is open, and turns a tap on one
 * into a game to open. The centre holds its delegate weakly, so `startQuestLog` keeps the instance alive.
 */
internal class ReleaseReminderNotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {

    // Without this the system swallows a notification that arrives with the app in the foreground.
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or
                UNNotificationPresentationOptionList or
                UNNotificationPresentationOptionSound
        )
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit
    ) {
        val userInfo = didReceiveNotificationResponse.notification.request.content.userInfo
        // No lookup of the game: one that is no longer saved opens its detail screen like any other.
        (userInfo[GAME_ID_KEY] as? NSNumber)?.intValue?.let(ReminderDeepLinks::emit)
        withCompletionHandler()
    }
}
