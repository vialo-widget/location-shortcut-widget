package com.vialo.app.ui.screens.carer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vialo.app.data.Graph
import com.vialo.app.data.model.Shortcut
import com.vialo.app.data.model.toDomain
import com.vialo.app.data.pairing.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HelpingCareeUiState(
    val careeDisplayName: String? = null,
    val shortcuts: List<Shortcut> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val updatedAt: Long = 0L,
)

/**
 * Backs the carer-side "Helping <name>" screen. The carer reads the
 * caree's snapshot via [com.vialo.app.data.pairing.VialoApi.getCareeState]
 * and renders it as the same tile grid as Home. Chunk 3 is read-only;
 * chunk 4 will add the edit surface.
 */
class HelpingCareeViewModel(
    private val graph: Graph,
    private val careeDeviceId: String,
) : ViewModel() {

    private val _ui = MutableStateFlow(HelpingCareeUiState())
    val uiState: StateFlow<HelpingCareeUiState> = _ui.asStateFlow()

    /** Track the display name from the carer's pair list reactively so a
     *  rename via the existing /pair/:id/names flow shows up immediately. */
    private val displayNameFlow: StateFlow<String?> = graph.pairingRepository.state
        .map { state -> state.asCarer.firstOrNull { it.otherDeviceId == careeDeviceId }?.otherDisplayName }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            displayNameFlow.collect { name ->
                _ui.update { it.copy(careeDisplayName = name) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            when (val result = graph.vialoApi.getCareeState(careeDeviceId)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(
                        shortcuts = result.value.shortcuts.map { remote -> remote.toDomain() },
                        updatedAt = result.value.updatedAt,
                        isLoading = false,
                        error = null,
                    )
                }
                is ApiResult.Failure -> _ui.update {
                    it.copy(
                        isLoading = false,
                        // Empty message → keep last-known shortcuts so a transient
                        // failure doesn't blank the screen.
                        error = result.message.ifBlank { "Couldn't refresh" },
                    )
                }
            }
        }
    }
}
