package com.example.myapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Photo handling
    private var currentPhotoPath: String? = null
    private var profileImageUri: Uri? = null

    // Permission request code
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private val STORAGE_PERMISSION_REQUEST_CODE = 1002

    // Activity result launchers
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            handleCameraResult()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                profileImageUri = uri
                loadImageFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance()
        firestore = Firebase.firestore

        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Edit Profile"

        // Load current user data
        loadUserData()

        // Set up click listeners
        setupClickListeners()
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Set email (read-only)
            findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_email)?.setText(currentUser.email)

            // Load saved profile image from SharedPreferences (user-specific)
            val prefs = getSharedPreferences("UserProfile_${currentUser.uid}", MODE_PRIVATE)
            val savedUriString = prefs.getString("profileImageUri", null)

            if (savedUriString != null) {
                try {
                    val uri = Uri.parse(savedUriString)
                    profileImageUri = uri

                    val profileImageView = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profile_image)

                    // Check if it's a file URI (internal storage)
                    if (uri.scheme == "file") {
                        val filePath = uri.path
                        if (filePath != null) {
                            val file = File(filePath)
                            if (file.exists()) {
                                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                profileImageView?.setImageBitmap(bitmap)
                            }
                        }
                    } else {
                        // For content URIs from gallery
                        try {
                            val inputStream = contentResolver.openInputStream(uri)
                            if (inputStream != null) {
                                val bitmap = BitmapFactory.decodeStream(inputStream)
                                inputStream.close()
                                profileImageView?.setImageBitmap(bitmap)
                            }
                        } catch (e: Exception) {
                            Log.e("EditProfileActivity", "Error reading content URI: ${e.message}", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("EditProfileActivity", "Error loading saved image: ${e.message}", e)
                }
            }

            // Load additional user data from Firestore
            lifecycleScope.launch {
                try {
                    val userDoc = firestore.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    if (userDoc.exists()) {
                        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_name)?.setText(userDoc.getString("name") ?: "")
                        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_phone)?.setText(userDoc.getString("phone") ?: "")
                    }
                } catch (e: Exception) {
                    Log.e("EditProfileActivity", "Error loading user data: ${e.message}", e)
                    Toast.makeText(this@EditProfileActivity, "Error loading profile data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Cancel button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)?.setOnClickListener {
            finish()
        }

        // Save button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)?.setOnClickListener {
            saveProfileData()
        }

        // Change photo button
        findViewById<TextView>(R.id.fab_change_photo)?.setOnClickListener {
            showPhotoSelectionDialog()
        }
    }

    private fun saveProfileData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Get form data
        val name = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_name)?.text?.toString()?.trim() ?: ""
        val phone = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_phone)?.text?.toString()?.trim() ?: ""

        // Validate required fields
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            return
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)?.isEnabled = false
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)?.text = "Saving..."

        // Update Firestore
        lifecycleScope.launch {
            try {
                val userData = hashMapOf(
                    "name" to name,
                    "phone" to phone,
                    "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                firestore.collection("users")
                    .document(currentUser.uid)
                    .update(userData)
                    .await()

                Toast.makeText(this@EditProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

                // Set result to notify ProfileFragment
                setResult(RESULT_OK)
                finish()

            } catch (e: Exception) {
                Log.e("EditProfileActivity", "Error updating profile: ${e.message}", e)
                Toast.makeText(this@EditProfileActivity, "Error updating profile: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // Re-enable button
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)?.isEnabled = true
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)?.text = "Save Changes"
            }
        }
    }

    // Photo handling methods
    private fun showPhotoSelectionDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Remove Photo", "Cancel")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (checkCameraPermission()) {
                            openCamera()
                        } else {
                            requestCameraPermission()
                        }
                    }
                    1 -> {
                        if (checkStoragePermission()) {
                            openGallery()
                        } else {
                            requestStoragePermission()
                        }
                    }
                    2 -> {
                        removeProfilePhoto()
                    }
                    3 -> {
                        // Cancel - do nothing
                    }
                }
            }
            .show()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ doesn't need READ_EXTERNAL_STORAGE for gallery access
            true
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
    }

    private fun requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ doesn't need READ_EXTERNAL_STORAGE permission
            openGallery()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
                }
            }
            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Storage permission is required to select photos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e("EditProfileActivity", "Error creating image file", ex)
                null
            }

            photoFile?.let { file ->
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                cameraLauncher.launch(intent)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir("Pictures") ?: filesDir
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun handleCameraResult() {
        currentPhotoPath?.let { path ->
            val imageFile = File(path)
            if (imageFile.exists()) {
                profileImageUri = Uri.fromFile(imageFile)
                loadImageFromUri(profileImageUri!!)
            }
        }
    }

    private fun loadImageFromUri(uri: Uri) {
        val currentUser = auth.currentUser ?: return

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Set the image to the profile image view
            findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profile_image)?.setImageBitmap(bitmap)

            // Save the image to internal storage for persistence (user-specific)
            val internalUri = saveImageToInternalStorage(bitmap, currentUser.uid)

            // Save internal URI to SharedPreferences for persistence (user-specific)
            val prefs = getSharedPreferences("UserProfile_${currentUser.uid}", MODE_PRIVATE)
            prefs.edit().putString("profileImageUri", internalUri.toString()).apply()

            Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("EditProfileActivity", "Error loading image: ${e.message}", e)
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap, userId: String): Uri {
        val filename = "profile_image_${userId}.jpg"
        val file = File(filesDir, filename)

        val outputStream = file.outputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()

        return Uri.fromFile(file)
    }

    private fun removeProfilePhoto() {
        val currentUser = auth.currentUser ?: return

        if (profileImageUri == null && currentPhotoPath == null) {
            Toast.makeText(this, "No profile photo to remove", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Remove Profile Photo")
            .setMessage("Are you sure you want to remove your profile photo?")
            .setPositiveButton("Remove") { _, _ ->
                // Reset to default profile placeholder
                findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profile_image)?.setImageResource(R.drawable.reshot_icon_profile_image_sbdvth9pea)

                // Clear the selected image URI
                profileImageUri = null
                currentPhotoPath = null

                // Clear from SharedPreferences (user-specific)
                val prefs = getSharedPreferences("UserProfile_${currentUser.uid}", MODE_PRIVATE)
                prefs.edit().remove("profileImageUri").apply()

                Toast.makeText(this, "Profile photo removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun compressImage(uri: Uri): ByteArray {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return outputStream.toByteArray()
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun finish() {
        setResult(RESULT_OK)
        super.finish()
    }
}