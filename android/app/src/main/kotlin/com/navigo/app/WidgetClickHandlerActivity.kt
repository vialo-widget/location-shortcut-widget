package com.navigo.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * Invisible trampoline launched by GridView tile clicks. The GridView uses a
 * single PendingIntent template with this activity as its target, and each
 * tile contributes a fill-in Intent with its own lat/lng — Android merges the
 * two at click time. We forward to Google Maps navigation, falling back to
 * any geo: handler if Maps isn't installed.
 */
class WidgetClickHandlerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lat = intent.getDoubleExtra(EXTRA_LAT, Double.NaN)
        val lng = intent.getDoubleExtra(EXTRA_LNG, Double.NaN)
        if (!lat.isNaN() && !lng.isNaN()) {
            val mapsIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=$lat,$lng"),
            ).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val fallback = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:$lat,$lng?q=$lat,$lng"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val resolved = mapsIntent.resolveActivity(packageManager)
            runCatching {
                startActivity(if (resolved != null) mapsIntent else fallback)
            }
        }
        finish()
    }

    companion object {
        const val EXTRA_LAT = "navigo.widget.lat"
        const val EXTRA_LNG = "navigo.widget.lng"
    }
}
