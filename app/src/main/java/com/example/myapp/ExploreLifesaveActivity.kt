package com.example.myapp

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.myapp.utils.FeaturePermissionHelper

class ExploreLifesaveActivity : AppCompatActivity() {
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
        private const val MAPS_OPEN_DELAY = 300L // Small delay for better UX
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explore_lifesave)
        
        // Set up back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }
        
        // Set up click listeners for each card
        setupCard(R.id.cardPoliceStation, getString(R.string.police_station))
        setupCard(R.id.cardHospital, getString(R.string.hospital))
        setupCard(R.id.cardPharmacy, getString(R.string.pharmacy))
        setupCard(R.id.cardEmergencyShelter, getString(R.string.emergency_shelter))
    }
    
    private fun showLoading() {
        // Show a simple loading toast instead of a dialog
        Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show()
    }
    
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }
    
    private fun showNoInternetDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("No Internet Connection")
            .setMessage("Please check your internet connection and try again.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupCard(cardId: Int, searchQuery: String) {
        val card = findViewById<CardView>(cardId)
        
        card.setOnClickListener {
            if (!isNetworkAvailable()) {
                showNoInternetDialog()
                return@setOnClickListener
            }
            
            if (checkLocationPermission()) {
                showLoading()
                Handler(Looper.getMainLooper()).postDelayed({
                    openMaps(searchQuery)
                }, MAPS_OPEN_DELAY)
            } else {
                // Show custom dialog guiding user to Settings
                FeaturePermissionHelper.Companion.LocationFeature.checkPermissions(this@ExploreLifesaveActivity) {
                    showLoading()
                    Handler(Looper.getMainLooper()).postDelayed({
                        openMaps(searchQuery)
                    }, MAPS_OPEN_DELAY)
                }
            }
        }
    }

    private fun openMaps(query: String) {
        try {
            // Check if Google Maps is installed
            packageManager.getPackageInfo(GOOGLE_MAPS_PACKAGE, PackageManager.GET_ACTIVITIES)
            
            // Create a URI for the search query
            val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode("$query near me")}")
            
            // Create an Intent from gmmIntentUri
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            
            // Set the package to ensure it opens in Google Maps
            mapIntent.setPackage(GOOGLE_MAPS_PACKAGE)
            
            // Verify that the intent will resolve to an activity
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // If Google Maps is not installed, open in browser
                openInBrowser(query)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Google Maps is not installed, open in browser
            openInBrowser(query)
        } catch (e: Exception) {
            // Handle any other exceptions
            Toast.makeText(this, "Error opening maps: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openInBrowser(query: String) {
        try {
            val searchUrl = "https://www.google.com/maps/search/${Uri.encode("$query near me")}"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
            startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    


    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, can proceed with location-related operations
                Toast.makeText(this, "Permission granted. Please tap the option again.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location permission is required to find nearby places.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Recheck location permissions when returning from settings
        FeaturePermissionHelper.Companion.LocationFeature.recheckPermissions(this) {
            // Location permission granted, can proceed with location features
        }
    }
}
