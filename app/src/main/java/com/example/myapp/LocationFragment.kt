package com.example.myapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapp.utils.LocationManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationFragment : Fragment(), LocationManager.LocationCallback, OnMapReadyCallback {

    private lateinit var locationManager: LocationManager
    
    // UI components
    private lateinit var shareToEmergencyButton: Button
    
    private var googleMap: GoogleMap? = null
    private var currentLocation: Location? = null
    private var isSharing = false
    private var locationUpdatesJob: Job? = null
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (locationGranted) {
            // Permission granted, get location
            getLocation()
        } else {
            // Permission denied
            Snackbar.make(
                requireView(),
                "Location permission is required to use this feature",
                Snackbar.LENGTH_LONG
            ).setAction("Settings") {
                // Open app settings
                locationManager.openLocationSettings()
            }.show()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize LocationManager
        locationManager = LocationManager(requireContext())
        
        // Initialize UI components
        initializeViews(view)
        setupListeners()
        
        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        // Check location permission
        checkLocationPermission()
    }
    
    private fun initializeViews(view: View) {
        shareToEmergencyButton = view.findViewById(R.id.share_to_emergency_button)
    }
    
    private fun setupListeners() {
        // Share to emergency contacts button
        shareToEmergencyButton.setOnClickListener {
            if (currentLocation == null) {
                Snackbar.make(requireView(), "Location not available yet", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            shareLocationToEmergencyContacts()
        }
    }
    
    private fun checkLocationPermission() {
        when {
            hasLocationPermission() -> {
                // Permission already granted
                getLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Show rationale and request permission
                Snackbar.make(
                    requireView(),
                    "Location permission is needed for this feature",
                    Snackbar.LENGTH_LONG
                ).setAction("Grant") {
                    requestLocationPermission()
                }.show()
            }
            else -> {
                // Request permission directly
                requestLocationPermission()
            }
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun getLocation() {
        viewLifecycleOwner.lifecycleScope.launch {
            locationManager.getCurrentLocation(callback = this@LocationFragment)
                .onSuccess { location ->
                    if (location != null) {
                        currentLocation = location
                        updateMapWithLocation(location)
                    } else {
                        Snackbar.make(requireView(), "Could not get location", Snackbar.LENGTH_SHORT).show()
                    }
                }
                .onFailure { exception ->
                    Snackbar.make(requireView(), "Error: ${exception.message}", Snackbar.LENGTH_SHORT).show()
                }
        }
    }
    

    
    private fun updateMapWithLocation(location: Location) {
        try {
            googleMap?.let { map ->
                val latLng = LatLng(location.latitude, location.longitude)
                
                // Clear previous markers
                map.clear()
                
                // Add marker at current location
                map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("My Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
                
                // Move camera to current location
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                )
                
                // Enable my location button if permission is granted
                if (hasLocationPermission()) {
                    try {
                        map.isMyLocationEnabled = true
                    } catch (e: SecurityException) {
                        Log.e("LocationFragment", "Location permission error", e)
                    }
                }
            } ?: run {
                Log.w("LocationFragment", "Map not initialized yet")
            }
        } catch (e: Exception) {
            Log.e("LocationFragment", "Error updating map with location", e)
            // Show error message to user
            view?.let {
                Snackbar.make(it, "Error updating map: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun shareLocationToEmergencyContacts() {
        currentLocation?.let { location ->
            // Get emergency contacts from shared preferences or database
            // For demo purposes, we'll just show a success message
            
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Simulate sharing to emergency contacts
                    // In a real app, you would send SMS or use another sharing method
                    
                    // Show success message
                    Snackbar.make(
                        requireView(),
                        "Location shared with emergency contacts",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    
                    // Start continuous location updates to keep emergency contacts updated
                    startLocationUpdates()
                } catch (e: Exception) {
                    Snackbar.make(
                        requireView(),
                        "Failed to share location: ${e.message}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun startLocationUpdates() {
        if (isSharing) return
        
        isSharing = true
        
        locationUpdatesJob = locationManager.startLocationUpdates(
            interval = 10000, // 10 seconds
            fastestInterval = 5000, // 5 seconds
            callback = this
        )
            .onEach { location ->
                currentLocation = location
                updateMapWithLocation(location)
                // In a real app, you would send updated location to emergency contacts
            }
            .catch { e ->
                Snackbar.make(
                    requireView(),
                    "Error updating location: ${e.message}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }
    
    override fun onMapReady(map: GoogleMap) {
        try {
            googleMap = map
            
            // Configure map settings
            map.uiSettings.apply {
                isZoomControlsEnabled = true
                isCompassEnabled = true
                isMapToolbarEnabled = true
            }
            
            // If we already have a location, update the map
            if (currentLocation != null) {
                updateMapWithLocation(currentLocation!!)
            } else {
                // Set a default location (India center) if no location is available yet
                val defaultLocation = LatLng(20.5937, 78.9629) // Center of India
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f))
                
                // Try to get location immediately
                getLocation()
            }
            
            // Enable my location button if permission is granted
            if (hasLocationPermission()) {
                try {
                    map.isMyLocationEnabled = true
                } catch (e: SecurityException) {
                    Log.e("LocationFragment", "Location permission error", e)
                }
            }
        } catch (e: Exception) {
            // Handle map initialization error
            Snackbar.make(
                requireView(),
                "Map initialization error: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
            
            // Log the error
            Log.e("LocationFragment", "Map initialization error", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Stop location updates when fragment is paused
        if (isSharing) {
            locationUpdatesJob?.cancel()
            locationUpdatesJob = null
            isSharing = false
        }
    }
    
    // LocationCallback implementation
    override fun onLocationReceived(location: Location) {
        // This will be called from the LocationManager
        // We already handle this in the flow collector
    }
    
    override fun onLocationError(errorMessage: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            Snackbar.make(requireView(), "Error: $errorMessage", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    override fun onStatusChanged(status: LocationManager.LocationStatus) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (!status.hasPermission) {
                requestLocationPermission()
                Snackbar.make(requireView(), "Location permission not granted", Snackbar.LENGTH_SHORT).show()
            } else if (!status.isEnabled) {
                Snackbar.make(
                    requireView(),
                    "Please enable location services",
                    Snackbar.LENGTH_LONG
                ).setAction("Settings") {
                    locationManager.openLocationSettings()
                }.show()
            }
        }
    }
}
