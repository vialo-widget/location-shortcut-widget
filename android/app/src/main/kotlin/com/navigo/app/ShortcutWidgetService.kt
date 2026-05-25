package com.navigo.app

import android.content.Intent
import android.widget.RemoteViewsService

/**
 * Bound by the widget's GridView via RemoteViews#setRemoteAdapter — the OS
 * calls into [ShortcutCollectionFactory] across processes to build each tile.
 */
class ShortcutWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        ShortcutCollectionFactory(applicationContext)
}
