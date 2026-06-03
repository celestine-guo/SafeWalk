package com.celestine.safewalk

object MapRenderingPolicy {
    fun shouldRenderAmap(
        hasAmapApiKey: Boolean,
        isRunningOnEmulator: Boolean,
        isDebugBuild: Boolean,
        forceAmapRenderingOnEmulator: Boolean,
    ): Boolean {
        if (!hasAmapApiKey) {
            return false
        }
        return !isRunningOnEmulator || (isDebugBuild && forceAmapRenderingOnEmulator)
    }
}
