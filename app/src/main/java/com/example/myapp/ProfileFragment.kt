package com.example.myapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userManager: UserManager

    // User data
    private var userName: String = ""
    private var userEmail: String = ""
    private var userPhone: String = ""
    private var memberSince: String = ""

    // Activity Result Launcher for Edit Profile
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh profile data when returning from edit
            loadSavedProfileImage()
            loadUserData()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize Firebase services
        auth = FirebaseAuth.getInstance()
        firestore = Firebase.firestore
        userManager = UserManager.getInstance(requireContext())

        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load user data
        loadUserData()

        // Set up click listeners
        setupClickListeners(view)

        // Load user statistics
        loadUserStatistics(view)
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userEmail = currentUser.email ?: ""

            // Load profile image from SharedPreferences first
            loadSavedProfileImage()

            // Fetch user data from Firestore
            lifecycleScope.launch {
                try {
                    val userDoc = firestore.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    if (userDoc.exists()) {
                        userName = userDoc.getString("name") ?: "User"
                        userPhone = userDoc.getString("phone") ?: "Not provided"

                        // Format member since date
                        val createdAt = userDoc.getTimestamp("createdAt")
                        memberSince = if (createdAt != null) {
                            val date = createdAt.toDate()
                            val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                            "Member since ${formatter.format(date)}"
                        } else {
                            "Member since ${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())}"
                        }

                        // Update UI
                        updateUserInfo()
                    } else {
                        // Fallback to basic info
                        userName = "User"
                        userPhone = "Not provided"
                        memberSince = "Member since ${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())}"
                        updateUserInfo()
                    }
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error loading user data: ${e.message}", e)
                    // Fallback to basic info
                    userName = "User"
                    userPhone = "Not provided"
                    memberSince = "Member since ${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())}"
                    updateUserInfo()
                }
            }
        } else {
            // No user logged in, show default values
            userName = "Guest User"
            userEmail = "Not logged in"
            userPhone = "Not available"
            memberSince = "Not a member"
            updateUserInfo()
        }
    }

    private fun loadSavedProfileImage() {
        val currentUser = auth.currentUser ?: return

        val prefs = requireContext().getSharedPreferences("UserProfile_${currentUser.uid}", android.content.Context.MODE_PRIVATE)
        val savedUriString = prefs.getString("profileImageUri", null)

        val profileImageView = view?.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profile_image)

        if (savedUriString != null) {
            try {
                val uri = Uri.parse(savedUriString)

                if (uri.scheme == "file") {
                    val filePath = uri.path
                    if (filePath != null) {
                        val file = File(filePath)
                        if (file.exists()) {
                            Glide.with(this)
                                .load(file)
                                .skipMemoryCache(true)  // ADD THIS
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)  // ADD THIS
                                .placeholder(R.drawable.reshot_icon_profile_image_sbdvth9pea)
                                .error(R.drawable.reshot_icon_profile_image_sbdvth9pea)
                                .into(profileImageView!!)
                        } else {
                            // File doesn't exist, show placeholder
                            profileImageView?.setImageResource(R.drawable.reshot_icon_profile_image_sbdvth9pea)
                        }
                    }
                } else {
                    Glide.with(this)
                        .load(uri)
                        .skipMemoryCache(true)  // ADD THIS
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)  // ADD THIS
                        .placeholder(R.drawable.reshot_icon_profile_image_sbdvth9pea)
                        .error(R.drawable.reshot_icon_profile_image_sbdvth9pea)
                        .into(profileImageView!!)
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error loading saved image: ${e.message}", e)
                profileImageView?.setImageResource(R.drawable.reshot_icon_profile_image_sbdvth9pea)
            }
        } else {
            // No saved image, show placeholder
            profileImageView?.setImageResource(R.drawable.reshot_icon_profile_image_sbdvth9pea)
        }
    }

    private fun updateUserInfo() {
        view?.let { view ->
            view.findViewById<android.widget.TextView>(R.id.tv_user_name)?.text = userName
            view.findViewById<android.widget.TextView>(R.id.tv_user_email)?.text = userEmail
            view.findViewById<android.widget.TextView>(R.id.tv_user_phone)?.text = userPhone
            view.findViewById<android.widget.TextView>(R.id.tv_member_since)?.text = memberSince
        }
    }

    private fun loadUserStatistics(view: View) {
        // Load emergency contacts count
        loadEmergencyContactsCount(view)

        // Load SOS alerts count (from SharedPreferences)
        loadSOSAlertsCount(view)

        // Calculate safety score
        calculateSafetyScore(view)
    }

    private fun loadEmergencyContactsCount(view: View) {
        val currentUser = auth.currentUser ?: return

        try {
            // CHANGE THIS: Add user ID to SharedPreferences name
            val prefs = requireContext().getSharedPreferences("SosPreferences_${currentUser.uid}", android.content.Context.MODE_PRIVATE)
            val contactsJson = prefs.getString("contacts", "[]")
            val contacts = com.google.gson.Gson().fromJson(contactsJson, Array<com.example.myapp.model.ContactData>::class.java)

            view.findViewById<android.widget.TextView>(R.id.tv_contacts_count)?.text = contacts.size.toString()
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error loading contacts count: ${e.message}", e)
            view.findViewById<android.widget.TextView>(R.id.tv_contacts_count)?.text = "0"
        }
    }

    private fun loadSOSAlertsCount(view: View) {
        val currentUser = auth.currentUser ?: return

        try {
            // CHANGE THIS: Add user ID to SharedPreferences name
            val prefs = requireContext().getSharedPreferences("SOSStats_${currentUser.uid}", android.content.Context.MODE_PRIVATE)
            val sosCount = prefs.getInt("sos_alerts_sent", 0)
            view.findViewById<android.widget.TextView>(R.id.tv_sos_count)?.text = sosCount.toString()
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error loading SOS count: ${e.message}", e)
            view.findViewById<android.widget.TextView>(R.id.tv_sos_count)?.text = "0"
        }
    }

    private fun calculateSafetyScore(view: View) {
        val currentUser = auth.currentUser ?: return

        try {
            // CHANGE THIS: Add user ID to SharedPreferences name
            val prefs = requireContext().getSharedPreferences("SosPreferences_${currentUser.uid}", android.content.Context.MODE_PRIVATE)
            val contactsJson = prefs.getString("contacts", "[]")
            val contacts = com.google.gson.Gson().fromJson(contactsJson, Array<com.example.myapp.model.ContactData>::class.java)

            // Calculate safety score based on:
            // - Number of emergency contacts (max 50 points)
            // - App usage (max 30 points)
            // - Permission grants (max 20 points)

            var score = 0

            // Emergency contacts score (0-50 points)
            score += minOf(contacts.size * 10, 50)

            // App usage score (0-30 points) - simplified
            score += 30

            // Permission score (0-20 points) - simplified
            score += 20

            val safetyScore = minOf(score, 100)
            view.findViewById<android.widget.TextView>(R.id.tv_safety_score)?.text = "$safetyScore%"

        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error calculating safety score: ${e.message}", e)
            view.findViewById<android.widget.TextView>(R.id.tv_safety_score)?.text = "85%"
        }
    }

    private fun setupClickListeners(view: View) {
        // Edit Profile Button (now a TextView)
        view.findViewById<android.widget.TextView>(R.id.fab_edit_profile)?.setOnClickListener {
            showEditProfileDialog()
        }

        // Change Password
        view.findViewById<android.widget.LinearLayout>(R.id.layout_change_password)?.setOnClickListener {
            showChangePasswordDialog()
        }

        // Privacy Settings
        view.findViewById<android.widget.LinearLayout>(R.id.layout_privacy_settings)?.setOnClickListener {
            showPrivacySettingsDialog()
        }

        // Notification Settings
        view.findViewById<android.widget.LinearLayout>(R.id.layout_notification_settings)?.setOnClickListener {
            showNotificationSettingsDialog()
        }

        // Terms & Conditions
        view.findViewById<android.widget.LinearLayout>(R.id.layout_terms_conditions)?.setOnClickListener {
            showTermsConditionsDialog()
        }

        // Privacy Policy
        view.findViewById<android.widget.LinearLayout>(R.id.layout_privacy_policy)?.setOnClickListener {
            showPrivacyPolicyDialog()
        }

        // About App
        view.findViewById<android.widget.LinearLayout>(R.id.layout_about_app)?.setOnClickListener {
            showAboutAppDialog()
        }

        // Logout Button
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_logout)?.setOnClickListener {
            performLogout()
        }
    }

    private fun showEditProfileDialog() {
        editProfileLauncher.launch(Intent(requireContext(), EditProfileActivity::class.java))
    }

    private fun showChangePasswordDialog() {
        startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
    }

    private fun showPrivacySettingsDialog() {
//      startActivity(Intent(requireContext(), PrivacySettingsActivity::class.java))
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Privacy Settings")
            .setMessage("Privacy settings will be available in the next update. We are committed to protecting your data:\n\n• All information is stored securely on your device\n• Your location is only shared during emergencies\n• No data is shared with third parties\n• You have full control over app permissions")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showNotificationSettingsDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Notification Settings")
            .setMessage("Notification settings will be available in the next update. Currently, you'll receive notifications for:\n\n• SOS alerts sent\n• Location sharing status\n• Emergency contact updates\n• App security alerts")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showTermsConditionsDialog() {
        startActivity(Intent(requireContext(), TermsAndConditionsActivity::class.java))
    }

    private fun showPrivacyPolicyDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Privacy Policy")
            .setMessage("We take your privacy seriously. Your personal information is encrypted and stored securely. We only collect data necessary for the app's safety features and never share your information with third parties without your consent.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAboutAppDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("About StreeRaksha")
            .setMessage("StreeRaksha v1.0.0\n\nA comprehensive safety app designed to help you stay safe and secure. Features include emergency SOS alerts, location sharing, hidden camera detection, and emergency contact management.\n\n© 2024 StreeRaksha Team")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun performLogout() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                // Sign out from Firebase
                auth.signOut()

                // Clear the UserManager session
                userManager.logout()

                // Navigate to LoginActivity
                val intent = Intent(activity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                activity?.finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when returning to profile
        loadUserData()
        view?.let { loadUserStatistics(it) }
    }
}