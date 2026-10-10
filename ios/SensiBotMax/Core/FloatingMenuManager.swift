import SwiftUI
import Combine

public enum TargetZoneType: String, CaseIterable {
    case head = "HEAD"
    case body = "BODY"
    case legs = "LEGS"

    public var title: String {
        switch self {
        case .head: return "HEAD SHOTS"
        case .body: return "BODY SHOTS"
        case .legs: return "LEG SHOTS"
        }
    }

    public var badge: String {
        switch self {
        case .head: return "100% LOCK"
        case .body: return "MAX DPS"
        case .legs: return "SWEEP HIT"
        }
    }

    public var color: Color {
        switch self {
        case .head: return SensiTheme.rubyRed
        case .body: return SensiTheme.cyanAccent
        case .legs: return SensiTheme.goldAccent
        }
    }
}

public enum CrosshairStyleType: String, CaseIterable {
    case classicCross = "Classic Cross"
    case centerDot = "Center Dot"
    case circleDot = "Circle Dot"
    case tStyle = "T-Style"
}

public final class FloatingMenuManager: ObservableObject {
    public static let shared = FloatingMenuManager()

    // Visibility & Expand State
    @Published public var isMenuEnabled: Bool {
        didSet { UserDefaults.standard.set(isMenuEnabled, forKey: "floating_menu_enabled") }
    }
    @Published public var isExpanded: Bool = false

    // Launcher Position (Persisted)
    @Published public var launcherX: CGFloat
    @Published public var launcherY: CGFloat

    // Target Zone
    @Published public var activeZone: TargetZoneType = .head {
        didSet {
            UserDefaults.standard.set(activeZone.rawValue, forKey: "floating_active_zone")
            updateMetricsForZone(activeZone)
        }
    }

    // Active Navigation Tab inside Mod Menu
    @Published public var selectedTab: Int = 0 // 0: Aim Zones, 1: Tuner, 2: Crosshair, 3: Telemetry

    // Sensi Metrics
    @Published public var generalSensi: Double = 196
    @Published public var redDotSensi: Double = 188
    @Published public var scope2xSensi: Double = 160
    @Published public var scope4xSensi: Double = 145
    @Published public var fireButtonSize: Double = 42
    @Published public var isJDrag: Bool = true

    // Crosshair State
    @Published public var crosshairStyle: CrosshairStyleType = .classicCross
    @Published public var crosshairColorHex: String = "#FF2A4D"
    @Published public var crosshairSize: Double = 24
    @Published public var crosshairThickness: Double = 2.5
    @Published public var crosshairOpacity: Double = 0.9

    // Animation & Feedback
    @Published public var isCalculating: Bool = false
    @Published public var calculationProgress: Double = 0.0
    @Published public var isApplied: Bool = false

    private init() {
        self.isMenuEnabled = UserDefaults.standard.object(forKey: "floating_menu_enabled") as? Bool ?? true

        // Default position: top right edge
        let savedX = UserDefaults.standard.object(forKey: "floating_pos_x") as? CGFloat
        let savedY = UserDefaults.standard.object(forKey: "floating_pos_y") as? CGFloat
        self.launcherX = savedX ?? 320
        self.launcherY = savedY ?? 220

        if let savedZone = UserDefaults.standard.string(forKey: "floating_active_zone"),
           let zone = TargetZoneType(rawValue: savedZone) {
            self.activeZone = zone
        }

        updateMetricsForZone(activeZone)
    }

    public func updateMetricsForZone(_ zone: TargetZoneType) {
        switch zone {
        case .head:
            generalSensi = 196
            redDotSensi = 188
            scope2xSensi = 175
            scope4xSensi = 162
            fireButtonSize = 42
            isJDrag = true
        case .body:
            generalSensi = 182
            redDotSensi = 175
            scope2xSensi = 168
            scope4xSensi = 154
            fireButtonSize = 48
            isJDrag = false
        case .legs:
            generalSensi = 174
            redDotSensi = 168
            scope2xSensi = 160
            scope4xSensi = 148
            fireButtonSize = 52
            isJDrag = false
        }
    }

    public func savePosition(x: CGFloat, y: CGFloat) {
        self.launcherX = x
        self.launcherY = y
        UserDefaults.standard.set(x, forKey: "floating_pos_x")
        UserDefaults.standard.set(y, forKey: "floating_pos_y")
    }

    public func snapToNearestEdge(screenWidth: CGFloat, safeTop: CGFloat, safeBottom: CGFloat) {
        let snapMargin: CGFloat = 12
        let targetX: CGFloat = (launcherX < screenWidth / 2) ? snapMargin : (screenWidth - 56 - snapMargin)
        let minY: CGFloat = safeTop + 20
        let maxY: CGFloat = safeBottom - 70
        let clampedY = min(max(launcherY, minY), maxY)

        withAnimation(.spring(response: 0.38, dampingFraction: 0.72)) {
            self.launcherX = targetX
            self.launcherY = clampedY
        }
        savePosition(x: targetX, y: clampedY)
    }

    public func triggerApplyAnimation() {
        guard !isCalculating else { return }
        isCalculating = true
        calculationProgress = 0.0
        isApplied = false

        // Haptic feedback
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.prepare()
        generator.impactOccurred()

        // 1.4s calculation simulation
        let steps = 14
        let stepDelay = 0.1
        for i in 1...steps {
            DispatchQueue.main.asyncAfter(deadline: .now() + (Double(i) * stepDelay)) {
                self.calculationProgress = Double(i) / Double(steps)
                if i == steps {
                    self.isCalculating = false
                    self.isApplied = true

                    let notif = UINotificationFeedbackGenerator()
                    notif.notificationOccurred(.success)

                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                        self.isApplied = false
                    }
                }
            }
        }
    }
}
