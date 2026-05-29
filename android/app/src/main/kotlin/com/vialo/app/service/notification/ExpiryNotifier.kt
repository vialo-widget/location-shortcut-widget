package com.vialo.app.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vialo.app.data.model.ExpiryOption
import com.vialo.app.data.model.Shortcut
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Schedules and cancels expiry-warning notifications via WorkManager.
 *
 * Each shortcut owns a single uniquely-named work entry (`expiry-<shortcutId>`)
 * so re-scheduling on update simply replaces the previous request, and
 * canceling on delete drops it cleanly.
 *
 * Warning timing matches the Flutter build: the chosen [ExpiryOption] dictates
 * how many days before the deadline the notification fires (1 day ahead for
 * 3-day shortcuts, 30 days ahead for yearly ones, etc.).
 */
class ExpiryNotifier(private val context: Context) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    init {
        ensureChannel()
    }

    fun schedule(shortcut: Shortcut) {
        val expiresAt = shortcut.expiresAt ?: run {
            // No expiry — make sure any old request is cleared.
            cancel(shortcut.id)
            return
        }
        val option = ExpiryOption.infer(expiresAt, shortcut.createdAt)
        if (option == ExpiryOption.NEVER) return

        val warningTime = expiresAt.minus(Duration.ofDays(option.warningDays.toLong()))
        val now = Instant.now()
        if (!warningTime.isAfter(now)) {
            // Window has already closed — no notification to schedule.
            return
        }

        val delayMillis = Duration.between(now, warningTime).toMillis()
        val request = OneTimeWorkRequestBuilder<ExpiryNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ExpiryNotificationWorker.KEY_SHORTCUT_ID to shortcut.id,
                    ExpiryNotificationWorker.KEY_LABEL to shortcut.label,
                    ExpiryNotificationWorker.KEY_EXPIRES_AT_MILLIS to expiresAt.toEpochMilli(),
                ),
            )
            .addTag(TAG_ALL)
            .build()

        workManager.enqueueUniqueWork(uniqueName(shortcut.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(shortcutId: String) {
        workManager.cancelUniqueWork(uniqueName(shortcutId))
    }

    fun cancelAll() {
        workManager.cancelAllWorkByTag(TAG_ALL)
    }

    private fun uniqueName(shortcutId: String) = "expiry-$shortcutId"

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = CHANNEL_DESCRIPTION },
        )
    }

    companion object {
        const val CHANNEL_ID = "vialo_expiry"
        const val CHANNEL_NAME = "Expiry warnings"
        const val CHANNEL_DESCRIPTION =
            "Notifies you when a saved location is about to expire"
        private const val TAG_ALL = "vialo-expiry"
    }
}
