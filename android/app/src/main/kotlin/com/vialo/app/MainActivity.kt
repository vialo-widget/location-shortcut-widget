package com.vialo.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vialo.app.deeplink.DeepLinkBus
import com.vialo.app.ui.VialoApp
import java.security.MessageDigest

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        logSigningFingerprint()
        handleIntent(intent)

        val graph = (application as VialoApplication).graph
        setContent {
            VialoApp(
                graph = graph,
                onRequestPinWidget = ::requestPinWidget,
                isWidgetPinned = ::isWidgetPinned,
                isAppLinkVerified = ::isAppLinkVerified,
                openAppLinkSettings = ::openAppLinkSettings,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Push deep-link URIs into [DeepLinkBus] for the Compose layer to consume.
     * Handles both the custom `vialo://add?…` scheme and verified app links
     * to `https://vialo-widget.github.io/…`, normalising the latter into the
     * custom-scheme form.
     */
    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        val uriStr = data.toString()

        val isCustomScheme = uriStr.startsWith("vialo://")
        val isAppLink = uriStr.startsWith("https://vialo-widget.github.io")
        if (!isCustomScheme && !isAppLink) return

        val normalised = if (isAppLink) {
            // encodedQuery preserves percent-escapes; data.query would decode
            // first, then we'd splice it back into a URI as if still encoded —
            // which mangles values containing `&`, `=`, or `+`.
            val params = data.encodedQuery ?: ""
            Uri.parse("vialo://add?$params")
        } else {
            data
        }
        DeepLinkBus.publish(normalised)
        intent.data = null
    }

    private fun requestPinWidget(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appWidgetManager = getSystemService(AppWidgetManager::class.java) ?: return false
        if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(this, ShortcutWidgetProvider::class.java)
        return appWidgetManager.requestPinAppWidget(provider, null, null)
    }

    private fun isWidgetPinned(): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, ShortcutWidgetProvider::class.java)
        return appWidgetManager.getAppWidgetIds(provider).isNotEmpty()
    }

    private fun isAppLinkVerified(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return try {
            val manager = getSystemService(DomainVerificationManager::class.java)
            val state = manager.getDomainVerificationUserState(packageName) ?: return false
            val domain = state.hostToStateMap["vialo-widget.github.io"]
            domain == DomainVerificationUserState.DOMAIN_STATE_VERIFIED ||
                domain == DomainVerificationUserState.DOMAIN_STATE_SELECTED
        } catch (e: Exception) {
            Log.e(TAG, "App-link verification check failed", e)
            false
        }
    }

    private fun openAppLinkSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(
            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
            Uri.parse("package:$packageName"),
        )
        startActivity(intent)
    }

    private fun logSigningFingerprint() {
        val fp = signingFingerprint() ?: return
        Log.i(TAG, "APK signing SHA-256: $fp")
    }

    private fun signingFingerprint(): String? = try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        }
        val sig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.firstOrNull()
        }
        sig?.let {
            MessageDigest.getInstance("SHA-256")
                .digest(it.toByteArray())
                .joinToString(":") { b -> "%02X".format(b) }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Signing fingerprint lookup failed", e)
        null
    }

    private companion object {
        const val TAG = "Vialo"
    }
}
