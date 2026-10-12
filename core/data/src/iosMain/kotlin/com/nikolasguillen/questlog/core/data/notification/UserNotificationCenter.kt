package com.nikolasguillen.questlog.core.data.notification

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.number
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.time.Instant

private const val IDENTIFIER_PREFIX = "release-reminder-"
private const val GAME_ID_KEY = "gameId"

/** How soon a reminder that is already due is delivered: a calendar trigger in the past would never fire. */
private const val IMMEDIATE_DELAY_SECONDS = 1.0

/**
 * [LocalNotificationCenter] over `UNUserNotificationCenter`. Every request has the identifier
 * `release-reminder-<gameId>`, so adding one for a game replaces the pending one, and carries the game id in its
 * `userInfo` for the tap handler.
 *
 * Failures never escape: a reminder that could not be armed is reported as `false`, and the next sync tries again.
 */
@OptIn(ExperimentalForeignApi::class)
internal class UserNotificationCenter : LocalNotificationCenter {

    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun add(request: LocalReminderRequest): Boolean {
        // The system accepts a request from an app that may not notify and then never delivers it, so an
        // unauthorized app has to be turned away here, or the ledger would claim a reminder that cannot arrive.
        if (!isAuthorized()) return false
        return try {
            suspendCancellableCoroutine { continuation ->
                center.addNotificationRequest(request.toNotificationRequest()) { error ->
                    continuation.resume(error == null)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun pendingReminders(): List<PendingReminder> = try {
        suspendCancellableCoroutine { continuation ->
            center.getPendingNotificationRequestsWithCompletionHandler { requests ->
                continuation.resume(requests.orEmpty().filterIsInstance<UNNotificationRequest>().mapNotNull { it.toPending() })
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun remove(gameIds: Collection<Int>) {
        if (gameIds.isEmpty()) return
        center.removePendingNotificationRequestsWithIdentifiers(gameIds.map { identifier(it) })
    }

    private suspend fun isAuthorized(): Boolean = suspendCancellableCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            continuation.resume(
                status == UNAuthorizationStatusAuthorized ||
                    status == UNAuthorizationStatusProvisional ||
                    status == UNAuthorizationStatusEphemeral
            )
        }
    }

    private fun LocalReminderRequest.toNotificationRequest(): UNNotificationRequest {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
            setUserInfo(mapOf<Any?, Any?>(GAME_ID_KEY to gameId))
        }
        val trigger = if (fireAt == null) {
            UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(IMMEDIATE_DELAY_SECONDS, repeats = false)
        } else {
            // Calendar components with no time zone: the 09:00 of the release day is the device's 09:00, wherever
            // the device is when it comes. The calendar is pinned to Gregorian because the components are.
            val components = NSDateComponents().apply {
                calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
                year = fireAt.year.toLong()
                month = fireAt.month.number.toLong()
                day = fireAt.day.toLong()
                hour = fireAt.hour.toLong()
                minute = fireAt.minute.toLong()
            }
            UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
        }
        return UNNotificationRequest.requestWithIdentifier(identifier(gameId), content, trigger)
    }

    private fun UNNotificationRequest.toPending(): PendingReminder? {
        val gameId = identifier.removePrefix(IDENTIFIER_PREFIX).takeIf { identifier.startsWith(IDENTIFIER_PREFIX) }
            ?.toIntOrNull() ?: return null
        val nextTriggerAt = (trigger as? UNCalendarNotificationTrigger)?.nextTriggerDate()?.toInstant()
        return PendingReminder(gameId, nextTriggerAt)
    }

    private fun identifier(gameId: Int) = "$IDENTIFIER_PREFIX$gameId"

    private fun NSDate.toInstant(): Instant = Instant.fromEpochMilliseconds((timeIntervalSince1970 * 1000).toLong())
}
