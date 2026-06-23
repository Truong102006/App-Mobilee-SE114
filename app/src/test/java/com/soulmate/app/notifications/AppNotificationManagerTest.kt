package com.soulmate.app.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.notifications.INotificationsManager
import com.onesignal.user.IUserManager
import com.onesignal.user.subscriptions.IPushSubscription
import com.soulmate.app.domain.model.User
import com.soulmate.app.domain.repository.ISettingsRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppNotificationManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var appContext: Context
    private lateinit var settingsRepository: ISettingsRepository
    private lateinit var notificationManager: AppNotificationManager

    private lateinit var mockNotifications: INotificationsManager
    private lateinit var mockUserManager: IUserManager
    private lateinit var mockPushSubscription: IPushSubscription
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockFirebaseUser: FirebaseUser

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any()) } returns 0

        mockkStatic(OneSignal::class)
        mockkStatic(FirebaseAuth::class)

        appContext = mockk(relaxed = true)
        settingsRepository = mockk()

        // Mock OneSignal inner managers
        mockNotifications = mockk(relaxed = true)
        mockUserManager = mockk(relaxed = true)
        mockPushSubscription = mockk(relaxed = true)

        every { OneSignal.Notifications } returns mockNotifications
        every { OneSignal.User } returns mockUserManager
        every { mockUserManager.pushSubscription } returns mockPushSubscription
        every { OneSignal.initWithContext(any(), any()) } returns Unit
        every { OneSignal.login(any()) } returns Unit
        every { OneSignal.logout() } returns Unit

        // Mock FirebaseAuth
        mockAuth = mockk()
        mockFirebaseUser = mockk()
        every { FirebaseAuth.getInstance() } returns mockAuth
        every { mockAuth.currentUser } returns mockFirebaseUser
        every { mockFirebaseUser.uid } returns "my-firebase-uid"

        // Mock settings repository Flow
        every { settingsRepository.notificationEnabled } returns flowOf(true)

        notificationManager = AppNotificationManager(appContext, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun testInitialize_success() {
        // Act
        notificationManager.initialize()

        // Assert
        verify { OneSignal.initWithContext(appContext, any()) }
        verify { OneSignal.login("my-firebase-uid") }
        verify { mockNotifications.addClickListener(any()) }
    }

    @Test
    fun testInitialize_userNotLoggedIn() {
        // Arrange
        every { mockAuth.currentUser } returns null // No user log in

        // Act
        notificationManager.initialize()

        // Assert
        verify { OneSignal.initWithContext(appContext, any()) }
        verify(exactly = 0) { OneSignal.login(any()) }
    }

    @Test
    fun testOnUserAuthenticated_loginAndPreferencesSynced() {
        // Arrange
        val user = User(
            userId = "authenticated-user-123",
            anonymousName = "Name",
            avatarUrl = "url",
            notificationEnabled = true,
            isPremium = false,
            premiumUntil = null,
            createdAt = 0L,
            updatedAt = 0L
        )

        // Act
        notificationManager.onUserAuthenticated(user, requestPermissionIfNeeded = true)

        // Assert
        verify { OneSignal.login("authenticated-user-123") }
        verify { mockPushSubscription.optIn() }
    }

    @Test
    fun testApplyNotificationPreference_optOutWhenDisabled() {
        // Act
        notificationManager.applyNotificationPreference(enabled = false, requestPermissionIfNeeded = false)

        // Assert
        verify { mockPushSubscription.optOut() }
        verify(exactly = 0) { mockPushSubscription.optIn() }
    }

    @Test
    fun testApplyNotificationPreference_optInWhenEnabledAndPermissionGranted() {
        // Arrange
        every { mockNotifications.permission } returns true

        // Act
        notificationManager.applyNotificationPreference(enabled = true, requestPermissionIfNeeded = false)

        // Assert
        verify { mockPushSubscription.optIn() }
    }

    @Test
    fun testApplyNotificationPreference_optInWhenForceRequestPermission() {
        // Act
        notificationManager.applyNotificationPreference(enabled = true, requestPermissionIfNeeded = true)

        // Assert
        verify { mockPushSubscription.optIn() }
    }

    @Test
    fun testLogout() {
        // Act
        notificationManager.logout()

        // Assert
        verify { OneSignal.logout() }
    }

    @Test
    fun testPendingNavigation_clickToChat() {
        // Arrange
        notificationManager.initialize()
        val listenerSlot = slot<INotificationClickListener>()
        verify { mockNotifications.addClickListener(capture(listenerSlot)) }

        val mockClickEvent = mockk<INotificationClickEvent>()
        val mockNotification = mockk<com.onesignal.notifications.INotification>()
        val mockData = mockk<JSONObject>()

        every { mockClickEvent.notification } returns mockNotification
        every { mockNotification.additionalData } returns mockData

        every { mockData.optString("screen") } returns "chat"
        every { mockData.optString("userId") } returns "peer-user-999"
        every { mockData.optString("userName") } returns "Peer User"
        every { mockData.optString("avatarUrl") } returns "http://avatar-peer"

        // Act
        listenerSlot.captured.onClick(mockClickEvent)

        // Assert
        val dest = notificationManager.pendingNavigation.value
        assertNotNull(dest)
        assertTrue(dest is NotificationDestination.Chat)
        val chatDest = dest as NotificationDestination.Chat
        assertEquals("peer-user-999", chatDest.userId)
        assertEquals("Peer User", chatDest.userName)
        assertEquals("http://avatar-peer", chatDest.avatarUrl)

        // Consume
        notificationManager.consumePendingNavigation()
        assertNull(notificationManager.pendingNavigation.value)
    }

    @Test
    fun testPendingNavigation_clickToCommunityHome() {
        // Arrange
        notificationManager.initialize()
        val listenerSlot = slot<INotificationClickListener>()
        verify { mockNotifications.addClickListener(capture(listenerSlot)) }

        val mockClickEvent = mockk<INotificationClickEvent>()
        val mockNotification = mockk<com.onesignal.notifications.INotification>()
        val mockData = mockk<JSONObject>()

        every { mockClickEvent.notification } returns mockNotification
        every { mockNotification.additionalData } returns mockData

        every { mockData.optString("screen") } returns "community"

        // Act
        listenerSlot.captured.onClick(mockClickEvent)

        // Assert
        val dest = notificationManager.pendingNavigation.value
        assertNotNull(dest)
        assertTrue(dest is NotificationDestination.Home)
    }
}
