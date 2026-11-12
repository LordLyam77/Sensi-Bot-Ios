import Foundation

public struct HeadlockEnableResult {
    public let ok: Bool
    public let needsAccessibilityPermission: Bool

    public init(ok: Bool, needsAccessibilityPermission: Bool = false) {
        self.ok = ok
        self.needsAccessibilityPermission = needsAccessibilityPermission
    }
}

public struct HeadlockProfileInstallResult {
    public let ok: Bool
    public let path: String?
    public let unsupported: Bool

    public init(ok: Bool, path: String? = nil, unsupported: Bool = false) {
        self.ok = ok
        self.path = path
        self.unsupported = unsupported
    }
}

/// Platform-agnostic Headlock contract.
/// HeadlockController depends only on this protocol.
public protocol HeadlockBridge {
    func enable() -> HeadlockEnableResult
    func disable()
    func isEnabled() -> Bool
    func setConfig(_ cfg: HeadlockConfig) -> Bool
    func getConfig() -> HeadlockConfig
    func recommend(_ deviceMetrics: HeadlockDeviceMetrics) -> HeadlockConfig
    func installTouchProfile() -> HeadlockProfileInstallResult
    func openSystemSettings() -> Bool
}

