package com.example.myapp

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SirenActivity : AppCompatActivity() {
    
    private var mediaPlayer: MediaPlayer? = null
    private var colorBackground: FrameLayout? = null
    private var edgeGlow: View? = null
    private var colorAnimator: ValueAnimator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    
    // Colors for the background animation - emergency/siren themed
    private val colors = listOf(
        Color.RED,
        Color.rgb(255, 100, 0),  // Orange-red
        Color.YELLOW,
        Color.rgb(255, 0, 255),  // Magenta
        Color.RED,
        Color.rgb(255, 50, 50),  // Bright red
        Color.rgb(255, 150, 0)   // Orange
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_siren)
        
        colorBackground = findViewById(R.id.colorBackground)
        edgeGlow = findViewById(R.id.edgeGlow)
        
        // Set initial state
        edgeGlow?.alpha = 0f
        
        // Set up the stop button
        val btnStopSiren = findViewById<Button>(R.id.btnStopSiren)
        btnStopSiren.setOnClickListener {
            stopSiren()
            saveSirenState(false)
            finish()
        }
        
        // Save siren state and start the siren automatically when activity is created
        saveSirenState(true)
        playSiren()
        startColorAnimation()
        }
    
    private fun saveSirenState(isActive: Boolean) {
        val sharedPref = getSharedPreferences("SirenState", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isSirenActive", isActive)
            apply()
        }
    }
    
    private fun updateSirenState() {
        if (isRunning) {
            stopSiren()
        } else {
            playSiren()
            startColorAnimation()
        }
    }
    
    
    private fun playSiren() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.siren2).apply {
                isLooping = true
                start()
                isRunning = true
                Toast.makeText(this@SirenActivity, "Siren started", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startColorAnimation() {
        if (colorAnimator?.isRunning == true) return
        
        // Start edge glow animation
        edgeGlow?.animate()
            ?.alpha(0.8f)
            ?.setDuration(500)
            ?.start()
        
        // Create a pulsating effect with faster color transitions
        colorAnimator = ValueAnimator().apply {
            setObjectValues(*colors.toTypedArray())
            setEvaluator(ArgbEvaluator())
            duration = 800  // Faster transition between colors
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animator ->
                if (isRunning && colorBackground != null && edgeGlow != null) {
                    val color = animator.animatedValue as Int
                    
                    // Create a gradient-like effect by slightly varying the alpha
                    val alphaAdjustedColor = Color.argb(
                        220,  // Slightly transparent for a glowing effect
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)
                    )
                    
                    // Update background color
                    colorBackground?.setBackgroundColor(alphaAdjustedColor)
                    
                    // Update edge glow color
                    val edgeColor = Color.argb(
                        100,
                        (Color.red(color) * 1.5f).coerceAtMost(255f).toInt(),
                        (Color.green(color) * 0.7f).toInt(),
                        (Color.blue(color) * 0.7f).toInt()
                    )
                    
                    colorBackground?.let { bg ->
                        val gradient = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(edgeColor, Color.TRANSPARENT)
                        )
                        gradient.gradientType = GradientDrawable.RADIAL_GRADIENT
                        gradient.gradientRadius = Math.max(bg.width, bg.height).toFloat() / 1.5f
                        gradient.setGradientCenter(0.5f, 0.5f)
                        
                        edgeGlow?.background = gradient
                    }
                    
                    // Add a subtle pulsing effect to the edge glow
                    if (System.currentTimeMillis() % 1000 < 100) {
                        edgeGlow?.animate()
                            ?.scaleX(1.1f)
                            ?.scaleY(1.1f)
                            ?.setDuration(100)
                            ?.withEndAction {
                                edgeGlow?.animate()
                                    ?.scaleX(1f)
                                    ?.scaleY(1f)
                                    ?.setDuration(100)
                                    ?.start()
                            }
                            ?.start()
                    }
                }
            }
            
            start()
        }
    }
    
    private fun stopSiren() {
        isRunning = false
        colorAnimator?.cancel()
        
        // Fade out edge glow
        edgeGlow?.animate()
            ?.alpha(0f)
            ?.setDuration(300)
            ?.start()
            
        // Reset any transformations
        colorBackground?.scaleX = 1f
        colorBackground?.scaleY = 1f
        edgeGlow?.scaleX = 1f
        edgeGlow?.scaleY = 1f
        
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.release()
                mediaPlayer = null
                Toast.makeText(this, "Siren stopped", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Complete resource cleanup to prevent memory leaks
        stopSiren()
        saveSirenState(false)
        
        // Clean up handler and remove all callbacks
        handler.removeCallbacksAndMessages(null)
        
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
        
        // Clean up view references to prevent leaks
        colorBackground = null
        edgeGlow = null
        
        isRunning = false
    }
    
    override fun onBackPressed() {
        stopSiren()
        saveSirenState(false)
        super.onBackPressed()
    }
}
