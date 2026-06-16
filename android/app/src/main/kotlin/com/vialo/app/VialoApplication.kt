package com.vialo.app

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.vialo.app.data.Graph
import com.vialo.app.data.pairing.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class VialoApplication : Application() {

    /** Process-scoped service locator. */
    val graph: Graph by lazy { Graph(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Touch the notifier eagerly so its notification channel exists before
        // any scheduled worker fires.
        graph.expiryNotifier

        // One-shot import from the legacy widget-prefs JSON (no-op after the
        // first successful run thanks to the DataStore flag), then drop any
        // shortcuts whose expiry has already passed. The Flutter build did
        // the same prune on every cold start so users never saw expired
        // tiles even momentarily — the ExpiryNotificationWorker is a
        // no-op after the row is gone (it re-checks the row at fire time).
        appScope.launch {
            graph.widgetPrefsImporter.migrateIfNeeded()
            graph.shortcutRepository.pruneExpired()
        }

        // Mirror every repo change into SharedPreferences("HomeWidgetPreferences")
        // so the home-screen widget keeps in step with whatever the app shows.
        appScope.launch {
            graph.shortcutRepository.shortcuts.collect { shortcuts ->
                graph.widgetMirror.mirrorShortcuts(shortcuts)
            }
        }

        // Paired-sync init: fetch the current FCM token and ensure the device
        // is registered with the backend. Idempotent on every cold start — if
        // either the token or the registration is already current, this is a
        // no-op. Failures are non-fatal; pairing UI re-tries on entry anyway.
        appScope.launch { initPairingSync() }
    }

    private suspend fun initPairingSync() {
        val identity = graph.deviceIdentity
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            identity.fcmToken = token
        } catch (e: Exception) {
            Log.w(TAG, "FCM token fetch failed; will retry on next launch", e)
        }
        when (val result = graph.vialoApi.registerDevice(identity.fcmToken)) {
            is ApiResult.Success -> Log.i(TAG, "device registered with sync backend")
            is ApiResult.Failure ->
                Log.w(TAG, "device register failed: ${result.code} ${result.message}")
        }
    }

    private companion object { const val TAG = "VialoApplication" }
}
