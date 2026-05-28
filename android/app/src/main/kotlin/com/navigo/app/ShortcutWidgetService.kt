package com.navigo.app

import android.content.Intent
import android.widget.RemoteViewsService
import com.navigo.app.service.widget.WidgetDebugLog

/**
 * Bound by the widget's GridView via RemoteViews#setRemoteAdapter — the OS
 * calls into [ShortcutCollectionFactory] across processes to build each tile.
 */
class ShortcutWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        WidgetDebugLog.log(
            applicationContext,
            "Service.onGetViewFactory data=${intent.data}",
        )
        return ShortcutCollectionFactory(applicationContext)
    }
}
