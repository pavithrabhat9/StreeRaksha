package com.example.myapp.domain.repository

import com.example.myapp.domain.entities.EmergencyContact
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for emergency contacts
 * Defines the contract for data operations without implementation details
 */
interface ContactRepository {
    
    /**
     * Observe all emergency contacts
     */
    fun observeContacts(): Flow<List<EmergencyContact>>
    
    /**
     * Get all emergency contacts
     */
    suspend fun getContacts(): Result<List<EmergencyContact>>
    
    /**
     * Save a new emergency contact
     */
    suspend fun saveContact(contact: EmergencyContact): Result<Unit>
    
    /**
     * Update an existing emergency contact
     */
    suspend fun updateContact(contact: EmergencyContact): Result<Unit>
    
    /**
     * Delete an emergency contact by ID
     */
    suspend fun deleteContact(contactId: String): Result<Unit>
    
    /**
     * Get primary contacts (marked as primary)
     */
    suspend fun getPrimaryContacts(): Result<List<EmergencyContact>>
    
    /**
     * Check if a contact already exists by phone number
     */
    suspend fun contactExists(phoneNumber: String): Result<Boolean>
    
    /**
     * Clear all contacts (for testing/reset purposes)
     */
    suspend fun clearAllContacts(): Result<Unit>
}
