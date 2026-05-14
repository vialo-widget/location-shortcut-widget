package com.navigo.app.service.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.navigo.app.R
import java.util.concurrent.TimeUnit

/**
 * Background worker that fires a single expiry-warning notification for a
 * shortcut. Enqueued via [ExpiryNotifier.schedule] — never instantiated
 * directly by the app.
 *
 * Recomputes `daysLeft` at run time (rather than at schedule time) so the
 * copy still makes sense if WorkManager runs us a little late under Doze.
 */
class ExpiryNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val shortcutId = inputData.getString(KEY_SHORTCUT_ID) ?: return Result.failure()
        val label = inputData.getString(KEY_LABEL) ?: return Result.failure()
        val expiresAtMillis = inputData.getLong(KEY_EXPIRES_AT_MILLIS, -1L)
        if (expiresAtMillis < 0L) return Result.failure()

        val now = System.currentTimeMillis()
        if (expiresAtMillis <= now) {
            // Already expired before we got a chance to warn — skip silently.
            return Result.success()
        }
        val daysLeft = ((expiresAtMillis - now) / TimeUnit.DAYS.toMillis(1)).toInt()
        val body = if (daysLeft <= 1) {
            "\"$label\" expires today — navigate before it's gone."
        } else {
            "\"$label\" expires in $daysLeft days."
        }

        show(applicationContext, shortcutId, body)
        return Result.success()
    }

    private fun show(context: Context, shortcutId: String, body: String) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val notifId = shortcutId.hashCode() and Int.MAX_VALUE
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context, notifId, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val notification = NotificationCompat.Builder(context, ExpiryNotifier.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Shortcut expiring soon")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .apply { pendingIntent?.let { setContentIntent(it) } }
            .setAutoCancel(true)
            .build()
        try {
            nm.notify(notifId, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS denied on Android 13+ — swallow rather than crash.
        }
    }

    companion object {
        const val KEY_SHORTCUT_ID = "shortcutId"
        const val KEY_LABEL = "label"
        const val KEY_EXPIRES_AT_MILLIS = "expiresAtMillis"
    }
}
