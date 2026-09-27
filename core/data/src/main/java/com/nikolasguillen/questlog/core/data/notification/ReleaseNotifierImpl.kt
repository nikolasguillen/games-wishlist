package com.nikolasguillen.questlog.core.data.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nikolasguillen.questlog.core.data.R
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val CHANNEL_ID = "release_notifications"

class ReleaseNotifierImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReleaseNotifier {

    override fun canDeliver(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    @SuppressLint("MissingPermission")
    override fun notifyReleased(gameId: Int, gameName: String) {
        val notificationManager = NotificationManagerCompat.from(context)
        ensureChannel(notificationManager)

        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse("questlog://game/$gameId"))
        val pendingIntent = PendingIntent.getActivity(
            context,
            gameId,
            deepLinkIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.release_notification_title_format, gameName))
            .setContentText(context.getString(R.string.release_notification_body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // gameId as the notification id: a second reminder for the same game replaces rather than stacks.
        notificationManager.notify(gameId, notification)
    }

    private fun ensureChannel(notificationManager: NotificationManagerCompat) {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.release_notification_channel_name))
            .setDescription(context.getString(R.string.release_notification_channel_description))
            .build()
        notificationManager.createNotificationChannel(channel)
    }
}
