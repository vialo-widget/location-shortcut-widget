package com.navigo.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navigo.app.data.Graph
import com.navigo.app.service.widget.WidgetMirror
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val widgetStyle: String,
    val expiredPruned: Int? = null,
)

class SettingsViewModel(private val graph: Graph) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(widgetStyle = graph.widgetMirror.getWidgetStyle()),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun setWidgetStyle(style: String) {
        graph.widgetMirror.setWidgetStyle(style)
        _state.update { it.copy(widgetStyle = style) }
    }

    fun pruneExpired() {
        viewModelScope.launch {
            val removed = graph.shortcutRepository.pruneExpired()
            _state.update { it.copy(expiredPruned = removed) }
        }
    }

    fun acknowledgePruneResult() = _state.update { it.copy(expiredPruned = null) }

    companion object {
        const val STYLE_BOLD = WidgetMirror.STYLE_BOLD
        const val STYLE_GREYSCALE = WidgetMirror.STYLE_GREYSCALE
    }
}
