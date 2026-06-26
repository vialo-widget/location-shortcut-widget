package com.vialo.app.data.model

import com.vialo.app.data.pairing.RemoteShortcut
import java.time.Instant
import java.util.UUID

/**
 * Domain model for a saved location shortcut. Mirrors the original Flutter
 * `LocationShortcut` field-for-field so deep-link payloads and the home-screen
 * widget JSON keep working unchanged.
 */
data class Shortcut(
    val id: String,
    val label: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val placeId: String,
    val iconName: String,
    val sortOrder: Int,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val transportMode: TransportMode = TransportMode.DRIVE,
) {
    companion object {
        /** Generate a fresh, stable id for a newly-created shortcut. */
        fun newId(): String = UUID.randomUUID().toString()
    }
}

fun Shortcut.toRemote(): RemoteShortcut = RemoteShortcut(
    id = id,
    label = label,
    address = address,
    latitude = latitude,
    longitude = longitude,
    placeId = placeId,
    iconName = iconName,
    sortOrder = sortOrder,
    createdAtMillis = createdAt.toEpochMilli(),
    expiresAtMillis = expiresAt?.toEpochMilli(),
    transportMode = transportMode.name,
)

fun RemoteShortcut.toDomain(): Shortcut = Shortcut(
    id = id,
    label = label,
    address = address,
    latitude = latitude,
    longitude = longitude,
    placeId = placeId,
    iconName = iconName,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAtMillis),
    expiresAt = expiresAtMillis?.let(Instant::ofEpochMilli),
    transportMode = TransportMode.fromName(transportMode),
)
