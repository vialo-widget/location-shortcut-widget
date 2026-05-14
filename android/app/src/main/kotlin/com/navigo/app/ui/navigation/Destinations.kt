package com.navigo.app.ui.navigation

/**
 * Stable route strings for the navigation graph. Kept as constants (not a
 * sealed class) so they're easy to compose with arguments via simple string
 * templates — overkill avoided until the surface area grows.
 */
object Destinations {
    const val HOME = "home"
    const val ADD = "add"
    const val CONFIRM_ADD = "confirm_add?payload={payload}"
    const val EDIT = "edit/{shortcutId}"
    const val SETTINGS = "settings"
}
