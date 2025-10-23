package com.example.myapp

import android.Manifest
import android.content.Intent
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
import androidx.appcompat.widget.Toolbar
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.example.myapp.data.LocationData
import com.example.myapp.databinding.FragmentLocationBinding
import kotlinx.coroutines.flow.launchIn

class LocationFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationManager: LocationManager
    private lateinit var database: FirebaseDatabase
    private lateinit var locationRef: DatabaseReference
    private var sharingSessionId: String? = null
    
    private var googleMap: GoogleMap? = null
    private var currentLocation: Location? = null
    private var isSharing = false
    private var locationUpdatesJob: Job? = null
    
    // UI components
    private lateinit var shareButton: MaterialButton
    private lateinit var sharingStatusText: TextView
    
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
    ): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Initialize LocationManager
        locationManager = LocationManager(requireContext())
        database = FirebaseDatabase.getInstance()
        locationRef = database.getReference("live_locations")
        
        // Initialize UI components
        shareButton = binding.shareToEmergencyButton
        sharingStatusText = binding.sharingStatusText
        
        setupListeners()
        updateSharingUI() // Set initial button state and text
        
        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        // Check location permission
        checkLocationPermission()
    }
    
    private fun setupListeners() {
        // Share to emergency contacts button
        shareButton.setOnClickListener {
            if (isSharing) {
                stopLocationUpdates()
            } else {
                if (currentLocation == null) {
                    Snackbar.make(requireView(), "Location not available yet", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                shareLocationToEmergencyContacts()
            }
        }
    }

    private fun stopLocationUpdates() {
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
        isSharing = false
        sharingSessionId?.let {
            locationRef.child(it).removeValue()
            sharingSessionId = null
        }
        updateSharingUI()
        Snackbar.make(requireView(), "Location sharing stopped", Snackbar.LENGTH_SHORT).show()
    }

    private fun updateSharingUI() {
        if (isSharing) {
            shareButton.text = "Stop Sharing"
            shareButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.emergency_red)
            sharingStatusText.text = "Location sharing is currently ON."
            sharingStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.location_primary))
        } else {
            shareButton.text = "Start Sharing"
            shareButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.periwinkle)
            sharingStatusText.text = "Location sharing is currently OFF."
            sharingStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_dark))
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
            locationManager.getCurrentLocation()
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
            // Generate a unique sharing session ID
            sharingSessionId = locationRef.push().key
            sharingSessionId?.let { sessionId ->
                val latitude = location.latitude
                val longitude = location.longitude
                "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                // In a real app, this would be a dynamic link to a web page that displays the live location
                val shareMessage = "I\'m sharing my live location with you. You can track me here: https://myapp-c30c0.web.app/?sessionId=$sessionId"


                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                }
                startActivity(Intent.createChooser(shareIntent, "Share live location via"))

                // Start continuous location updates to send to Firebase
                startLocationUpdates()
            }
        } ?: run {
            Snackbar.make(requireView(), "Location not available yet", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun startLocationUpdates() {
        if (isSharing) return
        
        isSharing = true
        updateSharingUI() // Update button state immediately after starting sharing
        
        locationUpdatesJob = locationManager.startLocationUpdates(
            interval = 10000, // 10 seconds
            fastestInterval = 5000 // 5 seconds
        )
            .onEach { location ->
                currentLocation = location
                updateMapWithLocation(location)
                sharingSessionId?.let { sessionId ->
                    val locationData = LocationData(location.latitude, location.longitude, System.currentTimeMillis())
                    locationRef.child(sessionId).setValue(locationData)
                } ?: run {
                    // No longer logging this warning as sharingSessionId should always be present here
                }
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
        // We no longer stop location updates here, as the fragment might only be paused (e.g., when switching tabs).
        // Location updates will continue in the background until explicitly stopped or the fragment is destroyed.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationUpdatesJob?.cancel()
        _binding = null
    }
}
