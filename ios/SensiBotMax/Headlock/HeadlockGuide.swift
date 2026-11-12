import Foundation

public struct HeadlockGuideStep: Identifiable {
    public let id: Int
    public let title: String
    public let detail: String
}

/// Manual Touch Accommodations setup checklist + exact recommended values.
/// Primary iOS delivery (no injection).
public enum HeadlockGuide {

    public static func checklist(for config: HeadlockConfig) -> [HeadlockGuideStep] {
        [
            HeadlockGuideStep(
                id: 1,
                title: "Open Touch Accommodations",
                detail: "Go to Settings → Accessibility → Touch → Touch Accommodations."
            ),
            HeadlockGuideStep(
                id: 2,
                title: "Enable Hold Duration",
                detail: "Turn ON Touch Accommodations, enable Hold Duration, and set it to 0.15s."
            ),
            HeadlockGuideStep(
                id: 3,
                title: "Set Ignore Repeat",
                detail: "Enable Ignore Repeat and set it to 0.30s to filter accidental micro-bounces."
            ),
            HeadlockGuideStep(
                id: 4,
                title: "Apply Calibrated Anchor & Bias",
                detail: "Anchor: (\(Int(config.startX * 100))%, \(Int(config.startY * 100))%) • Drag: \(config.distancePx)px • Bias: \(String(format: "%.1fx", config.verticalBias)) • Mode: \(config.mode.rawValue.uppercased())."
            ),
            HeadlockGuideStep(
                id: 5,
                title: "Optional: Export Profile",
                detail: "You can optionally export the unsigned .mobileconfig via the Share Sheet and install under Settings → General → VPN & Device Management."
            )
        ]
    }
}

