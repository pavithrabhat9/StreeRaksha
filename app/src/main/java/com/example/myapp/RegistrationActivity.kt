package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.myapp.databinding.ActivityRegistrationBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.text.TextWatcher
import android.text.Editable

class RegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        setupPasswordValidation()
    }

    private fun setupPasswordValidation() {
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    validatePassword(it.toString())
                    checkPasswordsMatch()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkPasswordsMatch()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun validatePassword(password: String): Boolean {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        val isLongEnough = password.length >= 6

        var errorMessage: String? = null

        if (!isLongEnough) {
            errorMessage = "Password must be at least 6 characters long."
        } else if (!hasUppercase) {
            errorMessage = "Password must contain at least one uppercase letter."
        } else if (!hasLowercase) {
            errorMessage = "Password must contain at least one lowercase letter."
        } else if (!hasDigit) {
            errorMessage = "Password must contain at least one digit."
        } else if (!hasSpecialChar) {
            errorMessage = "Password must contain at least one special character."
        }

        binding.tilPassword.error = errorMessage
        binding.tilPassword.isErrorEnabled = errorMessage != null

        return errorMessage == null
    }

    private fun checkPasswordsMatch() {
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match."
            binding.tilConfirmPassword.isErrorEnabled = true
        } else {
            binding.tilConfirmPassword.error = null
            binding.tilConfirmPassword.isErrorEnabled = false
        }
        updateRegisterButtonState()
    }

    private fun updateRegisterButtonState() {
        val isPasswordValid = validatePassword(binding.etPassword.text.toString())
        // The register button should be enabled if the main password field is valid.
        // The 'passwords do not match' error will be displayed under the confirm password field.
        binding.btnRegister.isEnabled = isPasswordValid
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
                Log.d("REG", "Starting registration for email")
                performRegistration()
            }
        }

        binding.tvLogin.setOnClickListener {
            // Navigate back to login
            startActivity(Intent(this, LoginActivity::class.java))
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
        val email = binding.etEmail.text.toString().trim()
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
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        Log.d("Registration", "Starting registration for email: $email")
        
        // Validate Firebase instance
        try {
            val auth = Firebase.auth
            val db = Firebase.firestore
            
            Log.d("Registration", "Firebase Auth instance: ${auth.app.name}, Firestore instance: ${db.app.name}")
            
            // Verify Firebase is properly initialized
            if (auth.app.options.projectId.isNullOrEmpty()) {
                Log.e("Registration", "Firebase project ID is not set")
                handleRegistrationError("Authentication service is not properly configured. Please reinstall the app.")
                return
            }
            
            // Verify API key is available
            if (auth.app.options.apiKey.isNullOrEmpty()) {
                Log.e("Registration", "Firebase API key is not set")
                handleRegistrationError("Authentication service is not properly configured. Please reinstall the app.")
                return
            }
        } catch (e: Exception) {
            Log.e("Registration", "Firebase initialization error: ${e.message}", e)
            handleRegistrationError("Authentication service is not available. Please try again later.")
            return
        }

        // Show loading state
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = getString(R.string.registering)
        
        // Create user with Firebase Authentication
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE // Hide progress bar regardless of outcome
                binding.btnRegister.isEnabled = true // Re-enable button
                binding.btnRegister.text = getString(R.string.register) // Reset button text

                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = Firebase.auth.currentUser
                    
                    // Create a user document in Firestore with additional user data
                    val userData = hashMapOf(
                        "name" to name,
                        "phone" to phone,
                        "email" to email,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "lastLogin" to FieldValue.serverTimestamp()
                    )
                    
                    // Add a new document with the user's UID as the document ID
                    Firebase.firestore.collection("users")
                        .document(user?.uid ?: "")
                        .set(userData)
                        .addOnSuccessListener {
                            // Update UserManager with the logged-in user
                            val userManager = UserManager.getInstance(this)
                            userManager.setLoggedIn(email)
                            
                            // Show success message
                            Toast.makeText(
                                this, 
                                getString(R.string.registration_successful), 
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // Navigate to MainActivity
                            startActivity(Intent(this, MainActivity::class.java))
                            finishAffinity() // Close all previous activities
                        }
                        .addOnFailureListener { e ->
                            // If Firestore write fails, log the error and delete the user to keep data consistent
                            Log.e("Firestore", "Failed to save user data: ${e.message}", e)
                            user?.delete()?.addOnCompleteListener { deleteTask ->
                                if (!deleteTask.isSuccessful) {
                                    Log.e("FirebaseAuth", "Failed to delete user after Firestore failure", deleteTask.exception)
                                }
                            }
                            handleRegistrationError("Failed to save user data: ${e.message}")
                        }
                } else {
                    // If sign in fails, log the full exception and display a message to the user.
                    task.exception?.printStackTrace()
                    Log.e("Registration", "Firebase Authentication failed: ${task.exception?.message}", task.exception)
                    handleRegistrationError(task.exception?.message ?: "Unknown error occurred during Firebase Authentication.")
                }
            }
    }
    
    private fun handleRegistrationError(errorMessage: String?) {
        val message = errorMessage ?: getString(R.string.registration_failed)
        
        // Handle specific error cases
        when {
            message.contains("email address is already in use", ignoreCase = true) -> {
                binding.tilEmail.error = getString(R.string.email_already_registered)
            }
            message.contains("badly formatted", ignoreCase = true) -> {
                binding.tilEmail.error = getString(R.string.invalid_email)
            }
            message.contains("password is invalid", ignoreCase = true) -> {
                binding.tilPassword.error = getString(R.string.password_too_short)
            }
            else -> {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
        
        // Reset UI is now handled in addOnCompleteListener
    }
}