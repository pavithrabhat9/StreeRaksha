package com.example.myapp.di

import android.content.Context
import com.example.myapp.data.repository.ContactRepositoryImpl
import com.example.myapp.data.service.LocationServiceImpl
import com.example.myapp.data.service.MessagingServiceImpl
import com.example.myapp.domain.repository.ContactRepository
import com.example.myapp.domain.repository.LocationService
import com.example.myapp.domain.repository.MessagingService
import com.example.myapp.domain.usecase.AddContactUseCase
import com.example.myapp.domain.usecase.DeleteContactUseCase
import com.example.myapp.domain.usecase.GetContactsUseCase
import com.example.myapp.domain.usecase.SendSosUseCase
import com.example.myapp.domain.usecase.UpdateContactUseCase

/**
 * Simple dependency injection container for Clean Architecture
 * In a real app, you might use Hilt, Koin, or similar DI framework
 */
class AppModule(private val context: Context) {
    
    // Data Layer
    private val contactRepository: ContactRepository by lazy {
        ContactRepositoryImpl(context)
    }
    
    private val locationService: LocationService by lazy {
        LocationServiceImpl(context)
    }
    
    private val messagingService: MessagingService by lazy {
        MessagingServiceImpl(context)
    }
    
    // Domain Layer - Use Cases
    val getContactsUseCase: GetContactsUseCase by lazy {
        GetContactsUseCase(contactRepository)
    }
    
    val addContactUseCase: AddContactUseCase by lazy {
        AddContactUseCase(contactRepository)
    }
    
    val updateContactUseCase: UpdateContactUseCase by lazy {
        UpdateContactUseCase(contactRepository)
    }
    
    val deleteContactUseCase: DeleteContactUseCase by lazy {
        DeleteContactUseCase(contactRepository)
    }
    
    val sendSosUseCase: SendSosUseCase by lazy {
        SendSosUseCase(contactRepository, locationService, messagingService)
    }
    
    // Provide repositories for ViewModels that need direct access
    fun provideContactRepository(): ContactRepository = contactRepository
    fun provideLocationService(): LocationService = locationService
    fun provideMessagingService(): MessagingService = messagingService
}
