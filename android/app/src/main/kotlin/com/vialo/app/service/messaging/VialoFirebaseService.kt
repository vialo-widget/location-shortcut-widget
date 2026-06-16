package com.vialo.app.service.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vialo.app.VialoApplication
import com.vialo.app.data.pairing.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives FCM events for the paired-device sync feature.
 *
 * Two responsibilities:
 *   1. Push token registration — when Firebase rotates the device's FCM
 *      token, we mirror it to the Vialo backend so future server-side
 *      sends reach the right device.
 *   2. Inbound messages — push payloads carry only event metadata (a
 *      `type` field and any required IDs). On receipt we surface a local
 *      notification through [PairingNotifications] and let the user tap
 *      into the right screen.
 *
 * Heavy work runs on an internal scope so we never block FCM's dispatch
 * thread.
 */
class VialoFirebaseService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = application as? VialoApplication ?: return
        val identity = app.graph.deviceIdentity
        val api = app.graph.vialoApi

        val previous = identity.fcmToken
        identity.fcmToken = token

        // First-ever token: defer to registerDevice (which sends fcm_token
        // along) on next foreground. We can't be sure DeviceIdentity is
        // registered yet — the service can fire before the user opens
        // the app for the first time.
        if (!identity.isRegistered) {
            Log.i(TAG, "FCM token cached; will be sent at first registration")
            return
        }

        if (previous == token) return // no-op idempotent rotation
        scope.launch {
            when (val result = api.updateFcmToken(token)) {
                is ApiResult.Success -> Log.i(TAG, "FCM token updated server-side")
                is ApiResult.Failure ->
                    Log.w(TAG, "FCM token update failed: ${result.code} ${result.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val type = message.data["type"]
        if (type == null) {
            Log.w(TAG, "FCM message without 'type' field; ignoring")
            return
        }

        val notif = PairingNotifications(applicationContext)
        when (type) {
            "pair_invite" -> {
                val pendingId = message.data["pending_id"]
                val title = message.notification?.title ?: "Someone wants to help you"
                val body = message.notification?.body ?: "Tap to accept or reject."
                if (pendingId != null) {
                    notif.pairInvite(pendingId = pendingId, title = title, body = body)
                }
            }
            "pair_accepted", "pair_rejected", "pair_ended", "carer_renamed" -> {
                val title = message.notification?.title ?: "Vialo"
                val body = message.notification?.body.orEmpty()
                notif.simple(channelType = type, title = title, body = body)
            }
            else -> Log.w(TAG, "Unknown FCM type: $type")
        }
    }

    private companion object {
        const val TAG = "VialoFirebaseService"
    }
}
