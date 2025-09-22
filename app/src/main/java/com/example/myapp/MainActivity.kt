package com.example.myapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.myapp.utils.PermissionManager
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {
    companion object {
        // Reserved for future request codes if needed
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

        // Set up Bottom Navigation to switch between fragments (including Home)
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)

        fun showFragment(tag: String, creator: () -> Fragment) {
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

        // Ensure HomeFragment is loaded initially
        if (supportFragmentManager.findFragmentByTag("home") == null) {
            showFragment("home") { HomeFragment() }
        }
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { showFragment("home") { HomeFragment() }; true }
                R.id.nav_tips -> { showFragment("tips") { TipsFragment() }; true }
                R.id.nav_location -> { showFragment("location") { LocationFragment() }; true }
                R.id.nav_profile -> { showFragment("profile") { ProfileFragment() }; true }
                else -> false
            }
        }

        // Preserve original behavior: show permission dialog on first launch
        checkFirstLaunchAndPermissions()
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
    }

    // Activity remains a lightweight host; per-tab logic lives in fragments
}
