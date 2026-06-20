package com.vialo.app.data.sync

import android.util.Log
import com.vialo.app.data.model.toRemote
import com.vialo.app.data.pairing.ApiResult
import com.vialo.app.data.pairing.PairingRepository
import com.vialo.app.data.pairing.VialoApi
import com.vialo.app.data.repo.ShortcutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Background syncer that mirrors the caree's local shortcut list to the
 * vialo-sync backend so paired carers can see and act on it.
 *
 * Rules:
 *   - Only syncs while at least one carer is paired with this device. With
 *     no carers there's no audience, so we skip the network call (and the
 *     server-side fan-out push that would otherwise fire for no one).
 *   - Pushes on every meaningful change to [ShortcutRepository.shortcuts].
 *     `distinctUntilChanged` skips re-emits where the list didn't actually
 *     change (Room sometimes re-emits identical rows on schema touches).
 *   - A 500 ms debounce coalesces bursts (e.g. a batch reorder writes one
 *     UPDATE per row) into a single POST.
 *   - On failure we log and move on. The next change will retry implicitly;
 *     for a no-change-but-server-is-stale case, the cold-start emission
 *     refreshes things.
 *
 * Lifecycle: created once via [com.vialo.app.data.Graph], started exactly
 * once from [com.vialo.app.VialoApplication.onCreate]. The collector runs
 * on the supplied [scope] for the process lifetime.
 */
@OptIn(FlowPreview::class)
class CareeStateSyncer(
    private val pairingRepo: PairingRepository,
    private val shortcutRepo: ShortcutRepository,
    private val api: VialoApi,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            combine(
                pairingRepo.state
                    .map { it.asCaree.isNotEmpty() }
                    .distinctUntilChanged(),
                shortcutRepo.shortcuts.distinctUntilChanged(),
            ) { hasCarer, shortcuts -> if (hasCarer) shortcuts else null }
                .filterNotNull()
                .debounce(DEBOUNCE_MS)
                .collect { shortcuts ->
                    val payload = shortcuts.map { it.toRemote() }
                    when (val result = api.pushMyShortcuts(payload)) {
                        is ApiResult.Success ->
                            Log.i(TAG, "pushed ${payload.size} shortcuts (server ts=${result.value})")
                        is ApiResult.Failure ->
                            Log.w(TAG, "push failed: ${result.code} ${result.message}")
                    }
                }
        }
    }

    private companion object {
        const val TAG = "CareeStateSyncer"
        const val DEBOUNCE_MS = 500L
    }
}
