package com.example.myapp.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.myapp.domain.repository.MessagingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of MessagingService for SMS operations
 * Handles SMS sending with proper error handling and permission checks
 */
class MessagingServiceImpl(
    private val context: Context
) : MessagingService {
    
    override suspend fun sendSms(phoneNumber: String, message: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasSmsPermission()) {
                    return@withContext Result.failure(SecurityException("SMS permission not granted"))
                }
                
                val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
                if (cleanNumber.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Invalid phone number"))
                }
                
                val smsManager = SmsManager.getDefault()
                
                // Handle long messages by dividing them
                val parts = smsManager.divideMessage(message)
                if (parts.size == 1) {
                    smsManager.sendTextMessage(cleanNumber, null, message, null, null)
                } else {
                    smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
                }
                
                Result.success(Unit)
            } catch (e: SecurityException) {
                Result.failure(SecurityException("SMS permission required: ${e.message}"))
            } catch (e: IllegalArgumentException) {
                Result.failure(IllegalArgumentException("Invalid SMS parameters: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(Exception("Failed to send SMS: ${e.message}"))
            }
        }
    }
    
    override suspend fun sendSmsToMultiple(recipients: List<String>, message: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasSmsPermission()) {
                    return@withContext Result.failure(SecurityException("SMS permission not granted"))
                }
                
                val failedNumbers = mutableListOf<String>()
                
                for (phoneNumber in recipients) {
                    val result = sendSms(phoneNumber, message)
                    if (result.isFailure) {
                        failedNumbers.add(phoneNumber)
                    }
                    
                    // Small delay between messages to avoid overwhelming the system
                    kotlinx.coroutines.delay(100)
                }
                
                Result.success(failedNumbers)
            } catch (e: Exception) {
                Result.failure(Exception("Failed to send SMS to multiple recipients: ${e.message}"))
            }
        }
    }
    
    override suspend fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
