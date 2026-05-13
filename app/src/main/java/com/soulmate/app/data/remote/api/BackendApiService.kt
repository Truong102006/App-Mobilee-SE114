package com.soulmate.app.data.remote.api

import com.soulmate.app.data.remote.dto.DeleteConversationResponseDto
import com.soulmate.app.data.remote.dto.DeleteDiaryResponseDto
import com.soulmate.app.data.remote.dto.ListConversationResponseDto
import com.soulmate.app.data.remote.dto.ListDiariesResponseDto
import com.soulmate.app.data.remote.dto.ListInboxResponseDto
import com.soulmate.app.data.remote.dto.PredictMoodRequestDto
import com.soulmate.app.data.remote.dto.PredictMoodResponseDto
import com.soulmate.app.data.remote.dto.SaveDiaryRequestDto
import com.soulmate.app.data.remote.dto.SaveDiaryResponseDto
import com.soulmate.app.data.remote.dto.SendChatMessageRequestDto
import com.soulmate.app.data.remote.dto.SendChatMessageResponseDto
import com.soulmate.app.data.remote.dto.SignUploadRequestDto
import com.soulmate.app.data.remote.dto.SignUploadResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BackendApiService {
    @POST("api/secure/ai/predict-mood")
    suspend fun predictMood(
        @Header("Authorization") authorization: String,
        @Body request: PredictMoodRequestDto
    ): PredictMoodResponseDto

    @POST("api/secure/diaries/save")
    suspend fun saveDiary(
        @Header("Authorization") authorization: String,
        @Body request: SaveDiaryRequestDto
    ): SaveDiaryResponseDto

    @GET("api/secure/diaries/me")
    suspend fun listMyDiaries(
        @Header("Authorization") authorization: String
    ): ListDiariesResponseDto

    @DELETE("api/secure/diaries/{diaryId}")
    suspend fun deleteDiary(
        @Header("Authorization") authorization: String,
        @Path("diaryId") diaryId: String
    ): DeleteDiaryResponseDto

    @POST("api/secure/chats/send")
    suspend fun sendChatMessage(
        @Header("Authorization") authorization: String,
        @Body request: SendChatMessageRequestDto
    ): SendChatMessageResponseDto

    @GET("api/secure/chats/conversation/{otherUserId}")
    suspend fun listConversation(
        @Header("Authorization") authorization: String,
        @Path("otherUserId") otherUserId: String,
        @Query("limit") limit: Int = 100
    ): ListConversationResponseDto

    @GET("api/secure/chats/inbox")
    suspend fun listInbox(
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 100
    ): ListInboxResponseDto

    @DELETE("api/secure/chats/conversation/{otherUserId}")
    suspend fun deleteConversation(
        @Header("Authorization") authorization: String,
        @Path("otherUserId") otherUserId: String
    ): DeleteConversationResponseDto

    @POST("api/secure/cloudinary/sign-upload")
    suspend fun signUpload(
        @Header("Authorization") authorization: String,
        @Body request: SignUploadRequestDto
    ): SignUploadResponseDto
}
