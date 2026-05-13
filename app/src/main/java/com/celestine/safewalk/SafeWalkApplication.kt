package com.celestine.safewalk

import android.app.Application
import com.amap.api.maps.MapsInitializer
import com.clerk.api.Clerk

class SafeWalkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        MapsInitializer.initialize(this)

        if (BuildConfig.CLERK_PUBLISHABLE_KEY.isNotBlank()) {
            Clerk.initialize(
                this,
                publishableKey = BuildConfig.CLERK_PUBLISHABLE_KEY
            )
        }
    }
}
