package com.example.myapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapp.domain.entities.EmergencyContact
import com.example.myapp.domain.entities.ContactRelationship
import com.example.myapp.domain.repository.ContactRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementation of ContactRepository using SharedPreferences
 * Handles data persistence and provides reactive data access
 */
class ContactRepositoryImpl(
    context: Context
) : ContactRepository {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Reactive data source
    private val _contactsFlow = MutableStateFlow<List<EmergencyContact>>(emptyList())
    
    init {
        // Load initial contacts
        loadContactsFromPrefs()
    }
    
    override fun observeContacts(): Flow<List<EmergencyContact>> {
        return _contactsFlow.asStateFlow()
    }
    
    override suspend fun getContacts(): Result<List<EmergencyContact>> {
        return try {
            val contacts = loadContactsFromPrefs()
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveContact(contact: EmergencyContact): Result<Unit> {
        return try {
            val currentContacts = _contactsFlow.value.toMutableList()
            
            // Check if contact already exists
            if (currentContacts.any { it.phoneNumber == contact.phoneNumber }) {
                return Result.failure(IllegalArgumentException("Contact already exists"))
            }
            
            currentContacts.add(contact)
            saveContactsToPrefs(currentContacts)
            _contactsFlow.value = currentContacts
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateContact(contact: EmergencyContact): Result<Unit> {
        return try {
            val currentContacts = _contactsFlow.value.toMutableList()
            val index = currentContacts.indexOfFirst { it.id == contact.id }
            
            if (index == -1) {
                return Result.failure(IllegalArgumentException("Contact not found"))
            }
            
            currentContacts[index] = contact
            saveContactsToPrefs(currentContacts)
            _contactsFlow.value = currentContacts
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteContact(contactId: String): Result<Unit> {
        return try {
            val currentContacts = _contactsFlow.value.toMutableList()
            val removed = currentContacts.removeAll { it.id == contactId }
            
            if (!removed) {
                return Result.failure(IllegalArgumentException("Contact not found"))
            }
            
            saveContactsToPrefs(currentContacts)
            _contactsFlow.value = currentContacts
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getPrimaryContacts(): Result<List<EmergencyContact>> {
        return try {
            val primaryContacts = _contactsFlow.value.filter { it.isPrimary }
            Result.success(primaryContacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun contactExists(phoneNumber: String): Result<Boolean> {
        return try {
            val cleanPhoneNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            val exists = _contactsFlow.value.any { 
                it.getFormattedPhoneNumber() == cleanPhoneNumber 
            }
            Result.success(exists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clearAllContacts(): Result<Unit> {
        return try {
            sharedPreferences.edit().remove(CONTACTS_KEY).apply()
            _contactsFlow.value = emptyList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun loadContactsFromPrefs(): List<EmergencyContact> {
        return try {
            val contactsJson = sharedPreferences.getString(CONTACTS_KEY, "[]") ?: "[]"
            val type = object : TypeToken<List<ContactData>>() {}.type
            val contactDataList: List<ContactData> = gson.fromJson(contactsJson, type) ?: emptyList()
            
            // Convert legacy ContactData to EmergencyContact
            val contacts = contactDataList.mapIndexedNotNull { index, contactData ->
                try {
                    EmergencyContact(
                        id = "legacy_$index",
                        name = contactData.name,
                        phoneNumber = contactData.phone,
                        relationship = ContactRelationship.OTHER,
                        isPrimary = false,
                        isVerified = false
                    )
                } catch (e: Exception) {
                    // Skip invalid contacts
                    null
                }
            }
            
            _contactsFlow.value = contacts
            contacts
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveContactsToPrefs(contacts: List<EmergencyContact>) {
        try {
            // Convert to legacy format for backward compatibility
            val contactDataList = contacts.map { contact ->
                ContactData(contact.name, contact.phoneNumber)
            }
            
            val contactsJson = gson.toJson(contactDataList)
            sharedPreferences.edit()
                .putString(CONTACTS_KEY, contactsJson)
                .apply()
        } catch (e: Exception) {
            throw e
        }
    }
    
    companion object {
        private const val PREFS_NAME = "SosPreferences"
        private const val CONTACTS_KEY = "contacts"
    }
    
    // Legacy data class for backward compatibility
    private data class ContactData(
        val name: String,
        val phone: String
    )
}
