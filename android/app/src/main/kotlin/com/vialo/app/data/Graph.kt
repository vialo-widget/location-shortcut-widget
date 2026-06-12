package com.vialo.app.data

import android.content.Context
import com.vialo.app.data.db.VialoDatabase
import com.vialo.app.data.migration.WidgetPrefsImporter
import com.vialo.app.data.pairing.DeviceIdentity
import com.vialo.app.data.pairing.PairingRepository
import com.vialo.app.data.pairing.VialoApi
import com.vialo.app.data.repo.ShortcutRepository
import com.vialo.app.data.settings.AppSettings
import com.vialo.app.service.location.LocationService
import com.vialo.app.service.notification.ExpiryNotifier
import com.vialo.app.service.search.NominatimClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency graph — every long-lived singleton the app needs.
 *
 * No Hilt: the surface is small enough that lazy properties on a single
 * holder are clearer than a DI framework. Access via
 * [com.vialo.app.VialoApplication.graph] or the `LocalGraph` CompositionLocal
 * in the Compose layer.
 */
class Graph(context: Context) {
    private val appContext = context.applicationContext

    val database: VialoDatabase by lazy { VialoDatabase.build(appContext) }
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
    val widgetMirror: com.vialo.app.service.widget.WidgetMirror by lazy {
        com.vialo.app.service.widget.WidgetMirror(appContext)
    }
    val pendingShortcutHolder: PendingShortcutHolder by lazy { PendingShortcutHolder() }

    // ─── Paired-device sync (carer-caree feature) ──────────────────────────
    val deviceIdentity: DeviceIdentity by lazy { DeviceIdentity(appContext) }
    val vialoApi: VialoApi by lazy { VialoApi(deviceIdentity) }

    /** Long-lived scope for the pairing repository's fire-and-forget refreshes. */
    private val pairingScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
    val pairingRepository: PairingRepository by lazy {
        PairingRepository(vialoApi, deviceIdentity, pairingScope)
    }
}
