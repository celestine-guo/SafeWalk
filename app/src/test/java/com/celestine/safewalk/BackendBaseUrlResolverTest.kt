package com.celestine.safewalk

import org.junit.Assert.assertEquals
import org.junit.Test

class BackendBaseUrlResolverTest {
    @Test
    fun runtimePropertyOverridesBuildConfigValue() {
        val resolved = BackendBaseUrlResolver.resolve(
            buildConfigBaseUrl = "https://build.example.com/api",
            runtimePropertyReader = { "http://10.0.2.2:8000/api/" },
        )

        assertEquals("http://10.0.2.2:8000/api", resolved)
    }

    @Test
    fun emptyRuntimePropertyFallsBackToBuildConfig() {
        val resolved = BackendBaseUrlResolver.resolve(
            buildConfigBaseUrl = "https://build.example.com/api/",
            runtimePropertyReader = { "   " },
        )

        assertEquals("https://build.example.com/api", resolved)
    }

    @Test
    fun runtimePropertyReaderFailureFallsBackToBuildConfig() {
        val resolved = BackendBaseUrlResolver.resolve(
            buildConfigBaseUrl = "https://build.example.com/api/",
            runtimePropertyReader = { throw IllegalStateException("unavailable") },
        )

        assertEquals("https://build.example.com/api", resolved)
    }
}
