package com.example.myapp.domain.repository

/**
 * Repository interface for messaging services
 * Defines the contract for SMS operations without implementation details
 */
interface MessagingService {
    
    /**
     * Send SMS to a single recipient
     * @param phoneNumber The recipient's phone number
     * @param message The message to send
     * @return Result indicating success or failure
     */
    suspend fun sendSms(phoneNumber: String, message: String): Result<Unit>
    
    /**
     * Send SMS to multiple recipients
     * @param recipients List of phone numbers
     * @param message The message to send
     * @return Result containing list of failed phone numbers
     */
    suspend fun sendSmsToMultiple(recipients: List<String>, message: String): Result<List<String>>
    
    /**
     * Check if the app has SMS permission
     */
    suspend fun hasSmsPermission(): Boolean
}
