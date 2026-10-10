import UIKit
import CoreMotion

// MARK: - 1. Device Profile (iOS has no DisplayMetrics/xdpi, so PPI comes from Apple hardware lookup)

public struct IOSDeviceProfile {
    public let identifier: String        // e.g. "iPhone15,2"
    public let modelMarketingName: String // e.g. "iPhone 14 Pro"
    public let ppi: Double               // physical pixels per inch
    public let widthInches: Double       // short side of the screen
    public let maxFPS: Int               // 60, or 120 on ProMotion
    public let hasGyro: Bool
    public let displayZoomed: Bool       // Settings > Display & Brightness > Display Zoom
    public let lowPowerMode: Bool
    public let thermalHot: Bool
}

public enum DeviceProbe {
    // PPI values published by Apple. Unknown/new models fall back via screen scale.
    public static let ppiTable: [String: Double] = [
        // 326 ppi (LCD / SE)
        "iPhone10,1": 326, "iPhone10,4": 326,   // iPhone 8
        "iPhone11,8": 326,                       // iPhone XR
        "iPhone12,1": 326,                       // iPhone 11
        "iPhone12,8": 326,                       // iPhone SE (2nd gen)
        "iPhone14,6": 326,                       // iPhone SE (3rd gen)
        
        // 401 ppi (Plus LCD)
        "iPhone10,2": 401, "iPhone10,5": 401,   // iPhone 8 Plus
        
        // 458 ppi (Super Retina OLED)
        "iPhone10,3": 458, "iPhone10,6": 458,   // iPhone X
        "iPhone11,2": 458,                       // iPhone XS
        "iPhone11,4": 458, "iPhone11,6": 458,   // iPhone XS Max
        "iPhone12,3": 458,                       // iPhone 11 Pro
        "iPhone12,5": 458,                       // iPhone 11 Pro Max
        "iPhone13,4": 458,                       // iPhone 12 Pro Max
        "iPhone14,3": 458,                       // iPhone 13 Pro Max
        "iPhone14,8": 458,                       // iPhone 14 Plus
        
        // 476 ppi (mini)
        "iPhone13,1": 476,                       // iPhone 12 mini
        "iPhone14,4": 476,                       // iPhone 13 mini
        
        // 460 ppi (Modern Super Retina XDR)
        "iPhone13,2": 460, "iPhone13,3": 460,   // iPhone 12, 12 Pro
        "iPhone14,5": 460, "iPhone14,2": 460,   // iPhone 13, 13 Pro
        "iPhone14,7": 460,                       // iPhone 14
        "iPhone15,2": 460,                       // iPhone 14 Pro
        "iPhone15,3": 460,                       // iPhone 14 Pro Max
        "iPhone15,4": 460,                       // iPhone 15
        "iPhone15,5": 460,                       // iPhone 15 Plus
        "iPhone16,1": 460,                       // iPhone 15 Pro
        "iPhone16,2": 460,                       // iPhone 15 Pro Max
        "iPhone17,1": 460, "iPhone17,2": 460,   // iPhone 16 Pro, 16 Pro Max
        "iPhone17,3": 460, "iPhone17,4": 460,   // iPhone 16, 16 Plus
        "iPhone17,5": 460,                       // iPhone 16e
        "iPhone18,1": 460, "iPhone18,2": 460,   // iPhone 17 Pro, 17 Pro Max
        "iPhone18,3": 460, "iPhone18,4": 460    // iPhone 17, 17 Plus / Air
    ]

    private static let marketingNames: [String: String] = [
        "iPhone10,1": "iPhone 8", "iPhone10,4": "iPhone 8",
        "iPhone10,2": "iPhone 8 Plus", "iPhone10,5": "iPhone 8 Plus",
        "iPhone10,3": "iPhone X", "iPhone10,6": "iPhone X",
        "iPhone11,2": "iPhone XS",
        "iPhone11,4": "iPhone XS Max", "iPhone11,6": "iPhone XS Max",
        "iPhone11,8": "iPhone XR",
        "iPhone12,1": "iPhone 11",
        "iPhone12,3": "iPhone 11 Pro",
        "iPhone12,5": "iPhone 11 Pro Max",
        "iPhone12,8": "iPhone SE (2nd gen)",
        "iPhone13,1": "iPhone 12 mini",
        "iPhone13,2": "iPhone 12",
        "iPhone13,3": "iPhone 12 Pro",
        "iPhone13,4": "iPhone 12 Pro Max",
        "iPhone14,4": "iPhone 13 mini",
        "iPhone14,5": "iPhone 13",
        "iPhone14,2": "iPhone 13 Pro",
        "iPhone14,3": "iPhone 13 Pro Max",
        "iPhone14,6": "iPhone SE (3rd gen)",
        "iPhone14,7": "iPhone 14",
        "iPhone14,8": "iPhone 14 Plus",
        "iPhone15,2": "iPhone 14 Pro",
        "iPhone15,3": "iPhone 14 Pro Max",
        "iPhone15,4": "iPhone 15",
        "iPhone15,5": "iPhone 15 Plus",
        "iPhone16,1": "iPhone 15 Pro",
        "iPhone16,2": "iPhone 15 Pro Max",
        "iPhone17,1": "iPhone 16 Pro",
        "iPhone17,2": "iPhone 16 Pro Max",
        "iPhone17,3": "iPhone 16",
        "iPhone17,4": "iPhone 16 Plus",
        "iPhone18,1": "iPhone 17 Pro",
        "iPhone18,2": "iPhone 17 Pro Max",
        "iPhone18,3": "iPhone 17",
        "iPhone18,4": "iPhone 17 Plus"
    ]

    public static func machineIdentifier() -> String {
        var info = utsname()
        uname(&info)
        return withUnsafePointer(to: &info.machine) {
            $0.withMemoryRebound(to: CChar.self, capacity: 1) { String(cString: $0) }
        }
    }

    public static func current() -> IOSDeviceProfile {
        let id = machineIdentifier()
        let screen = UIScreen.main
        let ppi = ppiTable[id] ?? (screen.nativeScale >= 3.0 ? 460.0 : 326.0)
        let nativeShort = Double(min(screen.nativeBounds.width, screen.nativeBounds.height))
        let marketingName = marketingNames[id] ?? "Apple iPhone"

        return IOSDeviceProfile(
            identifier: id,
            modelMarketingName: marketingName,
            ppi: ppi,
            widthInches: nativeShort / ppi,
            maxFPS: screen.maximumFramesPerSecond,
            hasGyro: CMMotionManager().isGyroAvailable,
            displayZoomed: screen.scale != screen.nativeScale,
            lowPowerMode: ProcessInfo.processInfo.isLowPowerModeEnabled,
            thermalHot: ProcessInfo.processInfo.thermalState.rawValue >= ProcessInfo.ThermalState.serious.rawValue
        )
    }
}

// MARK: - 2. Real Touch-Rate Measurement (Coalesced hardware polling rate in Hz)

public final class TouchRateProbeView: UIView {
    private var stamps: [TimeInterval] = []
    public var onResult: ((Double) -> Void)?

    public override init(frame: CGRect) {
        super.init(frame: frame)
        isMultipleTouchEnabled = true
        isUserInteractionEnabled = true
    }

    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        isMultipleTouchEnabled = true
        isUserInteractionEnabled = true
    }

    public override func touchesBegan(_ t: Set<UITouch>, with e: UIEvent?) {
        stamps.removeAll()
        if let firstTouch = t.first {
            stamps.append(firstTouch.timestamp)
        }
    }

    public override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let t = touches.first else { return }
        // coalescedTouches exposes every single sub-frame hardware sample, not just one per frame
        (event?.coalescedTouches(for: t) ?? [t]).forEach { stamps.append($0.timestamp) }
    }

    public override func touchesEnded(_ t: Set<UITouch>, with e: UIEvent?) {
        guard stamps.count > 10, let first = stamps.first, let last = stamps.last, last > first else { return }
        let measuredHz = Double(stamps.count - 1) / (last - first)
        onResult?(measuredHz)
    }
}

// MARK: - 3. Stage 1: Device Baseline

public struct SensiSet {
    public var general: Double
    public var redDot: Double
    public var scope2x: Double
    public var scope4x: Double
    public var sniper: Double
    public var freeLook: Double

    public init(general: Double, redDot: Double, scope2x: Double, scope4x: Double, sniper: Double, freeLook: Double) {
        self.general = general
        self.redDot = redDot
        self.scope2x = scope2x
        self.scope4x = scope4x
        self.sniper = sniper
        self.freeLook = freeLook
    }
}

public enum IOSBaseline {
    public static let referencePPI = 400.0
    public static let referenceWidthIn = 2.54            // iPhone 12 short side reference

    // Heuristic baseline constants aligned with the Free Fire 0-200 scale
    public static let base = SensiSet(
        general: 190,
        redDot: 182,
        scope2x: 172,
        scope4x: 158,
        sniper: 95,
        freeLook: 145
    )

    public static func compute(_ p: IOSDeviceProfile, measuredTouchHz: Double?) -> SensiSet {
        let ppiRatio = referencePPI / p.ppi
        // Physical screen width term: same PPI, wider screen = longer thumb travel
        let sizeFactor = min(max(sqrt(referenceWidthIn / p.widthInches), 0.97), 1.03)
        let k = ppiRatio * sizeFactor

        var s = SensiSet(
            general: base.general * k,
            redDot: base.redDot * k,
            scope2x: base.scope2x * k,
            scope4x: base.scope4x * k,
            sniper: base.sniper * k,
            freeLook: base.freeLook * k
        )

        let hz = measuredTouchHz ?? (p.maxFPS >= 120 && !p.lowPowerMode ? 120.0 : 60.0)
        if hz >= 110 {
            s.general -= 3
            s.redDot -= 3
        } else if hz <= 65 {
            s.general += 3
        }

        if p.thermalHot || p.lowPowerMode {
            s.general += 2
        }

        return s
    }
}

// MARK: - 4. Stage 2: Personal Multiplier

public struct PlayerSignals {
    public var overshoot: Double         // -1 (stuck on chest) ... +1 (flying over head)
    public var tempo: Double             // -1 sniper ... +1 rusher
    public var consistency: Double       //  0 erratic ... 1 steady
    public var weaponUrgency: Double     // -1 sniper ... +1 shotgun
    public var currentVsBaseline: Double // -1 ... +1 (existing in-game sens relative to baseline)

    public init(
        overshoot: Double = 0.0,
        tempo: Double = 0.8,
        consistency: Double = 0.8,
        weaponUrgency: Double = 0.8,
        currentVsBaseline: Double = 0.0
    ) {
        self.overshoot = overshoot
        self.tempo = tempo
        self.consistency = consistency
        self.weaponUrgency = weaponUrgency
        self.currentVsBaseline = currentVsBaseline
    }
}

public enum PersonalMultiplier {
    public static func compute(_ s: PlayerSignals, learnedBias: Double = 0.0) -> Double {
        let m = 1.0
            - 0.09 * s.overshoot
            + 0.05 * s.tempo
            - 0.04 * (1.0 - s.consistency)
            + 0.03 * s.weaponUrgency
            + 0.03 * s.currentVsBaseline
            + learnedBias
        return min(max(m, 0.80), 1.20)
    }
}

// MARK: - 5. Final Output & iOS-Specific Advice

public struct IOSRecommendation {
    public let sensi: SensiSet
    public let fireButtonPercent: ClosedRange<Int>
    public let tips: [String]
    public let multiplier: Double
}

public enum IOSSensitivityEngine {
    public static func recommend(
        profile p: IOSDeviceProfile,
        touchHz: Double?,
        signals: PlayerSignals,
        fingers: Int,
        learnedBias: Double
    ) -> IOSRecommendation {
        let b = IOSBaseline.compute(p, measuredTouchHz: touchHz)
        let m = PersonalMultiplier.compute(signals, learnedBias: learnedBias)

        func clampVal(_ v: Double, _ lo: Double, _ hi: Double) -> Double {
            min(max((v * m).rounded(), lo), hi)
        }

        let sensi = SensiSet(
            general: clampVal(b.general, 100, 200),
            redDot: clampVal(b.redDot, 100, 200),
            scope2x: clampVal(b.scope2x, 80, 200),
            scope4x: clampVal(b.scope4x, 70, 200),
            sniper: clampVal(b.sniper, 50, 180),
            freeLook: clampVal(b.freeLook, 50, 200)
        )

        var tips: [String] = []
        tips.append("iOS has no system DPI/Smallest Width setting. All fine-tuning applies directly in Free Fire's sensitivity menu.")
        if p.displayZoomed {
            tips.append("Display Zoom is ON. Switch to Standard in iOS Settings > Display & Brightness for consistent 1:1 pixel scaling.")
        }
        if p.lowPowerMode {
            tips.append("Low Power Mode is active. Turn it off to unlock the full 120Hz ProMotion touch sampling rate.")
        }
        if p.thermalHot {
            tips.append("Device is hot. iOS thermal throttling drops GPU frames and changes touch feel. Allow the phone to cool.")
        }
        if fingers >= 3 {
            tips.append("3/4 Finger Claw: Enable Guided Access (Triple-click Side Button) to prevent accidental Notification / Control Center gestures.")
        }
        if !p.hasGyro {
            tips.append("No gyroscope detected. Skip Gyroscope / Free Look tilt calibration.")
        }

        let fireRange: ClosedRange<Int> = fingers <= 2 ? 48...52 : 44...48
        return IOSRecommendation(sensi: sensi, fireButtonPercent: fireRange, tips: tips, multiplier: m)
    }
}

// MARK: - 6. Continuous Feedback Tuner

public enum FeedbackTuner {
    private static let key = "sensibot_ios_learned_bias"

    public static var bias: Double {
        get { UserDefaults.standard.double(forKey: key) }
        set { UserDefaults.standard.set(min(max(newValue, -0.10), 0.10), forKey: key) }
    }

    /// Call after a practice session:
    /// result: +1 = "Flew over head / too fast", -1 = "Too slow / stuck on body", 0 = "Clean Headshots!"
    public static func record(result: Int) {
        bias -= Double(result) * 0.015
    }

    public static func reset() {
        UserDefaults.standard.removeObject(forKey: key)
    }
}
