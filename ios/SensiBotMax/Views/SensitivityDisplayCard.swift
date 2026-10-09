import SwiftUI

public struct SensitivityDisplayCard: View {
    public let recommendation: IOSRecommendation
    public let deviceProfile: IOSDeviceProfile

    public init(recommendation: IOSRecommendation, deviceProfile: IOSDeviceProfile) {
        self.recommendation = recommendation
        self.deviceProfile = deviceProfile
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Header
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("RECOMMENDED FF PROFILE")
                        .font(.system(size: 15, weight: .black, design: .rounded))
                        .foregroundColor(SensiTheme.rubyRed)
                    Text("Calibrated for \(deviceProfile.modelMarketingName) (\(Int(deviceProfile.ppi)) PPI • \(deviceProfile.maxFPS)Hz)")
                        .font(.system(size: 11))
                        .foregroundColor(SensiTheme.textSecondary)
                }
                Spacer()
                Text("CALIBRATED")
                    .font(.system(size: 10, weight: .black, design: .monospaced))
                    .foregroundColor(SensiTheme.rubyRed)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(SensiTheme.rubyRed.opacity(0.15))
                    .overlay(
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(SensiTheme.rubyRed.opacity(0.5), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 6))
            }

            Divider().background(SensiTheme.glassBorder)

            // Sensitivity Gauges
            VStack(spacing: 12) {
                MetricRow(label: "General Sensitivity", value: recommendation.sensi.general, maxValue: 200, tint: SensiTheme.rubyRed)
                MetricRow(label: "Red Dot Sensitivity", value: recommendation.sensi.redDot, maxValue: 200, tint: SensiTheme.rubyRed)
                MetricRow(label: "2X Scope", value: recommendation.sensi.scope2x, maxValue: 200, tint: SensiTheme.cyanAccent)
                MetricRow(label: "4X Scope", value: recommendation.sensi.scope4x, maxValue: 200, tint: SensiTheme.cyanAccent)
                MetricRow(label: "Sniper Scope (6X-8X)", value: recommendation.sensi.sniper, maxValue: 180, tint: SensiTheme.goldAccent)
                MetricRow(label: "Free Look / Gyro", value: recommendation.sensi.freeLook, maxValue: 200, tint: SensiTheme.goldAccent)
            }

            Divider().background(SensiTheme.glassBorder)

            // HUD Stats Row
            HStack(spacing: 8) {
                HudBadge(
                    title: "FIRE BUTTON",
                    value: "\(recommendation.fireButtonPercent.lowerBound)% - \(recommendation.fireButtonPercent.upperBound)%",
                    tint: SensiTheme.greenFairPlay
                )
                HudBadge(
                    title: "MULTIPLIER",
                    value: String(format: "%.2fx", recommendation.multiplier),
                    tint: SensiTheme.goldAccent
                )
                HudBadge(
                    title: "POLLING",
                    value: "\(deviceProfile.maxFPS)Hz",
                    tint: SensiTheme.cyanAccent
                )
            }

            // iOS Specific Advice / Warnings
            if !recommendation.tips.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    Text("TACTICAL SYSTEM ADVICE")
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundColor(SensiTheme.textMuted)

                    ForEach(recommendation.tips, id: \.self) { tip in
                        HStack(alignment: .top, spacing: 8) {
                            Image(systemName: "shield.lefthalf.filled")
                                .font(.system(size: 11))
                                .foregroundColor(SensiTheme.rubyRed)
                                .padding(.top, 2)
                            Text(tip)
                                .font(.system(size: 11))
                                .foregroundColor(SensiTheme.textSecondary)
                        }
                    }
                }
                .padding(12)
                .background(Color.black.opacity(0.35))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
        }
        .gamingCard(borderColor: SensiTheme.rubyRed.opacity(0.45))
    }
}

struct MetricRow: View {
    let label: String
    let value: Double
    let maxValue: Double
    let tint: Color

    var body: some View {
        VStack(spacing: 4) {
            HStack {
                Text(label)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(SensiTheme.textSecondary)
                Spacer()
                Text("\(Int(value.rounded()))")
                    .font(.system(size: 13, weight: .black, design: .monospaced))
                    .foregroundColor(.white)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(Color.white.opacity(0.08))
                        .frame(height: 6)
                    RoundedRectangle(cornerRadius: 3)
                        .fill(tint)
                        .frame(width: geo.size.width * CGFloat(min(value / maxValue, 1.0)), height: 6)
                }
            }
            .frame(height: 6)
        }
    }
}

struct HudBadge: View {
    let title: String
    let value: String
    let tint: Color

    var body: some View {
        VStack(spacing: 2) {
            Text(title)
                .font(.system(size: 9, weight: .bold))
                .foregroundColor(SensiTheme.textMuted)
            Text(value)
                .font(.system(size: 12, weight: .black, design: .monospaced))
                .foregroundColor(tint)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(Color.black.opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}
