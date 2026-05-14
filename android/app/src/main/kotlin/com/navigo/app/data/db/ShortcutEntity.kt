package com.navigo.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.navigo.app.data.model.Shortcut
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
)
