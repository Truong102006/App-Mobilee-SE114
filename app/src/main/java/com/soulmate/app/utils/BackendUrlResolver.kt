package com.soulmate.app.utils

import android.os.Build
import com.soulmate.app.BuildConfig
import java.util.Locale

object BackendUrlResolver {

    fun resolveBaseUrl(): String =
        resolveBaseUrl(
            overrideUrl = BuildConfig.BACKEND_BASE_URL,
            emulatorUrl = BuildConfig.BACKEND_BASE_URL_EMULATOR,
            deviceUrl = BuildConfig.BACKEND_BASE_URL_DEVICE,
            isEmulator = isProbablyEmulator()
        )

    fun resolveBaseUrl(
        overrideUrl: String,
        emulatorUrl: String,
        deviceUrl: String,
        isEmulator: Boolean
    ): String {
        val explicitUrl = normalizeBaseUrl(overrideUrl)
        if (explicitUrl.isNotEmpty()) {
            return explicitUrl
        }
        return normalizeBaseUrl(if (isEmulator) emulatorUrl else deviceUrl)
    }

    fun buildEndpoint(path: String): String = buildEndpoint(resolveBaseUrl(), path)

    fun buildEndpoint(baseUrl: String, path: String): String =
        normalizeBaseUrl(baseUrl) + path.trimStart('/')

    fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            return ""
        }
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    // Keep emulator detection local so debug builds can switch URLs automatically.
    private fun isProbablyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase(Locale.US)
        val model = Build.MODEL.lowercase(Locale.US)
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
        val brand = Build.BRAND.lowercase(Locale.US)
        val device = Build.DEVICE.lowercase(Locale.US)
        val product = Build.PRODUCT.lowercase(Locale.US)

        return fingerprint.startsWith("generic") ||
            fingerprint.startsWith("unknown") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for x86") ||
            manufacturer.contains("genymotion") ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            product == "google_sdk" ||
            product.contains("sdk_gphone") ||
            product.contains("emulator") ||
            product.contains("simulator")
    }
}
