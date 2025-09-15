package com.example.myapp.domain.repository

import com.example.myapp.domain.entities.Location

/**
 * Repository interface for location services
 * Defines the contract for location operations without implementation details
 */
interface LocationService {
    
    /**
     * Get current device location
     * @param timeoutMs Maximum time to wait for location in milliseconds
     * @return Result containing location or null if not available
     */
    suspend fun getCurrentLocation(timeoutMs: Long = 10_000): Result<Location?>
    
    /**
     * Check if location services are enabled on the device
     */
    suspend fun isLocationEnabled(): Boolean
    
    /**
     * Check if the app has location permission
     */
    suspend fun hasLocationPermission(): Boolean
}
