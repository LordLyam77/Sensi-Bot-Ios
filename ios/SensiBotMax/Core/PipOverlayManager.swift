import UIKit
import Combine

public final class PipOverlayManager: NSObject, ObservableObject {
    public static let shared = PipOverlayManager()

    @Published public var isPipActive: Bool = false
    @Published public var isPipSupported: Bool = false
    @Published public var isPipPossible: Bool = false
    @Published public var statusMessage: String = "Assistant Active"

    public override init() {
        super.init()
    }

    public func setupAudioSession() {}
    public func attachSourceView(_ sourceView: UIView) {}
    public func updateHudMetrics(general: Int, redDot: Int, scope2x: Int, scope4x: Int, hz: Int) {}
    public func updateHudZone(name: String, color: UIColor = .clear) {}
    public func startPip() {}
    public func stopPip() {}
    public func togglePip() {}
}
