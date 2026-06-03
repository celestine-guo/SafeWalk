package com.celestine.safewalk

object BackendBaseUrlResolver {
    private const val BACKEND_BASE_URL_PROPERTY_KEY = "debug.safewalk.backend_base_url"
    private const val GETPROP_BINARY_PATH = "/system/bin/getprop"

    fun resolve(
        buildConfigBaseUrl: String,
        runtimePropertyReader: (String) -> String? = ::readRuntimeProperty,
    ): String {
        val runtimeValue = runCatching {
            runtimePropertyReader(BACKEND_BASE_URL_PROPERTY_KEY)
        }.getOrNull()
            .orEmpty()
            .trim()
            .trimEnd('/')

        if (runtimeValue.isNotBlank()) {
            return runtimeValue
        }

        return buildConfigBaseUrl.trim().trimEnd('/')
    }

    private fun readRuntimeProperty(key: String): String? {
        return try {
            val process = ProcessBuilder(GETPROP_BINARY_PATH, key).start()
            process.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }
}
