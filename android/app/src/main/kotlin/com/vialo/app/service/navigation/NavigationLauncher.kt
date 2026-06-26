package com.vialo.app.service.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.vialo.app.data.model.Shortcut
import com.vialo.app.data.model.TransportMode

/**
 * Resolves a [Shortcut] to the right external app based on its
 * [TransportMode] and launches it.
 *
 * Each mode has a primary intent and one or more fallbacks — we try them
 * in order and fire the first one any installed app can handle. Package
 * visibility for these is declared in the manifest `<queries>` block.
 *
 *   - **DRIVE**: `google.navigation:` → `geo:` → Maps URL. Same chain as
 *     before transport modes were a thing, so existing shortcuts behave
 *     identically.
 *   - **TRANSIT**: Google Maps URLs API with `travelmode=transit` — opens
 *     the Maps app on directions with the transit tab selected. Falls
 *     back to the same URL in a browser.
 *   - **UBER**: `m.uber.com/ul/` universal link with `pickup=my_location`
 *     and the dropoff coordinates. Opens the Uber app if installed, web
 *     flow otherwise.
 */
object NavigationLauncher {

    fun launch(context: Context, shortcut: Shortcut): Boolean {
        val attempts: Sequence<Intent> = when (shortcut.transportMode) {
            TransportMode.DRIVE -> sequenceOf(
                googleNavigationIntent(shortcut),
                geoIntent(shortcut),
                webMapsDirectionsIntent(shortcut, travelMode = null),
            )
            TransportMode.TRANSIT -> sequenceOf(
                webMapsDirectionsIntent(shortcut, travelMode = "transit"),
            )
            TransportMode.UBER -> sequenceOf(
                uberIntent(shortcut),
            )
        }
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
        Log.w(TAG, "No handler could launch ${shortcut.transportMode} for $shortcut")
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

    /** Maps URLs API. `travelMode` of "transit", "driving", "walking",
     *  "bicycling", or null (lets Maps pick its default). */
    private fun webMapsDirectionsIntent(shortcut: Shortcut, travelMode: String?): Intent {
        val base = "https://www.google.com/maps/dir/?api=1" +
            "&destination=${shortcut.latitude},${shortcut.longitude}"
        val url = if (travelMode != null) "$base&travelmode=$travelMode" else base
        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }

    /** Uber universal deep link. `pickup=my_location` lets Uber resolve the
     *  caller's current location itself, so we don't need a GPS fix. The
     *  `dropoff[formatted_address]` and `[nickname]` fields populate the
     *  confirm screen — using the shortcut's stored address keeps the
     *  field consistent with what we show elsewhere in-app. */
    private fun uberIntent(shortcut: Shortcut): Intent {
        val builder = Uri.parse("https://m.uber.com/ul/").buildUpon()
            .appendQueryParameter("action", "setPickup")
            .appendQueryParameter("pickup", "my_location")
            .appendQueryParameter("dropoff[latitude]", shortcut.latitude.toString())
            .appendQueryParameter("dropoff[longitude]", shortcut.longitude.toString())
            .appendQueryParameter("dropoff[nickname]", shortcut.label)
        if (shortcut.address.isNotBlank()) {
            builder.appendQueryParameter("dropoff[formatted_address]", shortcut.address)
        }
        return Intent(Intent.ACTION_VIEW, builder.build())
    }

    private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    private const val TAG = "NavigationLauncher"
}
