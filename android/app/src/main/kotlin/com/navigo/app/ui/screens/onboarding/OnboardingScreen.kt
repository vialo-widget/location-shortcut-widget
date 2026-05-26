package com.navigo.app.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.duotone.Bell
import com.adamglin.phosphoricons.duotone.CheckCircle
import com.adamglin.phosphoricons.duotone.MapPin
import com.adamglin.phosphoricons.duotone.SquaresFour
import com.navigo.app.ui.LocalActivityBridges
import com.navigo.app.ui.LocalGraph
import kotlinx.coroutines.launch

/**
 * First-launch screen. Walks the user through the three setup actions
 * NaviGo needs to feel useful out of the box:
 *
 *   1. Notification permission — so we can warn before a shortcut expires.
 *   2. Location permission — so "Save where I am" can pick up their pin.
 *   3. Pinning the widget — so they actually get the one-tap experience.
 *
 * Each step either shows an action button or a "done" checkmark. None of
 * them are required — the bottom "Get started" button is always live, so
 * the user can skip and grant later from Settings. Onboarding state is
 * persisted via [com.navigo.app.data.settings.AppSettings.markOnboardingSeen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val graph = LocalGraph.current
    val bridges = LocalActivityBridges.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var notificationsGranted by remember {
        mutableStateOf(isNotificationsGranted(context))
    }
    var locationGranted by remember { mutableStateOf(isLocationGranted(context)) }
    var widgetPinned by remember { mutableStateOf(bridges.isWidgetPinned()) }

    // Re-check on resume so granting via the system dialog or adding the
    // widget via the launcher's widget picker updates the UI when we come
    // back to the foreground.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsGranted = isNotificationsGranted(context)
        locationGranted = isLocationGranted(context)
        widgetPinned = bridges.isWidgetPinned()
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsGranted = granted }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> locationGranted = result.values.any { it } }

    var pinMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pinMessage) {
        pinMessage?.let {
            snackbar.showSnackbar(it)
            pinMessage = null
        }
    }

    val finishOnboarding: () -> Unit = {
        scope.launch {
            graph.appSettings.markOnboardingSeen()
            onDone()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Welcome to NaviGo") },
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
            Spacer(Modifier.height(8.dp))
            Text(
                "A few quick steps to get the most out of the app. You can " +
                    "skip any of these and grant them later from Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            // Notification permission is only relevant on API 33+; on older
            // devices the system grants it implicitly, so we hide the step.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                OnboardingStep(
                    icon = PhosphorIcons.Duotone.Bell,
                    title = "Allow notifications",
                    description = "Get reminded a few days before a shortcut expires.",
                    done = notificationsGranted,
                    actionLabel = "Allow",
                    onAction = {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
                Spacer(Modifier.height(12.dp))
            }

            OnboardingStep(
                icon = PhosphorIcons.Duotone.MapPin,
                title = "Allow location access",
                description = "Lets \"Save where I am\" capture your current spot.",
                done = locationGranted,
                actionLabel = "Allow",
                onAction = {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                },
            )
            Spacer(Modifier.height(12.dp))

            OnboardingStep(
                icon = PhosphorIcons.Duotone.SquaresFour,
                title = "Pin the widget",
                description = "Adds a NaviGo grid to your home screen for one-tap navigation.",
                done = widgetPinned,
                actionLabel = "Add",
                onAction = {
                    val launched = bridges.requestPinWidget()
                    pinMessage = if (launched) {
                        "Pin request sent — follow the system prompt."
                    } else {
                        "Drag the NaviGo widget from your launcher's widget picker."
                    }
                },
            )

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = finishOnboarding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Get started")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OnboardingStep(
    icon: ImageVector,
    title: String,
    description: String,
    done: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(4.dp))
            if (done) {
                Icon(
                    imageVector = PhosphorIcons.Duotone.CheckCircle,
                    contentDescription = "Done",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                OutlinedButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

private fun isNotificationsGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun isLocationGranted(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}
