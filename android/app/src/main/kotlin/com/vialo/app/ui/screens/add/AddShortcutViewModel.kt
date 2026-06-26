package com.vialo.app.ui.screens.add

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vialo.app.data.Graph
import com.vialo.app.data.model.ExpiryOption
import com.vialo.app.data.model.Shortcut
import com.vialo.app.data.model.TransportMode
import com.vialo.app.data.repo.ShortcutStore
import com.vialo.app.data.validation.DuplicateChecker
import com.vialo.app.data.validation.SaveBlocker
import com.vialo.app.data.validation.toBlocker
import com.vialo.app.service.search.PlaceResult
import com.vialo.app.ui.icons.autoDetectIconKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class AddUiState(
    val selectedPlace: PlaceResult? = null,
    val label: String = "",
    val address: String = "",
    val iconKey: String = "place",
    /** Flips to true the first time the user opens the icon picker and
     *  taps a tile. After that, label edits no longer overwrite their
     *  choice via auto-detect. */
    val userPickedIcon: Boolean = false,
    val expiryOption: ExpiryOption = ExpiryOption.NEVER,
    val transportMode: TransportMode = TransportMode.DRIVE,
    val isSaving: Boolean = false,
    val savedShortcutId: String? = null,
    val error: String? = null,
    /** Non-null while a duplicate-check dialog needs to be shown. */
    val blocker: SaveBlocker? = null,
)

class AddShortcutViewModel(
    private val graph: Graph,
    private val store: ShortcutStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AddUiState())
    val state: StateFlow<AddUiState> = _state.asStateFlow()

    suspend fun search(query: String): List<PlaceResult> =
        graph.nominatimClient.search(query)

    fun onPlaceSelected(place: PlaceResult) {
        val guessedLabel = place.displayName.substringBefore(",").trim().take(LABEL_MAX_LEN)
        _state.update {
            it.copy(
                selectedPlace = place,
                address = place.displayName,
                label = guessedLabel,
                iconKey = if (!it.userPickedIcon) autoDetectIconKey(guessedLabel) else it.iconKey,
                error = null,
            )
        }
    }

    fun setLabel(value: String) = _state.update {
        val nextIcon = if (!it.userPickedIcon) autoDetectIconKey(value) else it.iconKey
        it.copy(label = value, iconKey = nextIcon)
    }

    fun setIcon(key: String) = _state.update { it.copy(iconKey = key, userPickedIcon = true) }
    fun setExpiry(option: ExpiryOption) = _state.update { it.copy(expiryOption = option) }
    fun setTransportMode(mode: TransportMode) = _state.update { it.copy(transportMode = mode) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun useCurrentLocation() {
        viewModelScope.launch {
            val coords = graph.locationService.getCurrentLocation()
            if (coords == null) {
                _state.update {
                    it.copy(error = "Location unavailable — check permissions and that GPS is on.")
                }
                return@launch
            }
            val address = graph.nominatimClient.reverse(coords.latitude, coords.longitude)
            val place = PlaceResult(
                placeId = "",
                displayName = address,
                latitude = coords.latitude,
                longitude = coords.longitude,
            )
            onPlaceSelected(place)
        }
    }

    fun save() {
        val s = _state.value
        val place = s.selectedPlace ?: run {
            _state.update { it.copy(error = "Pick a place first.") }
            return
        }
        val label = s.label.trim()
        if (label.isBlank()) {
            _state.update { it.copy(error = "Enter a label.") }
            return
        }
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val all = runCatching { store.list() }.getOrElse { e ->
                Log.w(TAG, "list() failed", e)
                _state.update { it.copy(isSaving = false, error = e.message ?: "Couldn't load existing places.") }
                return@launch
            }
            val blocker = DuplicateChecker
                .check(all, place.latitude, place.longitude, label)
                .toBlocker()
            if (blocker != null) {
                _state.update { it.copy(isSaving = false, blocker = blocker) }
                return@launch
            }
            persistNew(all.size, label, place)
        }
    }

    fun confirmReplace() {
        val s = _state.value
        val matched = (s.blocker as? SaveBlocker.ReplacePrompt)?.matched ?: return
        val place = s.selectedPlace ?: return
        val label = s.label.trim().ifBlank { matched.label }
        _state.update { it.copy(blocker = null, isSaving = true) }
        viewModelScope.launch {
            val now = Instant.now()
            // Preserve the matched shortcut's id + sortOrder so anything that
            // referenced it (pinned tiles, widget order) stays put.
            val updated = matched.copy(
                label = label,
                address = place.displayName,
                latitude = place.latitude,
                longitude = place.longitude,
                placeId = place.placeId,
                iconName = s.iconKey,
                expiresAt = s.expiryOption.expiresAt(now),
                transportMode = s.transportMode,
            )
            runCatching { store.update(updated) }.fold(
                onSuccess = { _state.update { it.copy(isSaving = false, savedShortcutId = updated.id) } },
                onFailure = { e ->
                    Log.w(TAG, "update() failed", e)
                    _state.update { it.copy(isSaving = false, error = e.message ?: "Couldn't save.") }
                },
            )
        }
    }

    fun dismissBlocker() = _state.update { it.copy(blocker = null) }

    private suspend fun persistNew(currentCount: Int, label: String, place: PlaceResult) {
        val now = Instant.now()
        val shortcut = Shortcut(
            id = Shortcut.newId(),
            label = label,
            address = place.displayName,
            latitude = place.latitude,
            longitude = place.longitude,
            placeId = place.placeId,
            iconName = _state.value.iconKey,
            sortOrder = currentCount,
            createdAt = now,
            expiresAt = _state.value.expiryOption.expiresAt(now),
            transportMode = _state.value.transportMode,
        )
        runCatching { store.add(shortcut) }.fold(
            onSuccess = { _state.update { it.copy(isSaving = false, savedShortcutId = shortcut.id) } },
            onFailure = { e ->
                Log.w(TAG, "add() failed", e)
                _state.update { it.copy(isSaving = false, error = e.message ?: "Couldn't save.") }
            },
        )
    }

    private companion object {
        const val LABEL_MAX_LEN = 30
        const val TAG = "AddShortcutViewModel"
    }
}
