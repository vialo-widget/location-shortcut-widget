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
    /** Caree generates a 6-digit code for a helper to redeem. */
    const val GENERATE_CODE = "generate_code"
    /** Caree accepts or rejects a specific pending invite, addressed by pendingId. */
    const val ACCEPT_INVITE_ROUTE = "accept_invite/{pendingId}"
    fun acceptInvite(pendingId: String) = "accept_invite/$pendingId"

    /** Carer redeems a code + types display names. */
    const val ENTER_CODE = "enter_code"

    /** Carer opens a per-caree screen showing that caree's shortcuts. */
    const val HELPING_CAREE_ROUTE = "helping/{careeDeviceId}"
    fun helpingCaree(careeDeviceId: String) = "helping/$careeDeviceId"

    /** Carer adds a new shortcut on a caree's behalf. */
    const val CARER_ADD_ROUTE = "carer/{careeDeviceId}/add"
    fun carerAdd(careeDeviceId: String) = "carer/$careeDeviceId/add"

    /** Carer edits one of a caree's existing shortcuts. */
    const val CARER_EDIT_ROUTE = "carer/{careeDeviceId}/edit/{shortcutId}"
    fun carerEdit(careeDeviceId: String, shortcutId: String) =
        "carer/$careeDeviceId/edit/$shortcutId"
}
