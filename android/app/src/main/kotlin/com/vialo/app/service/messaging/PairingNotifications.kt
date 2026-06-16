package com.vialo.app.service.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vialo.app.MainActivity
import com.vialo.app.R

/**
 * Builds and shows the local notifications surfaced from FCM events.
 *
 * Two flavors:
 *   - [pairInvite] is the actionable one — tapping it opens the
 *     AcceptInviteScreen for that specific pendingId via deep link
 *     extras that MainActivity reads on resume.
 *   - [simple] is informational (pair accepted/rejected/ended, carer
 *     renamed) — tapping it just opens the app.
 *
 * One channel per event flavor so users can tune them in system
 * Settings → Notifications without an all-or-nothing toggle.
 */
class PairingNotifications(private val context: Context) {

    init {
        ensureChannels()
    }

    fun pairInvite(pendingId: String, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TARGET, NAV_ACCEPT_INVITE)
            putExtra(EXTRA_PENDING_ID, pendingId)
        }
        val pending = PendingIntent.getActivity(
            context, pendingId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_PAIR_INVITE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .build()
        NotificationManagerCompat.from(context).safelyNotify(pendingId.hashCode(), notification)
    }

    fun simple(channelType: String, title: String, body: String) {
        val channel = when (channelType) {
            "pair_accepted" -> CHANNEL_PAIR_UPDATES
            "pair_rejected" -> CHANNEL_PAIR_UPDATES
            "pair_ended" -> CHANNEL_PAIR_UPDATES
            "carer_renamed" -> CHANNEL_PAIR_UPDATES
            else -> CHANNEL_PAIR_UPDATES
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, channelType.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).safelyNotify(channelType.hashCode(), notification)
    }

    private fun ensureChannels() {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_PAIR_INVITE) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PAIR_INVITE,
                    "Helper requests",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "When someone asks to help manage your Vialo."
                },
            )
        }
        if (nm.getNotificationChannel(CHANNEL_PAIR_UPDATES) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PAIR_UPDATES,
                    "Helper updates",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Status changes from your helpers or carees."
                },
            )
        }
    }

    /** Wrap notify() so a missing POST_NOTIFICATIONS permission is silent
     *  rather than a crash. On API 33+ the permission is runtime-only. */
    private fun NotificationManagerCompat.safelyNotify(id: Int, notification: android.app.Notification) {
        try {
            notify(id, notification)
        } catch (_: SecurityException) {
            // permission not granted; nothing we can do from here
        }
    }

    companion object {
        const val CHANNEL_PAIR_INVITE = "vialo_pair_invite"
        const val CHANNEL_PAIR_UPDATES = "vialo_pair_updates"

        // MainActivity reads these to navigate after the intent fires.
        const val EXTRA_NAV_TARGET = "nav_target"
        const val EXTRA_PENDING_ID = "pending_id"
        const val NAV_ACCEPT_INVITE = "accept_invite"
    }
}
