package com.navigo.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
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
 * NaviGo home screen widget — displays up to 6 location shortcuts.
 * Each shortcut button directly opens Google Maps navigation.
 *
 * Supports two visual styles controlled by the `widget_style` shared preference:
 *   • frostedGlass (default) – translucent glass cards
 *   • boldColors – vibrant solid-color blocks
 *
 * The widget is fully resizable — icons and layout adapt to the widget boundaries.
 * Tiles are kept square and capped at a maximum size.
 */
class ShortcutWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val MAX_CELL_DP = 120f
        private const val GRID_GAP_DP = 4f

        private data class SlotIds(val container: Int, val icon: Int, val label: Int)

        private val slots = listOf(
            SlotIds(R.id.slot_0, R.id.icon_0, R.id.label_0),
            SlotIds(R.id.slot_1, R.id.icon_1, R.id.label_1),
            SlotIds(R.id.slot_2, R.id.icon_2, R.id.label_2),
            SlotIds(R.id.slot_3, R.id.icon_3, R.id.label_3),
            SlotIds(R.id.slot_4, R.id.icon_4, R.id.label_4),
            SlotIds(R.id.slot_5, R.id.icon_5, R.id.label_5),
        )

        /** Bold-scheme tile backgrounds — one drawable per Tailwind 600
         *  colour. Selected at runtime via setBackgroundResource so each
         *  tile gets a rounded shape (the colour-only path through
         *  setBackgroundColor would replace the rounded widget_bold_bg
         *  with a square ColorDrawable, leaving the corners poking out
         *  past the rounded foreground overlay). */
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
            // Fallback to place icon if custom icon not found
            return if (resId != 0) resId else {
                val fallback = context.resources.getIdentifier("ic_shortcut_place", "drawable", context.packageName)
                if (fallback != 0) fallback else android.R.drawable.ic_menu_mylocation
            }
        }

        /**
         * Apply padding to the grid so tiles remain square and don't exceed [MAX_CELL_DP].
         *
         * The grid uses weight-based sizing so cells fill all available space. By adding
         * symmetric padding we shrink the available area until each cell is square and
         * within the max size.
         */
        private fun applySquareTilePadding(
            context: Context,
            views: RemoteViews,
            widthDp: Int,
            heightDp: Int,
            visibleCount: Int,
            isBold: Boolean
        ) {
            if (widthDp <= 0 || heightDp <= 0 || visibleCount <= 0) return

            val density = context.resources.displayMetrics.density
            val numRows = ceil(visibleCount / 2.0).toInt()

            // Existing outer padding around the grid
            val outerPadDp = if (isBold) 4f else 12f
            // Approximate title height for the glass widget
            val titleDp = if (isBold) 0f else 36f

            val gridW = widthDp - outerPadDp * 2
            val gridH = heightDp - outerPadDp * 2 - titleDp

            val cellW = (gridW - GRID_GAP_DP) / 2f
            val cellH = (gridH - GRID_GAP_DP * (numRows - 1)) / numRows

            // Target: smallest of width-cell, height-cell, and max cap
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

        fun buildRemoteViews(
            context: Context,
            widgetData: android.content.SharedPreferences,
            widthDp: Int = 0,
            heightDp: Int = 0
        ): RemoteViews {
            val styleName = widgetData.getString("widget_style", "frostedGlass") ?: "frostedGlass"
            val isBold = styleName == "boldColors"

            val layoutRes = if (isBold) R.layout.shortcut_widget_bold else R.layout.shortcut_widget
            val views = RemoteViews(context.packageName, layoutRes)

            val jsonString = widgetData.getString("shortcuts_json", "[]") ?: "[]"
            val shortcuts = JSONArray(jsonString)
            val visibleCount = min(shortcuts.length(), slots.size)

            for (i in slots.indices) {
                val slot = slots[i]

                if (i < shortcuts.length()) {
                    val shortcut = shortcuts.getJSONObject(i)
                    val label = shortcut.getString("label")
                    val lat = shortcut.getDouble("latitude")
                    val lng = shortcut.getDouble("longitude")
                    val iconName = shortcut.optString("iconName", "place")

                    views.setViewVisibility(slot.container, View.VISIBLE)
                    views.setTextViewText(slot.label, label)
                    views.setImageViewResource(slot.icon, getIconRes(context, iconName))

                    // Apply per-slot color for bold style. setBackgroundResource
                    // (not setBackgroundColor) so the runtime swap keeps the
                    // 20dp rounded shape from widget_bold_bg_*.xml instead of
                    // replacing it with a square ColorDrawable.
                    if (isBold) {
                        views.setInt(
                            slot.container, "setBackgroundResource",
                            boldSlotDrawables[i % boldSlotDrawables.size],
                        )
                    }

                    val navUri = Uri.parse("google.navigation:q=$lat,$lng")
                    val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        i,
                        navIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(slot.container, pendingIntent)
                } else {
                    views.setViewVisibility(slot.container, View.GONE)
                }
            }

            // Enforce square tiles with max size cap
            applySquareTilePadding(context, views, widthDp, heightDp, visibleCount, isBold)

            return views
        }

        /** Extract widget dimensions (in dp) from the options bundle. */
        private fun getWidgetSizeDp(options: Bundle): Pair<Int, Int> {
            // In portrait the width is the min and height is the max
            val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
            return Pair(w, h)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
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
        newOptions: Bundle
    ) {
        val (w, h) = getWidgetSizeDp(newOptions)
        val prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
        val views = buildRemoteViews(context, prefs, w, h)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
