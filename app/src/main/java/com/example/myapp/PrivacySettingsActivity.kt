package com.example.myapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class PrivacySettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_settings)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("PrivacySettings", MODE_PRIVATE)

        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Privacy Settings"

        // Load current settings
        loadSettings()

        // Set up click listeners
        setupClickListeners()
    }

    private fun loadSettings() {
        // Load location sharing setting
        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_location_sharing)?.isChecked = 
            sharedPreferences.getBoolean("location_sharing_enabled", true)

        // Load high accuracy setting
        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_high_accuracy)?.isChecked = 
            sharedPreferences.getBoolean("high_accuracy_enabled", true)

        // Load analytics setting
        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_analytics)?.isChecked = 
            sharedPreferences.getBoolean("analytics_enabled", true)

        // Load crash reports setting
        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_crash_reports)?.isChecked = 
            sharedPreferences.getBoolean("crash_reports_enabled", true)

        // Load emergency access setting
        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_emergency_access)?.isChecked = 
            sharedPreferences.getBoolean("emergency_access_enabled", true)

        // Load data retention setting
        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_data_retention)?.isChecked = 
            sharedPreferences.getBoolean("data_retention_enabled", true)
    }

    private fun setupClickListeners() {
        // Reset to default button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_reset)?.setOnClickListener {
            resetToDefaults()
        }

        // Save settings button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)?.setOnClickListener {
            saveSettings()
        }

        // Set up switch change listeners for real-time updates
        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_location_sharing)?.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("location_sharing_enabled", isChecked).apply()
            if (isChecked) {
                Toast.makeText(this, "Location sharing enabled for SOS alerts", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location sharing disabled", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_high_accuracy)?.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("high_accuracy_enabled", isChecked).apply()
        }

        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_analytics)?.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("analytics_enabled", isChecked).apply()
        }

        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_crash_reports)?.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("crash_reports_enabled", isChecked).apply()
        }

        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_emergency_access)?.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("emergency_access_enabled", isChecked).apply()
        }

        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_data_retention)?.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("data_retention_enabled", isChecked).apply()
        }
    }

    private fun resetToDefaults() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset to Default")
            .setMessage("Are you sure you want to reset all privacy settings to their default values?")
            .setPositiveButton("Reset") { _, _ ->
                // Reset all switches to default values
                findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_location_sharing)?.isChecked = true
                findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_high_accuracy)?.isChecked = true
                findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_analytics)?.isChecked = true
                findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_crash_reports)?.isChecked = true
                findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_emergency_access)?.isChecked = true
                findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_data_retention)?.isChecked = true

                // Clear all preferences
                sharedPreferences.edit().clear().apply()

                Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveSettings() {
        // Settings are already saved in real-time via switch listeners
        Toast.makeText(this, "Privacy settings saved successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}


