import UIKit

/// iOS HeadlockBridge implementation.
/// Persists under "headlock." UserDefaults prefix so existing sensi storage is untouched.
public final class HeadlockChannel: HeadlockBridge {
    public static let shared = HeadlockChannel()
    private let defaults = UserDefaults.standard
    private let enabledKey = "\(HeadlockConfig.storageKeyPrefix)enabled"

    public init() {}

    public func enable() -> HeadlockEnableResult {
        defaults.set(true, forKey: enabledKey)
        return HeadlockEnableResult(ok: true, needsAccessibilityPermission: false)
    }

    public func disable() {
        defaults.set(false, forKey: enabledKey)
    }

    public func isEnabled() -> Bool {
        defaults.bool(forKey: enabledKey)
    }

    public func setConfig(_ cfg: HeadlockConfig) -> Bool {
        let clamped = cfg.clamped()
        defaults.set(clamped.toJson(), forKey: HeadlockConfig.storageConfigKey)
        return true
    }

    public func getConfig() -> HeadlockConfig {
        let raw = defaults.string(forKey: HeadlockConfig.storageConfigKey)
        return HeadlockConfig.fromJson(raw).clamped()
    }

    public func recommend(_ deviceMetrics: HeadlockDeviceMetrics) -> HeadlockConfig {
        HeadlockCalibration.recommend(deviceMetrics)
    }

    public func installTouchProfile() -> HeadlockProfileInstallResult {
        guard let url = MobileConfigBuilder.buildUnsignedProfile() else {
            return HeadlockProfileInstallResult(ok: false, path: nil, unsupported: false)
        }
        return HeadlockProfileInstallResult(ok: true, path: url.path, unsupported: false)
    }

    public func openSystemSettings() -> Bool {
        let targets = [
            "App-prefs:root=ACCESSIBILITY&path=TOUCH",
            "App-prefs:root=ACCESSIBILITY",
            UIApplication.openSettingsURLString
        ]
        for raw in targets {
            if let url = URL(string: raw), UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url, options: [:], completionHandler: nil)
                return true
            }
        }
        if let fallback = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(fallback, options: [:], completionHandler: nil)
            return true
        }
        return false
    }
}

