package com.example.myapp.domain.entities

/**
 * Core business entity for emergency contacts
 * Contains business rules and validation logic
 */
data class EmergencyContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: ContactRelationship = ContactRelationship.OTHER,
    val isPrimary: Boolean = false,
    val isVerified: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "Contact name cannot be blank" }
        require(phoneNumber.isNotBlank()) { "Phone number cannot be blank" }
        require(isValidPhoneNumber(phoneNumber)) { "Invalid phone number format" }
    }
    
    private fun isValidPhoneNumber(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        return cleanPhone.length >= 10 && cleanPhone.matches(Regex("^[+]?[0-9]{10,15}$"))
    }
    
    fun getFormattedPhoneNumber(): String {
        return phoneNumber.replace(Regex("[^0-9+]"), "")
    }
    
    fun isEmergencyService(): Boolean {
        return relationship == ContactRelationship.EMERGENCY_SERVICE
    }
}

enum class ContactRelationship {
    FAMILY,
    FRIEND,
    PARTNER,
    COLLEAGUE,
    EMERGENCY_SERVICE,
    OTHER
}
