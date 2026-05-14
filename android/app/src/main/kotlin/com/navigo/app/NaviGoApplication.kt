package com.navigo.app

import android.app.Application
import com.navigo.app.data.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NaviGoApplication : Application() {

    /** Process-scoped service locator. */
    val graph: Graph by lazy { Graph(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Touch the notifier eagerly so its notification channel exists before
        // any scheduled worker fires.
        graph.expiryNotifier

        // One-shot import from the legacy widget-prefs JSON (no-op after the
        // first successful run thanks to the DataStore flag).
        appScope.launch { graph.widgetPrefsImporter.migrateIfNeeded() }

        // Mirror every repo change into SharedPreferences("HomeWidgetPreferences")
        // so the home-screen widget keeps in step with whatever the app shows.
        appScope.launch {
            graph.shortcutRepository.shortcuts.collect { shortcuts ->
                graph.widgetMirror.mirrorShortcuts(shortcuts)
            }
        }
    }
}
