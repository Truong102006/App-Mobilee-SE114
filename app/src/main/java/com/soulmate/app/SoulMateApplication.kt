package com.soulmate.app

import android.app.Application
import com.soulmate.app.utils.CloudinaryHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SoulMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CloudinaryHelper.init(this)
    }
}
