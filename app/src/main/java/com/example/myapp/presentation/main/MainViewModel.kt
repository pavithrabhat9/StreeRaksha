package com.example.myapp.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.domain.repository.LocationService
import com.example.myapp.domain.usecase.SendSosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.location.Location
import com.google.android.gms.maps.model.LatLng

data class Place(
    val id: String,
    val name: String,
    val address: String,
    val location: LatLng,
    val type: String
)

/**
 * ViewModel for MainActivity following Clean Architecture principles
 * Handles UI state and coordinates with use cases
 */
class MainViewModel(
    private val sendSosUseCase: SendSosUseCase,
    private val locationService: LocationService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    sealed class UiEvent {
        object SosClicked : UiEvent()
        object SosConfirmed : UiEvent()
        data class NearbySearchRequested(val query: String) : UiEvent()
        object DismissError : UiEvent()
        object DismissSuccess : UiEvent()
        object CheckPermissions : UiEvent()
        object RefreshContactStatus : UiEvent()
    }
    
    fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.SosClicked -> handleSosClick()
            is UiEvent.SosConfirmed -> sendSosAlert()
            is UiEvent.NearbySearchRequested -> handleNearbySearch(event.query)
            is UiEvent.DismissError -> dismissError()
            is UiEvent.DismissSuccess -> dismissSuccess()
            is UiEvent.CheckPermissions -> checkPermissions()
            is UiEvent.RefreshContactStatus -> refreshContactStatus()
        }
    }
    
    private fun handleSosClick() {
        sendSosAlert()
    }
    
    private fun sendSosAlert() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                when (val result = sendSosUseCase()) {
                    is SendSosUseCase.SosResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "SOS sent to ${result.sentCount} contact(s)",
                            error = null
                        )
                    }
                    is SendSosUseCase.SosResult.PartialSuccess -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "SOS sent to ${result.sentCount} contact(s). ${result.failedContacts.size} failed.",
                            error = null
                        )
                    }
                    is SendSosUseCase.SosResult.Failure -> {
                        val errorMessage = when (result.error) {
                            is SendSosUseCase.SosError.NoContacts -> "No emergency contacts added yet!"
                            is SendSosUseCase.SosError.NoSmsPermission -> "SMS permission required"
                            is SendSosUseCase.SosError.AllMessagesFailed -> "Failed to send SOS messages"
                            is SendSosUseCase.SosError.SystemError -> result.error.message
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage,
                            successMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}",
                    successMessage = null
                )
            }
        }
    }
    
    private fun handleNearbySearch(query: String) {
        _uiState.value = _uiState.value.copy(
            nearbySearchQuery = query
        )
        // Clear the query after processing
        _uiState.value = _uiState.value.copy(
            nearbySearchQuery = null
        )
    }
    
    private fun checkPermissions() {
        viewModelScope.launch {
            val hasLocationPermission = locationService.hasLocationPermission()
            val isLocationEnabled = locationService.isLocationEnabled()
            
            _uiState.value = _uiState.value.copy(
                hasLocationPermission = hasLocationPermission,
                isLocationEnabled = isLocationEnabled
            )
        }
    }
    
    private fun refreshContactStatus() {
        _uiState.value = _uiState.value.copy(
            hasContactsConfigured = true
        )
    }
    
    private fun dismissSuccess() {
        _uiState.value = _uiState.value.copy(
            successMessage = null
        )
    }
    
    private fun dismissError() {
        _uiState.value = _uiState.value.copy(
            error = null
        )
    }
    
    fun onContactStatusRefreshed() {
        _uiState.value = _uiState.value.copy(
            shouldRefreshContacts = false
        )
    }
    
    fun requestContactRefresh() {
        _uiState.value = _uiState.value.copy(
            shouldRefreshContacts = true
        )
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val nearbyPlaces: List<Place> = emptyList(),
    val isLocationEnabled: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasContactsConfigured: Boolean = false,
    val currentLocation: Location? = null,
    val nearbySearchQuery: String? = null,
    val shouldRefreshContacts: Boolean = false
)
