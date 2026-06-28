package com.vialo.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale used everywhere instead of raw `.dp` literals.
 *
 * One scale, not multiple ad-hoc values, makes layouts rhythmically
 * consistent — the difference between `padding(16.dp)` and `padding(20.dp)`
 * is invisible in code review but feels random across screens. The names
 * are non-numeric on purpose so changing a token (e.g. bumping `screen`
 * from 20 to 24) lifts every screen together.
 */
object VialoDimens {
    /** 4 dp — micro gap between paired controls. */
    val gapXs = 4.dp
    /** 8 dp — default gap between adjacent UI elements in a row. */
    val gapSm = 8.dp
    /** 12 dp — gap between cards in a list, or between sub-elements
     *  inside a single card. */
    val gapMd = 12.dp
    /** 20 dp — block-level gap separating sections. */
    val gapLg = 20.dp
    /** 32 dp — generous gap reserved for empty states and onboarding-style
     *  page rhythm. */
    val gapXl = 32.dp

    /** Horizontal page inset. Every screen body uses this so content lines
     *  up across the app. */
    val screenH = 20.dp
    /** Vertical inset at the top of a screen body (under the TopAppBar). */
    val screenTop = 8.dp
    /** Vertical inset at the bottom of a screen body (above the system
     *  nav bar / bottom button). */
    val screenBottom = 20.dp

    /** Corner radius for cards and tiles. */
    val cornerLg = 20.dp
    /** Corner radius for chips, buttons, sheet headers. */
    val cornerMd = 16.dp
    /** Pill corner radius for small badges. */
    val cornerPill = 50.dp

    /** Minimum touch target — guideline floor for any tappable area. */
    val touchTarget = 48.dp

    /** Standard ImageVector icon size inside chips, list rows, etc. */
    val iconSm = 20.dp
    val iconMd = 28.dp
    val iconLg = 40.dp
    val iconXl = 64.dp

    /** Avatar / hero icon container — used in empty states and the invite
     *  overlay so the "this is what matters" element has presence. */
    val avatarLg = 96.dp
}
