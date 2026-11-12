package com.sensibotpro.headlock

data class HeadlockEnableResult(
    val ok: Boolean,
    val needsAccessibilityPermission: Boolean = false
)

data class HeadlockProfileInstallResult(
    val ok: Boolean,
    val path: String? = null,
    val unsupported: Boolean = false
)

/**
 * Platform-agnostic Headlock contract.
 * HeadlockController depends only on this interface with zero platform imports.
 */
interface HeadlockBridge {
    fun enable(): HeadlockEnableResult
    fun disable()
    fun isEnabled(): Boolean
    fun setConfig(cfg: HeadlockConfig): Boolean
    fun getConfig(): HeadlockConfig
    fun recommend(deviceMetrics: HeadlockDeviceMetrics): HeadlockConfig
    fun installTouchProfile(): HeadlockProfileInstallResult
    fun openSystemSettings(): Boolean
}

