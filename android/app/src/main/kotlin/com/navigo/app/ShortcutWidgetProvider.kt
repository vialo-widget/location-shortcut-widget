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
import kotlin.math.ceil
import kotlin.math.min

/** SharedPreferences file name carried over from the Flutter build — the
 *  widget reads its data here so existing pinned widgets keep working through
 *  the rewrite. Native code writes to this same file in Phase 5. */
private const val WIDGET_PREFS = "HomeWidgetPreferences"

/**
 * NaviGo home-screen widget.
 *
 * Two visual styles:
 *   • `frostedGlass` (default) — 2×3 fixed grid of 6 shortcuts. Tiles square
 *     up via padding to a max cell size. Layout: `shortcut_widget`.
 *   • `boldColors`              — fixed 1×1-icon tiles (64 dp) in a flowing
 *     GridLayout. Column count is set at runtime from the widget's current
 *     width so the tiles wrap as many across as fit, up to 6 per row × 2
 *     rows (12 max). Layout: `shortcut_widget_bold`.
 *
 * Default widget size at add-time (Android 12+) is configured via
 * `targetCellWidth=5, targetCellHeight=2` in `xml/shortcut_widget_info.xml`.
 */
class ShortcutWidgetProvider : AppWidgetProvider() {

    companion object {
        // ─── Glass-style sizing (unchanged 2×3 layout) ─────────────────
        private const val MAX_CELL_DP = 120f
        private const val GRID_GAP_DP = 4f

        // ─── Bold-style sizing (fixed-tile flowing grid) ───────────────
        /** Edge length of each bold tile in dp — close to a launcher app
         *  icon cell. Combined with [BOLD_SLOT_MARGIN_DP] this defines the
         *  per-cell pitch used to decide how many columns fit in the widget. */
        private const val BOLD_SLOT_DP = 64
        private const val BOLD_SLOT_MARGIN_DP = 2
        private const val BOLD_GRID_PADDING_DP = 2
        /** Hard cap — the bold layout ships 12 slot views (6 × 2). */
        private const val BOLD_MAX_SLOTS = 12

        private data class SlotIds(val container: Int, val icon: Int, val label: Int)

        /** Glass layout — slot_0…slot_5 with their icon/label children. */
        private val glassSlots = listOf(
            SlotIds(R.id.slot_0, R.id.icon_0, R.id.label_0),
            SlotIds(R.id.slot_1, R.id.icon_1, R.id.label_1),
            SlotIds(R.id.slot_2, R.id.icon_2, R.id.label_2),
            SlotIds(R.id.slot_3, R.id.icon_3, R.id.label_3),
            SlotIds(R.id.slot_4, R.id.icon_4, R.id.label_4),
            SlotIds(R.id.slot_5, R.id.icon_5, R.id.label_5),
        )

        /** Bold layout — 12 slots so the grid can wrap up to 6 × 2 tiles. */
        private val boldSlots = listOf(
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

        /** Bold-scheme tile backgrounds — six Tailwind 600 colours, each
         *  baked into its own rounded shape drawable so setBackgroundResource
         *  preserves the rounded silhouette (setBackgroundColor would
         *  replace it with a square ColorDrawable). Cycled across the 12
         *  slot positions via `i % size`. */
        private val boldSlotDrawables = intArrayOf(
            R.drawable.widget_bold_bg_0, // Indigo  #4F46E5
            R.drawable.widget_bold_bg_1, // Teal    #0D9488
            R.drawable.widget_bold_bg_2, // Orange  #EA580C
            R.drawable.widget_bold_bg_3, // Violet  #7C3AED
            R.drawable.widget_bold_bg_4, // Emerald #059669
            R.drawable.widget_bold_bg_5, // Rose    #E11D48
        )

        // Map icon names to custom drawable resources (matching Flutter app icons)
        private fun getIconRes(context: Context, iconName: String): Int {
            val resName = "ic_shortcut_$iconName"
            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
            return if (resId != 0) resId else {
                val fallback = context.resources.getIdentifier("ic_shortcut_place", "drawable", context.packageName)
                if (fallback != 0) fallback else android.R.drawable.ic_menu_mylocation
            }
        }

        fun buildRemoteViews(
            context: Context,
            widgetData: SharedPreferences,
            widthDp: Int = 0,
            heightDp: Int = 0,
        ): RemoteViews {
            val styleName = widgetData.getString("widget_style", "frostedGlass") ?: "frostedGlass"
            val isBold = styleName == "boldColors"
            val shortcuts = parseShortcuts(widgetData)
            return if (isBold) {
                buildBoldViews(context, shortcuts, widthDp, heightDp)
            } else {
                buildGlassViews(context, shortcuts, widthDp, heightDp)
            }
        }

        // ─── Glass path (unchanged 2×3 frosted layout) ─────────────────
        private fun buildGlassViews(
            context: Context,
            shortcuts: JSONArray,
            widthDp: Int,
            heightDp: Int,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.shortcut_widget)
            val visibleCount = min(shortcuts.length(), glassSlots.size)

            for (i in glassSlots.indices) {
                val slot = glassSlots[i]
                if (i < shortcuts.length()) {
                    val sc = shortcuts.getJSONObject(i)
                    val label = sc.getString("label")
                    val iconName = sc.optString("iconName", "place")
                    views.setViewVisibility(slot.container, View.VISIBLE)
                    views.setTextViewText(slot.label, label)
                    views.setImageViewResource(slot.icon, getIconRes(context, iconName))
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
            applyGlassSquarePadding(context, views, widthDp, heightDp, visibleCount)
            return views
        }

        // ─── Bold path (flowing grid of fixed 1×1-icon tiles) ──────────
        private fun buildBoldViews(
            context: Context,
            shortcuts: JSONArray,
            widthDp: Int,
            heightDp: Int,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.shortcut_widget_bold)

            // Figure out how many tile columns fit in the current widget width.
            // Pitch = tile width + horizontal margins on both sides.
            val pitchDp = BOLD_SLOT_DP + 2 * BOLD_SLOT_MARGIN_DP
            val gridPaddingDp = 2 * BOLD_GRID_PADDING_DP
            val availableDp = (widthDp - gridPaddingDp).coerceAtLeast(pitchDp)
            val numCols = (availableDp / pitchDp).coerceIn(1, BOLD_MAX_SLOTS / 2)
            views.setInt(R.id.shortcut_grid, "setColumnCount", numCols)

            // Two rows max; show as many tiles as fit (capped by both the
            // user's shortcut count and the grid's 12-slot capacity).
            val maxVisible = (numCols * 2).coerceAtMost(BOLD_MAX_SLOTS)
            val visibleCount = min(shortcuts.length(), maxVisible)

            for (i in boldSlots.indices) {
                val slot = boldSlots[i]
                if (i < visibleCount) {
                    val sc = shortcuts.getJSONObject(i)
                    val label = sc.getString("label")
                    val iconName = sc.optString("iconName", "place")
                    views.setViewVisibility(slot.container, View.VISIBLE)
                    views.setTextViewText(slot.label, label)
                    views.setImageViewResource(slot.icon, getIconRes(context, iconName))
                    views.setInt(
                        slot.container, "setBackgroundResource",
                        boldSlotDrawables[i % boldSlotDrawables.size],
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

        /**
         * Square the glass-style grid by adding extra padding so the cells
         * stay square and don't exceed [MAX_CELL_DP]. Only used by the
         * glass path — the bold path uses fixed-size tiles and doesn't
         * need this.
         */
        private fun applyGlassSquarePadding(
            context: Context,
            views: RemoteViews,
            widthDp: Int,
            heightDp: Int,
            visibleCount: Int,
        ) {
            if (widthDp <= 0 || heightDp <= 0 || visibleCount <= 0) return

            val density = context.resources.displayMetrics.density
            val numRows = ceil(visibleCount / 2.0).toInt()
            val outerPadDp = 12f
            val titleDp = 36f

            val gridW = widthDp - outerPadDp * 2
            val gridH = heightDp - outerPadDp * 2 - titleDp
            val cellW = (gridW - GRID_GAP_DP) / 2f
            val cellH = (gridH - GRID_GAP_DP * (numRows - 1)) / numRows

            val target = minOf(cellW, cellH, MAX_CELL_DP)
            if (target <= 0) return

            val usedW = target * 2 + GRID_GAP_DP
            val usedH = target * numRows + GRID_GAP_DP * (numRows - 1)
            val extraH = ((gridW - usedW) / 2f).coerceAtLeast(0f)
            val extraV = ((gridH - usedH) / 2f).coerceAtLeast(0f)
            val padHPx = (extraH * density).toInt()
            val padVPx = (extraV * density).toInt()

            views.setViewPadding(R.id.shortcut_grid, padHPx, padVPx, padHPx, padVPx)
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
