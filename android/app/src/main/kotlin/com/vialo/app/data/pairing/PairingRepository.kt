package com.vialo.app.data.pairing

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * High-level operations for the carer-caree pairing feature.
 *
 * Wraps [VialoApi] with:
 *   - lazy device registration (callers don't have to think about it)
 *   - observable state for paired devices and pending invites
 *   - debounced refresh on demand
 *
 * Phase 1 surface: pairing handshake only. Phase 2 will add caree-state
 * sync and carer-side edits.
 */
class PairingRepository(
    private val api: VialoApi,
    private val identity: DeviceIdentity,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    data class State(
        val asCaree: List<Pair> = emptyList(),
        val asCarer: List<Pair> = emptyList(),
        val inbox: List<InboundInvite> = emptyList(),
        val outbound: List<OutboundInvite> = emptyList(),
        val activeCode: ActiveCode? = null,
        val lastError: String? = null,
        val refreshing: Boolean = false,
    )

    /** Idempotent. Safe to call from app start and on every UI entry. */
    suspend fun ensureRegistered(): Boolean {
        if (identity.isRegistered) return true
        return when (val result = api.registerDevice(identity.fcmToken)) {
            is ApiResult.Success -> true
            is ApiResult.Failure -> {
                Log.w(TAG, "registerDevice failed: ${result.code} ${result.message}")
                _state.value = _state.value.copy(lastError = result.message)
                false
            }
        }
    }

    /** Pull every server-side list and update [state]. Cheap (3 endpoints). */
    suspend fun refresh() {
        if (!ensureRegistered()) return
        _state.value = _state.value.copy(refreshing = true, lastError = null)

        val pairs = api.listPairs()
        val inbox = api.inbox()
        val pending = api.pending()

        val merged = State(
            asCaree = (pairs as? ApiResult.Success)?.value?.asCaree ?: _state.value.asCaree,
            asCarer = (pairs as? ApiResult.Success)?.value?.asCarer ?: _state.value.asCarer,
            inbox = (inbox as? ApiResult.Success)?.value ?: _state.value.inbox,
            outbound = (pending as? ApiResult.Success)?.value ?: _state.value.outbound,
            activeCode = _state.value.activeCode,
            refreshing = false,
            lastError = listOfNotNull(
                (pairs as? ApiResult.Failure)?.message,
                (inbox as? ApiResult.Failure)?.message,
                (pending as? ApiResult.Failure)?.message,
            ).firstOrNull(),
        )
        _state.value = merged
    }

    // ─── Caree-side operations ─────────────────────────────────────────────

    /** Generate a fresh 6-digit code. Old code (if any) is invalidated server-side. */
    suspend fun generateCode(): ApiResult<ActiveCode> {
        if (!ensureRegistered()) {
            return ApiResult.Failure(0, "device not registered")
        }
        return api.generateCode().also {
            if (it is ApiResult.Success) {
                _state.value = _state.value.copy(activeCode = it.value)
            }
        }
    }

    /** Discard any local active-code reference (e.g. on screen exit). */
    fun clearActiveCode() {
        _state.value = _state.value.copy(activeCode = null)
    }

    suspend fun accept(pendingId: String): ApiResult<String> {
        val result = api.accept(pendingId)
        if (result is ApiResult.Success) scope.launch { refresh() }
        return result
    }

    suspend fun reject(pendingId: String): ApiResult<Unit> {
        val result = api.reject(pendingId)
        if (result is ApiResult.Success) scope.launch { refresh() }
        return result
    }

    // ─── Carer-side operations ─────────────────────────────────────────────

    suspend fun redeem(
        code: String,
        carerDisplayName: String,
        careeDisplayName: String,
    ): ApiResult<OutboundInvite> {
        if (!ensureRegistered()) {
            return ApiResult.Failure(0, "device not registered")
        }
        val result = api.redeemCode(code, carerDisplayName, careeDisplayName)
        if (result is ApiResult.Success) scope.launch { refresh() }
        return result
    }

    suspend fun rename(
        pairId: String,
        carerDisplayName: String? = null,
        careeDisplayName: String? = null,
    ): ApiResult<Unit> {
        val result = api.rename(pairId, carerDisplayName, careeDisplayName)
        if (result is ApiResult.Success) scope.launch { refresh() }
        return result
    }

    suspend fun unpair(pairId: String): ApiResult<Unit> {
        val result = api.unpair(pairId)
        if (result is ApiResult.Success) scope.launch { refresh() }
        return result
    }

    private companion object { const val TAG = "PairingRepository" }
}
