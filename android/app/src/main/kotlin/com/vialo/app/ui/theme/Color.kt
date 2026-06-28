package com.vialo.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Light theme ───────────────────────────────────────────────────────────
// Primary — deep forest green, matches the launcher icon background.
val LightPrimary = Color(0xFF0A3D0F)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFB6F0BB)
val LightOnPrimaryContainer = Color(0xFF002106)

// Secondary — muted sage, distinct from primary in saturation but same family.
val LightSecondary = Color(0xFF52634F)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD5E8CF)
val LightOnSecondaryContainer = Color(0xFF101F0F)

// Tertiary — soft teal-green, picks up the "navigation/map" association.
val LightTertiary = Color(0xFF386666)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFBBEBEB)
val LightOnTertiaryContainer = Color(0xFF002020)

// Surfaces & neutrals — warm off-white with a hint of green/yellow.
val LightSurface = Color(0xFFFCFCF7)
val LightOnSurface = Color(0xFF1B1C18)
val LightSurfaceVariant = Color(0xFFE1E4D6)
val LightOnSurfaceVariant = Color(0xFF44483D)
val LightOutline = Color(0xFF74796D)
val LightOutlineVariant = Color(0xFFC4C8B9)

// ─── Dark theme ────────────────────────────────────────────────────────────
val DarkPrimary = Color(0xFF8FDB91)
val DarkOnPrimary = Color(0xFF003910)
val DarkPrimaryContainer = Color(0xFF00531D)
val DarkOnPrimaryContainer = Color(0xFFAAF7AC)

val DarkSecondary = Color(0xFFB9CCB4)
val DarkOnSecondary = Color(0xFF243424)
val DarkSecondaryContainer = Color(0xFF3A4B39)
val DarkOnSecondaryContainer = Color(0xFFD5E8CF)

val DarkTertiary = Color(0xFFA0CFCE)
val DarkOnTertiary = Color(0xFF003736)
val DarkTertiaryContainer = Color(0xFF1F4E4D)
val DarkOnTertiaryContainer = Color(0xFFBBEBEB)

val DarkSurface = Color(0xFF11140F)
val DarkOnSurface = Color(0xFFE2E3DB)
val DarkSurfaceVariant = Color(0xFF44483D)
val DarkOnSurfaceVariant = Color(0xFFC5C8BA)
val DarkOutline = Color(0xFF8E9285)
val DarkOutlineVariant = Color(0xFF44483D)

// ─── Semantic accents ──────────────────────────────────────────────────────
// Expiry states — used by the shortcut tile and the badge inside it. The
// backgrounds are deliberately muted so an expiring shortcut reads as
// "needs attention" instead of "screaming red wall"; the foregrounds land
// on those backgrounds with WCAG-AA contrast on both themes.
val ExpiryUrgentBgLight = Color(0xFFFDE8E8)
val ExpiryUrgentBgDark = Color(0xFF3D0A0A)
val ExpiryUrgentFg = Color(0xFFC62828)

val ExpiryWarningBgLight = Color(0xFFFFF3E0)
val ExpiryWarningBgDark = Color(0xFF2E1A00)
val ExpiryWarningFg = Color(0xFFFF8F00)

val ExpirySubtleFg = Color(0xFF9E9E9E)
val BadgeOnAccent = Color(0xFFFFFFFF)
