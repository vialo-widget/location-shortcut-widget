package com.vialo.app.data.model

import java.time.Instant

/**
 * A shortcut as decoded from a deep link — before the receiving user has
 * confirmed they want to add it to their home screen.
 *
 * Distinct from [Shortcut]: it has no id or sortOrder (the receiving app
 * assigns those at insert time), and the expiry window is *re-anchored to
 * now*: the link carries the original duration as a URL token, not an
 * absolute timestamp, so the recipient gets the full window starting from
 * when they accept it.
 */
data class PendingShortcut(
    val label: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val placeId: String,
    val iconName: String,
    val expiryOption: ExpiryOption,
) {
    /** Promote to a persisted [Shortcut], assigning an id and timestamps. */
    fun toShortcut(sortOrder: Int, now: Instant = Instant.now()): Shortcut = Shortcut(
        id = Shortcut.newId(),
        label = label,
        address = address,
        latitude = latitude,
        longitude = longitude,
        placeId = placeId,
        iconName = iconName,
        sortOrder = sortOrder,
        createdAt = now,
        expiresAt = expiryOption.expiresAt(now),
    )
}
