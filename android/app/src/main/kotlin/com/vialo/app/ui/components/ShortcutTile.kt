package com.vialo.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.vialo.app.data.model.ExpiryStatus
import com.vialo.app.data.model.Shortcut
import com.vialo.app.data.model.TransportMode
import com.vialo.app.data.model.computeExpiryStatus
import com.vialo.app.data.model.expiryBadgeText
import com.vialo.app.ui.icons.ShortcutIcon
import com.vialo.app.ui.icons.ShortcutIconCatalog
import com.vialo.app.ui.theme.BadgeOnAccent
import com.vialo.app.ui.theme.ExpirySubtleFg
import com.vialo.app.ui.theme.ExpiryUrgentBgDark
import com.vialo.app.ui.theme.ExpiryUrgentBgLight
import com.vialo.app.ui.theme.ExpiryUrgentFg
import com.vialo.app.ui.theme.ExpiryWarningBgDark
import com.vialo.app.ui.theme.ExpiryWarningBgLight
import com.vialo.app.ui.theme.ExpiryWarningFg
import com.vialo.app.ui.theme.VialoDimens

/**
 * The 2-column tile shared by Home and the carer-side "Helping" grid.
 *
 * Layout:
 *   - Top row: shortcut icon (left), expiry badge (right) if the shortcut
 *     has one — collapses to just the icon for evergreen shortcuts.
 *   - Bottom: label (max 2 lines) and a small transport-mode glyph hinting
 *     at what'll happen on tap (car / bus / taxi). The glyph is a quiet
 *     affordance — visually consistent across tiles, no extra label
 *     because the picker on Add/Edit covers naming.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutTile(
    shortcut: Shortcut,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = computeExpiryStatus(shortcut.expiresAt, shortcut.createdAt)
    Surface(
        shape = RoundedCornerShape(VialoDimens.cornerLg),
        color = tileBackground(status),
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VialoDimens.gapMd + VialoDimens.gapXs),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                ShortcutIcon(
                    imageVector = ShortcutIconCatalog.forKey(shortcut.iconName),
                    contentDescription = null,
                    modifier = Modifier.size(VialoDimens.iconLg),
                )
                shortcut.expiresAt?.let { ExpiryBadge(status, expiryBadgeText(it)) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(VialoDimens.gapXs)) {
                Text(
                    text = shortcut.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = transportIcon(shortcut.transportMode),
                    contentDescription = transportDescription(shortcut.transportMode),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(VialoDimens.iconSm),
                )
            }
        }
    }
}

@Composable
private fun ExpiryBadge(status: ExpiryStatus, text: String) {
    val (bg, fg) = badgeColors(status)
    Box(
        modifier = Modifier
            .background(bg, shape = RoundedCornerShape(VialoDimens.cornerPill))
            .padding(horizontal = VialoDimens.gapSm, vertical = VialoDimens.gapXs / 2),
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun tileBackground(status: ExpiryStatus): Color {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    return when (status) {
        ExpiryStatus.URGENT -> if (isDark) ExpiryUrgentBgDark else ExpiryUrgentBgLight
        ExpiryStatus.WARNING -> if (isDark) ExpiryWarningBgDark else ExpiryWarningBgLight
        else -> scheme.surfaceVariant
    }
}

private fun badgeColors(status: ExpiryStatus): Pair<Color, Color> = when (status) {
    ExpiryStatus.URGENT -> ExpiryUrgentFg to BadgeOnAccent
    ExpiryStatus.WARNING -> ExpiryWarningFg to BadgeOnAccent
    ExpiryStatus.SUBTLE -> ExpirySubtleFg to BadgeOnAccent
    ExpiryStatus.NONE -> Color.Transparent to Color.Transparent
}

private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue

private fun transportIcon(mode: TransportMode): ImageVector = when (mode) {
    TransportMode.DRIVE -> Icons.Outlined.DirectionsCar
    TransportMode.TRANSIT -> Icons.Outlined.DirectionsBus
    TransportMode.UBER -> Icons.Outlined.LocalTaxi
}

private fun transportDescription(mode: TransportMode): String = when (mode) {
    TransportMode.DRIVE -> "Opens driving directions"
    TransportMode.TRANSIT -> "Opens transit directions"
    TransportMode.UBER -> "Opens Uber"
}
