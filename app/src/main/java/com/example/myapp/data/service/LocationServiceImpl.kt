package com.example.myapp.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.myapp.domain.entities.Location
import com.example.myapp.domain.repository.LocationService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Implementation of LocationService using Google Play Services
 * Handles location retrieval with proper permissions and timeout
 */
class LocationServiceImpl(
    private val context: Context
) : LocationService {
    
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    override suspend fun getCurrentLocation(timeoutMs: Long): Result<Location?> {
        return try {
            if (!hasLocationPermission()) {
                return Result.failure(SecurityException("Location permission not granted"))
            }
            
            if (!isLocationEnabled()) {
                return Result.failure(IllegalStateException("Location services are disabled"))
            }
            
            val location = withTimeoutOrNull(timeoutMs) {
                getCurrentLocationInternal()
            }
            
            if (location != null) {
                Result.success(location)
            } else {
                Result.success(null) // Timeout occurred, but not an error
            }
        } catch (e: SecurityException) {
            Result.failure(SecurityException("Location permission required: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get location: ${e.message}"))
        }
    }
    
    private suspend fun getCurrentLocationInternal(): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Check permission again before making the call
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { androidLocation ->
                        if (androidLocation != null) {
                            val location = Location(
                                latitude = androidLocation.latitude,
                                longitude = androidLocation.longitude,
                                accuracy = androidLocation.accuracy
                            )
                            continuation.resume(location)
                        } else {
                            continuation.resume(null)
                        }
                    }
                    .addOnFailureListener { exception ->
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
    
    override suspend fun isLocationEnabled(): Boolean {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
