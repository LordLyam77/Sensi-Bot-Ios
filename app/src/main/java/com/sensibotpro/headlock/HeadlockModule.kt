package com.sensibotpro.headlock

/**
 * Isolated public surface for the Headlock feature.
 * Controlled by features.headlock.enabled (default false until final enable commit).
 */
object HeadlockModule {
    const val FEATURE_FLAG_KEY = "features.headlock.enabled"

    @Volatile
    var isFeatureEnabled: Boolean = true
}

