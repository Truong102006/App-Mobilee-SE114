package com.soulmate.app.utils

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudinaryHelper {

    fun uploadImage(
        uri: Uri,
        onProgress: (Double) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        MediaManager.get().upload(uri)
            .unsigned("soulmate_upload")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    onProgress(0.0)
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = (bytes.toDouble() / totalBytes)
                    onProgress(progress)
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val secureUrl = resultData["secure_url"] as? String ?: ""
                    onSuccess(secureUrl)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    onError(error.description)
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    onError("Rescheduled: ${error.description}")
                }
            })
            .dispatch()
    }

    // Hàm suspend để dùng trong Repository với Coroutines
    suspend fun uploadImageSuspend(uri: Uri): String = suspendCancellableCoroutine { continuation ->
        MediaManager.get().upload(uri)
            .unsigned("soulmate_upload")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val secureUrl = resultData["secure_url"] as? String ?: ""
                    continuation.resume(secureUrl)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    continuation.resumeWithException(Exception(error.description))
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    continuation.resumeWithException(Exception("Rescheduled: ${error.description}"))
                }
            })
            .dispatch()
    }
}
