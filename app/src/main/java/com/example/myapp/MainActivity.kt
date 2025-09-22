package com.example.myapp

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.media.MediaPlayer
import android.media.audiofx.BassBoost.Settings
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.myapp.model.ContactData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.myapp.presentation.ViewModelFactory
import com.example.myapp.presentation.main.MainViewModel
import com.example.myapp.presentation.main.MainUiState
import com.example.myapp.presentation.contacts.ContactsViewModel
import com.example.myapp.utils.PermissionManager
import com.example.myapp.utils.FeaturePermissionHelper
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSION_REQUEST_SMS = 1001
        private const val PERMISSION_REQUEST_LOCATION = 1002
        private const val PERMISSION_REQUEST_CAMERA = 1003
        private const val PERMISSION_REQUEST_NOTIFICATION = 1004
        private const val PERMISSION_REQUEST_BLUETOOTH = 1005
        private const val PERMISSION_REQUEST_EMERGENCY = 1006
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var viewModelFactory: ViewModelFactory
    private var pendingSosContacts: List<ContactData> = emptyList()
    private var isSirenActive = false

    // Siren-related variables
    private var mediaPlayer: MediaPlayer? = null
    private var colorAnimator: ValueAnimator? = null
    private val sirenHandler = Handler(Looper.getMainLooper())

    // Colors for the siren animation
    private val sirenColors = listOf(
        Color.RED,
        Color.rgb(255, 100, 0),  // Orange-red
        Color.YELLOW,
        Color.rgb(255, 0, 255),  // Magenta
        Color.RED,
        Color.rgb(255, 50, 50),  // Bright red
        Color.rgb(255, 150, 0)   // Orange
    )

    private fun setupContactsObserver() {
        lifecycleScope.launch {
            contactsViewModel.uiState.collect { state ->
                val hasContacts = state.contacts.isNotEmpty()
                val tvContactsConfigured = findViewById<android.widget.TextView>(R.id.tv_contacts_configured)
                val tvContactsNotConfigured = findViewById<android.widget.TextView>(R.id.tv_contacts_not_configured)

                if (hasContacts) {
                    tvContactsConfigured.visibility = android.view.View.VISIBLE
                    tvContactsNotConfigured.visibility = android.view.View.GONE
                } else {
                    tvContactsConfigured.visibility = android.view.View.GONE
                    tvContactsNotConfigured.visibility = android.view.View.VISIBLE
                }
            }
        }

        // Initial load
        contactsViewModel.handleEvent(ContactsViewModel.UiEvent.LoadContacts)
    }

    fun onConfigureContactsClick(view: android.view.View) {
        startActivity(Intent(this, SosActivity::class.java))
    }

    private fun checkContactsAndProceed() {
        // Get contacts from the current UI state
        val currentState = contactsViewModel.uiState.value
        val contacts = currentState?.contacts ?: emptyList()

        if (contacts.isEmpty()) {
            // If no contacts, show error message with option to add them
            android.app.AlertDialog.Builder(this)
                .setTitle("No Emergency Contacts")
                .setMessage("You need to add at least one emergency contact to use this feature.")
                .setPositiveButton("Add Contacts") { _, _ ->
                    startActivity(Intent(this, SosActivity::class.java))
                }
                .setNegativeButton("Not Now", null)
                .show()
        } else {
            // If we have contacts, proceed with emergency
            proceedWithEmergency()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = listOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS
        )

        // Check if all permissions are already granted
        val ungrantedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungrantedPermissions.isEmpty()) {
            // All permissions already granted, proceed with emergency
            proceedWithEmergency()
            return
        }

        // Use our PermissionManager to handle the permission request
        val permissionManager = PermissionManager.getInstance()
        permissionManager.checkAndRequestPermissions(this, object : PermissionManager.PermissionCallback {
            override fun onPermissionsGranted() {
                // All permissions granted, proceed with emergency
                proceedWithEmergency()
            }

            override fun onPermissionsDenied(deniedPermissions: List<String>) {
                // Handle denied permissions
                Toast.makeText(
                    this@MainActivity,
                    "Emergency features require these permissions to work properly",
                    Toast.LENGTH_LONG
                ).show()
                // Still proceed with what we can do
                proceedWithEmergency()
            }

            override fun onPermissionsPermanentlyDenied(permanentlyDeniedPermissions: List<String>) {
                // Handle permanently denied permissions
                Toast.makeText(
                    this@MainActivity,
                    "Please enable permissions in app settings",
                    Toast.LENGTH_LONG
                ).show()
                // Still try to proceed with what we can do
                proceedWithEmergency()
            }
        })
    }

    private fun proceedWithEmergency() {
        val currentState = contactsViewModel.uiState.value
        val emergencyContacts = currentState?.contacts ?: emptyList()
        if (emergencyContacts.isEmpty()) return

        // Convert to legacy ContactData for compatibility
        val contacts = emergencyContacts.map { contact ->
            ContactData(contact.name, contact.phoneNumber)
        }

        // Try to get location if we have permission
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    val message = if (location != null) {
                        val lat = location.latitude
                        val lon = location.longitude
                        "EMERGENCY! I need help! My location: https://maps.google.com/?q=$lat,$lon"
                    } else {
                        "EMERGENCY! I need help!"
                    }
                    sendEmergencyMessages(contacts, message)
                }.addOnFailureListener {
                    sendEmergencyMessages(contacts, "EMERGENCY! I need help!")
                }
            } catch (e: Exception) {
                sendEmergencyMessages(contacts, "EMERGENCY! I need help!")
            }
        } else {
            sendEmergencyMessages(contacts, "EMERGENCY! I need help!")
        }
    }

    private fun sendEmergencyMessages(contacts: List<ContactData>, message: String) {
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            // Request SMS permission if not granted
            requestPermissions(arrayOf(Manifest.permission.SEND_SMS), PERMISSION_REQUEST_SMS)
            return
        }

        val smsManager = SmsManager.getDefault()
        var successCount = 0

        for (contact in contacts) {
            try {
                // Clean up the phone number (remove any non-digit characters except +)
                val phoneNumber = contact.phone.replace("[^0-9+]".toRegex(), "")
                if (phoneNumber.isNotBlank()) {
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                    successCount++
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to send SMS to ${contact.phone}", e)
            }
        }

        // Show confirmation dialog
        val message = if (successCount > 0) {
            "Emergency alert has been sent to $successCount contact${if (successCount > 1) "s" else ""}!"
        } else {
            "Failed to send emergency alerts. Please check your SMS permissions and try again."
        }

        runOnUiThread {
            android.app.AlertDialog.Builder(this)
                .setTitle(if (successCount > 0) "Alert Sent" else "Sending Failed")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure ViewModel is initialized before using it
        if (!::contactsViewModel.isInitialized) {
            val app = application as MyApplication
            viewModelFactory = ViewModelFactory(app.appModule)
            contactsViewModel = ViewModelProvider(this, viewModelFactory)[ContactsViewModel::class.java]
            setupContactsObserver()
        }
        // Refresh contacts when returning to the activity
        contactsViewModel.handleEvent(ContactsViewModel.UiEvent.LoadContacts)
        // Check if siren is still active
        checkSirenState()
    }

    private fun checkSirenState() {
        val sharedPref = getSharedPreferences("SirenState", MODE_PRIVATE)
        isSirenActive = sharedPref.getBoolean("isSirenActive", false)
        updateSirenCardUI()
    }

    private fun saveSirenState(isActive: Boolean) {
        val sharedPref = getSharedPreferences("SirenState", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isSirenActive", isActive)
            apply()
        }
    }
    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the splash screen visible for this Activity
        splashScreen.setKeepOnScreenCondition { true }

        // Set the theme for the main content
        setTheme(R.style.Theme_MyApp)

        // Set the content view
        setContentView(R.layout.activity_main)

        // Hide the splash screen once the layout is ready
        window.decorView.post {
            splashScreen.setKeepOnScreenCondition { false }
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Initialize ViewModels with Clean Architecture
        val app = application as MyApplication
        viewModelFactory = ViewModelFactory(app.appModule)

        contactsViewModel = ViewModelProvider(this, viewModelFactory)[ContactsViewModel::class.java]
        mainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        // Set up contact status views (non-clickable)
        findViewById<android.widget.TextView>(R.id.tv_contacts_configured)
        findViewById<android.widget.TextView>(R.id.tv_contacts_not_configured)

        // Set up contacts observer
        setupContactsObserver()

        // Set up emergency button click listener with Clean Architecture
        findViewById<Button>(R.id.btn_activate_emergency).setOnClickListener {
            mainViewModel.handleEvent(MainViewModel.UiEvent.SosClicked)
        }

        // Observe MainViewModel state
        observeMainViewModel()

        // Check permissions on startup
        mainViewModel.handleEvent(MainViewModel.UiEvent.CheckPermissions)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check first launch and show permission dialog if needed
        checkFirstLaunchAndPermissions()

        // Hidden Camera Detector Click Handler
        findViewById<CardView>(R.id.card_camera_detector).setOnClickListener {
            CameraDetectorActivity.start(this)
        }

        // Emergency Helpline Click Handler
        findViewById<CardView>(R.id.card_emergency_helpline).setOnClickListener {
            startActivity(Intent(this, EmergencyHelplineActivity::class.java))
        }

        // Siren Click Handler
        findViewById<CardView>(R.id.card_siren).setOnClickListener {
            if (isSirenActive) {
                // If siren is already active, stop it
                stopSiren()
            } else {
                // If siren is not active, show confirmation dialog
                showSirenConfirmationDialog()
            }
        }

        // Contacts Card Click Handler - Navigates to SosActivity (Emergency Contacts)
        findViewById<CardView>(R.id.card_contacts).setOnClickListener {
            startActivity(Intent(this, SosActivity::class.java))
        }

        // Function to open Google Maps with nearby search
        fun openNearbySearch(query: String) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Show custom dialog guiding user to Settings
                FeaturePermissionHelper.Companion.LocationFeature.checkPermissions(this) {
                    // After returning from settings, try again
                    performNearbySearch(query)
                }
                return
            }
            performNearbySearch(query)
        }

        // Police Card Click Handler
        findViewById<CardView>(R.id.card_police).setOnClickListener {
            openNearbySearch("nearby police station")
        }

        // Hospital Card Click Handler
        findViewById<CardView>(R.id.card_hospital).setOnClickListener {
            openNearbySearch("nearby hospital")
        }

        // Fire Station Card Click Handler
        findViewById<CardView>(R.id.card_pharmacy).setOnClickListener {
            openNearbySearch("nearby pharmacy")
        }

        // Women's Helpline Card Click Handler
        findViewById<CardView>(R.id.card_women_shelter).setOnClickListener {
            openNearbySearch("nearby women's shelter")
        }

        // Set up a ViewTreeObserver to find the Explore card after layout
        val rootView = findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.main)
        val viewTreeObserver = rootView.viewTreeObserver

        viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            private var isCardFound = false

            override fun onGlobalLayout() {
                if (isCardFound) {
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    return
                }

                // Function to recursively find the Explore card
                fun findExploreCard(view: android.view.View): android.view.View? {
                    if (view is ViewGroup) {
                        // Check if this is the card we're looking for
                        if (view is FrameLayout && view.childCount > 0) {
                            for (i in 0 until view.childCount) {
                                val child = view.getChildAt(i)
                                if (child is android.widget.TextView &&
                                    child.text.toString().contains("Explore")) {
                                    return view
                                }
                            }
                        }

                        // Recursively check children
                        for (i in 0 until view.childCount) {
                            val found = findExploreCard(view.getChildAt(i))
                            if (found != null) return found
                        }
                    }
                    return null
                }

                // Try to find the card
                val exploreCard = findExploreCard(rootView)
                if (exploreCard != null) {
                    isCardFound = true
                    exploreCard.setOnClickListener {
                        startActivity(Intent(this@MainActivity, ExploreLifesaveActivity::class.java))
                    }
                    // Remove the listener to prevent memory leaks
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })

        // Set up Bottom Navigation to switch between Home content and fragments
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        val homeScroll = findViewById<android.widget.ScrollView>(R.id.home_scroll)
        val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)

        fun showHome() {
            // Hide fragments and show home content
            fragmentContainer.visibility = android.view.View.GONE
            homeScroll.visibility = android.view.View.VISIBLE
        }

        fun showFragment(tag: String, creator: () -> Fragment) {
            homeScroll.visibility = android.view.View.GONE
            fragmentContainer.visibility = android.view.View.VISIBLE

            val fm = supportFragmentManager
            val transaction = fm.beginTransaction()

            // Hide all existing fragments
            fm.fragments.forEach { transaction.hide(it) }

            // Show or add the requested fragment
            val existing = fm.findFragmentByTag(tag)
            if (existing != null) {
                transaction.show(existing)
            } else {
                transaction.add(R.id.fragment_container, creator(), tag)
            }
            transaction.commit()
        }

        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showHome()
                    true
                }
                R.id.nav_tips -> {
                    showFragment("tips") { TipsFragment() }
                    true
                }
                R.id.nav_location -> {
                    showFragment("location") { LocationFragment() }
                    true
                }
                R.id.nav_profile -> {
                    showFragment("profile") { ProfileFragment() }
                    true
                }
                else -> false
            }
        }
    }

    private fun checkFirstLaunchAndPermissions() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isFirstLaunch = sharedPref.getBoolean("isFirstLaunch", true)
        
        if (isFirstLaunch) {
            // Mark as not first launch anymore
            sharedPref.edit().putBoolean("isFirstLaunch", false).apply()
            
            // Show permission dialog on first launch
            val permissionManager = PermissionManager.getInstance()
            permissionManager.checkAndRequestPermissions(this, object : PermissionManager.PermissionCallback {
                override fun onPermissionsGranted() {
                    // All permissions granted, app works normally
                }

                override fun onPermissionsDenied(deniedPermissions: List<String>) {
                    // User denied permissions, let them continue exploring
                }

                override fun onPermissionsPermanentlyDenied(permanentlyDeniedPermissions: List<String>) {
                    // User permanently denied, guide to settings later
                }
            })
        }
    }

    private fun handleSosClick() {
        if (!::contactsViewModel.isInitialized) {
            val app = application as MyApplication
            viewModelFactory = ViewModelFactory(app.appModule)
            contactsViewModel = ViewModelProvider(this, viewModelFactory)[ContactsViewModel::class.java]
        }

        // Get current contacts from ViewModel
        val currentState = contactsViewModel.uiState.value
        val emergencyContacts = currentState?.contacts ?: emptyList()

        if (emergencyContacts.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_emergency_contacts), Toast.LENGTH_LONG).show()
            return
        }

        // Convert to legacy ContactData for compatibility
        val currentContacts = emergencyContacts.map { contact ->
            ContactData(contact.name, contact.phoneNumber)
        }

        val permissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            sendSosWithLocation(currentContacts)
        } else {
            // Store the contacts in a temporary variable to use after permission is granted
            pendingSosContacts = currentContacts
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_SMS)
        }
    }

    private fun sendSosWithLocation(contacts: List<ContactData>) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                val message = if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    getString(R.string.sos_message_with_location, lat, lon)
                } else {
                    getString(R.string.sos_message_no_location)
                }
                sendSmsToContacts(contacts, message)
            }.addOnFailureListener {
                sendSmsToContacts(contacts, getString(R.string.sos_message_no_location))
            }
        } else {
            sendSmsToContacts(contacts, getString(R.string.sos_message_no_location))
        }
    }

    private fun sendSmsToContacts(contacts: List<ContactData>, message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.sms_permission_required), Toast.LENGTH_SHORT).show()
            return
        }

        val smsManager = SmsManager.getDefault()
        var sent = 0

        for (contact in contacts) {
            try {
                // Clean up the phone number by removing any non-digit characters
                val phoneNumber = contact.phone.replace("[^0-9+]".toRegex(), "")
                if (phoneNumber.isNotBlank()) {
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                    sent++
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to send to ${contact.phone}", e)
            }
        }

        if (sent > 0) {
            Toast.makeText(this, getString(R.string.sos_sent_to_contacts, sent), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.failed_to_send_sos), Toast.LENGTH_SHORT).show()
        }
    }

    private var pendingSearchQuery: String? = null

    private fun performNearbySearch(query: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            openGeneralSearch(query)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                try {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$query&z=14")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")

                    if (mapIntent.resolveActivity(packageManager) != null) {
                        startActivity(mapIntent)
                    } else {
                        // Fallback to browser with coordinates
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/$query/@$latitude,$longitude,14z")
                        )
                        startActivity(browserIntent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                // If location is null, fall back to general search
                openGeneralSearch(query)
            }
        }.addOnFailureListener {
            // If location fetch fails, fall back to general search
            openGeneralSearch(query)
        }
    }

    // Fallback function for when location is not available
    private fun openGeneralSearch(query: String) {
        try {
            val gmmIntentUri = Uri.parse("geo:0,0?q=nearby $query")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/nearby+$query")
                )
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSirenConfirmationDialog() {
        var dialog: AlertDialog? = null
        var isCanceled = false

        dialog = AlertDialog.Builder(this)
            .setTitle("Siren Activation")
            .setMessage("Siren will start in 3 seconds")
            .setPositiveButton("Undo") { _, _ ->
                isCanceled = true
            }
            .setCancelable(false)
            .create()

        dialog.show()

        // Auto dismiss after 3 seconds and start siren if not canceled
        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog?.isShowing == true) {
                dialog.dismiss()
                if (!isCanceled) {
                    isSirenActive = true
                    saveSirenState(true)
                    updateSirenCardUI()
                    startSiren()
                }
            }
        }, 3000)
    }

    private fun startSiren() {
        try {
            // Start the siren sound
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.siren2).apply {
                    isLooping = true
                    start()
                }
            }

            // Start visual animation on the siren card
            startSirenCardAnimation()

            Toast.makeText(this, "Siren started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start siren", e)
            Toast.makeText(this, "Failed to start siren", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startSirenCardAnimation() {
        val sirenCard = findViewById<CardView>(R.id.card_siren)

        // Stop any existing animation
        colorAnimator?.cancel()

        // Create pulsating color animation for the siren card
        colorAnimator = ValueAnimator().apply {
            setObjectValues(*sirenColors.toTypedArray())
            setEvaluator(ArgbEvaluator())
            duration = 800  // Fast transition between colors
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animator ->
                if (isSirenActive) {
                    val color = animator.animatedValue as Int
                    sirenCard.setCardBackgroundColor(color)
                }
            }

            start()
        }
    }

    private fun stopSiren(silent: Boolean = false) {
        isSirenActive = false
        saveSirenState(false)

        // Stop the sound
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.release()
                mediaPlayer = null
            }
        }

        // Stop the animation
        colorAnimator?.cancel()

        // Update UI
        updateSirenCardUI()

        if (!silent) {
            Toast.makeText(this, "Siren stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSirenCardUI() {
        val sirenCard = findViewById<CardView>(R.id.card_siren)
        if (isSirenActive) {
            // Update card appearance for active state
            sirenCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.emergency_red_dark))
            // Find the text views and update them
            val sirenText = sirenCard.findViewById<android.widget.TextView>(R.id.siren_title)
            val sirenDesc = sirenCard.findViewById<android.widget.TextView>(R.id.siren_description)
            sirenText?.text = "Stop Siren"
            sirenDesc?.text = "Tap to stop alarm"
        } else {
            // Restore original appearance
            sirenCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.emergency_accent))
            val sirenText = sirenCard.findViewById<android.widget.TextView>(R.id.siren_title)
            val sirenDesc = sirenCard.findViewById<android.widget.TextView>(R.id.siren_description)
            sirenText?.text = "Siren"
            sirenDesc?.text = "Play emergency alarm"
        }
    }

    // Permission handling is now done by PermissionManager
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        // Forward permission results to PermissionManager
        val permissionManager = PermissionManager.getInstance()
        permissionManager.handlePermissionResult(this, requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSION_REQUEST_LOCATION -> {
                pendingSearchQuery?.let { query ->
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        performNearbySearch(query)
                    } else {
                        openGeneralSearch(query)
                    }
                }
                pendingSearchQuery = null
            }
            // Handle other permission requests...
        }
    }

    private fun observeMainViewModel() {
        // Observe UI state changes
        lifecycleScope.launch {
            mainViewModel.uiState.collect { state ->
                handleMainUiState(state)
            }
        }
    }

    private fun handleMainUiState(state: MainUiState) {
        // Handle loading state
        if (state.isLoading) {
            // You can show a loading indicator here if needed
        }

        // Handle error messages
        state.error?.let { error ->
            // If SOS failed due to permissions, guide user to settings with custom dialog
            if (error.contains("SMS permission", ignoreCase = true)) {
                FeaturePermissionHelper.Companion.SOSFeature.checkPermissions(this) {
                    // No-op here; user will retry SOS manually
                }
                mainViewModel.handleEvent(MainViewModel.UiEvent.DismissError)
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(error)
                    .setPositiveButton("OK") { _, _ ->
                        mainViewModel.handleEvent(MainViewModel.UiEvent.DismissError)
                    }
                    .show()
            }
        }

        // Handle success messages
        state.successMessage?.let { message ->
            AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton("OK") { _, _ ->
                    mainViewModel.handleEvent(MainViewModel.UiEvent.DismissSuccess)
                }
                .show()
        }

        // Handle nearby search requests
        state.nearbySearchQuery?.let { query ->
            performNearbySearch(query)
        }

        // Handle permission status updates
        if (!state.hasLocationPermission) {
            // Handle location permission not granted
        }

        if (!state.isLocationEnabled) {
            // Handle location services disabled
        }

        // Handle contact refresh requests
        if (state.shouldRefreshContacts) {
            contactsViewModel.handleEvent(ContactsViewModel.UiEvent.LoadContacts)
            mainViewModel.onContactStatusRefreshed()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up siren resources silently (avoid showing toast during navigation)
        if (isSirenActive) {
            stopSiren(silent = true)
        }
        
        // Clean up handlers
        sirenHandler.removeCallbacksAndMessages(null)
        
        // Clean up color animator
        colorAnimator?.apply {
            removeAllUpdateListeners()
            removeAllListeners()
            cancel()
        }
        colorAnimator = null
        
        // Clean up media player
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
}
