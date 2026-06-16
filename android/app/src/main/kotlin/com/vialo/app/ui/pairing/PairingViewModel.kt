package com.vialo.app.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vialo.app.data.Graph
import com.vialo.app.data.pairing.ApiResult
import com.vialo.app.data.pairing.PairingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for the pairing flow (both caree and carer screens).
 *
 * Wraps [PairingRepository] with screen-friendly intents and a single
 * transient status (last action's outcome). UI surfaces the repository's
 * State for lists + this VM's status for actions/errors.
 */
class PairingViewModel(graph: Graph) : ViewModel() {
    private val repo = graph.pairingRepository

    val state: StateFlow<PairingRepository.State> = repo.state

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status.asStateFlow()

    init {
        viewModelScope.launch { repo.ensureRegistered() }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { repo.refresh() }
    }

    fun ackStatus() {
        _status.value = Status.Idle
    }

    // ─── Caree ─────────────────────────────────────────────────────────────

    /** Generate a fresh 6-digit code. */
    fun generateCode() {
        viewModelScope.launch {
            _status.value = Status.Working
            _status.value = when (val r = repo.generateCode()) {
                is ApiResult.Success -> Status.CodeReady
                is ApiResult.Failure -> Status.Error(r.message)
            }
        }
    }

    fun clearActiveCode() = repo.clearActiveCode()

    fun acceptInvite(pendingId: String, onAccepted: (String) -> Unit = {}) {
        viewModelScope.launch {
            _status.value = Status.Working
            _status.value = when (val r = repo.accept(pendingId)) {
                is ApiResult.Success -> { onAccepted(r.value); Status.Accepted }
                is ApiResult.Failure -> Status.Error(r.message)
            }
        }
    }

    fun rejectInvite(pendingId: String, onRejected: () -> Unit = {}) {
        viewModelScope.launch {
            _status.value = Status.Working
            _status.value = when (val r = repo.reject(pendingId)) {
                is ApiResult.Success -> { onRejected(); Status.Rejected }
                is ApiResult.Failure -> Status.Error(r.message)
            }
        }
    }

    // ─── Carer ─────────────────────────────────────────────────────────────

    fun redeemCode(
        code: String,
        carerDisplayName: String,
        careeDisplayName: String,
        onRedeemed: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _status.value = Status.Working
            _status.value = when (val r = repo.redeem(code, carerDisplayName, careeDisplayName)) {
                is ApiResult.Success -> { onRedeemed(); Status.Redeemed }
                is ApiResult.Failure -> Status.Error(r.message)
            }
        }
    }

    // ─── Either ────────────────────────────────────────────────────────────

    fun unpair(pairId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _status.value = Status.Working
            _status.value = when (val r = repo.unpair(pairId)) {
                is ApiResult.Success -> { onDone(); Status.Unpaired }
                is ApiResult.Failure -> Status.Error(r.message)
            }
        }
    }

    fun rename(
        pairId: String,
        carerDisplayName: String? = null,
        careeDisplayName: String? = null,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _status.value = Status.Working
            _status.value = when (val r = repo.rename(pairId, carerDisplayName, careeDisplayName)) {
                is ApiResult.Success -> { onDone(); Status.Renamed }
                is ApiResult.Failure -> Status.Error(r.message)
            }
        }
    }

    sealed class Status {
        data object Idle : Status()
        data object Working : Status()
        data object CodeReady : Status()
        data object Accepted : Status()
        data object Rejected : Status()
        data object Redeemed : Status()
        data object Unpaired : Status()
        data object Renamed : Status()
        data class Error(val message: String) : Status()
    }
}
