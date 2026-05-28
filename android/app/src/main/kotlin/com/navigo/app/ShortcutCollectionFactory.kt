package com.navigo.app

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.navigo.app.service.widget.WidgetDebugLog
import org.json.JSONArray

private const val WIDGET_PREFS = "HomeWidgetPreferences"
private const val KEY_SHORTCUTS_JSON = "shortcuts_json"
private const val KEY_WIDGET_STYLE = "widget_style"
private const val STYLE_GREYSCALE = "greyscale"

/**
 * Reads the shortcut list (mirrored by [com.navigo.app.service.widget.WidgetMirror]
 * into [WIDGET_PREFS]) and produces one RemoteViews per tile. The provider
 * calls notifyAppWidgetViewDataChanged when prefs change, which triggers
 * [onDataSetChanged] and re-renders the GridView.
 */
class ShortcutCollectionFactory(
    private val context: Context,
) : RemoteViewsService.RemoteViewsFactory {

    private var items: JSONArray = JSONArray()
    private var palette: IntArray = boldDrawables

    override fun onCreate() {
        WidgetDebugLog.log(context, "Factory.onCreate")
    }

    override fun onDestroy() {
        WidgetDebugLog.log(context, "Factory.onDestroy")
    }

    override fun onDataSetChanged() {
        val prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
        items = runCatching { JSONArray(prefs.getString(KEY_SHORTCUTS_JSON, "[]") ?: "[]") }
            .onFailure {
                WidgetDebugLog.log(context, "Factory.onDataSetChanged JSON parse failed: $it")
            }
            .getOrDefault(JSONArray())
        val style = prefs.getString(KEY_WIDGET_STYLE, "boldColors") ?: "boldColors"
        palette = if (style == STYLE_GREYSCALE) greyDrawables else boldDrawables
        WidgetDebugLog.log(
            context,
            "Factory.onDataSetChanged items=${items.length()} style=$style",
        )
    }

    override fun getCount(): Int {
        val n = items.length()
        WidgetDebugLog.log(context, "Factory.getCount=$n")
        return n
    }

    override fun getViewAt(position: Int): RemoteViews {
        WidgetDebugLog.log(context, "Factory.getViewAt position=$position")
        val rv = RemoteViews(context.packageName, R.layout.shortcut_widget_tile)
        val sc = items.optJSONObject(position) ?: return rv

        val label = sc.optString("label", "")
        val iconName = sc.optString("iconName", "place")
        val lat = sc.optDouble("latitude", Double.NaN)
        val lng = sc.optDouble("longitude", Double.NaN)

        rv.setTextViewText(R.id.label, label)
        rv.setImageViewResource(R.id.icon, getIconRes(iconName))
        rv.setInt(R.id.slot, "setBackgroundResource", palette[position % palette.size])

        if (!lat.isNaN() && !lng.isNaN()) {
            val fillIn = Intent().apply {
                putExtra(WidgetClickHandlerActivity.EXTRA_LAT, lat)
                putExtra(WidgetClickHandlerActivity.EXTRA_LNG, lng)
            }
            rv.setOnClickFillInIntent(R.id.slot, fillIn)
        }
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true

    private fun getIconRes(iconName: String): Int {
        val resName = "ic_shortcut_$iconName"
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        if (resId != 0) return resId
        val fallback = context.resources
            .getIdentifier("ic_shortcut_place", "drawable", context.packageName)
        return if (fallback != 0) fallback else android.R.drawable.ic_menu_mylocation
    }

    companion object {
        // Cycled per position. Kept in sync with the provider's old in-class arrays.
        private val boldDrawables = intArrayOf(
            R.drawable.widget_bold_bg_0,
            R.drawable.widget_bold_bg_1,
            R.drawable.widget_bold_bg_2,
            R.drawable.widget_bold_bg_3,
            R.drawable.widget_bold_bg_4,
            R.drawable.widget_bold_bg_5,
        )
        private val greyDrawables = intArrayOf(
            R.drawable.widget_grey_bg_0,
            R.drawable.widget_grey_bg_1,
            R.drawable.widget_grey_bg_2,
            R.drawable.widget_grey_bg_3,
            R.drawable.widget_grey_bg_4,
            R.drawable.widget_grey_bg_5,
        )
    }
}
