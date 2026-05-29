package com.vialo.app.data.model

import java.time.Duration
import java.time.Instant

/**
 * The five expiry options a user can pick when creating a shortcut.
 *
 * URL tokens (`3d`, `7d`, `30d`, `1y`) are kept verbatim from the Flutter
 * build's share-link format so existing share links remain decodable.
 */
enum class ExpiryOption(
    val shortLabel: String,
    val longLabel: String,
    val urlParam: String?,
    private val duration: Duration?,
    val warningDays: Int,
) {
    NEVER("Never", "Never", null, null, 0),
    THREE_DAYS("3d", "3 days", "3d", Duration.ofDays(3), 1),
    ONE_WEEK("1w", "1 week", "7d", Duration.ofDays(7), 2),
    ONE_MONTH("1m", "1 month", "30d", Duration.ofDays(30), 7),
    ONE_YEAR("1y", "1 year", "1y", Duration.ofDays(365), 30);

    /** Absolute expiry instant counting from [from], or null for [NEVER]. */
    fun expiresAt(from: Instant = Instant.now()): Instant? = duration?.let(from::plus)

    companion object {
        fun fromUrlParam(param: String?): ExpiryOption? = when (param) {
            "3d" -> THREE_DAYS
            "7d" -> ONE_WEEK
            "30d" -> ONE_MONTH
            "1y" -> ONE_YEAR
            else -> null
        }

        /**
         * Back-infer the original [ExpiryOption] from a stored expiry+created
         * pair. Used by the edit screen to pre-select the right chip.
         */
        fun infer(expiresAt: Instant?, createdAt: Instant): ExpiryOption {
            if (expiresAt == null) return NEVER
            val totalDays = Duration.between(createdAt, expiresAt).toDays()
            return when {
                totalDays <= 4 -> THREE_DAYS
                totalDays <= 10 -> ONE_WEEK
                totalDays <= 45 -> ONE_MONTH
                else -> ONE_YEAR
            }
        }
    }
}
