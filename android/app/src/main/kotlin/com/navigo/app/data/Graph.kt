package com.navigo.app.data

import android.content.Context
import com.navigo.app.data.db.NaviGoDatabase
import com.navigo.app.data.migration.WidgetPrefsImporter
import com.navigo.app.data.repo.ShortcutRepository
import com.navigo.app.data.settings.AppSettings
import com.navigo.app.service.location.LocationService
import com.navigo.app.service.notification.ExpiryNotifier
import com.navigo.app.service.search.NominatimClient

/**
 * Manual dependency graph — every long-lived singleton the app needs.
 *
 * No Hilt: the surface is small enough that lazy properties on a single
 * holder are clearer than a DI framework. Access via
 * [com.navigo.app.NaviGoApplication.graph] or the `LocalGraph` CompositionLocal
 * in the Compose layer.
 */
class Graph(context: Context) {
    private val appContext = context.applicationContext

    val database: NaviGoDatabase by lazy { NaviGoDatabase.build(appContext) }
    val appSettings: AppSettings by lazy { AppSettings(appContext) }
    val shortcutRepository: ShortcutRepository by lazy {
        ShortcutRepository(database.shortcutDao())
    }
    val widgetPrefsImporter: WidgetPrefsImporter by lazy {
        WidgetPrefsImporter(appContext, shortcutRepository, appSettings)
    }

    val nominatimClient: NominatimClient by lazy { NominatimClient() }
    val locationService: LocationService by lazy { LocationService(appContext) }
    val expiryNotifier: ExpiryNotifier by lazy { ExpiryNotifier(appContext) }
}
