@file:OptIn(ExperimentalTextApi::class)

package com.navigo.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.navigo.app.R

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

/** Material 3 [Typography] with every scale re-pointed at [PlusJakartaSans]. */
val NaviGoTypography: Typography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = PlusJakartaSans),
    displayMedium = Base.displayMedium.copy(fontFamily = PlusJakartaSans),
    displaySmall = Base.displaySmall.copy(fontFamily = PlusJakartaSans),
    headlineLarge = Base.headlineLarge.copy(fontFamily = PlusJakartaSans),
    headlineMedium = Base.headlineMedium.copy(fontFamily = PlusJakartaSans),
    headlineSmall = Base.headlineSmall.copy(fontFamily = PlusJakartaSans),
    titleLarge = Base.titleLarge.copy(fontFamily = PlusJakartaSans),
    titleMedium = Base.titleMedium.copy(fontFamily = PlusJakartaSans),
    titleSmall = Base.titleSmall.copy(fontFamily = PlusJakartaSans),
    bodyLarge = Base.bodyLarge.copy(fontFamily = PlusJakartaSans),
    bodyMedium = Base.bodyMedium.copy(fontFamily = PlusJakartaSans),
    bodySmall = Base.bodySmall.copy(fontFamily = PlusJakartaSans),
    labelLarge = Base.labelLarge.copy(fontFamily = PlusJakartaSans),
    labelMedium = Base.labelMedium.copy(fontFamily = PlusJakartaSans),
    labelSmall = Base.labelSmall.copy(fontFamily = PlusJakartaSans),
)
