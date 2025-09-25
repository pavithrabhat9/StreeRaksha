package com.example.myapp

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.myapp.di.AppModule
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize

/**
 * Application class for dependency injection and global app state
 */
class MyApplication : Application() {
    
    lateinit var appModule: AppModule
        private set
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Initialize Firebase as early as possible
        initializeFirebase()
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize dependency injection
        appModule = AppModule(this)
        
        // Store reference for global access
        instance = this
    }
    
    private fun initializeFirebase() {
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps(this).isEmpty()) {
                Log.d("MyApplication", "Initializing Firebase...")
                
                // Initialize Firebase with the options from google-services.json
                FirebaseApp.initializeApp(this)
                
                // Verify Firebase components
                try {
                    val auth = Firebase.auth
                    val db = Firebase.firestore
                    Log.d("MyApplication", "Firebase initialized successfully. Auth: ${auth.app.name}, Firestore: ${db.app.name}")
                } catch (e: Exception) {
                    Log.e("MyApplication", "Firebase component initialization error: ${e.message}", e)
                }
            } else {
                Log.d("MyApplication", "Firebase already initialized")
            }
        } catch (e: Exception) {
            Log.e("MyApplication", "Failed to initialize Firebase: ${e.message}", e)
            // Try to recover by initializing with explicit options
            try {
                val options = FirebaseOptions.Builder()
                    .setProjectId("myapp-c30c0")
                    .setApplicationId("1:974537963820:android:e661da56c1f71912f41489")
                    .setApiKey("AIzaSyDvTxqWYjwsW21xaa9UGf-i-uzMunb-hBU")
                    .setDatabaseUrl("https://myapp-c30c0.firebaseio.com")
                    .setStorageBucket("myapp-c30c0.appspot.com")
                    .build()
                
                FirebaseApp.initializeApp(this, options, "MyApp")
                Log.d("MyApplication", "Firebase initialized with explicit options")
            } catch (e2: Exception) {
                Log.e("MyApplication", "Failed to initialize Firebase with explicit options: ${e2.message}", e2)
            }
        }
    }
    
    companion object {
        lateinit var instance: MyApplication
            private set
    }
}
