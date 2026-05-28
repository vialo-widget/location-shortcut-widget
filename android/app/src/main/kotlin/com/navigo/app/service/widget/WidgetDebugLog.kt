package com.navigo.app.service.widget

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tiny in-app event log for diagnosing widget binding/render problems
 * without adb. The launcher's widget host runs in our process when it
 * binds the RemoteViewsService, so events from the provider, service,
 * and factory all end up in the same SharedPreferences file — visible
 * to the foreground app in Settings → Diagnostics.
 *
 * Capped at [MAX_EVENTS] lines (newest first) so it can't grow without
 * bound. A small `synchronized` block keeps the provider / service /
 * factory / mirror from shredding each other's writes.
 *
 * Temporary debugging surface — remove this and its Settings card once
 * the widget pipeline is solid.
 */
object WidgetDebugLog {

    private const val PREFS = "widget_debug_log"
    private const val KEY = "events"
    private const val MAX_EVENTS = 80
    private const val TAG = "NaviGoWidget"

    private val timestampFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val lock = Any()

    fun log(context: Context, message: String) {
        Log.d(TAG, message)
        synchronized(lock) {
            val prefs = context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val existing = prefs.getString(KEY, "") ?: ""
            val stamped = "${timestampFmt.format(Date())} $message"
            val combined = if (existing.isEmpty()) stamped else "$stamped\n$existing"
            val trimmed = combined
                .lineSequence()
                .filter { it.isNotBlank() }
                .take(MAX_EVENTS)
                .joinToString("\n")
            prefs.edit().putString(KEY, trimmed).apply()
        }
    }

    fun read(context: Context): String =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "")
            .orEmpty()

    fun clear(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .apply()
    }
}
