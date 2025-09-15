package com.example.myapp.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import android.provider.Settings
import android.net.Uri

class FeaturePermissionHelper {
    
    companion object {
        fun checkFeaturePermissions(
            activity: Activity,
            requiredPermissions: Array<String>,
            featureName: String,
            onGranted: () -> Unit
        ) {
            val permissionManager = PermissionManager.getInstance()
            val missingPermissions = requiredPermissions.filter { permission ->
                !permissionManager.hasPermission(activity, permission)
            }
            
            if (missingPermissions.isEmpty()) {
                onGranted()
            } else {
                showFeaturePermissionDialog(activity, featureName, missingPermissions)
            }
        }
        
        private fun showFeaturePermissionDialog(
            activity: Activity,
            featureName: String,
            missingPermissions: List<String>
        ) {
            val permissionNames = missingPermissions.map { permission ->
                when (permission) {
                    Manifest.permission.CAMERA -> "Camera"
                    Manifest.permission.SEND_SMS -> "SMS"
                    Manifest.permission.ACCESS_FINE_LOCATION -> "Location"
                    Manifest.permission.ACCESS_COARSE_LOCATION -> "Location"
                    Manifest.permission.CALL_PHONE -> "Phone"
                    Manifest.permission.READ_CONTACTS -> "Contacts"
                    Manifest.permission.VIBRATE -> "Vibration"
                    else -> permission.substringAfterLast(".")
                }
            }.distinct()
            
            val message = "$featureName requires the following permissions:\n\n" +
                    permissionNames.joinToString("\n• ", "• ") +
                    "\n\nPlease enable these permissions in Settings to use this feature."
            
            val dialogView = LayoutInflater.from(activity).inflate(com.example.myapp.R.layout.dialog_permission, null)
            val titleView = dialogView.findViewById<TextView>(com.example.myapp.R.id.tv_permission_title)
            val messageView = dialogView.findViewById<TextView>(com.example.myapp.R.id.tv_permission_message)
            val btnGrant = dialogView.findViewById<Button>(com.example.myapp.R.id.btn_grant)
            val btnCancel = dialogView.findViewById<Button>(com.example.myapp.R.id.btn_cancel)

            titleView.text = "Permission Required"
            messageView.text = message
            btnGrant.text = "Go to Settings"
            btnCancel.text = "Cancel"

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
            }

            alertDialog.show()
        }
        
        private fun openAppSettings(activity: Activity) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(intent)
        }
        
        // Method to recheck permissions when returning from settings
        fun recheckPermissionsOnResume(
            activity: Activity,
            requiredPermissions: Array<String>,
            onGranted: () -> Unit
        ) {
            val permissionManager = PermissionManager.getInstance()
            val missingPermissions = requiredPermissions.filter { permission ->
                !permissionManager.hasPermission(activity, permission)
            }
            
            if (missingPermissions.isEmpty()) {
                onGranted()
            }
            // If still missing permissions, don't show dialog again - let user access feature
            // and they'll see the "Go to Settings" dialog when they try to use it
        }
        
        // Specific feature permission checks
        object ContactsFeature {
            fun checkPermissions(activity: Activity, onGranted: () -> Unit) {
                checkFeaturePermissions(
                    activity,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    "Emergency Contacts",
                    onGranted
                )
            }
            
            fun recheckPermissions(activity: Activity, onGranted: () -> Unit) {
                recheckPermissionsOnResume(
                    activity,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    onGranted
                )
            }
        }
        
        object CameraFeature {
            fun checkPermissions(activity: Activity, onGranted: () -> Unit) {
                checkFeaturePermissions(
                    activity,
                    arrayOf(Manifest.permission.CAMERA),
                    "Camera",
                    onGranted
                )
            }
            
            fun recheckPermissions(activity: Activity, onGranted: () -> Unit) {
                recheckPermissionsOnResume(
                    activity,
                    arrayOf(Manifest.permission.CAMERA),
                    onGranted
                )
            }
        }
        
        object SOSFeature {
            fun checkPermissions(activity: Activity, onGranted: () -> Unit) {
                checkFeaturePermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_CONTACTS
                    ),
                    "SOS Emergency Alert",
                    onGranted
                )
            }
            
            fun recheckPermissions(activity: Activity, onGranted: () -> Unit) {
                recheckPermissionsOnResume(
                    activity,
                    arrayOf(
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_CONTACTS
                    ),
                    onGranted
                )
            }
        }
        
        object LocationFeature {
            fun checkPermissions(activity: Activity, onGranted: () -> Unit) {
                checkFeaturePermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    "Location Services",
                    onGranted
                )
            }
            
            fun recheckPermissions(activity: Activity, onGranted: () -> Unit) {
                recheckPermissionsOnResume(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    onGranted
                )
            }
        }
        
        object PhoneFeature {
            fun checkPermissions(activity: Activity, onGranted: () -> Unit) {
                checkFeaturePermissions(
                    activity,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    "Phone Calls",
                    onGranted
                )
            }
            
            fun recheckPermissions(activity: Activity, onGranted: () -> Unit) {
                recheckPermissionsOnResume(
                    activity,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    onGranted
                )
            }
        }
    }
}
