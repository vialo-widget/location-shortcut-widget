package com.navigo.app.service.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.navigo.app.data.model.Shortcut

/**
 * Launches turn-by-turn navigation to a [Shortcut].
 *
 * Fallback chain (in order):
 *   1. `google.navigation:q=lat,lng` with the Google Maps package — fastest,
 *      goes straight into navigation mode.
 *   2. `geo:lat,lng?q=lat,lng(label)` — handled by any maps app on the device
 *      (Waze, OsmAnd, Maps.me, …).
 *   3. `https://www.google.com/maps/dir/?api=1&destination=…` — opens the
 *      browser as the final resort.
 *
 * Needs the corresponding `<queries>` block in the manifest so
 * Intent#resolveActivity can see other apps' filters on Android 11+.
 */
object NavigationLauncher {

    fun launch(context: Context, shortcut: Shortcut): Boolean {
        val attempts = sequenceOf(
            googleNavigationIntent(shortcut),
            geoIntent(shortcut),
            webMapsIntent(shortcut),
        )
        for (intent in attempts) {
            if (intent.resolveActivity(context.packageManager) != null) {
                return try {
                    context.startActivity(intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    true
                } catch (e: ActivityNotFoundException) {
                    Log.w(TAG, "Resolved but failed to start: ${intent.data}", e)
                    false
                }
            }
        }
        Log.w(TAG, "No handler could launch navigation for $shortcut")
        return false
    }

    private fun googleNavigationIntent(shortcut: Shortcut): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=${shortcut.latitude},${shortcut.longitude}"),
        ).setPackage(GOOGLE_MAPS_PACKAGE)

    private fun geoIntent(shortcut: Shortcut): Intent {
        val labelEncoded = Uri.encode(shortcut.label)
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "geo:${shortcut.latitude},${shortcut.longitude}" +
                    "?q=${shortcut.latitude},${shortcut.longitude}($labelEncoded)",
            ),
        )
    }

    private fun webMapsIntent(shortcut: Shortcut): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "https://www.google.com/maps/dir/?api=1" +
                    "&destination=${shortcut.latitude},${shortcut.longitude}",
            ),
        )

    private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    private const val TAG = "NavigationLauncher"
}
