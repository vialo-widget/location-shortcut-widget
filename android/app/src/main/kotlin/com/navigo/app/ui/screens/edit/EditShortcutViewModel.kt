package com.navigo.app.ui.screens.edit

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

data class EditUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val original: Shortcut? = null,
    val label: String = "",
    val iconKey: String = "place",
    /** True once the user has tapped an icon in the picker — disables
     *  the label-driven auto-detect after that point. */
    val userPickedIcon: Boolean = false,
    val expiryOption: ExpiryOption = ExpiryOption.NEVER,
    /** Mutable location — initialised from [original] but the user can
     *  swap it out via the "Change location" picker. */
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val placeId: String = "",
    val isSaving: Boolean = false,
    val closed: Boolean = false,
)

class EditShortcutViewModel(
    private val graph: Graph,
    private val shortcutId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = graph.shortcutRepository.get(shortcutId)
            _state.value = if (existing == null) {
                EditUiState(loading = false, notFound = true)
            } else {
                EditUiState(
                    loading = false,
                    original = existing,
                    label = existing.label,
                    iconKey = existing.iconName,
                    expiryOption = ExpiryOption.infer(existing.expiresAt, existing.createdAt),
                    latitude = existing.latitude,
                    longitude = existing.longitude,
                    address = existing.address,
                    placeId = existing.placeId,
                )
            }
        }
    }

    fun setLabel(value: String) = _state.update {
        // Re-run auto-detect on label edits *unless* the user has explicitly
        // chosen an icon in the picker — so renaming "Home" → "Work" can
        // retarget the icon, but a manual pick survives subsequent edits.
        val nextIcon = if (!it.userPickedIcon) autoDetectIconKey(value) else it.iconKey
        it.copy(label = value, iconKey = nextIcon)
    }

    fun setIcon(k: String) = _state.update { it.copy(iconKey = k, userPickedIcon = true) }

    fun setExpiry(o: ExpiryOption) = _state.update { it.copy(expiryOption = o) }

    /** Swap the saved coordinates / address. Label and icon are intentionally
     *  left alone — they belong to the user's existing customisation. */
    fun setLocation(place: PlaceResult) = _state.update {
        it.copy(
            latitude = place.latitude,
            longitude = place.longitude,
            address = place.displayName,
            placeId = place.placeId,
        )
    }

    suspend fun searchPlaces(query: String): List<PlaceResult> =
        graph.nominatimClient.search(query)

    fun save() {
        val s = _state.value
        val original = s.original ?: return
        if (s.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val now = Instant.now()
            // Only reset the expiry clock if the user actually picked a new
            // option — leaving the chip alone keeps the original deadline.
            val originalOption = ExpiryOption.infer(original.expiresAt, original.createdAt)
            val nextExpiresAt = when {
                s.expiryOption == ExpiryOption.NEVER -> null
                s.expiryOption == originalOption -> original.expiresAt
                else -> s.expiryOption.expiresAt(now)
            }
            val updated = original.copy(
                label = s.label.trim().ifBlank { original.label },
                iconName = s.iconKey,
                latitude = s.latitude,
                longitude = s.longitude,
                address = s.address,
                placeId = s.placeId,
                expiresAt = nextExpiresAt,
            )
            graph.shortcutRepository.update(updated)
            graph.expiryNotifier.schedule(updated)
            _state.update { it.copy(isSaving = false, closed = true) }
        }
    }

    fun delete() {
        val original = _state.value.original ?: return
        viewModelScope.launch {
            graph.shortcutRepository.delete(original.id)
            graph.expiryNotifier.cancel(original.id)
            _state.update { it.copy(closed = true) }
        }
    }
}
