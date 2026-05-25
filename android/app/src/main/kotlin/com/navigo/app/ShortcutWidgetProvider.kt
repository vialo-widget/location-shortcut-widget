package com.navigo.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews

/**
 * NaviGo home-screen widget — a scrollable grid of fixed-width tiles.
 *
 * The grid is a [android.widget.GridView] driven by [ShortcutWidgetService] /
 * [ShortcutCollectionFactory], so tile count is unbounded and the GridView
 * scrolls vertically when content overflows. Column count is set at runtime
 * from the current widget width; tile height stays constant so growing the
 * widget vertically just exposes more rows.
 *
 * Default add-time size is configured via `targetCellWidth=5,
 * targetCellHeight=2` in `xml/shortcut_widget_info.xml`.
 */
class ShortcutWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val SLOT_DP = 76
        private const val SLOT_MARGIN_DP = 2
        private const val GRID_PADDING_DP = 2

        fun buildRemoteViews(
            context: Context,
            appWidgetId: Int,
            widthDp: Int,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.shortcut_widget)

            // Bind the GridView to the RemoteViewsService. The Intent's data
            // must be unique per widget ID, otherwise Android caches and
            // reuses a single factory across widget instances.
            val serviceIntent = Intent(context, ShortcutWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.shortcut_grid, serviceIntent)
            views.setEmptyView(R.id.shortcut_grid, R.id.empty_view)

            // Compute column count from width.
            val pitchDp = SLOT_DP + 2 * SLOT_MARGIN_DP
            val availableDp = (widthDp - 2 * GRID_PADDING_DP).coerceAtLeast(pitchDp)
            val numCols = (availableDp / pitchDp).coerceAtLeast(1)
            views.setInt(R.id.shortcut_grid, "setNumColumns", numCols)

            // Single pending-intent template; each tile's fill-in Intent
            // supplies its own lat/lng extras. Must be MUTABLE so the framework
            // can merge the per-tile extras at click time.
            val templateIntent = Intent(context, WidgetClickHandlerActivity::class.java)
            val template = PendingIntent.getActivity(
                context, appWidgetId, templateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            views.setPendingIntentTemplate(R.id.shortcut_grid, template)

            return views
        }

        private fun widgetWidthDp(options: Bundle): Int =
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(id)
            val views = buildRemoteViews(context, id, widgetWidthDp(options))
            appWidgetManager.updateAppWidget(id, views)
            // Force the factory to refresh in case prefs changed since the
            // last render (e.g. shortcut list edited, style switched).
            appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.shortcut_grid)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        val views = buildRemoteViews(context, appWidgetId, widgetWidthDp(newOptions))
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
