package com.soulmate.app.utils

import com.google.gson.Gson
import retrofit2.HttpException

object BackendErrorParser {

    private val gson = Gson()

    private data class ApiErrorBody(
        val message: String? = null
    )

    fun toUserMessage(throwable: Throwable): String {
        if (throwable is HttpException) {
            val errorBody = runCatching {
                throwable.response()?.errorBody()?.string().orEmpty()
            }.getOrDefault("")

            if (errorBody.isNotBlank()) {
                val parsed = runCatching {
                    gson.fromJson(errorBody, ApiErrorBody::class.java)
                }.getOrNull()

                if (!parsed?.message.isNullOrBlank()) {
                    return parsed?.message ?: throwable.message ?: "Unexpected error"
                }
            }
        }

        return throwable.message ?: "Unexpected error"
    }
}
