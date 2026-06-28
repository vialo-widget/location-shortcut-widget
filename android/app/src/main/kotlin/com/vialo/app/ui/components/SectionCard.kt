package com.vialo.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vialo.app.ui.theme.VialoDimens

/**
 * Section header used above [SectionCard]s. Small, all-caps, primary-tinted
 * — same shape that the original SettingsScreen and OnboardingScreen used
 * inline, lifted into one place so every screen agrees on the look.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

/**
 * Surface card with consistent radius and padding for grouping related
 * settings rows or form fields.
 *
 * Using a card per logical group (instead of dividers between rows) gives
 * the eye a clearer scan-line and lets the background gradient breathe
 * around the content. Internal vertical spacing is the caller's job —
 * sprinkle [VialoDimens.gapMd] / [VialoDimens.gapLg] Spacers between rows.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VialoDimens.cornerMd),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = VialoDimens.gapLg,
                vertical = VialoDimens.gapMd,
            ),
            content = content,
        )
    }
}

/** Standard gap to drop between a [SectionLabel] and its [SectionCard]. */
@Composable
fun SectionLabelGap() = Spacer(Modifier.height(VialoDimens.gapSm))

/** Standard gap to drop between two stacked sections (label + card,
 *  then another label + card). */
@Composable
fun SectionSpacer() = Spacer(Modifier.height(VialoDimens.gapLg))
