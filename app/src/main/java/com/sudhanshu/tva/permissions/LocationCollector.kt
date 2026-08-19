package com.sudhanshu.tva.permissions

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

 data class CollectedLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long
)

/** Foreground, on-demand location only. No continuous/background tracking. */
object LocationCollector {

    fun hasLocationPermission(context: Context): Boolean =
        PermissionManager.hasLocationPermission(context)

    /**
     * Requests one fresh location fix while the app is in use. If a fresh fix
     * cannot be obtained within 12 seconds, it falls back to the newest
     * last-known location. Nothing is persisted locally by this collector.
     */
    suspend fun getLastKnownLocation(context: Context): CollectedLocation? {
        if (!hasLocationPermission(context)) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val provider = when {
            PermissionManager.hasPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) &&
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            return newestLastKnown(manager)?.let {
                CollectedLocation(it.latitude, it.longitude, it.accuracy, it.time)
            }
        }

        val fresh = suspendCancellableCoroutine<Location?> { continuation ->
            var finished = false
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (finished) return
                    finished = true
                    try { manager.removeUpdates(this) } catch (_: Exception) {}
                    if (continuation.isActive) continuation.resume(location)
                }
            }

            try {
                manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    if (finished) return@postDelayed
                    finished = true
                    try { manager.removeUpdates(listener) } catch (_: Exception) {}
                    if (continuation.isActive) continuation.resume(newestLastKnown(manager))
                }, 12_000L)

                continuation.invokeOnCancellation {
                    try { manager.removeUpdates(listener) } catch (_: Exception) {}
                }
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(newestLastKnown(manager))
            }
        }

        return fresh?.let {
            CollectedLocation(it.latitude, it.longitude, it.accuracy, it.time)
        }
    }

    private fun newestLastKnown(manager: LocationManager): Location? {
        var best: Location? = null
        for (provider in manager.getProviders(true)) {
            val loc = try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
            if (loc != null && (best == null || loc.time > best!!.time)) best = loc
        }
        return best
    }
}
