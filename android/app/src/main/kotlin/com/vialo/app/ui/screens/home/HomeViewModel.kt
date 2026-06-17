package com.vialo.app.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vialo.app.data.Graph
import com.vialo.app.data.model.Shortcut
import com.vialo.app.service.navigation.NavigationLauncher
import com.vialo.app.service.sharing.ShareService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val shortcuts: List<Shortcut> = emptyList(),
    val careeModeEnabled: Boolean = false,
    val carerModeEnabled: Boolean = false,
    val isLoading: Boolean = true,
)

class HomeViewModel(private val graph: Graph) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        graph.shortcutRepository.shortcuts,
        graph.appSettings.careeModeEnabled,
        graph.appSettings.carerModeEnabled,
    ) { shortcuts, caree, carer ->
        HomeUiState(
            shortcuts = shortcuts,
            careeModeEnabled = caree,
            carerModeEnabled = carer,
            isLoading = false,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        HomeUiState(isLoading = true),
    )

    fun launchNavigation(context: Context, shortcut: Shortcut) {
        NavigationLauncher.launch(context, shortcut)
    }

    fun share(context: Context, shortcut: Shortcut) {
        ShareService.share(context, shortcut)
    }

    fun delete(shortcut: Shortcut) {
        viewModelScope.launch {
            graph.shortcutRepository.delete(shortcut.id)
            graph.expiryNotifier.cancel(shortcut.id)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
