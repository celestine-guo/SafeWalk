package com.celestine.safewalk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackendBaseUrlResolverAndroidTest {
    @Test
    fun resolvesRuntimePropertyFromAdbSetprop() {
        val expected = InstrumentationRegistry.getArguments()
            .getString("expected_backend_url")
            ?.trim()
            ?.trimEnd('/')
            .orEmpty()

        assumeTrue(
            "Missing instrumentation argument: expected_backend_url",
            expected.isNotBlank()
        )

        val resolved = BackendBaseUrlResolver.resolve(buildConfigBaseUrl = "https://fallback.example.com/api")

        assertEquals(expected, resolved)
    }
}
