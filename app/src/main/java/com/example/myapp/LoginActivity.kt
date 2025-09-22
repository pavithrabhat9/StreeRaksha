package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.myapp.databinding.ActivityLoginBinding
import com.google.android.material.textfield.TextInputLayout

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
            // TODO: Implement forgot password functionality
            Toast.makeText(this, "Forgot password clicked", Toast.LENGTH_SHORT).show()
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
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email format"
            isValid = false
        }

        // Validate password
        val password = binding.etPassword.text.toString()
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    private fun performLogin(email: String, password: String) {
        // Get UserManager instance
        val userManager = UserManager.getInstance(this)
        
        // Check if user is registered
        if (!userManager.isUserRegistered(email)) {
            binding.tilEmail.error = getString(R.string.user_not_registered)
            return
        }
        
        // Show loading state
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = getString(R.string.logging_in)

        // Authenticate user
        val isAuthenticated = userManager.authenticateUser(email, password)
        
        // Simulating network delay
        binding.btnLogin.postDelayed({
            if (isAuthenticated) {
                // Persist session
                userManager.setLoggedIn(email)
                // Navigate to MainActivity after successful login
                Toast.makeText(this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                // Show error for incorrect password
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = getString(R.string.login)
                binding.tilPassword.error = getString(R.string.incorrect_password)
            }
        }, 1000)
    }
}