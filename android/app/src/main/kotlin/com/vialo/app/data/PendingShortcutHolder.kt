package com.vialo.app.data

import com.vialo.app.data.model.PendingShortcut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lives on the [Graph] so the deep-link router in
 * [com.vialo.app.ui.VialoApp] can hand a [PendingShortcut] off to the
 * Confirm-Add screen across a navigation jump without smuggling the entire
 * object through a route argument.
 *
 * Cleared by the Confirm screen on accept or dismiss.
 */
class PendingShortcutHolder {
    private val _pending = MutableStateFlow<PendingShortcut?>(null)
    val pending: StateFlow<PendingShortcut?> = _pending.asStateFlow()

    fun set(value: PendingShortcut?) {
        _pending.value = value
    }

    fun consume(): PendingShortcut? = _pending.value.also { _pending.value = null }
}
