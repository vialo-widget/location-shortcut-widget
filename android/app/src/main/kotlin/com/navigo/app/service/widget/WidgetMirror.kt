package com.navigo.app.service.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.navigo.app.ShortcutWidgetProvider
import com.navigo.app.data.model.Shortcut
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bridges the Room-backed [com.navigo.app.data.repo.ShortcutRepository] to the
 * legacy `SharedPreferences("HomeWidgetPreferences")` file that
 * [ShortcutWidgetProvider] reads from.
 *
 * Same JSON shape the Flutter `home_widget` package used, so any widget pinned
 * before the rewrite keeps rendering through the upgrade — and the new app
 * adds + edits propagate live by calling [mirrorShortcuts] after every DB
 * change (see [com.navigo.app.NaviGoApplication.onCreate]).
 *
 * The widget shows at most [MAX_WIDGET_SHORTCUTS] tiles; we trim here rather
 * than push the full list across.
 */
class WidgetMirror(private val context: Context) {

    fun mirrorShortcuts(shortcuts: List<Shortcut>) {
        val limited = shortcuts
            .sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
            .take(MAX_WIDGET_SHORTCUTS)
        val json = JSONArray()
        limited.forEach { json.put(it.toWidgetJson()) }

        prefs().edit()
            .putString(KEY_SHORTCUTS_JSON, json.toString())
            .apply()
        broadcastUpdate()
    }

    /** Currently-stored widget style — `frostedGlass` (default) or `boldColors`. */
    fun getWidgetStyle(): String =
        prefs().getString(KEY_WIDGET_STYLE, DEFAULT_WIDGET_STYLE) ?: DEFAULT_WIDGET_STYLE

    fun setWidgetStyle(style: String) {
        prefs().edit().putString(KEY_WIDGET_STYLE, style).apply()
        broadcastUpdate()
    }

    private fun prefs() =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private fun broadcastUpdate() {
        val component = ComponentName(context, ShortcutWidgetProvider::class.java)
        val widgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(component)
        if (widgetIds.isEmpty()) return
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            this.component = component
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        }
        context.sendBroadcast(intent)
    }

    private fun Shortcut.toWidgetJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("label", label)
        put("address", address)
        put("latitude", latitude)
        put("longitude", longitude)
        put("placeId", placeId)
        put("iconName", iconName)
        put("sortOrder", sortOrder)
    }

    companion object {
        /** Mirror up to a 6×2 = 12-tile bold grid; the glass widget still
         *  only paints the first six but tolerates extras in the JSON. */
        const val MAX_WIDGET_SHORTCUTS = 12
        const val STYLE_FROSTED_GLASS = "frostedGlass"
        const val STYLE_BOLD_COLORS = "boldColors"

        private const val PREFS_FILE = "HomeWidgetPreferences"
        private const val KEY_SHORTCUTS_JSON = "shortcuts_json"
        private const val KEY_WIDGET_STYLE = "widget_style"
        private const val DEFAULT_WIDGET_STYLE = STYLE_FROSTED_GLASS
    }
}
