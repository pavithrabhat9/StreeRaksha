package com.example.myapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import java.security.MessageDigest

/**
 * Manages user registration and authentication
 */
class UserManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME, Context.MODE_PRIVATE
    )
    
    /**
     * Register a new user
     * @return true if registration successful, false if user already exists
     */
    fun registerUser(email: String, password: String): Boolean {
        if (isUserRegistered(email)) {
            return false
        }
        
        val hashedPassword = hashPassword(password)
        sharedPreferences.edit().apply {
            putString("$KEY_USER_PREFIX$email", hashedPassword)
            apply()
        }
        return true
    }
    
    /**
     * Check if user exists and credentials are valid
     */
    fun authenticateUser(email: String, password: String): Boolean {
        val storedPassword = sharedPreferences.getString("$KEY_USER_PREFIX$email", null) ?: return false
        return storedPassword == hashPassword(password)
    }
    
    /**
     * Check if a user with this email is already registered
     */
    fun isUserRegistered(email: String): Boolean {
        return sharedPreferences.contains("$KEY_USER_PREFIX$email")
    }
    
    /**
     * Simple password hashing
     * Note: In a production app, use a more secure hashing algorithm with salt
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Validate password (minimum 6 characters)
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
    
    /**
     * Session management: persist and query logged-in user
     */
    fun setLoggedIn(email: String) {
        sharedPreferences.edit().apply {
            putString(KEY_LOGGED_IN_EMAIL, email)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.contains(KEY_LOGGED_IN_EMAIL)
    }

    fun getLoggedInEmail(): String? {
        return sharedPreferences.getString(KEY_LOGGED_IN_EMAIL, null)
    }

    fun logout() {
        sharedPreferences.edit().apply {
            remove(KEY_LOGGED_IN_EMAIL)
            apply()
        }
    }
    
    companion object {
        private const val PREF_NAME = "user_credentials"
        private const val KEY_USER_PREFIX = "user_"
        private const val KEY_LOGGED_IN_EMAIL = "logged_in_email"
        
        @Volatile
        private var instance: UserManager? = null
        
        fun getInstance(context: Context): UserManager {
            return instance ?: synchronized(this) {
                instance ?: UserManager(context.applicationContext).also { instance = it }
            }
        }
    }
}