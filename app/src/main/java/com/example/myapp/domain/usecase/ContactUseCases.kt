package com.example.myapp.domain.usecase

import com.example.myapp.domain.entities.EmergencyContact
import com.example.myapp.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Use case for getting all emergency contacts
 */
class GetContactsUseCase(
    private val contactRepository: ContactRepository
) {
    fun observeContacts(): Flow<List<EmergencyContact>> {
        return contactRepository.observeContacts()
    }
    
    suspend fun getContacts(): Result<List<EmergencyContact>> {
        return contactRepository.getContacts()
    }
}

/**
 * Use case for adding a new emergency contact
 */
class AddContactUseCase(
    private val contactRepository: ContactRepository
) {
    
    sealed class AddContactResult {
        data class Success(val contact: EmergencyContact) : AddContactResult()
        data class Failure(val error: AddContactError) : AddContactResult()
    }
    
    sealed class AddContactError {
        object ContactAlreadyExists : AddContactError()
        object InvalidPhoneNumber : AddContactError()
        object InvalidName : AddContactError()
        data class SystemError(val message: String) : AddContactError()
    }
    
    suspend operator fun invoke(contact: EmergencyContact): AddContactResult {
        return invoke(contact.name, contact.phoneNumber, contact.relationship, contact.isPrimary)
    }
    
    suspend fun invoke(
        name: String,
        phoneNumber: String,
        relationship: com.example.myapp.domain.entities.ContactRelationship = com.example.myapp.domain.entities.ContactRelationship.OTHER,
        isPrimary: Boolean = false
    ): AddContactResult {
        try {
            // Validate inputs
            if (name.isBlank()) {
                return AddContactResult.Failure(AddContactError.InvalidName)
            }
            
            if (phoneNumber.isBlank()) {
                return AddContactResult.Failure(AddContactError.InvalidPhoneNumber)
            }
            
            // Check if contact already exists
            val existsResult = contactRepository.contactExists(phoneNumber)
            if (existsResult.isSuccess && existsResult.getOrNull() == true) {
                return AddContactResult.Failure(AddContactError.ContactAlreadyExists)
            }
            
            // Create new contact
            val contact = EmergencyContact(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                phoneNumber = phoneNumber.trim(),
                relationship = relationship,
                isPrimary = isPrimary,
                isVerified = false
            )
            
            // Save contact
            val saveResult = contactRepository.saveContact(contact)
            return if (saveResult.isSuccess) {
                AddContactResult.Success(contact)
            } else {
                AddContactResult.Failure(AddContactError.SystemError("Failed to save contact"))
            }
            
        } catch (e: IllegalArgumentException) {
            return AddContactResult.Failure(AddContactError.InvalidPhoneNumber)
        } catch (e: Exception) {
            return AddContactResult.Failure(AddContactError.SystemError(e.message ?: "Unknown error"))
        }
    }
}

/**
 * Use case for deleting an emergency contact
 */
class DeleteContactUseCase(
    private val contactRepository: ContactRepository
) {
    
    sealed class DeleteContactResult {
        object Success : DeleteContactResult()
        data class Failure(val error: DeleteContactError) : DeleteContactResult()
    }
    
    sealed class DeleteContactError {
        object ContactNotFound : DeleteContactError()
        data class SystemError(val message: String) : DeleteContactError()
    }
    
    suspend operator fun invoke(contactId: String): DeleteContactResult {
        return try {
            val deleteResult = contactRepository.deleteContact(contactId)
            if (deleteResult.isSuccess) {
                DeleteContactResult.Success
            } else {
                DeleteContactResult.Failure(DeleteContactError.SystemError("Failed to delete contact"))
            }
        } catch (e: Exception) {
            DeleteContactResult.Failure(DeleteContactError.SystemError(e.message ?: "Unknown error"))
        }
    }
}

/**
 * Use case for updating an emergency contact
 */
class UpdateContactUseCase(
    private val contactRepository: ContactRepository
) {
    
    suspend operator fun invoke(contact: EmergencyContact): Result<Unit> {
        return try {
            contactRepository.updateContact(contact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
