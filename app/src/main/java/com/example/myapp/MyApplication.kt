package com.example.myapp

import android.app.Application
import com.example.myapp.di.AppModule

/**
 * Application class for dependency injection and global app state
 */
class MyApplication : Application() {
    
    lateinit var appModule: AppModule
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize dependency injection
        appModule = AppModule(this)
        
        // Store reference for global access
        instance = this
    }
    
    companion object {
        lateinit var instance: MyApplication
            private set
    }
}
