package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.myapp.databinding.ActivityRegistrationBinding
 

class RegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.etName.addTextChangedListener {
            binding.tilName.error = null
        }
        binding.etPhone.addTextChangedListener {
            binding.tilPhone.error = null
        }
        binding.etEmail.addTextChangedListener { 
            binding.tilEmail.error = null
        }
        
        binding.etPassword.addTextChangedListener {
            binding.tilPassword.error = null
        }

        binding.etConfirmPassword.addTextChangedListener {
            binding.tilConfirmPassword.error = null
        }
        binding.cbTerms.setOnCheckedChangeListener { _, _ ->
            // no-op; validated on submit
        }
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            if (validateInputs()) {
                performRegistration()
            }
        }

        binding.tvLogin.setOnClickListener {
            // Navigate back to login
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate name
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.name_required)
            isValid = false
        }

        // Validate phone (basic 10-15 digits, optional +)
        val phone = binding.etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.tilPhone.error = getString(R.string.phone_required)
            isValid = false
        } else if (!phone.matches(Regex("^\\+?[0-9]{10,15}$"))) {
            binding.tilPhone.error = getString(R.string.invalid_phone)
            isValid = false
        }

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
            binding.tilPassword.error = getString(R.string.password_length)
            isValid = false
        }

        // Validate confirm password
        val confirmPassword = binding.etConfirmPassword.text.toString()
        if (confirmPassword != password) {
            binding.tilConfirmPassword.error = getString(R.string.passwords_dont_match)
            isValid = false
        }

        // Validate terms acceptance
        if (!binding.cbTerms.isChecked) {
            Toast.makeText(this, getString(R.string.please_accept_terms), Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun performRegistration() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        
        // Get UserManager instance
        val userManager = UserManager.getInstance(this)

        // Validate inputs
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.email_required)
            return
        }

        if (!userManager.isValidEmail(email)) {
            binding.tilEmail.error = getString(R.string.invalid_email)
            return
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.password_required)
            return
        }

        if (!userManager.isValidPassword(password)) {
            binding.tilPassword.error = getString(R.string.password_length)
            return
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = getString(R.string.confirm_password_required)
            return
        }

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.passwords_dont_match)
            return
        }
        
        // Check if user already exists
        if (userManager.isUserRegistered(email)) {
            binding.tilEmail.error = getString(R.string.email_already_registered)
            return
        }

        // Clear errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        // Show loading state
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = getString(R.string.registering)

        // Register the user
        val registrationSuccessful = userManager.registerUser(email, password)
        
        // Simulate network delay
        Handler(Looper.getMainLooper()).postDelayed({
            if (registrationSuccessful) {
                Toast.makeText(this, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show()
                
                // Persist session and navigate to MainActivity after successful registration
                userManager.setLoggedIn(email)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                // This should rarely happen as we already checked if user exists
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = getString(R.string.register)
                Toast.makeText(this, getString(R.string.registration_failed), Toast.LENGTH_SHORT).show()
            }
        }, 1500)
    }
}