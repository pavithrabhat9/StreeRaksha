package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance()
        firestore = Firebase.firestore

        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Edit Profile"

        // Load current user data
        loadUserData()

        // Set up click listeners
        setupClickListeners()
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Set email (read-only)
            findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_email)?.setText(currentUser.email)
            
            // Load additional user data from Firestore
            lifecycleScope.launch {
                try {
                    val userDoc = firestore.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    if (userDoc.exists()) {
                        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_name)?.setText(userDoc.getString("name") ?: "")
                        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_phone)?.setText(userDoc.getString("phone") ?: "")
                        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_emergency_contact_name)?.setText(userDoc.getString("emergencyContactName") ?: "")
                        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_emergency_contact_phone)?.setText(userDoc.getString("emergencyContactPhone") ?: "")
                        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_medical_info)?.setText(userDoc.getString("medicalInfo") ?: "")
                    }
                } catch (e: Exception) {
                    Log.e("EditProfileActivity", "Error loading user data: ${e.message}", e)
                    Toast.makeText(this@EditProfileActivity, "Error loading profile data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Cancel button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)?.setOnClickListener {
            finish()
        }

        // Save button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)?.setOnClickListener {
            saveProfileData()
        }

        // Change photo button
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_change_photo)?.setOnClickListener {
            Toast.makeText(this, "Photo change feature will be available in the next update", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Get form data
        val name = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_name)?.text?.toString()?.trim() ?: ""
        val phone = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_phone)?.text?.toString()?.trim() ?: ""
        val emergencyContactName = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_emergency_contact_name)?.text?.toString()?.trim() ?: ""
        val emergencyContactPhone = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_emergency_contact_phone)?.text?.toString()?.trim() ?: ""
        val medicalInfo = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_medical_info)?.text?.toString()?.trim() ?: ""

        // Validate required fields
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            return
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)?.isEnabled = false
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)?.text = "Saving..."

        // Update Firestore
        lifecycleScope.launch {
            try {
                val userData = hashMapOf(
                    "name" to name,
                    "phone" to phone,
                    "emergencyContactName" to emergencyContactName,
                    "emergencyContactPhone" to emergencyContactPhone,
                    "medicalInfo" to medicalInfo,
                    "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                firestore.collection("users")
                    .document(currentUser.uid)
                    .update(userData)
                    .await()

                Toast.makeText(this@EditProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                Log.e("EditProfileActivity", "Error updating profile: ${e.message}", e)
                Toast.makeText(this@EditProfileActivity, "Error updating profile: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // Re-enable button
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)?.isEnabled = true
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)?.text = "Save Changes"
            }
        }
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

