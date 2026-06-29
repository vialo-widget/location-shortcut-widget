package com.vialo.app.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.duotone.Bell
import com.adamglin.phosphoricons.duotone.CheckCircle
import com.adamglin.phosphoricons.duotone.Compass
import com.adamglin.phosphoricons.duotone.MapPin
import com.adamglin.phosphoricons.duotone.SquaresFour
import com.adamglin.phosphoricons.duotone.User
import com.adamglin.phosphoricons.duotone.UsersThree
import com.vialo.app.ui.LocalActivityBridges
import com.vialo.app.ui.LocalGraph
import com.vialo.app.ui.components.SectionCard
import com.vialo.app.ui.components.SectionLabel
import com.vialo.app.ui.components.SectionLabelGap
import com.vialo.app.ui.components.SectionSpacer
import com.vialo.app.ui.theme.VialoDimens
import com.vialo.app.ui.theme.ctaSize
import kotlinx.coroutines.launch

/**
 * First-launch screen. Walks the user through a small number of grouped
 * decisions:
 *
 *   1. Hero — sets tone ("here's what Vialo does").
 *   2. **How will you use Vialo?** — Caree / Carer mode toggles.
 *   3. **Quick setup** — notifications, location, widget pin.
 *
 * Every step is optional; the bottom "Get started" button is always live
 * so a hesitant user can skip everything and grant permissions later from
 * Settings. Onboarding-seen state is persisted via
 * [com.vialo.app.data.settings.AppSettings.markOnboardingSeen].
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

    val careeModeEnabled by graph.appSettings.careeModeEnabled
        .collectAsStateWithLifecycle(initialValue = false)
    val carerModeEnabled by graph.appSettings.carerModeEnabled
        .collectAsStateWithLifecycle(initialValue = false)

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
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VialoDimens.screenH),
        ) {
            Spacer(Modifier.height(VialoDimens.gapXl))
            OnboardingHero()
            Spacer(Modifier.height(VialoDimens.gapXl))

            SectionLabel("How will you use Vialo?")
            SectionLabelGap()
            SectionCard {
                ToggleRow(
                    icon = PhosphorIcons.Duotone.User,
                    title = "Caree mode",
                    description = "I want help managing my places. Adds a Caree tab on Home.",
                    checked = careeModeEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { graph.appSettings.setCareeModeEnabled(enabled) }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ToggleRow(
                    icon = PhosphorIcons.Duotone.UsersThree,
                    title = "Carer mode",
                    description = "I want to help someone manage their places. Adds a Carer tab on Home.",
                    checked = carerModeEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { graph.appSettings.setCarerModeEnabled(enabled) }
                    },
                )
            }

            SectionSpacer()
            SectionLabel("Quick setup")
            SectionLabelGap()
            SectionCard {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActionRow(
                        icon = PhosphorIcons.Duotone.Bell,
                        title = "Allow notifications",
                        description = "Get reminded a few days before a shortcut expires.",
                        done = notificationsGranted,
                        actionLabel = "Allow",
                        onAction = {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                ActionRow(
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ActionRow(
                    icon = PhosphorIcons.Duotone.SquaresFour,
                    title = "Pin the widget",
                    description = "Adds a Vialo grid to your home screen for one-tap navigation.",
                    done = widgetPinned,
                    actionLabel = "Add",
                    onAction = {
                        val launched = bridges.requestPinWidget()
                        pinMessage = if (launched) {
                            "Pin request sent — follow the system prompt."
                        } else {
                            "Drag the Vialo widget from your launcher's widget picker."
                        }
                    },
                )
            }

            Spacer(Modifier.height(VialoDimens.gapXl))
            Button(
                onClick = finishOnboarding,
                modifier = Modifier.fillMaxWidth().ctaSize(),
            ) {
                Text("Get started", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(VialoDimens.screenBottom))
        }
    }
}

/** Visual hero — friendly headline + tagline, no animation. Sets first-
 *  impression tone without spending ages tuning a Lottie. */
@Composable
private fun OnboardingHero() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(VialoDimens.avatarLg)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PhosphorIcons.Duotone.Compass,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(VialoDimens.iconXl),
            )
        }
        Spacer(Modifier.height(VialoDimens.gapLg))
        Text(
            "Welcome to Vialo",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(VialoDimens.gapSm))
        Text(
            "One-tap shortcuts to the places that matter. " +
                "A few quick choices to get you set up — change any of them later in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VialoDimens.gapMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VialoDimens.gapMd),
    ) {
        StepIcon(icon)
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
        Spacer(Modifier.width(VialoDimens.gapXs))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    done: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VialoDimens.gapMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VialoDimens.gapMd),
    ) {
        StepIcon(icon)
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
        Spacer(Modifier.width(VialoDimens.gapXs))
        if (done) {
            Icon(
                imageVector = PhosphorIcons.Duotone.CheckCircle,
                contentDescription = "Done",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(VialoDimens.iconMd),
            )
        } else {
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun StepIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(VialoDimens.iconLg)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(VialoDimens.iconMd - 4.dp),
        )
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
