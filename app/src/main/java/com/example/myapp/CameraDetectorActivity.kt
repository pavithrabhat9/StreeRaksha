package com.example.myapp

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.view.MenuItem
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.myapp.databinding.ActivityCameraDetectorBinding
import com.example.myapp.utils.FeaturePermissionHelper
import kotlin.math.abs
import kotlin.math.sqrt

class CameraDetectorActivity : AppCompatActivity(), SensorEventListener {
    
    private lateinit var binding: ActivityCameraDetectorBinding
    private lateinit var sensorManager: SensorManager
    private var magneticSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    
    private var magneticValues = FloatArray(3)
    private var accelerometerValues = FloatArray(3)
    private var isDetecting = false
    private var baselineMagnetic = 0f
    private var baselineAccel = 0f
    
    private val handler = Handler(Looper.getMainLooper())
    private val CAMERA_PERMISSION_CODE = 100
    private val DETECTION_THRESHOLD = 5.0f
    
    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera_detector)
        binding.lifecycleOwner = this
        binding.activity = this
        
        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        // Initialize sensors and vibrator
        setupSensors()
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        
        // Set up click listeners
        setupClickListeners()
    }
    
    private fun setupSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }
    
    private fun setupClickListeners() {
        binding.btnSensorDetection.setOnClickListener {
            startSensorDetection()
        }
    }
    
    private fun startSensorDetection() {
        if (isDetecting) {
            stopSensorDetection()
            return
        }
        
        if (magneticSensor == null && accelerometer == null) {
            showToast("Device sensors not available for detection")
            return
        }
        
        isDetecting = true
        binding.btnSensorDetection.text = "STOP"
        binding.btnSensorDetection.backgroundTintList = ContextCompat.getColorStateList(this, R.color.emergency_red)
        
        // Register sensor listeners
        magneticSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        
        showToast(getString(R.string.scanning_in_progress))
        
        // Set baseline values after a short delay
        handler.postDelayed({
            if (isDetecting) {
                baselineMagnetic = sqrt(
                    magneticValues[0] * magneticValues[0] +
                    magneticValues[1] * magneticValues[1] +
                    magneticValues[2] * magneticValues[2]
                )
                baselineAccel = sqrt(
                    accelerometerValues[0] * accelerometerValues[0] +
                    accelerometerValues[1] * accelerometerValues[1] +
                    accelerometerValues[2] * accelerometerValues[2]
                )
                showToast("Baseline established. Move device around to scan.")
            }
        }, 2000)
    }
    
    private fun stopSensorDetection() {
        isDetecting = false
        sensorManager.unregisterListener(this)
        binding.btnSensorDetection.text = "START"
        binding.btnSensorDetection.backgroundTintList = ContextCompat.getColorStateList(this, R.color.trust_blue)
        showToast("Sensor detection stopped")
    }
    
//    private fun showManualDetectionGuide() {
//        val guideSteps = arrayOf(
//            "Step 1: Turn off all lights in the room",
//            "Step 2: Use your phone's flashlight to shine on suspicious objects",
//            "Step 3: Look for small glints or reflections from camera lenses",
//            "Step 4: Check common hiding spots:",
//            "   • Smoke detectors",
//            "   • Picture frames",
//            "   • Clock faces",
//            "   • Power outlets",
//            "   • Air vents",
//            "   • Decorative objects",
//            "Step 5: Use your phone's camera to look for infrared lights",
//            "Step 6: Check for unusual wifi networks or bluetooth devices"
//        )
//
//        AlertDialog.Builder(this)
//            .setTitle("Manual Detection Guide")
//            .setMessage(guideSteps.joinToString("\n\n"))
//            .setPositiveButton("Start Detection") { _, _ ->
//                startInfraredDetection()
//            }
//            .setNegativeButton("Close", null)
//            .show()
//
//        showToast(getString(R.string.manual_detection_started))
//    }
//
//    private fun startInfraredDetection() {
//        FeaturePermissionHelper.Companion.CameraFeature.checkPermissions(this) {
//            startCameraAfterPermissionGranted()
//        }
//
//        showToast(getString(R.string.infrared_detection_started))
//
//        // Intent to open camera for infrared detection
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        if (intent.resolveActivity(packageManager) != null) {
//            AlertDialog.Builder(this)
//                .setTitle("Infrared Detection Instructions")
//                .setMessage("1. Turn off all lights\n2. Use the camera to scan the room\n3. Look for small bright spots or glowing lights\n4. Hidden cameras often emit infrared light visible through phone cameras")
//                .setPositiveButton("Open Camera") { _, _ ->
//                    try {
//                        startActivity(intent)
//                    } catch (e: Exception) {
//                        showToast("Unable to open camera: ${e.message}")
//                    }
//                }
//                .setNegativeButton("Cancel", null)
//                .show()
//        } else {
//            showToast("Camera not available")
//        }
//    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (!isDetecting || event == null) return
        
        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magneticValues = event.values.clone()
                checkMagneticAnomaly()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerValues = event.values.clone()
            }
        }
    }
    
    private fun checkMagneticAnomaly() {
        if (baselineMagnetic == 0f) return
        
        val currentMagnetic = sqrt(
            magneticValues[0] * magneticValues[0] +
            magneticValues[1] * magneticValues[1] +
            magneticValues[2] * magneticValues[2]
        )
        
        val difference = abs(currentMagnetic - baselineMagnetic)
        
        if (difference > DETECTION_THRESHOLD) {
            onPotentialCameraDetected()
        }
    }
    
    private fun onPotentialCameraDetected() {
        // Vibrate
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(500)
            }
        }
        
        // Show alert
        showToast(getString(R.string.hidden_camera_detected))
        
        // Visual feedback
        binding.root.setBackgroundColor(Color.RED)
        handler.postDelayed({
            binding.root.setBackgroundColor(Color.WHITE)
        }, 1000)
        
        // Temporarily pause detection to avoid spam
        val wasDetecting = isDetecting
        stopSensorDetection()
        
        // Show detailed alert
        AlertDialog.Builder(this)
            .setTitle("⚠️ Potential Hidden Camera Detected!")
            .setMessage("A magnetic field anomaly was detected. This could indicate:\n\n• Electronic device nearby\n• Hidden camera\n• Metal object\n\nInvestigate the area carefully and consider moving to a different location if you feel unsafe.")
            .setPositiveButton("Continue Scanning") { _, _ ->
                if (wasDetecting) {
                    handler.postDelayed({ startSensorDetection() }, 500)
                }
            }
            .setNegativeButton("Stop", null)
            .show()
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (isDetecting) {
                    stopSensorDetection()
                }
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isDetecting) {
            stopSensorDetection()
        }
        mediaPlayer?.release()
    }
    
    override fun onPause() {
        super.onPause()
        if (isDetecting) {
            sensorManager.unregisterListener(this)
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Recheck camera permissions when returning from settings
        FeaturePermissionHelper.Companion.CameraFeature.recheckPermissions(this) {
            // Camera permission granted, can proceed with camera features
        }
        
        if (isDetecting) {
            magneticSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun start(activity: AppCompatActivity) {
            val intent = Intent(activity, CameraDetectorActivity::class.java)
            activity.startActivity(intent)
        }
    }
}
