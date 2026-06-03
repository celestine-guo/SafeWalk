package com.celestine.safewalk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapRenderingPolicyTest {
    @Test
    fun emulatorDoesNotRenderAmapByDefault() {
        assertFalse(
            MapRenderingPolicy.shouldRenderAmap(
                hasAmapApiKey = true,
                isRunningOnEmulator = true,
                isDebugBuild = true,
                forceAmapRenderingOnEmulator = false,
            )
        )
    }

    @Test
    fun debugOverrideAllowsAmapOnEmulator() {
        assertTrue(
            MapRenderingPolicy.shouldRenderAmap(
                hasAmapApiKey = true,
                isRunningOnEmulator = true,
                isDebugBuild = true,
                forceAmapRenderingOnEmulator = true,
            )
        )
    }

    @Test
    fun releaseBuildIgnoresEmulatorOverride() {
        assertFalse(
            MapRenderingPolicy.shouldRenderAmap(
                hasAmapApiKey = true,
                isRunningOnEmulator = true,
                isDebugBuild = false,
                forceAmapRenderingOnEmulator = true,
            )
        )
    }

    @Test
    fun missingAmapKeyDisablesRendering() {
        assertFalse(
            MapRenderingPolicy.shouldRenderAmap(
                hasAmapApiKey = false,
                isRunningOnEmulator = false,
                isDebugBuild = true,
                forceAmapRenderingOnEmulator = true,
            )
        )
    }
}
