package com.example.myapp.domain.entities

import java.util.Date

/**
 * Core business entity representing an SOS alert
 */
data class SosAlert(
    val id: String,
    val message: String,
    val location: Location?,
    val timestamp: Date,
    val status: SosStatus,
    val recipients: List<EmergencyContact>
) {
    init {
        require(message.isNotBlank()) { "SOS message cannot be blank" }
        require(recipients.isNotEmpty()) { "At least one recipient is required" }
    }
    
    fun hasLocation(): Boolean = location != null
    
    fun getFormattedMessage(): String {
        return if (hasLocation()) {
            "$message My location: ${location!!.toGoogleMapsUrl()}"
        } else {
            message
        }
    }
    
    fun getPrimaryRecipients(): List<EmergencyContact> {
        return recipients.filter { it.isPrimary }
    }
}

data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null
) {
    fun toGoogleMapsUrl(): String {
        return "https://maps.google.com/?q=$latitude,$longitude"
    }
    
    fun isValid(): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
}

enum class SosStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED,
    PARTIAL_SUCCESS
}
