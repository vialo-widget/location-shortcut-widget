package com.navigo.app

import android.app.Application
import com.navigo.app.data.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NaviGoApplication : Application() {

    /** Process-scoped service locator. Created lazily so unit tests can stub
     *  it before [onCreate] runs (when wired). */
    val graph: Graph by lazy { Graph(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Touch the notifier eagerly so its notification channel exists before
        // any scheduled worker fires.
        graph.expiryNotifier
        appScope.launch {
            graph.widgetPrefsImporter.migrateIfNeeded()
        }
    }
}
