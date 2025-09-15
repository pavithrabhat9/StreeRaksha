package com.example.myapp.domain.usecase

import com.example.myapp.domain.entities.EmergencyContact
import com.example.myapp.domain.entities.ContactRelationship
import com.example.myapp.domain.entities.Location
import com.example.myapp.domain.repository.ContactRepository
import com.example.myapp.domain.repository.LocationService
import com.example.myapp.domain.repository.MessagingService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Unit tests for SendSosUseCase
 * Tests critical emergency functionality to ensure reliability
 */
class SendSosUseCaseTest {
    
    @Mock
    private lateinit var contactRepository: ContactRepository
    
    @Mock
    private lateinit var locationService: LocationService
    
    @Mock
    private lateinit var messagingService: MessagingService
    
    private lateinit var sendSosUseCase: SendSosUseCase
    
    private val testContact = EmergencyContact(
        id = "test-id",
        name = "Test Contact",
        phoneNumber = "+1234567890",
        relationship = ContactRelationship.FAMILY,
        isPrimary = true,
        isVerified = true
    )
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        sendSosUseCase = SendSosUseCase(
            contactRepository,
            locationService,
            messagingService
        )
    }
    
    @Test
    fun `sendSos should return failure when no SMS permission`() = runTest {
        // Given
        `when`(messagingService.hasSmsPermission()).thenReturn(false)
        
        // When
        val result = sendSosUseCase()
        
        // Then
        assertTrue(result is SendSosUseCase.SosResult.Failure)
        assertEquals(
            SendSosUseCase.SosError.NoSmsPermission,
            (result as SendSosUseCase.SosResult.Failure).error
        )
    }
    
    @Test
    fun `sendSos should return failure when no contacts available`() = runTest {
        // Given
        `when`(messagingService.hasSmsPermission()).thenReturn(true)
        `when`(contactRepository.getContacts()).thenReturn(Result.success(emptyList()))
        
        // When
        val result = sendSosUseCase()
        
        // Then
        assertTrue(result is SendSosUseCase.SosResult.Failure)
        assertEquals(
            SendSosUseCase.SosError.NoContacts,
            (result as SendSosUseCase.SosResult.Failure).error
        )
    }
    
    @Test
    fun `sendSos should return success when message sent successfully`() = runTest {
        // Given
        val contacts = listOf(testContact)
        `when`(messagingService.hasSmsPermission()).thenReturn(true)
        `when`(contactRepository.getContacts()).thenReturn(Result.success(contacts))
        `when`(locationService.getCurrentLocation()).thenReturn(
            Result.success(Location(37.7749, -122.4194))
        )
        `when`(messagingService.sendSmsToMultiple(any(), any())).thenReturn(
            Result.success(emptyList()) // No failed numbers
        )
        
        // When
        val result = sendSosUseCase()
        
        // Then
        assertTrue(result is SendSosUseCase.SosResult.Success)
        assertEquals(1, (result as SendSosUseCase.SosResult.Success).sentCount)
    }
    
    @Test
    fun `sendSos should include location in message when available`() = runTest {
        // Given
        val contacts = listOf(testContact)
        val testLocation = Location(37.7749, -122.4194)
        `when`(messagingService.hasSmsPermission()).thenReturn(true)
        `when`(contactRepository.getContacts()).thenReturn(Result.success(contacts))
        `when`(locationService.getCurrentLocation()).thenReturn(Result.success(testLocation))
        `when`(messagingService.sendSmsToMultiple(any(), any())).thenReturn(
            Result.success(emptyList())
        )
        
        // When
        sendSosUseCase()
        
        // Then
        verify(messagingService).sendSmsToMultiple(
            eq(listOf(testContact.getFormattedPhoneNumber())),
            argThat { message ->
                message.contains("https://maps.google.com/?q=${testLocation.latitude},${testLocation.longitude}")
            }
        )
    }
    
    @Test
    fun `sendSos should return partial success when some messages fail`() = runTest {
        // Given
        val contacts = listOf(testContact, testContact.copy(id = "test-2", phoneNumber = "+0987654321"))
        `when`(messagingService.hasSmsPermission()).thenReturn(true)
        `when`(contactRepository.getContacts()).thenReturn(Result.success(contacts))
        `when`(locationService.getCurrentLocation()).thenReturn(Result.success(null))
        `when`(messagingService.sendSmsToMultiple(any(), any())).thenReturn(
            Result.success(listOf("+0987654321")) // One failed number
        )
        
        // When
        val result = sendSosUseCase()
        
        // Then
        assertTrue(result is SendSosUseCase.SosResult.PartialSuccess)
        val partialResult = result as SendSosUseCase.SosResult.PartialSuccess
        assertEquals(1, partialResult.sentCount)
        assertEquals(1, partialResult.failedContacts.size)
    }
}
