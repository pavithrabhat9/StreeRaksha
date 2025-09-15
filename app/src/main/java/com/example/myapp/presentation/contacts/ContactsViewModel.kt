package com.example.myapp.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.domain.entities.ContactRelationship
import com.example.myapp.domain.entities.EmergencyContact
import com.example.myapp.domain.usecase.AddContactUseCase
import com.example.myapp.domain.usecase.DeleteContactUseCase
import com.example.myapp.domain.usecase.GetContactsUseCase
import com.example.myapp.domain.usecase.UpdateContactUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for contacts management following Clean Architecture principles
 * Handles contact operations through use cases
 */
class ContactsViewModel(
    private val getContactsUseCase: GetContactsUseCase,
    private val addContactUseCase: AddContactUseCase,
    private val updateContactUseCase: UpdateContactUseCase,
    private val deleteContactUseCase: DeleteContactUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()
    
    sealed class UiEvent {
        data class AddContact(
            val name: String,
            val phoneNumber: String,
            val relationship: ContactRelationship = ContactRelationship.OTHER
        ) : UiEvent()
        
        data class UpdateContact(val contact: EmergencyContact) : UiEvent()
        data class DeleteContact(val contactId: String) : UiEvent()
        object LoadContacts : UiEvent()
        object DismissError : UiEvent()
        object DismissSuccess : UiEvent()
    }
    
    init {
        loadContacts()
        observeContacts()
    }
    
    fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.AddContact -> addContact(event.name, event.phoneNumber, event.relationship)
            is UiEvent.UpdateContact -> updateContact(event.contact)
            is UiEvent.DeleteContact -> deleteContact(event.contactId)
            is UiEvent.LoadContacts -> loadContacts()
            is UiEvent.DismissError -> dismissError()
            is UiEvent.DismissSuccess -> dismissSuccess()
        }
    }
    
    private fun observeContacts() {
        viewModelScope.launch {
            getContactsUseCase.observeContacts()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to observe contacts: ${e.message}"
                    )
                }
                .collect { contacts ->
                    _uiState.value = _uiState.value.copy(
                        contacts = contacts,
                        isLoading = false
                    )
                }
        }
    }
    
    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = getContactsUseCase.getContacts()
                if (result.isSuccess) {
                    val contacts = result.getOrNull() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        contacts = contacts,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load contacts: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }
    
    private fun addContact(name: String, phoneNumber: String, relationship: ContactRelationship) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val contact = EmergencyContact(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    phoneNumber = phoneNumber.trim(),
                    relationship = relationship,
                    isPrimary = false,
                    isVerified = false
                )
                
                when (val result = addContactUseCase(contact)) {
                    is AddContactUseCase.AddContactResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Contact added successfully",
                            error = null
                        )
                    }
                    is AddContactUseCase.AddContactResult.Failure -> {
                        val errorMessage = when (result.error) {
                            is AddContactUseCase.AddContactError.ContactAlreadyExists -> 
                                "Contact with this phone number already exists"
                            is AddContactUseCase.AddContactError.InvalidName -> 
                                "Please enter a valid name"
                            is AddContactUseCase.AddContactError.InvalidPhoneNumber -> 
                                "Please enter a valid phone number"
                            is AddContactUseCase.AddContactError.SystemError -> 
                                result.error.message
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to add contact: ${e.message}"
                )
            }
        }
    }
    
    private fun updateContact(contact: EmergencyContact) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = updateContactUseCase(contact)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Contact updated successfully",
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to update contact: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }
    
    private fun deleteContact(contactId: String) {
        viewModelScope.launch {
            try {
                when (val result = deleteContactUseCase(contactId)) {
                    is DeleteContactUseCase.DeleteContactResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Contact deleted successfully",
                            error = null
                        )
                    }
                    is DeleteContactUseCase.DeleteContactResult.Failure -> {
                        val errorMessage = when (result.error) {
                            is DeleteContactUseCase.DeleteContactError.ContactNotFound -> 
                                "Contact not found"
                            is DeleteContactUseCase.DeleteContactError.SystemError -> 
                                result.error.message
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete contact: ${e.message}"
                )
            }
        }
    }
    
    private fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun dismissSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

data class ContactsUiState(
    val contacts: List<EmergencyContact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
