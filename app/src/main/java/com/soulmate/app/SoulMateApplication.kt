package com.soulmate.app

import android.app.Application
import com.soulmate.app.notifications.AppNotificationManager
import com.soulmate.app.presence.PresenceSyncManager
import com.soulmate.app.utils.CloudinaryHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SoulMateApplication : Application() {

    @Inject
    lateinit var notificationManager: AppNotificationManager

    @Inject
    lateinit var presenceSyncManager: PresenceSyncManager

    override fun onCreate() {
        super.onCreate()
        CloudinaryHelper.init(this)
        notificationManager.initialize()
        presenceSyncManager.start()
    }
}
