import Foundation
import Combine

public enum HeadlockState: String {
    case disabled
    case needsPermission
    case enabled
}

public struct HeadlockUiSnapshot {
    public var state: HeadlockState
    public var config: HeadlockConfig
    public var inFlightGesture: Bool
    public var statusMessage: String

    public init(
        state: HeadlockState = .disabled,
        config: HeadlockConfig = HeadlockConfig(),
        inFlightGesture: Bool = false,
        statusMessage: String = "Headlock idle"
    ) {
        self.state = state
        self.config = config
        self.inFlightGesture = inFlightGesture
        self.statusMessage = statusMessage
    }
}

/// Pure state machine for Headlock; depends solely on HeadlockBridge (zero UIKit/platform imports).
public final class HeadlockController: ObservableObject {
    @Published public private(set) var snapshot: HeadlockUiSnapshot
    private let bridge: HeadlockBridge

    public init(bridge: HeadlockBridge) {
        self.bridge = bridge
        self.snapshot = HeadlockUiSnapshot(
            state: bridge.isEnabled() ? .enabled : .disabled,
            config: bridge.getConfig().clamped()
        )
    }

    @discardableResult
    public func enable() -> HeadlockEnableResult {
        if snapshot.state == .enabled && bridge.isEnabled() {
            return HeadlockEnableResult(ok: true, needsAccessibilityPermission: false)
        }
        let result = bridge.enable()
        if result.ok {
            snapshot.state = .enabled
            snapshot.statusMessage = "Headlock active"
        } else if result.needsAccessibilityPermission {
            snapshot.state = .needsPermission
            snapshot.inFlightGesture = false
            snapshot.statusMessage = "Touch Accommodations permission required"
        } else {
            snapshot.state = .disabled
            snapshot.inFlightGesture = false
            snapshot.statusMessage = "Unable to enable Headlock"
        }
        return result
    }

    public func disable() {
        bridge.disable()
        snapshot.state = .disabled
        snapshot.inFlightGesture = false
        snapshot.statusMessage = "Headlock disabled"
    }

    @discardableResult
    public func updateConfig(_ cfg: HeadlockConfig) -> Bool {
        let clamped = cfg.clamped()
        let ok = bridge.setConfig(clamped)
        if ok {
            snapshot.config = clamped
            snapshot.statusMessage = "Config saved"
        }
        return ok
    }

    @discardableResult
    public func applyRecommendation(_ metrics: HeadlockDeviceMetrics) -> HeadlockConfig {
        let rec = bridge.recommend(metrics).clamped()
        _ = bridge.setConfig(rec)
        snapshot.config = rec
        snapshot.statusMessage = "Recommended profile applied"
        return rec
    }

    @discardableResult
    public func openSystemSettings() -> Bool {
        bridge.openSystemSettings()
    }

    public func installTouchProfile() -> HeadlockProfileInstallResult {
        bridge.installTouchProfile()
    }
}

