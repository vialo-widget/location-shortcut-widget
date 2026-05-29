package com.vialo.app.data.migration

import android.content.Context
import android.util.Log
import com.vialo.app.data.model.Shortcut
import com.vialo.app.data.repo.ShortcutRepository
import com.vialo.app.data.settings.AppSettings
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import java.time.Instant

/**
 * One-shot importer that lifts existing users' shortcuts out of the legacy
 * `HomeWidgetPreferences` SharedPreferences (written by the Flutter build via
 * the `home_widget` package) into Room.
 *
 * The widget JSON only stores the subset of fields the home-screen widget
 * needs (no `createdAt` / `expiresAt`), so migrated rows get `createdAt = now`
 * and `expiresAt = null`. That's lossy but acceptable: the lossless source
 * lives in Hive's binary format which is impractical to read from Kotlin,
 * and the SharedPreferences JSON is what the widget already mirrors live.
 *
 * Guarded by [AppSettings.importedFromWidgetPrefs] so it runs at most once.
 */
class WidgetPrefsImporter(
    private val context: Context,
    private val repository: ShortcutRepository,
    private val settings: AppSettings,
) {

    suspend fun migrateIfNeeded() {
        if (settings.importedFromWidgetPrefs.first()) return

        try {
            val raw = legacyPrefs().getString(SHORTCUTS_JSON_KEY, null)
            if (!raw.isNullOrBlank() && raw != "[]") {
                val parsed = parse(raw, Instant.now())
                if (parsed.isNotEmpty()) {
                    repository.addAll(parsed)
                    Log.i(TAG, "Imported ${parsed.size} shortcut(s) from legacy widget prefs")
                }
            }
        } catch (e: Exception) {
            // Don't block future launches on a parse failure — mark migrated.
            Log.e(TAG, "Legacy widget-prefs import failed; marking migrated to avoid retry loop", e)
        }

        settings.markImportedFromWidgetPrefs()
    }

    private fun legacyPrefs() =
        context.getSharedPreferences(LEGACY_PREFS_FILE, Context.MODE_PRIVATE)

    private fun parse(json: String, now: Instant): List<Shortcut> {
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            Shortcut(
                id = obj.optString("id").ifBlank { Shortcut.newId() },
                label = obj.optString("label", "Place"),
                address = obj.optString("address", ""),
                latitude = obj.optDouble("latitude", 0.0),
                longitude = obj.optDouble("longitude", 0.0),
                placeId = obj.optString("placeId", ""),
                iconName = obj.optString("iconName", "place"),
                sortOrder = obj.optInt("sortOrder", i),
                createdAt = now,
                expiresAt = null,
            )
        }
    }

    private companion object {
        const val LEGACY_PREFS_FILE = "HomeWidgetPreferences"
        const val SHORTCUTS_JSON_KEY = "shortcuts_json"
        const val TAG = "WidgetPrefsImporter"
    }
}
