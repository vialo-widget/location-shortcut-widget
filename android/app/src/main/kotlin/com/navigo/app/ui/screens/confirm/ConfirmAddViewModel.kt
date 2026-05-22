package com.navigo.app.ui.screens.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navigo.app.data.Graph
import com.navigo.app.data.model.ExpiryOption
import com.navigo.app.data.model.PendingShortcut
import com.navigo.app.data.model.Shortcut
import com.navigo.app.data.validation.DuplicateChecker
import com.navigo.app.data.validation.SaveBlocker
import com.navigo.app.data.validation.toBlocker
import com.navigo.app.ui.icons.autoDetectIconKey
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
    /** Inline hint shown beneath the label field when the incoming label
     *  collided with an existing one and was auto-suffixed. */
    val labelHint: String? = null,
    val blocker: SaveBlocker? = null,
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
            // Auto-rename if the incoming label collides with an existing
            // shortcut. Matches the Flutter build's "feel free to change
            // the label" hint — Confirm doesn't block on label conflicts.
            viewModelScope.launch {
                val all = graph.shortcutRepository.list()
                val resolved = DuplicateChecker.nextAvailableLabel(all, p.label)
                if (resolved != p.label) {
                    _state.update {
                        it.copy(
                            label = resolved,
                            labelHint = "\"${p.label}\" already exists — feel free to change the label below.",
                        )
                    }
                }
            }
        }
    }

    fun setLabel(v: String) = _state.update {
        val nextIcon = if (!it.userPickedIcon) autoDetectIconKey(v) else it.iconKey
        it.copy(label = v, iconKey = nextIcon, labelHint = null)
    }

    fun setIcon(k: String) = _state.update { it.copy(iconKey = k, userPickedIcon = true) }
    fun setExpiry(o: ExpiryOption) = _state.update { it.copy(expiryOption = o) }

    fun confirm() {
        val s = _state.value
        val p = s.pending ?: return
        if (s.isSaving || s.saved) return
        val label = s.label.trim().ifBlank { p.label }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val all = graph.shortcutRepository.list()
            val blocker = DuplicateChecker
                .check(all, p.latitude, p.longitude, label)
                .toBlocker()
            if (blocker != null) {
                _state.update { it.copy(isSaving = false, blocker = blocker) }
                return@launch
            }
            persistNew(all.size, label, p)
        }
    }

    fun confirmReplace() {
        val s = _state.value
        val p = s.pending ?: return
        val matched = (s.blocker as? SaveBlocker.ReplacePrompt)?.matched ?: return
        val label = s.label.trim().ifBlank { matched.label }
        _state.update { it.copy(blocker = null, isSaving = true) }
        viewModelScope.launch {
            val now = Instant.now()
            val updated = matched.copy(
                label = label,
                address = p.address,
                latitude = p.latitude,
                longitude = p.longitude,
                placeId = p.placeId,
                iconName = s.iconKey,
                expiresAt = s.expiryOption.expiresAt(now),
            )
            graph.shortcutRepository.update(updated)
            graph.expiryNotifier.cancel(matched.id)
            graph.expiryNotifier.schedule(updated)
            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }

    fun dismissBlocker() = _state.update { it.copy(blocker = null) }

    private suspend fun persistNew(currentCount: Int, label: String, pending: PendingShortcut) {
        val now = Instant.now()
        val shortcut = Shortcut(
            id = Shortcut.newId(),
            label = label,
            address = pending.address,
            latitude = pending.latitude,
            longitude = pending.longitude,
            placeId = pending.placeId,
            iconName = _state.value.iconKey,
            sortOrder = currentCount,
            createdAt = now,
            expiresAt = _state.value.expiryOption.expiresAt(now),
        )
        graph.shortcutRepository.add(shortcut)
        graph.expiryNotifier.schedule(shortcut)
        _state.update { it.copy(isSaving = false, saved = true) }
    }
}
