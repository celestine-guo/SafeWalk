package com.celestine.safewalk

import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.coroutines.runBlocking

interface AuthTokenProvider {
    fun getToken(): String?
}

class ClerkAuthTokenProvider : AuthTokenProvider {
    override fun getToken(): String? = runBlocking {
        when (val result = Clerk.auth.getToken()) {
            is ClerkResult.Success -> result.value
            is ClerkResult.Failure -> null
        }
    }
}
