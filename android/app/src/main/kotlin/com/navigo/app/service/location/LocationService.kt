package com.navigo.app.service.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** A coarse-grained position result, decoupled from `android.location.Location`. */
data class Coordinates(val latitude: Double, val longitude: Double)

/**
 * Thin wrapper over [com.google.android.gms.location.FusedLocationProviderClient]
 * for the "Save where I am" flow.
 *
 * Returns null whenever a fix can't be obtained — no exceptions cross this
 * boundary. The caller (typically a ViewModel) decides how to surface
 * permission denials vs. "GPS off" vs. "no signal" states.
 *
 * Runtime permission is the caller's responsibility — see
 * [com.navigo.app.MainActivity] or a Compose
 * `rememberLauncherForActivityResult` in Phase 4.
 */
class LocationService(private val context: Context) {

    private val client by lazy {
        LocationServices.getFusedLocationProviderClient(context.applicationContext)
    }

    fun hasLocationPermission(): Boolean =
        granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            granted(Manifest.permission.ACCESS_COARSE_LOCATION)

    @SuppressLint("MissingPermission") // Guarded by hasLocationPermission().
    suspend fun getCurrentLocation(): Coordinates? {
        if (!hasLocationPermission()) {
            Log.i(TAG, "Skipping location fetch — no permission")
            return null
        }
        return try {
            suspendCancellableCoroutine { cont ->
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        cont.resume(loc?.let { Coordinates(it.latitude, it.longitude) })
                    }
                    .addOnFailureListener {
                        Log.w(TAG, "getCurrentLocation failed", it)
                        cont.resume(null)
                    }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error in getCurrentLocation", e)
            null
        }
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "LocationService"
    }
}
