package com.example.myapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapp.di.AppModule
import com.example.myapp.presentation.contacts.ContactsViewModel
import com.example.myapp.presentation.main.MainViewModel

/**
 * ViewModelFactory for creating ViewModels with dependencies
 * Following Clean Architecture principles with proper dependency injection
 */
class ViewModelFactory(private val appModule: AppModule) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            MainViewModel::class.java -> {
                MainViewModel(
                    sendSosUseCase = appModule.sendSosUseCase,
                    locationService = appModule.provideLocationService()
                ) as T
            }
            ContactsViewModel::class.java -> {
                ContactsViewModel(
                    getContactsUseCase = appModule.getContactsUseCase,
                    addContactUseCase = appModule.addContactUseCase,
                    updateContactUseCase = appModule.updateContactUseCase,
                    deleteContactUseCase = appModule.deleteContactUseCase
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
