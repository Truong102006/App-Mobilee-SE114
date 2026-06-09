package com.soulmate.app.utils

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.soulmate.app.data.remote.dto.SignUploadResponseDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object CloudinaryHelper {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val helperScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun uploadImage(
        uri: Uri,
        onProgress: (Double) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        onProgress(0.0)
        helperScope.launch {
            runCatching { uploadImageSuspend(uri) }
                .onSuccess { url ->
                    withContext(Dispatchers.Main) {
                        onProgress(1.0)
                        onSuccess(url)
                    }
                }
                .onFailure { error ->
                    withContext(Dispatchers.Main) {
                        onError(error.message ?: "Upload failed")
                    }
                }
        }
    }

    suspend fun uploadImageSuspend(uri: Uri): String = withContext(Dispatchers.IO) {
        val context = appContext ?: throw IllegalStateException("CloudinaryHelper is not initialized")
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("User must be logged in before upload")
        val idToken = user.getIdToken(true).await().token
            ?: throw IllegalStateException("Cannot get Firebase ID token")

        val signResponse = requestUploadSignature(idToken)
        val requestBody = buildMultipartBody(context, uri, signResponse)

        val uploadRequest = Request.Builder()
            .url(signResponse.uploadUrl)
            .post(requestBody)
            .build()

        client.newCall(uploadRequest).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Cloudinary upload failed: HTTP ${response.code}")
            }

            val payload = gson.fromJson(body, JsonObject::class.java)
            val secureUrl = payload?.get("secure_url")?.asString
            if (secureUrl.isNullOrBlank()) {
                throw IllegalStateException("Cloudinary upload missing secure_url")
            }
            secureUrl
        }
    }

    private fun requestUploadSignature(idToken: String): SignUploadResponseDto {
        val requestJson = gson.toJson(mapOf("publicId" to null, "context" to "source=android"))

        val request = Request.Builder()
            .url(BackendUrlResolver.buildEndpoint("api/secure/cloudinary/sign-upload"))
            .addHeader("Authorization", "Bearer $idToken")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Sign upload failed: HTTP ${response.code}")
            }

            val signResponse = gson.fromJson(body, SignUploadResponseDto::class.java)
            if (signResponse == null || signResponse.uploadUrl.isBlank()) {
                throw IllegalStateException("Invalid sign-upload response")
            }
            return signResponse
        }
    }

    private fun buildMultipartBody(
        context: Context,
        uri: Uri,
        sign: SignUploadResponseDto
    ): MultipartBody {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "upload.jpg"
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot open image stream")

        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, bytes.toRequestBody(mimeType.toMediaType()))
            .addFormDataPart("api_key", sign.apiKey)
            .addFormDataPart("timestamp", sign.timestamp.toString())
            .addFormDataPart("signature", sign.signature)
            .addFormDataPart("folder", sign.folder)

        if (!sign.publicId.isNullOrBlank()) {
            builder.addFormDataPart("public_id", sign.publicId)
        }
        if (!sign.context.isNullOrBlank()) {
            builder.addFormDataPart("context", sign.context)
        }

        return builder.build()
    }
}
