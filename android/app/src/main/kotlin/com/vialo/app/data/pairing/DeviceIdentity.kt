package com.vialo.app.data.pairing

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * Per-device identity for the paired-sync backend.
 *
 * On first launch we generate a random UUID (`device_id`) and persist it.
 * After registering with the backend, the server returns a `session_token`
 * which we also persist; every authenticated request carries
 *   Authorization: Bearer <session_token>
 *   X-Device-Id: <device_id>
 *
 * Storage is `EncryptedSharedPreferences` (AES-256-GCM, hardware-backed
 * key on devices with a Keystore). Losing the token is recoverable — the
 * client just re-registers with the same device_id and gets a fresh
 * token — but encrypting it at rest is cheap protection against
 * casual root snooping.
 */
class DeviceIdentity(context: Context) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Stable per-install identifier. Generated lazily on first access. */
    val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: run {
            val fresh = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, fresh).apply()
            fresh
        }

    /** Bearer token from the backend. Null until `registered()` is called. */
    val sessionToken: String?
        get() = prefs.getString(KEY_SESSION_TOKEN, null)

    /** True once we've successfully registered with the backend at least once. */
    val isRegistered: Boolean get() = sessionToken != null

    /** Persist a fresh session token returned by `/device/register`. */
    fun saveSessionToken(token: String) {
        prefs.edit().putString(KEY_SESSION_TOKEN, token).apply()
    }

    /** Persist the FCM token reported by Firebase Messaging. Cached so we
     *  can detect changes and push only when it actually rotates. */
    var fcmToken: String?
        get() = prefs.getString(KEY_FCM_TOKEN, null)
        set(value) { prefs.edit().putString(KEY_FCM_TOKEN, value).apply() }

    /** Drop everything. Useful for "sign out" testing flows; not exposed in UI. */
    fun clearForDebug() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE = "vialo_device_identity"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_SESSION_TOKEN = "session_token"
        const val KEY_FCM_TOKEN = "fcm_token"
    }
}
