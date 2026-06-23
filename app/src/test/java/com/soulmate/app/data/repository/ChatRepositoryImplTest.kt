package com.soulmate.app.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.soulmate.app.data.remote.api.BackendApiService
import com.soulmate.app.data.remote.dto.ChatMessageItemDto
import com.soulmate.app.data.remote.dto.ListConversationResponseDto
import com.soulmate.app.data.remote.dto.ListInboxResponseDto
import com.soulmate.app.data.remote.dto.DeleteConversationResponseDto
import com.soulmate.app.data.remote.dto.SendChatMessageResponseDto
import com.soulmate.app.data.remote.dto.SendChatMessageRequestDto
import com.soulmate.app.domain.model.ChatMessage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ChatRepositoryImplTest {

    private lateinit var backendApiService: BackendApiService
    private lateinit var auth: FirebaseAuth
    private lateinit var chatRepository: ChatRepositoryImpl

    private lateinit var mockUser: FirebaseUser
    private lateinit var mockTokenTask: Task<GetTokenResult>
    private lateinit var mockTokenResult: GetTokenResult

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        backendApiService = mockk()
        auth = mockk()

        mockUser = mockk()
        mockTokenTask = mockk()
        mockTokenResult = mockk()

        // Mock Firebase instances
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "my-user-id"
        every { mockUser.getIdToken(any()) } returns mockTokenTask

        // Mock Task properties to instantly resolve Task.await()
        every { mockTokenTask.isComplete } returns true
        every { mockTokenTask.isCanceled } returns false
        every { mockTokenTask.exception } returns null
        every { mockTokenTask.result } returns mockTokenResult
        every { mockTokenResult.token } returns "fake-firebase-token"

        chatRepository = ChatRepositoryImpl(backendApiService, auth)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testSendMessage_success() = runBlocking {
        // Arrange
        val message = ChatMessage(
            id = "msg-123",
            senderId = "my-user-id",
            receiverId = "their-user-id",
            messageText = "Hello Test",
            imageUrl = null,
            timestamp = null
        )

        coEvery {
            backendApiService.sendChatMessage(
                authorization = "Bearer fake-firebase-token",
                request = SendChatMessageRequestDto(
                    receiverId = "their-user-id",
                    messageText = "Hello Test",
                    imageUrl = null
                )
            )
        } returns SendChatMessageResponseDto(messageId = "msg-123", conversationId = "conv-456")

        // Act
        val result = chatRepository.sendMessage(message)

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            backendApiService.sendChatMessage("Bearer fake-firebase-token", any())
        }
    }

    @Test
    fun testSendMessage_blankReceiver_returnsFailure() = runBlocking {
        val message = ChatMessage(
            id = "msg-123",
            senderId = "my-user-id",
            receiverId = "  ",
            messageText = "Hello Test",
            imageUrl = null,
            timestamp = null
        )

        val result = chatRepository.sendMessage(message)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("receiverId is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun testSendMessage_blankTextAndImage_returnsFailure() = runBlocking {
        val message = ChatMessage(
            id = "msg-123",
            senderId = "my-user-id",
            receiverId = "their-user-id",
            messageText = "   ",
            imageUrl = "",
            timestamp = null
        )

        val result = chatRepository.sendMessage(message)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("messageText or imageUrl is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun testGetMessages_success() = runBlocking {
        // Arrange
        val mockDtoList = listOf(
            ChatMessageItemDto(
                id = "msg-1",
                senderId = "their-user-id",
                receiverId = "my-user-id",
                messageText = "Hi there",
                imageUrl = null,
                timestamp = 1000L
            ),
            ChatMessageItemDto(
                id = "msg-2",
                senderId = "my-user-id",
                receiverId = "their-user-id",
                messageText = "Hello",
                imageUrl = "http://image-url",
                timestamp = 2000L
            )
        )

        coEvery {
            backendApiService.listConversation(
                authorization = "Bearer fake-firebase-token",
                otherUserId = "their-user-id",
                limit = 200
            )
        } returns ListConversationResponseDto(conversationId = "conv-456", messages = mockDtoList)

        // Act
        val flow = chatRepository.getMessages("my-user-id", "their-user-id")
        val resultList = flow.first()

        // Assert
        assertEquals(2, resultList.size)
        assertEquals("msg-1", resultList[0].id)
        assertEquals("their-user-id", resultList[0].senderId)
        assertEquals("Hi there", resultList[0].messageText)
        assertEquals(1000L, resultList[0].timestamp?.seconds?.times(1000L))

        assertEquals("msg-2", resultList[1].id)
        assertEquals("http://image-url", resultList[1].imageUrl)
    }

    @Test
    fun testGetMessages_exception_returnsEmptyList() = runBlocking {
        // Arrange
        coEvery {
            backendApiService.listConversation(any(), any(), any())
        } throws RuntimeException("Network Error")

        // Act
        val flow = chatRepository.getMessages("my-user-id", "their-user-id")
        val resultList = flow.first()

        // Assert
        assertTrue(resultList.isEmpty())
    }

    @Test
    fun testGetLastMessages_success() = runBlocking {
        // Arrange
        val mockDtoList = listOf(
            ChatMessageItemDto(
                id = "msg-1",
                senderId = "their-user-id",
                receiverId = "my-user-id",
                messageText = "Hi",
                imageUrl = null,
                timestamp = 1000L
            )
        )

        coEvery {
            backendApiService.listInbox(
                authorization = "Bearer fake-firebase-token",
                limit = 100
            )
        } returns ListInboxResponseDto(messages = mockDtoList)

        // Act
        val flow = chatRepository.getLastMessages("my-user-id")
        val resultList = flow.first()

        // Assert
        assertEquals(1, resultList.size)
        assertEquals("msg-1", resultList[0].id)
    }

    @Test
    fun testDeleteConversation_success() = runBlocking {
        // Arrange
        coEvery {
            backendApiService.deleteConversation(
                authorization = "Bearer fake-firebase-token",
                otherUserId = "their-user-id"
            )
        } returns DeleteConversationResponseDto(conversationId = "conv-456", deletedCount = 1)

        // Act
        val result = chatRepository.deleteConversation("my-user-id", "their-user-id")

        // Assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun testExecuteWithToken_tokenExpired_retriesWithForceRefresh() = runBlocking {
        // Arrange
        val message = ChatMessage(
            id = "msg-123",
            senderId = "my-user-id",
            receiverId = "their-user-id",
            messageText = "Hello",
            imageUrl = null,
            timestamp = null
        )

        // Mock a 401 HttpException first
        val mock401Error = Response.error<Unit>(401, "".toResponseBody(null))
        val httpException = HttpException(mock401Error)

        coEvery {
            backendApiService.sendChatMessage(
                authorization = "Bearer fake-firebase-token",
                request = any()
            )
        } throws httpException andThen SendChatMessageResponseDto(messageId = "msg-123") // First call throws 401, second succeeds

        val mockRefreshedTokenTask = mockk<Task<GetTokenResult>>()
        val mockRefreshedTokenResult = mockk<GetTokenResult>()

        // Mock target behavior on force refresh
        every { mockUser.getIdToken(true) } returns mockRefreshedTokenTask
        every { mockRefreshedTokenTask.isComplete } returns true
        every { mockRefreshedTokenTask.isCanceled } returns false
        every { mockRefreshedTokenTask.exception } returns null
        every { mockRefreshedTokenTask.result } returns mockRefreshedTokenResult
        every { mockRefreshedTokenResult.token } returns "refreshed-firebase-token"

        coEvery {
            backendApiService.sendChatMessage(
                authorization = "Bearer refreshed-firebase-token",
                request = any()
            )
        } returns SendChatMessageResponseDto(messageId = "msg-123")

        // Act
        val result = chatRepository.sendMessage(message)

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { mockUser.getIdToken(false) }
        coVerify(exactly = 1) { mockUser.getIdToken(true) }
        coVerify(exactly = 1) {
            backendApiService.sendChatMessage("Bearer fake-firebase-token", any())
        }
        coVerify(exactly = 1) {
            backendApiService.sendChatMessage("Bearer refreshed-firebase-token", any())
        }
    }
}
