package com.soulmate.app

import android.app.Application
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SoulMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initCloudinary()
    }

    private fun initCloudinary() {
        val config = mapOf(
            "cloud_name" to "dkc8quqpl",
            "api_key" to "674811689495777",
            "api_secret" to "xmLENsUeqI8Z57WjrOTotMjEw6c"
        )
        try {
            MediaManager.init(this, config)
        } catch (e: Exception) {
            // MediaManager already initialized or other error
            e.printStackTrace()
        }
    }
}
