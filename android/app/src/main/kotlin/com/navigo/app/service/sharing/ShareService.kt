package com.navigo.app.service.sharing

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.navigo.app.data.model.ExpiryOption
import com.navigo.app.data.model.Shortcut

/**
 * Builds shareable HTTPS links and triggers Android's share sheet.
 *
 * The link points at a GitHub Pages redirect page that:
 *   1. Tries to open the NaviGo app via the verified App Link.
 *   2. Falls back to a "Open in Google Maps" button if the app isn't installed.
 *
 * Expiry is encoded as a duration token (e.g. `expiry=3d`) instead of an
 * absolute timestamp so the recipient gets a fresh window starting when
 * they accept the link, not a truncated slice of the sender's remaining time.
 */
object ShareService {

    private const val BASE_URL = "https://navigo-widget.github.io/location-shortcut-widget/"
    private const val ADDRESS_MAX_LEN = 200

    fun buildShareUrl(shortcut: Shortcut): String {
        val expiryToken = ExpiryOption
            .infer(shortcut.expiresAt, shortcut.createdAt)
            .urlParam
        val builder = Uri.parse(BASE_URL).buildUpon()
            .appendQueryParameter("label", shortcut.label)
            .appendQueryParameter("lat", shortcut.latitude.toString())
            .appendQueryParameter("lng", shortcut.longitude.toString())
            .appendQueryParameter("icon", shortcut.iconName)
            .appendQueryParameter(
                "address",
                shortcut.address.take(ADDRESS_MAX_LEN),
            )
        if (expiryToken != null) {
            builder.appendQueryParameter("expiry", expiryToken)
        }
        return builder.build().toString()
    }

    fun share(context: Context, shortcut: Shortcut) {
        val url = buildShareUrl(shortcut)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "NaviGo: ${shortcut.label}")
            putExtra(Intent.EXTRA_TEXT, buildMessage(shortcut, url))
        }
        context.startActivity(
            Intent.createChooser(intent, "Share with")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun buildMessage(shortcut: Shortcut, url: String): String =
        """Here's a location shared via NaviGo!

Location: ${shortcut.label}

Tap the link to add it to your NaviGo app for one-tap navigation. If you don't have NaviGo yet, the link will let you download it for free.

$url"""
}
