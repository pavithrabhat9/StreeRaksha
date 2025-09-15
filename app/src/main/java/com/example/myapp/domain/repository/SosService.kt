package com.example.myapp.domain.repository

import com.example.myapp.domain.entities.SosAlert
import com.example.myapp.domain.entities.Location

/**
 * Service interface for SOS operations
 * Defines the contract for emergency messaging and location services
 */
interface SosService {
    
    /**
     * Send SOS alert to emergency contacts
     */
    suspend fun sendSosAlert(alert: SosAlert): Result<SosAlert>
    
    /**
     * Get current device location
     */
    suspend fun getCurrentLocation(): Result<Location?>
    
    /**
     * Check if SMS permissions are granted
     */
    suspend fun hasSmsPermission(): Boolean
    
    /**
     * Check if location permissions are granted
     */
    suspend fun hasLocationPermission(): Boolean
}

