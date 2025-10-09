package com.example.myapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapp.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Handle Reset Password button click
        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            
            if (validateEmail(email)) {
                sendPasswordResetEmail(email)
            }
        }

        // Handle Back to Login button click
        binding.tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.tilEmail.error = "Email is required"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Please enter a valid email address"
                false
            }
            else -> {
                binding.tilEmail.error = null
                true
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        showLoading(true)
        
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)
                
                if (task.isSuccessful) {
                    // Show success message
                    Toast.makeText(
                        this,
                        "Password reset email sent. Please check your inbox.",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Close the activity after a short delay
                    binding.root.postDelayed({
                        finish()
                    }, 2000)
                } else {
                    // Show error message
                    Log.d("ForgotPassword", "Password reset failed: ${task.exception?.message}")
                    val errorMessage = task.exception?.message
                    if (errorMessage != null) {
                        if (errorMessage.contains("There is no user record corresponding to this identifier")) {
                            Toast.makeText(this, "No user found with this email address. Please check the email or register.", Toast.LENGTH_LONG).show()
                        } else if (errorMessage.contains("The email address is badly formatted")) {
                            Toast.makeText(this, "Invalid email address format.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Failed to send reset email: ${errorMessage}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, "Failed to send reset email. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnResetPassword.isEnabled = !show
    }
}
