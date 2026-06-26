package com.vialo.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vialo.app.data.model.Shortcut
import com.vialo.app.data.model.TransportMode
import java.time.Instant

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey val id: String,
    val label: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val placeId: String,
    val iconName: String,
    val sortOrder: Int,
    val createdAtMillis: Long,
    val expiresAtMillis: Long?,
    /** Persisted as the [TransportMode] name. `defaultValue` makes Room's
     *  migration framework happy on existing rows during the v1→v2 bump. */
    @ColumnInfo(defaultValue = "DRIVE")
    val transportMode: String = "DRIVE",
)

fun ShortcutEntity.toDomain(): Shortcut = Shortcut(
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

fun Shortcut.toEntity(): ShortcutEntity = ShortcutEntity(
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
