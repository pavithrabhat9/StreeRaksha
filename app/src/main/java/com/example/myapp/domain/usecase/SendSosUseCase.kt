package com.example.myapp.domain.usecase

import com.example.myapp.domain.entities.EmergencyContact
import com.example.myapp.domain.entities.SosAlert
import com.example.myapp.domain.entities.SosStatus
import com.example.myapp.domain.repository.ContactRepository
import com.example.myapp.domain.repository.LocationService
import com.example.myapp.domain.repository.MessagingService
import java.util.Date
import java.util.UUID

/**
 * Use case for sending SOS alerts
 * Contains the business logic for emergency situations
 */
class SendSosUseCase(
    private val contactRepository: ContactRepository,
    private val locationService: LocationService,
    private val messagingService: MessagingService
) {
    
    sealed class SosResult {
        data class Success(val alert: SosAlert, val sentCount: Int) : SosResult()
        data class PartialSuccess(val alert: SosAlert, val sentCount: Int, val failedContacts: List<EmergencyContact>) : SosResult()
        data class Failure(val error: SosError) : SosResult()
    }
    
    sealed class SosError {
        object NoContacts : SosError()
        object NoSmsPermission : SosError()
        object AllMessagesFailed : SosError()
        data class SystemError(val message: String) : SosError()
    }
    
    suspend operator fun invoke(customMessage: String? = null): SosResult {
        try {
            // 1. Check SMS permission
            if (!messagingService.hasSmsPermission()) {
                return SosResult.Failure(SosError.NoSmsPermission)
            }
            
            // 2. Get emergency contacts
            val contactsResult = contactRepository.getContacts()
            if (contactsResult.isFailure) {
                return SosResult.Failure(SosError.SystemError("Failed to load contacts"))
            }
            
            val contacts = contactsResult.getOrNull() ?: emptyList()
            if (contacts.isEmpty()) {
                return SosResult.Failure(SosError.NoContacts)
            }
            
            // 3. Get current location (optional)
            val location = locationService.getCurrentLocation().getOrNull()
            
            // 4. Create SOS alert
            val message = customMessage ?: "SOS! I'm in danger."
            val alert = SosAlert(
                id = UUID.randomUUID().toString(),
                message = message,
                location = location,
                timestamp = Date(),
                status = SosStatus.SENDING,
                recipients = contacts
            )
            
            // 5. Send messages to all contacts
            val phoneNumbers = contacts.map { it.getFormattedPhoneNumber() }
            val sendResult = messagingService.sendSmsToMultiple(phoneNumbers, alert.getFormattedMessage())
            
            return when {
                sendResult.isSuccess -> {
                    val failedNumbers = sendResult.getOrNull() ?: emptyList()
                    val sentCount = contacts.size - failedNumbers.size
                    
                    when {
                        failedNumbers.isEmpty() -> {
                            SosResult.Success(
                                alert.copy(status = SosStatus.SENT),
                                sentCount
                            )
                        }
                        sentCount > 0 -> {
                            val failedContacts = contacts.filter { contact ->
                                failedNumbers.contains(contact.getFormattedPhoneNumber())
                            }
                            SosResult.PartialSuccess(
                                alert.copy(status = SosStatus.PARTIAL_SUCCESS),
                                sentCount,
                                failedContacts
                            )
                        }
                        else -> {
                            SosResult.Failure(SosError.AllMessagesFailed)
                        }
                    }
                }
                else -> {
                    SosResult.Failure(SosError.AllMessagesFailed)
                }
            }
        } catch (e: Exception) {
            return SosResult.Failure(SosError.SystemError(e.message ?: "Unknown error"))
        }
    }
}
