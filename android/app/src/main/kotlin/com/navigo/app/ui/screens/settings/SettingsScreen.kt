package com.navigo.app.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.navigo.app.ui.LocalActivityBridges
import com.navigo.app.ui.LocalGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val graph = LocalGraph.current
    val bridges = LocalActivityBridges.current
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(graph) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // POST_NOTIFICATIONS permission launcher (Android 13+).
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result handled implicitly by the system */ }

    var pinResultMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pinResultMessage) {
        pinResultMessage?.let {
            snackbar.showSnackbar(it)
            pinResultMessage = null
        }
    }
    LaunchedEffect(state.expiredPruned) {
        state.expiredPruned?.let {
            snackbar.showSnackbar(
                if (it == 0) "No expired shortcuts to remove."
                else "Removed $it expired shortcut${if (it == 1) "" else "s"}.",
            )
            vm.acknowledgePruneResult()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            SectionLabel("Widget")
            SettingRow(
                title = "Pin widget to home screen",
                subtitle = "Adds a NaviGo widget for one-tap navigation.",
                action = {
                    Button(onClick = {
                        val launched = bridges.requestPinWidget()
                        pinResultMessage = if (launched) {
                            "Pin request sent — follow the system prompt."
                        } else {
                            "Drag the NaviGo widget from your launcher's widget picker."
                        }
                    }) { Text("Add widget") }
                },
            )

            Spacer(Modifier.height(12.dp))
            Text(
                "Widget style",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            val styles = listOf(
                SettingsViewModel.STYLE_BOLD to "Bold",
                SettingsViewModel.STYLE_GREYSCALE to "Grey",
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                styles.forEachIndexed { index, (key, label) ->
                    SegmentedButton(
                        selected = state.widgetStyle == key,
                        onClick = { vm.setWidgetStyle(key) },
                        shape = SegmentedButtonDefaults.itemShape(index, styles.size),
                    ) { Text(label) }
                }
            }

            /*
            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
            SectionLabel("Sharing")
            SettingRow(
                title = "Open shared links in NaviGo",
                subtitle = if (bridges.isAppLinkVerified())
                    "Verified — shared links open the app directly."
                else "Not verified — Android may show a chooser dialog.",
                action = {
                    OutlinedButton(onClick = bridges.openAppLinkSettings) {
                        Text("Open settings")
                    }
                },
            )
            */

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
            SectionLabel("Notifications")
            SettingRow(
                title = "Allow expiry warnings",
                subtitle = "Notifies you a few days before a shortcut expires.",
                action = {
                    OutlinedButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }) { Text("Request") }
                },
            )

            /*
            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
            SectionLabel("Maintenance")
            SettingRow(
                title = "Remove expired shortcuts",
                subtitle = "Clears shortcuts whose expiry has passed.",
                action = {
                    OutlinedButton(onClick = vm::pruneExpired) { Text("Clear") }
                },
            )
            */

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
            Text(
                "Search powered by OpenStreetMap — © OpenStreetMap contributors.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    action: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f, fill = true)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        action()
    }
}
