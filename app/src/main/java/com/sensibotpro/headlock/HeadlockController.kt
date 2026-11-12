package com.sensibotpro.headlock

enum class HeadlockState {
    DISABLED,
    NEEDS_PERMISSION,
    ENABLED
}

data class HeadlockUiSnapshot(
    val state: HeadlockState = HeadlockState.DISABLED,
    val config: HeadlockConfig = HeadlockConfig(),
    val inFlightGesture: Boolean = false,
    val statusMessage: String = "Headlock idle"
)

/**
 * Pure state machine for Headlock; depends solely on HeadlockBridge.
 */
class HeadlockController(private val bridge: HeadlockBridge) {

    var snapshot: HeadlockUiSnapshot = HeadlockUiSnapshot(
        state = if (bridge.isEnabled()) HeadlockState.ENABLED else HeadlockState.DISABLED,
        config = bridge.getConfig()
    )
        private set

    fun enable(): HeadlockEnableResult {
        if (snapshot.state == HeadlockState.ENABLED && bridge.isEnabled()) {
            return HeadlockEnableResult(ok = true, needsAccessibilityPermission = false)
        }
        val result = bridge.enable()
        snapshot = when {
            result.ok -> snapshot.copy(
                state = HeadlockState.ENABLED,
                statusMessage = "Headlock active"
            )
            result.needsAccessibilityPermission -> snapshot.copy(
                state = HeadlockState.NEEDS_PERMISSION,
                inFlightGesture = false,
                statusMessage = "Accessibility permission required"
            )
            else -> snapshot.copy(
                state = HeadlockState.DISABLED,
                inFlightGesture = false,
                statusMessage = "Unable to enable Headlock"
            )
        }
        return result
    }

    fun disable() {
        bridge.disable()
        snapshot = snapshot.copy(
            state = HeadlockState.DISABLED,
            inFlightGesture = false,
            statusMessage = "Headlock disabled"
        )
    }

    fun syncEnabledState(): Boolean {
        val active = bridge.isEnabled()
        snapshot = snapshot.copy(
            state = if (active) HeadlockState.ENABLED else HeadlockState.DISABLED,
            inFlightGesture = if (active) snapshot.inFlightGesture else false
        )
        return active
    }

    fun updateConfig(cfg: HeadlockConfig): Boolean {
        val clamped = cfg.clamped()
        val ok = bridge.setConfig(clamped)
        if (ok) {
            snapshot = snapshot.copy(
                config = clamped,
                statusMessage = "Config saved"
            )
        }
        return ok
    }

    fun refreshConfig(): HeadlockConfig {
        val loaded = bridge.getConfig().clamped()
        snapshot = snapshot.copy(config = loaded)
        return loaded
    }

    fun applyRecommendation(metrics: HeadlockDeviceMetrics): HeadlockConfig {
        val rec = bridge.recommend(metrics).clamped()
        bridge.setConfig(rec)
        snapshot = snapshot.copy(
            config = rec,
            statusMessage = "Recommended profile applied"
        )
        return rec
    }

    fun markGestureInFlight(inFlight: Boolean): Boolean {
        if (inFlight && (snapshot.state != HeadlockState.ENABLED || snapshot.inFlightGesture)) {
            return false
        }
        snapshot = snapshot.copy(inFlightGesture = inFlight)
        return true
    }

    fun openSystemSettings(): Boolean = bridge.openSystemSettings()

    fun installTouchProfile(): HeadlockProfileInstallResult = bridge.installTouchProfile()
}

