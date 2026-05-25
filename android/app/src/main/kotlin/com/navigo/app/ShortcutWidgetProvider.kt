package com.navigo.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import org.json.JSONArray
import kotlin.math.min

/** SharedPreferences file carried over from the Flutter build — the widget
 *  reads its data here so existing pinned widgets keep working through the
 *  rewrite. Native code writes to this same file via WidgetMirror. */
private const val WIDGET_PREFS = "HomeWidgetPreferences"

/**
 * NaviGo home-screen widget — a flowing grid of fixed 1×1-icon tiles.
 *
 * Each tile is a fixed 64 dp square; the grid's columnCount is set at
 * runtime from the widget's current width so the tiles wrap as many across
 * as fit (1 → 6 columns) with two rows below. Hidden slots disappear via
 * [View.GONE].
 *
 * Default add-time size is configured via `targetCellWidth=5,
 * targetCellHeight=2` in `xml/shortcut_widget_info.xml`, so on most
 * launchers the widget claims a full launcher row × two rows on first add.
 */
class ShortcutWidgetProvider : AppWidgetProvider() {

    companion object {
        /** Edge length of each tile in dp — roughly one launcher icon cell. */
        private const val SLOT_DP = 64
        private const val SLOT_MARGIN_DP = 2
        private const val GRID_PADDING_DP = 2

        /** Hard cap — the layout ships 12 slot views (6 × 2). */
        private const val MAX_SLOTS = 12

        private data class SlotIds(val container: Int, val icon: Int, val label: Int)

        private val slots = listOf(
            SlotIds(R.id.slot_0, R.id.icon_0, R.id.label_0),
            SlotIds(R.id.slot_1, R.id.icon_1, R.id.label_1),
            SlotIds(R.id.slot_2, R.id.icon_2, R.id.label_2),
            SlotIds(R.id.slot_3, R.id.icon_3, R.id.label_3),
            SlotIds(R.id.slot_4, R.id.icon_4, R.id.label_4),
            SlotIds(R.id.slot_5, R.id.icon_5, R.id.label_5),
            SlotIds(R.id.slot_6, R.id.icon_6, R.id.label_6),
            SlotIds(R.id.slot_7, R.id.icon_7, R.id.label_7),
            SlotIds(R.id.slot_8, R.id.icon_8, R.id.label_8),
            SlotIds(R.id.slot_9, R.id.icon_9, R.id.label_9),
            SlotIds(R.id.slot_10, R.id.icon_10, R.id.label_10),
            SlotIds(R.id.slot_11, R.id.icon_11, R.id.label_11),
        )

        /** Tile backgrounds for the default "bold" style — six Tailwind 600
         *  colours, each baked into its own rounded shape drawable so
         *  setBackgroundResource preserves the rounded silhouette
         *  (setBackgroundColor would replace it with a square ColorDrawable).
         *  Cycled across the 12 slot positions via `i % size`. */
        private val boldDrawables = intArrayOf(
            R.drawable.widget_bold_bg_0, // Indigo  #4F46E5
            R.drawable.widget_bold_bg_1, // Teal    #0D9488
            R.drawable.widget_bold_bg_2, // Orange  #EA580C
            R.drawable.widget_bold_bg_3, // Violet  #7C3AED
            R.drawable.widget_bold_bg_4, // Emerald #059669
            R.drawable.widget_bold_bg_5, // Rose    #E11D48
        )

        /** Tile backgrounds for the greyscale style — six neutrals (Tailwind
         *  Gray + Zinc, 500–700) that read as a coordinated muted palette. */
        private val greyDrawables = intArrayOf(
            R.drawable.widget_grey_bg_0, // Gray 700 #374151
            R.drawable.widget_grey_bg_1, // Gray 600 #4B5563
            R.drawable.widget_grey_bg_2, // Gray 500 #6B7280
            R.drawable.widget_grey_bg_3, // Zinc 700 #3F3F46
            R.drawable.widget_grey_bg_4, // Zinc 600 #52525B
            R.drawable.widget_grey_bg_5, // Zinc 500 #71717A
        )

        // Map icon name → drawable resource (matching the Phosphor duotone set).
        private fun getIconRes(context: Context, iconName: String): Int {
            val resName = "ic_shortcut_$iconName"
            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
            return if (resId != 0) resId else {
                val fallback = context.resources
                    .getIdentifier("ic_shortcut_place", "drawable", context.packageName)
                if (fallback != 0) fallback else android.R.drawable.ic_menu_mylocation
            }
        }

        fun buildRemoteViews(
            context: Context,
            widgetData: SharedPreferences,
            widthDp: Int = 0,
            heightDp: Int = 0,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.shortcut_widget)
            val shortcuts = parseShortcuts(widgetData)
            val styleName = widgetData.getString("widget_style", "boldColors") ?: "boldColors"
            val palette = if (styleName == "greyscale") greyDrawables else boldDrawables

            // Figure out how many tile columns fit in the current widget width.
            // pitch = tile width + horizontal margins on both sides.
            val pitchDp = SLOT_DP + 2 * SLOT_MARGIN_DP
            val gridPaddingDp = 2 * GRID_PADDING_DP
            val availableDp = (widthDp - gridPaddingDp).coerceAtLeast(pitchDp)
            val numCols = (availableDp / pitchDp).coerceIn(1, MAX_SLOTS / 2)
            views.setInt(R.id.shortcut_grid, "setColumnCount", numCols)

            // Two rows max; show as many tiles as fit (capped by both the
            // user's shortcut count and the layout's 12-slot capacity).
            val maxVisible = (numCols * 2).coerceAtMost(MAX_SLOTS)
            val visibleCount = min(shortcuts.length(), maxVisible)

            for (i in slots.indices) {
                val slot = slots[i]
                if (i < visibleCount) {
                    val sc = shortcuts.getJSONObject(i)
                    val label = sc.getString("label")
                    val iconName = sc.optString("iconName", "place")
                    views.setViewVisibility(slot.container, View.VISIBLE)
                    views.setTextViewText(slot.label, label)
                    views.setImageViewResource(slot.icon, getIconRes(context, iconName))
                    views.setInt(
                        slot.container, "setBackgroundResource",
                        palette[i % palette.size],
                    )
                    views.setOnClickPendingIntent(
                        slot.container,
                        navPendingIntent(
                            context, i,
                            sc.getDouble("latitude"), sc.getDouble("longitude"),
                        ),
                    )
                } else {
                    views.setViewVisibility(slot.container, View.GONE)
                }
            }
            return views
        }

        private fun parseShortcuts(widgetData: SharedPreferences): JSONArray {
            val json = widgetData.getString("shortcuts_json", "[]") ?: "[]"
            return JSONArray(json)
        }

        private fun navPendingIntent(
            context: Context,
            requestCode: Int,
            lat: Double,
            lng: Double,
        ): PendingIntent {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=$lat,$lng"),
            ).apply { setPackage("com.google.android.apps.maps") }
            return PendingIntent.getActivity(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /** Extract widget dimensions (in dp) from the options bundle. */
        private fun getWidgetSizeDp(options: Bundle): Pair<Int, Int> {
            val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
            return Pair(w, h)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
        for (appWidgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val (w, h) = getWidgetSizeDp(options)
            val views = buildRemoteViews(context, prefs, w, h)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        val (w, h) = getWidgetSizeDp(newOptions)
        val prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
        val views = buildRemoteViews(context, prefs, w, h)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
