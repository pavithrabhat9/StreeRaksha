package com.example.myapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.adapter.EmergencyContactAdapter
import com.example.myapp.databinding.ActivityEmergencyHelplineBinding
import com.example.myapp.model.EmergencyContacts
import com.example.myapp.utils.FeaturePermissionHelper

class EmergencyHelplineActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmergencyHelplineBinding
    companion object {
        private const val CALL_PERMISSION_REQUEST_CODE = 123
        private const val EMERGENCY_CALL_PERMISSION_REQUEST_CODE = 124
    }

    private var pendingPhoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyHelplineBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set the activity reference for data binding
        binding.lifecycleOwner = this
        binding.activity = this

        setupToolbar()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        val adapter = EmergencyContactAdapter()
        
        binding.rvEmergencyContacts.apply {
            layoutManager = LinearLayoutManager(this@EmergencyHelplineActivity)
            this.adapter = adapter
        }
        
        // Submit the list to the adapter
        adapter.submitList(EmergencyContacts.contacts)
    }
    
    // This function can be called from XML via data binding for the emergency call button
    fun onEmergencyCallClick() {
        try {
            pendingPhoneNumber = "112"
            makePhoneCall()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun makePhoneCall() {
        pendingPhoneNumber?.let { number ->
            try {
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$number")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startActivity(callIntent)
                } else {
                    // Show custom dialog guiding user to Settings
                    FeaturePermissionHelper.Companion.PhoneFeature.checkPermissions(this) {
                        startActivity(callIntent)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == CALL_PERMISSION_REQUEST_CODE || 
            requestCode == EMERGENCY_CALL_PERMISSION_REQUEST_CODE) {
            
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, make the call
                makePhoneCall()
            } else {
                Toast.makeText(
                    this,
                    "Call permission is required to make calls directly",
                    Toast.LENGTH_SHORT
                ).show()
            }
            pendingPhoneNumber = null
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Recheck phone permissions when returning from settings
        FeaturePermissionHelper.Companion.PhoneFeature.recheckPermissions(this) {
            // Phone permission granted, can proceed with call features
        }
    }

    // This method is no longer needed as we're handling everything in checkCallPermission
}
