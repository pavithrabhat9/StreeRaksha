package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.myapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // If already logged in, skip login screen
        val userManager = UserManager.getInstance(this)
        if (userManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Add text change listeners for real-time validation
        binding.etEmail.addTextChangedListener {
            binding.tilEmail.error = null
        }
        
        binding.etPassword.addTextChangedListener {
            binding.tilPassword.error = null
        }
    }

    private fun setupListeners() {
        // Login button click listener
        binding.btnLogin.setOnClickListener {
            if (validateInputs()) {
                // TODO: Implement actual login logic
                val email = binding.etEmail.text.toString()
                val password = binding.etPassword.text.toString()
                performLogin(email, password)
            }
        }

        // Forgot password click listener
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Sign up click listener
        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate email
        val email = binding.etEmail.text.toString()
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.email_required)
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.invalid_email)
            isValid = false
        }

        // Validate password
        val password = binding.etPassword.text.toString()
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.password_required)
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.password_too_short)
            isValid = false
        }

        return isValid
    }

    private fun performLogin(email: String, password: String) {
        // Show loading state
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = getString(R.string.logging_in)

        // Authenticate with Firebase
        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = getString(R.string.login)

                if (task.isSuccessful) {
                    // Sign in success
                    val user = Firebase.auth.currentUser
                    if (user != null) {
                        // Update UserManager with the logged-in user
                        val userManager = UserManager.getInstance(this)
                        userManager.setLoggedIn(user.email ?: "")

                        // Show success message
                        Toast.makeText(
                            this,
                            getString(R.string.login_successful),
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navigate to MainActivity
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                } else {
                    // Handle different error types with custom messages
                    val exception = task.exception
                    val errorMessage = when {
                        exception is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                            "Invalid email or password. Please try again."
                        }
                        exception is com.google.firebase.auth.FirebaseAuthInvalidUserException -> {
                            "No account found with this email. Please sign up first."
                        }
                        exception?.message?.contains("password is invalid", ignoreCase = true) == true -> {
                            "Incorrect password. Please try again."
                        }
                        exception?.message?.contains("no user record", ignoreCase = true) == true -> {
                            "No account found with this email."
                        }
                        exception?.message?.contains("network error", ignoreCase = true) == true -> {
                            "Network error. Please check your internet connection."
                        }
                        exception?.message?.contains("too many requests", ignoreCase = true) == true -> {
                            "Too many failed login attempts. Please try again later."
                        }
                        else -> {
                            "Login failed. Please check your credentials and try again."
                        }
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }
}