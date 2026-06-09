package com.soulmate.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class BackendUrlResolverTest {

    @Test
    fun `explicit override wins and gets normalized`() {
        val resolved = BackendUrlResolver.resolveBaseUrl(
            overrideUrl = "http://192.168.1.10:8080",
            emulatorUrl = "http://10.0.2.2:8080/",
            deviceUrl = "http://localhost:8080/",
            isEmulator = false
        )

        assertEquals("http://192.168.1.10:8080/", resolved)
    }

    @Test
    fun `emulator default is used when no override is set`() {
        val resolved = BackendUrlResolver.resolveBaseUrl(
            overrideUrl = "",
            emulatorUrl = "http://10.0.2.2:8080",
            deviceUrl = "http://localhost:8080/",
            isEmulator = true
        )

        assertEquals("http://10.0.2.2:8080/", resolved)
    }

    @Test
    fun `device default is used when not running on emulator`() {
        val resolved = BackendUrlResolver.resolveBaseUrl(
            overrideUrl = "",
            emulatorUrl = "http://10.0.2.2:8080/",
            deviceUrl = "http://localhost:8080",
            isEmulator = false
        )

        assertEquals("http://localhost:8080/", resolved)
    }

    @Test
    fun `endpoint appends path without duplicate slash`() {
        val endpoint = BackendUrlResolver.buildEndpoint(
            baseUrl = "http://localhost:8080/",
            path = "/api/secure/cloudinary/sign-upload"
        )

        assertEquals("http://localhost:8080/api/secure/cloudinary/sign-upload", endpoint)
    }
}
