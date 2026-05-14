package com.navigo.app.data.model

import java.time.Duration
import java.time.Instant

/** Visual urgency of a shortcut's expiry, used by the home tile's badge / tint. */
enum class ExpiryStatus { NONE, SUBTLE, WARNING, URGENT }

/** Compute the [ExpiryStatus] for a stored shortcut. */
fun computeExpiryStatus(
    expiresAt: Instant?,
    createdAt: Instant,
    now: Instant = Instant.now(),
): ExpiryStatus {
    if (expiresAt == null) return ExpiryStatus.NONE
    val remaining = Duration.between(now, expiresAt)
    val daysLeft = remaining.toDays()
    if (daysLeft <= 0L) return ExpiryStatus.URGENT
    val option = ExpiryOption.infer(expiresAt, createdAt)
    return if (daysLeft <= option.warningDays) ExpiryStatus.WARNING else ExpiryStatus.SUBTLE
}

/** Human-readable badge text for the remaining time. */
fun expiryBadgeText(expiresAt: Instant, now: Instant = Instant.now()): String {
    val days = Duration.between(now, expiresAt).toDays().toInt()
    return when {
        days <= 0 -> "Today"
        days == 1 -> "Tomorrow"
        days < 7 -> "$days days"
        days < 30 -> {
            val weeks = (days + 3) / 7 // .round()
            "$weeks ${if (weeks == 1) "wk" else "wks"}"
        }
        days < 365 -> {
            val months = (days + 15) / 30
            "$months ${if (months == 1) "mo" else "mos"}"
        }
        else -> "${(days + 182) / 365} yr"
    }
}
