import Foundation

public enum CalibrationStep: Int, CaseIterable {
    case deviceClass = 0
    case fireAnchor = 1
    case verticalBiasSweep = 2
    case commit = 3

    public var title: String {
        switch self {
        case .deviceClass: return "Device Class"
        case .fireAnchor: return "Fire-Button Anchor"
        case .verticalBiasSweep: return "Vertical Bias Sweep"
        case .commit: return "Review & Commit"
        }
    }
}

public struct BiasSweepState {
    public var currentBias: Double = 0.6
    public var repsCompleted: Int = 0
    public var hitsRecorded: Int = 0
    public var bestBias: Double = 1.0
    public var bestHitScore: Int = -1

    public let totalSteps: Int = 11
    public let targetRepsPerStep: Int = 20
    public let targetSweetSpotTenScale: Int = 6 // 6/10 == 12/20

    public init() {}
}

public enum HeadlockCalibration {

    public static func recommend(_ metrics: HeadlockDeviceMetrics) -> HeadlockConfig {
        let height = min(max(metrics.screenHeightPx, 1000), 4000)
        let hz = min(max(metrics.refreshRateHz, 60), 240)

        let baseDistance: Int
        switch metrics.deviceClass {
        case .compact: baseDistance = 34
        case .phone: baseDistance = 42
        case .large: baseDistance = 50
        case .tablet: baseDistance = 64
        }

        let heightFactor = min(max(Double(height) / 2400.0, 0.8), 1.35)
        let distancePx = min(max(Int((Double(baseDistance) * heightFactor).rounded()), 10), 400)

        let durationMs = hz >= 120 ? 78 : (hz >= 90 ? 86 : 96)
        let repeatIntervalMs = hz >= 120 ? 48 : (hz >= 90 ? 54 : 62)

        let verticalBias: Double
        switch metrics.deviceClass {
        case .compact: verticalBias = 0.9
        case .phone: verticalBias = 1.0
        case .large: verticalBias = 1.1
        case .tablet: verticalBias = 1.2
        }

        let startX = metrics.deviceClass == .tablet ? 0.85 : 0.82
        let startY = metrics.deviceClass == .tablet ? 0.80 : 0.78

        return HeadlockConfig(
            v: HeadlockConfig.currentVersion,
            mode: .hold,
            startX: startX,
            startY: startY,
            distancePx: distancePx,
            durationMs: durationMs,
            repeatIntervalMs: repeatIntervalMs,
            burstCount: 3,
            curve: hz >= 120 ? .easeOut : .linear,
            verticalBias: verticalBias,
            deviceClass: metrics.deviceClass
        ).clamped()
    }

    public static func recordSweepRep(_ state: BiasSweepState, hit: Bool) -> BiasSweepState {
        guard state.repsCompleted < state.targetRepsPerStep else { return state }
        var next = state
        next.repsCompleted += 1
        if hit { next.hitsRecorded += 1 }
        if next.hitsRecorded > next.bestHitScore {
            next.bestHitScore = next.hitsRecorded
            next.bestBias = next.currentBias
        }
        return next
    }

    public static func advanceSweepBias(_ state: BiasSweepState) -> BiasSweepState {
        var next = state
        next.currentBias = min(((next.currentBias + 0.1) * 10.0).rounded() / 10.0, HeadlockConfig.maxVerticalBias)
        next.repsCompleted = 0
        next.hitsRecorded = 0
        return next
    }
}

