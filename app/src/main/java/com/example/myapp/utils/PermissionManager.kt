package com.example.myapp.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView

class PermissionManager private constructor() {
    
    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
        
        private var instance: PermissionManager? = null
        
        fun getInstance(): PermissionManager {
            if (instance == null) {
                instance = PermissionManager()
            }
            return instance!!
        }
        
        // All required permissions
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.CAMERA,
            Manifest.permission.VIBRATE
        )
        
        // Permission descriptions for user-friendly display
        private val PERMISSION_DESCRIPTIONS = mapOf(
            Manifest.permission.READ_CONTACTS to "Contacts - To access emergency contacts",
            Manifest.permission.SEND_SMS to "SMS - To send emergency messages",
            Manifest.permission.ACCESS_FINE_LOCATION to "Location - To share your location in emergencies",
            Manifest.permission.ACCESS_COARSE_LOCATION to "Location - To share your location in emergencies",
            Manifest.permission.CALL_PHONE to "Phone - To make emergency calls",
            Manifest.permission.CAMERA to "Camera - For safety detection features",
            Manifest.permission.VIBRATE to "Vibration - For emergency alerts"
        )
    }
    
    interface PermissionCallback {
        fun onPermissionsGranted()
        fun onPermissionsDenied(deniedPermissions: List<String>)
        fun onPermissionsPermanentlyDenied(permanentlyDeniedPermissions: List<String>)
    }
    
    private var callback: PermissionCallback? = null
    private var isFirstTimeRequest = true

    fun checkAndRequestPermissions(activity: Activity, callback: PermissionCallback) {
        this.callback = callback
        
        val sharedPref = activity.getSharedPreferences("PermissionPrefs", Context.MODE_PRIVATE)
        isFirstTimeRequest = sharedPref.getBoolean("isFirstTimeRequest", true)
        
        val deniedPermissions = getDeniedPermissions(activity)

        if (deniedPermissions.isEmpty()) {
            callback.onPermissionsGranted()
            return
        }
        
        if (isFirstTimeRequest) {
            // First time - show explanation and request permissions
            showFirstTimePermissionDialog(activity, deniedPermissions)
        } else {
            // Not first time - check if any permissions are permanently denied
            val permanentlyDenied = deniedPermissions.filter { permission ->
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }

            if (permanentlyDenied.isNotEmpty()) {
                showSettingsDialog(activity, permanentlyDenied)
            } else {
                showPermissionDeniedDialog(activity, deniedPermissions)
            }
        }
    }
    
    fun handlePermissionResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val sharedPref = activity.getSharedPreferences("PermissionPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("isFirstTimeRequest", false).apply()

            val deniedPermissions = mutableListOf<String>()
            val permanentlyDeniedPermissions = mutableListOf<String>()
            
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[i])) {
                        deniedPermissions.add(permissions[i])
                    } else {
                        permanentlyDeniedPermissions.add(permissions[i])
                    }
                }
            }
            
            when {
                deniedPermissions.isEmpty() && permanentlyDeniedPermissions.isEmpty() -> {
                    callback?.onPermissionsGranted()
                }
                permanentlyDeniedPermissions.isNotEmpty() -> {
                    callback?.onPermissionsPermanentlyDenied(permanentlyDeniedPermissions)
                }
                else -> {
                    callback?.onPermissionsDenied(deniedPermissions)
                }
            }
        }
    }
    
    private fun getDeniedPermissions(context: Context): List<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showFirstTimePermissionDialog(activity: Activity, deniedPermissions: List<String>) {
        val permissionDescriptions = deniedPermissions.mapNotNull { permission ->
            PERMISSION_DESCRIPTIONS[permission]
        }

        val message = "This app requires certain permissions to work properly:\n\n" +
                permissionDescriptions.joinToString("\n• ", "• ") +
                "\n\nTap Continue to request these permissions."

        val dialogView = LayoutInflater.from(activity).inflate(com.example.myapp.R.layout.dialog_permission, null)
        val titleView = dialogView.findViewById<TextView>(com.example.myapp.R.id.tv_permission_title)
        val messageView = dialogView.findViewById<TextView>(com.example.myapp.R.id.tv_permission_message)
        val btnGrant = dialogView.findViewById<Button>(com.example.myapp.R.id.btn_grant)
        val btnCancel = dialogView.findViewById<Button>(com.example.myapp.R.id.btn_cancel)

        titleView.text = "Permissions Required"
        messageView.text = message
        btnGrant.text = "Continue"

        val alertDialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnGrant.setOnClickListener {
            alertDialog.dismiss()
            ActivityCompat.requestPermissions(
                activity,
                deniedPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
        btnCancel.setOnClickListener {
            alertDialog.dismiss()
            callback?.onPermissionsDenied(deniedPermissions)
        }

        alertDialog.show()
    }

    private fun showPermissionDeniedDialog(activity: Activity, deniedPermissions: List<String>) {
        val permissionDescriptions = deniedPermissions.mapNotNull { permission ->
            PERMISSION_DESCRIPTIONS[permission]
        }

        val message = "The following permissions are still required:\n\n" +
                permissionDescriptions.joinToString("\n• ", "• ") +
                "\n\nSome features may not work without them."

        val dialogView = LayoutInflater.from(activity).inflate(com.example.myapp.R.layout.dialog_permission, null)
        val titleView = dialogView.findViewById<TextView>(com.example.myapp.R.id.tv_permission_title)
        val messageView = dialogView.findViewById<TextView>(com.example.myapp.R.id.tv_permission_message)
        val btnGrant = dialogView.findViewById<Button>(com.example.myapp.R.id.btn_grant)
        val btnCancel = dialogView.findViewById<Button>(com.example.myapp.R.id.btn_cancel)

        titleView.text = "Permissions Required"
        messageView.text = message
        btnGrant.text = "Grant Permissions"

        val alertDialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnGrant.setOnClickListener {
            alertDialog.dismiss()
            ActivityCompat.requestPermissions(
                activity,
                deniedPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
        btnCancel.setOnClickListener {
            alertDialog.dismiss()
            callback?.onPermissionsDenied(deniedPermissions)
        }

        alertDialog.show()
    }

    private fun showSettingsDialog(activity: Activity, permanentlyDeniedPermissions: List<String>) {
        val permissionDescriptions = permanentlyDeniedPermissions.mapNotNull { permission ->
            PERMISSION_DESCRIPTIONS[permission]
        }

        val message = "This feature requires permissions that were denied.\n\n" +
                permissionDescriptions.joinToString("\n• ", "• ") +
                "\n\nPlease enable them in Settings."

        val dialogView = LayoutInflater.from(activity).inflate(com.example.myapp.R.layout.dialog_permission, null)
        val titleView = dialogView.findViewById<TextView>(com.example.myapp.R.id.tv_permission_title)
        val messageView = dialogView.findViewById<TextView>(com.example.myapp.R.id.tv_permission_message)
        val btnGrant = dialogView.findViewById<Button>(com.example.myapp.R.id.btn_grant)
        val btnCancel = dialogView.findViewById<Button>(com.example.myapp.R.id.btn_cancel)

        titleView.text = "Permission Needed"
        messageView.text = message
        btnGrant.text = "Go to Settings"

        val alertDialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnGrant.setOnClickListener {
            alertDialog.dismiss()
            openAppSettings(activity)
        }
        btnCancel.setOnClickListener {
            alertDialog.dismiss()
            callback?.onPermissionsPermanentlyDenied(permanentlyDeniedPermissions)
        }

        alertDialog.show()
    }

    private fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }

    fun hasAllPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    fun getMissingPermissions(context: Context): List<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    // Method to check specific feature permissions
    fun canUseSMS(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.SEND_SMS)
    }
    
    fun canUseLocation(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    
    fun canUseContacts(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.READ_CONTACTS)
    }
    
    fun canUseCamera(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.CAMERA)
    }
    
    fun canMakePhoneCalls(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.CALL_PHONE)
    }
}
