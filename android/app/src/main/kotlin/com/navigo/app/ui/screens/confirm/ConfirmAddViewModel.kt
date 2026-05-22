package com.navigo.app.ui.screens.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navigo.app.data.Graph
import com.navigo.app.data.model.ExpiryOption
import com.navigo.app.data.model.PendingShortcut
import com.navigo.app.data.model.Shortcut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class ConfirmUiState(
    val pending: PendingShortcut? = null,
    val label: String = "",
    val iconKey: String = "place",
    val userPickedIcon: Boolean = false,
    val expiryOption: ExpiryOption = ExpiryOption.NEVER,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
)

class ConfirmAddViewModel(private val graph: Graph) : ViewModel() {

    private val _state = MutableStateFlow(ConfirmUiState())
    val state: StateFlow<ConfirmUiState> = _state.asStateFlow()

    init {
        graph.pendingShortcutHolder.consume()?.let { p ->
            _state.value = ConfirmUiState(
                pending = p,
                label = p.label,
                iconKey = p.iconName,
                expiryOption = p.expiryOption,
            )
        }
    }

    fun setLabel(v: String) = _state.update {
        val nextIcon = if (!it.userPickedIcon) {
            com.navigo.app.ui.icons.autoDetectIconKey(v)
        } else it.iconKey
        it.copy(label = v, iconKey = nextIcon)
    }
    fun setIcon(k: String) = _state.update { it.copy(iconKey = k, userPickedIcon = true) }
    fun setExpiry(o: ExpiryOption) = _state.update { it.copy(expiryOption = o) }

    fun confirm() {
        val s = _state.value
        val p = s.pending ?: return
        if (s.isSaving || s.saved) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val count = graph.shortcutRepository.count()
            val now = Instant.now()
            val shortcut = Shortcut(
                id = Shortcut.newId(),
                label = s.label.trim().ifBlank { p.label },
                address = p.address,
                latitude = p.latitude,
                longitude = p.longitude,
                placeId = p.placeId,
                iconName = s.iconKey,
                sortOrder = count,
                createdAt = now,
                expiresAt = s.expiryOption.expiresAt(now),
            )
            graph.shortcutRepository.add(shortcut)
            graph.expiryNotifier.schedule(shortcut)
            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }
}
