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
}
