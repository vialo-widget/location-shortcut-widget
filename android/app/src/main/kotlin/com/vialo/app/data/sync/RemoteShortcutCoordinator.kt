package com.vialo.app.data.sync

import android.util.Log
import com.vialo.app.data.model.toDomain
import com.vialo.app.data.pairing.ApiResult
import com.vialo.app.data.pairing.DeviceIdentity
import com.vialo.app.data.pairing.VialoApi
import com.vialo.app.data.repo.ShortcutRepository
import com.vialo.app.service.notification.ExpiryNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Routes a `shortcut_changed` FCM to the right local handler depending on
 * which device the change targets.
 *
 *   - **Targeting *this* device** (as caree) — a paired carer edited our
 *     list. We GET the fresh snapshot and `replaceAll` the local Room
 *     table. Expiry notifications are rescheduled from scratch so a
 *     newly-added shortcut still warns the caree before it expires.
 *   - **Targeting another device** (we're a carer of that caree, or a
 *     fellow carer of that caree) — emit on [carerSideChanges] so any
 *     open carer-side ViewModel showing that caree can refresh.
 *
 * Note the auto-apply path writes to [ShortcutRepository], which the
 * [CareeStateSyncer] also observes. That would loop if the new local
 * snapshot were not byte-identical to what the server now holds —
 * `distinctUntilChanged` in the syncer suppresses the re-push because the
 * Shortcut list (data class equality) matches the value it just emitted.
 */
class RemoteShortcutCoordinator(
    private val identity: DeviceIdentity,
    private val api: VialoApi,
    private val shortcutRepository: ShortcutRepository,
    private val expiryNotifier: ExpiryNotifier,
    private val scope: CoroutineScope,
) {
    private val _carerSideChanges = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Emits `caree_device_id` whenever a remote change targets a caree
     *  *other* than this device — i.e. one we're helping. Carer-side
     *  ViewModels subscribe to refresh when their caree changes. */
    val carerSideChanges: SharedFlow<String> = _carerSideChanges.asSharedFlow()

    /** Entry point from [com.vialo.app.service.messaging.VialoFirebaseService]. */
    fun onShortcutChanged(careeDeviceId: String) {
        if (careeDeviceId.isBlank()) {
            Log.w(TAG, "shortcut_changed missing caree_device_id; ignoring")
            return
        }
        if (careeDeviceId == identity.deviceId) {
            scope.launch { applyToSelf() }
        } else {
            scope.launch { _carerSideChanges.emit(careeDeviceId) }
        }
    }

    private suspend fun applyToSelf() {
        when (val result = api.getCareeState(identity.deviceId)) {
            is ApiResult.Success -> {
                val shortcuts = result.value.shortcuts.map { it.toDomain() }
                shortcutRepository.replaceAll(shortcuts)
                expiryNotifier.cancelAll()
                shortcuts.forEach { expiryNotifier.schedule(it) }
                Log.i(TAG, "applied ${shortcuts.size} remote shortcuts")
            }
            is ApiResult.Failure ->
                Log.w(TAG, "auto-apply fetch failed: ${result.code} ${result.message}")
        }
    }

    private companion object {
        const val TAG = "RemoteShortcutCoordinator"
    }
}
