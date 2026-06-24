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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vialo.app.data.model.ExpiryStatus
import com.vialo.app.data.model.Shortcut
import com.vialo.app.data.model.computeExpiryStatus
import com.vialo.app.data.model.expiryBadgeText
import com.vialo.app.ui.icons.ShortcutIcon
import com.vialo.app.ui.icons.ShortcutIconCatalog

/**
 * The 2-column tile used on the Home grid and on the carer-side
 * "Helping <name>" grid. Reusing it from one place keeps the two
 * surfaces visually identical and avoids drift on tweaks like the
 * expiry badge or icon size.
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
        shape = RoundedCornerShape(20.dp),
        color = tileBackground(status),
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                    modifier = Modifier.size(40.dp),
                )
                shortcut.expiresAt?.let { ExpiryBadge(status, expiryBadgeText(it)) }
            }
            Text(
                text = shortcut.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExpiryBadge(status: ExpiryStatus, text: String) {
    val (bg, fg) = badgeColors(status)
    Box(
        modifier = Modifier
            .background(bg, shape = RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun tileBackground(status: ExpiryStatus): Color {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    return when (status) {
        ExpiryStatus.URGENT -> if (isDark) Color(0xFF3D0A0A) else Color(0xFFFDE8E8)
        ExpiryStatus.WARNING -> if (isDark) Color(0xFF2E1A00) else Color(0xFFFFF3E0)
        else -> scheme.surfaceVariant
    }
}

private fun badgeColors(status: ExpiryStatus): Pair<Color, Color> = when (status) {
    ExpiryStatus.URGENT -> Color(0xFFC62828) to Color.White
    ExpiryStatus.WARNING -> Color(0xFFFF8F00) to Color.White
    ExpiryStatus.SUBTLE -> Color(0xFF9E9E9E) to Color.White
    ExpiryStatus.NONE -> Color.Transparent to Color.Transparent
}

private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue
