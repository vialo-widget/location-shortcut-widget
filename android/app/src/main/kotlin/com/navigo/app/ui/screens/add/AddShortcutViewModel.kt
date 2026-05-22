package com.navigo.app.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navigo.app.data.Graph
import com.navigo.app.data.model.ExpiryOption
import com.navigo.app.data.model.Shortcut
import com.navigo.app.service.search.PlaceResult
import com.navigo.app.ui.icons.autoDetectIconKey
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
    val isSaving: Boolean = false,
    val savedShortcutId: String? = null,
    val error: String? = null,
)

class AddShortcutViewModel(private val graph: Graph) : ViewModel() {

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
                // Selecting a place isn't a manual icon pick, so leave
                // userPickedIcon alone and let the label-derived icon stand.
                iconKey = if (!it.userPickedIcon) autoDetectIconKey(guessedLabel) else it.iconKey,
                error = null,
            )
        }
    }

    fun setLabel(value: String) = _state.update {
        // Re-run auto-detect on every label change *unless* the user has
        // explicitly tapped an icon in the picker. That preserves manual
        // choices but lets a "Home" → "Work" edit retarget the icon.
        val nextIcon = if (!it.userPickedIcon) autoDetectIconKey(value) else it.iconKey
        it.copy(label = value, iconKey = nextIcon)
    }

    fun setIcon(key: String) = _state.update { it.copy(iconKey = key, userPickedIcon = true) }
    fun setExpiry(option: ExpiryOption) = _state.update { it.copy(expiryOption = option) }
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
        if (s.label.isBlank()) {
            _state.update { it.copy(error = "Enter a label.") }
            return
        }
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val count = graph.shortcutRepository.count()
            val now = Instant.now()
            val shortcut = Shortcut(
                id = Shortcut.newId(),
                label = s.label.trim(),
                address = s.address,
                latitude = place.latitude,
                longitude = place.longitude,
                placeId = place.placeId,
                iconName = s.iconKey,
                sortOrder = count,
                createdAt = now,
                expiresAt = s.expiryOption.expiresAt(now),
            )
            graph.shortcutRepository.add(shortcut)
            graph.expiryNotifier.schedule(shortcut)
            _state.update { it.copy(isSaving = false, savedShortcutId = shortcut.id) }
        }
    }

    private companion object {
        const val LABEL_MAX_LEN = 30
    }
}
