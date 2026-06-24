package com.vialo.app.ui.screens.edit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vialo.app.data.Graph
import com.vialo.app.data.model.ExpiryOption
import com.vialo.app.data.model.Shortcut
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

data class EditUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val original: Shortcut? = null,
    val label: String = "",
    val iconKey: String = "place",
    val userPickedIcon: Boolean = false,
    val expiryOption: ExpiryOption = ExpiryOption.NEVER,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val placeId: String = "",
    val isSaving: Boolean = false,
    val closed: Boolean = false,
    val blocker: SaveBlocker? = null,
)

class EditShortcutViewModel(
    private val graph: Graph,
    private val store: ShortcutStore,
    private val shortcutId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = runCatching { store.get(shortcutId) }.getOrElse { e ->
                Log.w(TAG, "get() failed", e)
                null
            }
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
        val nextIcon = if (!it.userPickedIcon) autoDetectIconKey(value) else it.iconKey
        it.copy(label = value, iconKey = nextIcon)
    }

    fun setIcon(k: String) = _state.update { it.copy(iconKey = k, userPickedIcon = true) }
    fun setExpiry(o: ExpiryOption) = _state.update { it.copy(expiryOption = o) }

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
        val label = s.label.trim().ifBlank { original.label }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val all = runCatching { store.list() }.getOrElse { e ->
                Log.w(TAG, "list() failed", e)
                _state.update { it.copy(isSaving = false) }
                return@launch
            }
            val blocker = DuplicateChecker
                .check(all, s.latitude, s.longitude, label, excludeId = original.id)
                .toBlocker()
            if (blocker != null) {
                _state.update { it.copy(isSaving = false, blocker = blocker) }
                return@launch
            }
            persistInPlace(original, s, label)
        }
    }

    fun confirmReplace() {
        val s = _state.value
        val original = s.original ?: return
        val matched = (s.blocker as? SaveBlocker.ReplacePrompt)?.matched ?: return
        val label = s.label.trim().ifBlank { matched.label }
        _state.update { it.copy(blocker = null, isSaving = true) }
        viewModelScope.launch {
            val now = Instant.now()
            // Edit's replace flow: delete the row being edited AND update
            // the matched row in-place. Net result is one row, with the
            // matched shortcut's id/sortOrder preserved.
            val updated = matched.copy(
                label = label,
                address = s.address,
                latitude = s.latitude,
                longitude = s.longitude,
                placeId = s.placeId,
                iconName = s.iconKey,
                expiresAt = nextExpiresAt(s, original, now),
            )
            val result = runCatching {
                store.delete(original.id)
                store.update(updated)
            }
            result.fold(
                onSuccess = { _state.update { it.copy(isSaving = false, closed = true) } },
                onFailure = { e ->
                    Log.w(TAG, "replace failed", e)
                    _state.update { it.copy(isSaving = false) }
                },
            )
        }
    }

    fun dismissBlocker() = _state.update { it.copy(blocker = null) }

    fun delete() {
        val original = _state.value.original ?: return
        viewModelScope.launch {
            runCatching { store.delete(original.id) }.fold(
                onSuccess = { _state.update { it.copy(closed = true) } },
                onFailure = { e -> Log.w(TAG, "delete() failed", e) },
            )
        }
    }

    private suspend fun persistInPlace(original: Shortcut, s: EditUiState, label: String) {
        val now = Instant.now()
        val updated = original.copy(
            label = label,
            iconName = s.iconKey,
            latitude = s.latitude,
            longitude = s.longitude,
            address = s.address,
            placeId = s.placeId,
            expiresAt = nextExpiresAt(s, original, now),
        )
        runCatching { store.update(updated) }.fold(
            onSuccess = { _state.update { it.copy(isSaving = false, closed = true) } },
            onFailure = { e ->
                Log.w(TAG, "update() failed", e)
                _state.update { it.copy(isSaving = false) }
            },
        )
    }

    /** Only reset the expiry clock when the user actually picked a new
     *  option — leaving the chip alone keeps the original deadline. */
    private fun nextExpiresAt(s: EditUiState, original: Shortcut, now: Instant): Instant? {
        val originalOption = ExpiryOption.infer(original.expiresAt, original.createdAt)
        return when {
            s.expiryOption == ExpiryOption.NEVER -> null
            s.expiryOption == originalOption -> original.expiresAt
            else -> s.expiryOption.expiresAt(now)
        }
    }

    private companion object {
        const val TAG = "EditShortcutViewModel"
    }
}
