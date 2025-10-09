package com.example.myapp.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * A modular and reusable location manager utility class
 * Handles location operations with proper error handling and configuration options
 */
class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Data class representing location status
     * @property isEnabled Whether location services are enabled
     * @property hasPermission Whether the app has location permission
     * @property errorMessage Any error message if applicable
     */
    data class LocationStatus(
        val isEnabled: Boolean = false,
        val hasPermission: Boolean = false,
        val errorMessage: String? = null
    )

    /**
     * Data class representing location sharing status
     * @property isSharing Whether location is currently being shared
     * @property sharingMode The current sharing mode (ONE_TIME or CONTINUOUS)
     * @property lastSharedLocation The last location that was shared
     */
    data class SharingStatus(
        val isSharing: Boolean = false,
        val sharingMode: SharingMode = SharingMode.ONE_TIME,
        val lastSharedLocation: Location? = null
    )

    /**
     * Enum representing location sharing modes
     */
    enum class SharingMode {
        ONE_TIME,
        CONTINUOUS
    }

    /**
     * Interface for location callbacks
     */
    interface LocationCallback {
        fun onLocationReceived(location: Location)
        fun onLocationError(errorMessage: String)
        fun onStatusChanged(status: LocationStatus)
    }

    /**
     * Get current location with timeout
     * @param timeoutMs Maximum time to wait for location in milliseconds
     * @param callback Optional callback for location updates
     * @return Result containing location or error
     */
    suspend fun getCurrentLocation(
        timeoutMs: Long = 10_000,
        callback: LocationCallback? = null
    ): Result<Location?> {
        return try {
            // Check permissions and settings
            val status = getLocationStatus()
            callback?.onStatusChanged(status)
            
            if (!status.hasPermission) {
                return Result.failure(SecurityException("Location permission not granted"))
            }
            
            if (!status.isEnabled) {
                return Result.failure(IllegalStateException("Location services are disabled"))
            }
            
            // Get location with timeout
            val location = withTimeoutOrNull(timeoutMs) {
                getCurrentLocationInternal()
            }
            
            if (location != null) {
                callback?.onLocationReceived(location)
                Result.success(location)
            } else {
                val errorMessage = "Failed to get location within timeout"
                callback?.onLocationError(errorMessage)
                Result.success(null) // Timeout occurred, but not an error
            }
        } catch (e: SecurityException) {
            val errorMessage = "Location permission required: ${e.message}"
            callback?.onLocationError(errorMessage)
            Result.failure(SecurityException(errorMessage))
        } catch (e: Exception) {
            val errorMessage = "Failed to get location: ${e.message}"
            callback?.onLocationError(errorMessage)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Get location status including permissions and settings
     * @return LocationStatus object with current status
     */
    fun getLocationStatus(): LocationStatus {
        val hasPermission = hasLocationPermission()
        val isEnabled = isLocationEnabled()
        
        val errorMessage = when {
            !hasPermission -> "Location permission not granted"
            !isEnabled -> "Location services are disabled"
            else -> null
        }
        
        return LocationStatus(
            isEnabled = isEnabled,
            hasPermission = hasPermission,
            errorMessage = errorMessage
        )
    }

    /**
     * Start continuous location updates
     * @param interval Update interval in milliseconds
     * @param fastestInterval Fastest update interval in milliseconds
     * @param callback Callback for location updates
     * @return Flow of Location objects
     */
    fun startLocationUpdates(
        interval: Long = 10000,
        fastestInterval: Long = 5000,
        callback: LocationCallback? = null
    ): Flow<Location> = callbackFlow {
        val status = getLocationStatus()
        callback?.onStatusChanged(status)
        
        if (!status.hasPermission) {
            callback?.onLocationError("Location permission not granted")
            close()
            return@callbackFlow
        }
        
        if (!status.isEnabled) {
            callback?.onLocationError("Location services are disabled")
            close()
            return@callbackFlow
        }
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateIntervalMillis(fastestInterval)
            .build()
            
        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    callback?.onLocationReceived(location)
                    trySend(location)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            callback?.onLocationError("Location permission required: ${e.message}")
            close()
            return@callbackFlow
        } catch (e: Exception) {
            callback?.onLocationError("Failed to start location updates: ${e.message}")
            close()
            return@callbackFlow
        }
        
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * Share location via intent
     * @param location Location to share
     * @param message Optional message to include with shared location
     * @return Result indicating success or failure
     */
    fun shareLocation(location: Location, message: String? = null): Result<Boolean> {
        return try {
            val latitude = location.latitude
            val longitude = location.longitude
            val uri = if (message != null) {
                Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($message)")
            } else {
                Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
            }
            
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(mapIntent)
                Result.success(true)
            } else {
                // Fallback to browser if Maps app is not available
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                )
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(browserIntent)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Open location settings
     */
    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Check if location services are enabled
     * @return true if enabled, false otherwise
     */
    private fun isLocationEnabled(): Boolean {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if the app has location permission
     * @return true if permission granted, false otherwise
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Internal method to get current location
     * @return Location or null if not available
     */
    private suspend fun getCurrentLocationInternal(): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Check permission again before making the call
                if (!hasLocationPermission()) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        continuation.resume(location)
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
                    .addOnCanceledListener {
                        continuation.resume(null)
                    }
            } catch (e: SecurityException) {
                continuation.resume(null)
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }
}