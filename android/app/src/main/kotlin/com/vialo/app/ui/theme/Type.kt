@file:OptIn(ExperimentalTextApi::class)

package com.vialo.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vialo.app.R

/**
 * Plus Jakarta Sans — bundled as a single variable font (200–800 wght axis)
 * at [R.font.plus_jakarta_sans]. Each [Font] declaration tells Compose which
 * weight to pull out of the variable axis, so callers can keep using
 * [FontWeight.Normal], [FontWeight.Medium], etc. and get the right glyphs.
 *
 * License: SIL Open Font License — see res/font/OFL.txt.
 */
private fun variableFont(weight: FontWeight) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val PlusJakartaSans: FontFamily = FontFamily(
    variableFont(FontWeight.Normal),
    variableFont(FontWeight.Medium),
    variableFont(FontWeight.SemiBold),
    variableFont(FontWeight.Bold),
    variableFont(FontWeight.ExtraBold),
)

private val Base = Typography()

/**
 * Material 3 [Typography] re-pointed at Plus Jakarta Sans, with tweaks to:
 *
 *   - Bump headline weights to **SemiBold** so titles read as decisive
 *     rather than airy default Normal.
 *   - Tighten letter-spacing on display + headline scales — geometric sans
 *     looks crisper with negative tracking at large sizes.
 *   - Set explicit line-heights on titleLarge/headlineSmall to keep
 *     two-line strings (e.g. "Helping <Long Name>") from feeling cramped.
 *   - Promote labelLarge to **SemiBold** so primary button text gets the
 *     hierarchy bump it earns at the bottom of every screen.
 */
val VialoTypography: Typography = Typography(
    displayLarge = Base.displayLarge.copy(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = Base.displayMedium.copy(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp,
    ),
    displaySmall = Base.displaySmall.copy(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
    ),
    headlineLarge = Base.headlineLarge.copy(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = Base.headlineMedium.copy(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.25).sp,
    ),
    headlineSmall = Base.headlineSmall.copy(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp,
    ),
    titleLarge = Base.titleLarge.copy(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
    ),
    titleMedium = Base.titleMedium.copy(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = Base.titleSmall.copy(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = Base.bodyLarge.copy(fontFamily = PlusJakartaSans),
    bodyMedium = Base.bodyMedium.copy(fontFamily = PlusJakartaSans),
    bodySmall = Base.bodySmall.copy(fontFamily = PlusJakartaSans),
    labelLarge = Base.labelLarge.copy(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = Base.labelMedium.copy(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = Base.labelSmall.copy(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
    ),
)
