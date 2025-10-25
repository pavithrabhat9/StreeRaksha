package com.example.myapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Change Password"

        // Set up click listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Cancel button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)?.setOnClickListener {
            finish()
        }

        // Change password button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_change_password)?.setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Get form data
        val currentPassword = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_current_password)?.text?.toString() ?: ""
        val newPassword = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_new_password)?.text?.toString() ?: ""
        val confirmPassword = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_confirm_password)?.text?.toString() ?: ""

        // Validate inputs
        if (currentPassword.isEmpty()) {
            Toast.makeText(this, "Please enter your current password", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Please enter a new password", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentPassword == newPassword) {
            Toast.makeText(this, "New password must be different from current password", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_change_password)?.isEnabled = false
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_change_password)?.text = "Changing..."

        // Re-authenticate and change password
        lifecycleScope.launch {
            try {
                // Re-authenticate user with current password
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
                    currentUser.email ?: "", currentPassword
                )
                currentUser.reauthenticate(credential).await()

                // Change password
                currentUser.updatePassword(newPassword).await()

                Toast.makeText(this@ChangePasswordActivity, "Password changed successfully!", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                Log.e("ChangePasswordActivity", "Error changing password: ${e.message}", e)
                when {
                    e.message?.contains("wrong-password", ignoreCase = true) == true -> {
                        Toast.makeText(this@ChangePasswordActivity, "Current password is incorrect", Toast.LENGTH_LONG).show()
                    }
                    e.message?.contains("weak-password", ignoreCase = true) == true -> {
                        Toast.makeText(this@ChangePasswordActivity, "Password is too weak. Please choose a stronger password.", Toast.LENGTH_LONG).show()
                    }
                    e.message?.contains("requires-recent-login", ignoreCase = true) == true -> {
                        Toast.makeText(this@ChangePasswordActivity, "Please log out and log back in, then try again", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(this@ChangePasswordActivity, "Error changing password: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } finally {
                // Re-enable button
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_change_password)?.isEnabled = true
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_change_password)?.text = "Change Password"
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


