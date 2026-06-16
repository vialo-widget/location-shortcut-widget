package com.vialo.app.ui.navigation

/**
 * Stable route strings for the navigation graph. Kept as constants (not a
 * sealed class) so they're easy to compose with arguments via simple string
 * templates — overkill avoided until the surface area grows.
 */
object Destinations {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ADD = "add"
    const val CONFIRM_ADD = "confirm_add"
    const val EDIT_ROUTE = "edit/{shortcutId}"
    const val SETTINGS = "settings"

    fun edit(shortcutId: String) = "edit/$shortcutId"

    // ─── Pairing (carer-caree feature) ─────────────────────────────────
    /** Caree's list of carers + entry to add a new helper. */
    const val WHO_HELPING_ME = "who_helping_me"
    /** Caree generates a 6-digit code for a helper to redeem. */
    const val GENERATE_CODE = "generate_code"
    /** Caree accepts or rejects a specific pending invite, addressed by pendingId. */
    const val ACCEPT_INVITE_ROUTE = "accept_invite/{pendingId}"
    fun acceptInvite(pendingId: String) = "accept_invite/$pendingId"

    /** Carer-mode placeholder (chunk 3 will flesh out). */
    const val CARER_MODE = "carer_mode"
    /** Carer redeems a code + types display names. */
    const val ENTER_CODE = "enter_code"
}
