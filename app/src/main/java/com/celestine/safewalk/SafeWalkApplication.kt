package com.celestine.safewalk

import android.app.Application
import com.amap.api.maps.MapsInitializer
import com.clerk.api.Clerk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SafeWalkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        MapsInitializer.initialize(this)

        if (BuildConfig.CLERK_PUBLISHABLE_KEY.isNotBlank()) {
            SafeWalkAuthInitialization.clearError()
            try {
                Clerk.initialize(
                    this,
                    publishableKey = BuildConfig.CLERK_PUBLISHABLE_KEY
                )
            } catch (exception: Exception) {
                SafeWalkAuthInitialization.recordError(exception)
            }
        }
    }
}

object SafeWalkAuthInitialization {
    private val _initializationError = MutableStateFlow<Throwable?>(null)
    val initializationError: StateFlow<Throwable?> = _initializationError

    fun recordError(error: Throwable) {
        _initializationError.value = error
    }

    fun clearError() {
        _initializationError.value = null
    }
}
