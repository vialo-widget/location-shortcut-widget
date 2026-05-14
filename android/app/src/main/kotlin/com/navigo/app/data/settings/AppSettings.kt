package com.navigo.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Lightweight wrapper around a DataStore<Preferences> for user-facing app
 * settings and one-shot migration flags.
 *
 * Widget rendering preferences (e.g. `widget_style`) intentionally live in the
 * legacy `HomeWidgetPreferences` SharedPreferences instead — the [com.navigo.app.ShortcutWidgetProvider]
 * reads them there, and using a shared file means the widget keeps working
 * without an extra IPC layer.
 */
class AppSettings(private val context: Context) {

    private val store: DataStore<Preferences> get() = context.appSettingsStore

    val importedFromWidgetPrefs: Flow<Boolean> =
        store.data.map { it[Keys.ImportedFromWidgetPrefs] ?: false }

    suspend fun markImportedFromWidgetPrefs() {
        store.edit { it[Keys.ImportedFromWidgetPrefs] = true }
    }

    private object Keys {
        val ImportedFromWidgetPrefs = booleanPreferencesKey("imported_from_widget_prefs")
    }

    private companion object {
        private val Context.appSettingsStore: DataStore<Preferences> by preferencesDataStore(
            name = "app_settings",
        )
    }
}
