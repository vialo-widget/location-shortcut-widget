package com.vialo.app.service.deeplink

import android.net.Uri
import com.vialo.app.data.model.ExpiryOption
import com.vialo.app.data.model.PendingShortcut

/**
 * Decodes a `vialo://add?…` URI (or any URI carrying `label`/`lat`/`lng`
 * params, as a fallback) into a [PendingShortcut].
 *
 * The activity normalises HTTPS app-link URIs into the custom scheme before
 * publishing on the bus — see [com.vialo.app.MainActivity.handleIntent].
 *
 * Returns null if the URI is malformed (missing required params, non-numeric
 * coordinates).
 */
object DeepLinkParser {

    fun parse(uri: Uri): PendingShortcut? {
        val label = uri.getQueryParameter("label") ?: return null
        val lat = uri.getQueryParameter("lat")?.toDoubleOrNull() ?: return null
        val lng = uri.getQueryParameter("lng")?.toDoubleOrNull() ?: return null

        return PendingShortcut(
            label = label,
            address = uri.getQueryParameter("address").orEmpty(),
            latitude = lat,
            longitude = lng,
            placeId = uri.getQueryParameter("placeId").orEmpty(),
            iconName = uri.getQueryParameter("icon") ?: "place",
            expiryOption = ExpiryOption
                .fromUrlParam(uri.getQueryParameter("expiry"))
                ?: ExpiryOption.NEVER,
        )
    }
}
