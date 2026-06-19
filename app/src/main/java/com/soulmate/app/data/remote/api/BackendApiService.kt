package com.soulmate.app.data.remote.api

import com.soulmate.app.data.remote.dto.CommonResponseDto
import com.soulmate.app.data.remote.dto.DeleteConversationResponseDto
import com.soulmate.app.data.remote.dto.DeleteDiaryResponseDto
import com.soulmate.app.data.remote.dto.ListConversationResponseDto
import com.soulmate.app.data.remote.dto.ListDiariesResponseDto
import com.soulmate.app.data.remote.dto.ListInboxResponseDto
import com.soulmate.app.data.remote.dto.PaymentOrderStatusResponseDto
import com.soulmate.app.data.remote.dto.PredictMoodRequestDto
import com.soulmate.app.data.remote.dto.PredictMoodResponseDto
import com.soulmate.app.data.remote.dto.PremiumOfferResponseDto
import com.soulmate.app.data.remote.dto.CreatePremiumOrderResponseDto
import com.soulmate.app.data.remote.dto.ReconcilePaymentOrderResponseDto
import com.soulmate.app.data.remote.dto.ResolveReportRequestDto
import com.soulmate.app.data.remote.dto.SaveDiaryRequestDto
import com.soulmate.app.data.remote.dto.SaveDiaryResponseDto
import com.soulmate.app.data.remote.dto.SendChatMessageRequestDto
import com.soulmate.app.data.remote.dto.SendChatMessageResponseDto
import com.soulmate.app.data.remote.dto.SignUploadRequestDto
import com.soulmate.app.data.remote.dto.SignUploadResponseDto
import com.soulmate.app.ui.social.CommunityPost
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @POST("api/secure/chats/conversation/{otherUserId}/read")
    suspend fun markAsRead(
        @Header("Authorization") authorization: String,
        @Path("otherUserId") otherUserId: String
    ): Map<String, Any>

    @DELETE("api/secure/chats/messages/{messageId}")
    suspend fun deleteMessage(
        @Header("Authorization") authorization: String,
        @Path("messageId") messageId: String
    ): Map<String, Any>

    @POST("api/secure/cloudinary/sign-upload")
    suspend fun signUpload(
        @Header("Authorization") authorization: String,
        @Body request: SignUploadRequestDto
    ): SignUploadResponseDto

    @POST("api/secure/community/report/{postId}")
    suspend fun reportPost(
        @Header("Authorization") authorization: String,
        @Path("postId") postId: String
    ): CommonResponseDto

    @GET("api/secure/admin/community/reported-posts")
    suspend fun getReportedPosts(
        @Header("Authorization") authorization: String
    ): List<CommunityPost>

    @POST("api/secure/admin/community/resolve-report")
    suspend fun resolveReport(
        @Header("Authorization") authorization: String,
        @Body request: ResolveReportRequestDto
    ): CommonResponseDto

    @POST("api/secure/admin/users/social-ban")
    suspend fun toggleSocialBan(
        @Header("Authorization") authorization: String,
        @Query("targetUserId") targetUserId: String,
        @Query("isBanned") isBanned: Boolean
    ): CommonResponseDto

    @GET("api/secure/payments/premium/offer")
    suspend fun getPremiumOffer(
        @Header("Authorization") authorization: String
    ): PremiumOfferResponseDto

    @POST("api/secure/payments/premium/orders")
    suspend fun createOrResumePremiumOrder(
        @Header("Authorization") authorization: String
    ): CreatePremiumOrderResponseDto

    @GET("api/secure/payments/orders/{orderId}")
    suspend fun getPaymentOrderStatus(
        @Header("Authorization") authorization: String,
        @Path("orderId") orderId: String
    ): PaymentOrderStatusResponseDto

    @POST("api/secure/payments/orders/{orderId}/reconcile")
    suspend fun reconcilePaymentOrder(
        @Header("Authorization") authorization: String,
        @Path("orderId") orderId: String
    ): ReconcilePaymentOrderResponseDto
}

