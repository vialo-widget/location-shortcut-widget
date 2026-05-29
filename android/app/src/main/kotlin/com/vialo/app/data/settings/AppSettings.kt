package com.vialo.app.data.settings

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
 * Widget shortcut data intentionally lives in the legacy
 * `HomeWidgetPreferences` SharedPreferences instead — the
 * [com.vialo.app.ShortcutWidgetProvider] reads it there, and using a shared
 * file means the widget keeps working without an extra IPC layer.
 */
class AppSettings(private val context: Context) {

    private val store: DataStore<Preferences> get() = context.appSettingsStore

    val importedFromWidgetPrefs: Flow<Boolean> =
        store.data.map { it[Keys.ImportedFromWidgetPrefs] ?: false }

    suspend fun markImportedFromWidgetPrefs() {
        store.edit { it[Keys.ImportedFromWidgetPrefs] = true }
    }

    /** Has the user already been through the first-launch onboarding screen
     *  (where we ask for notification + location permission and offer to pin
     *  the widget)? Gates whether [com.vialo.app.ui.VialoApp] starts on
     *  the Home destination or the Onboarding destination. */
    val hasSeenOnboarding: Flow<Boolean> =
        store.data.map { it[Keys.HasSeenOnboarding] ?: false }

    suspend fun markOnboardingSeen() {
        store.edit { it[Keys.HasSeenOnboarding] = true }
    }

    private object Keys {
        val ImportedFromWidgetPrefs = booleanPreferencesKey("imported_from_widget_prefs")
        val HasSeenOnboarding = booleanPreferencesKey("has_seen_onboarding")
    }

    private companion object {
        private val Context.appSettingsStore: DataStore<Preferences> by preferencesDataStore(
            name = "app_settings",
        )
    }
}
