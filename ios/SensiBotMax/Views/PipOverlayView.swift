import SwiftUI

public typealias PipOverlayView = FloatingAssistantView

public struct FloatingAssistantView: View {
    @ObservedObject var menuManager = FloatingMenuManager.shared

    public init() {}

    public var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 16) {
                // 1. Header Bar
                headerSection

                // 2. Aim Zones Section (Red Criminal Character & Head/Body/Legs Calibration)
                aimZonesSection
                    .gamingCard(borderColor: SensiTheme.rubyRed.opacity(0.45))

                // 3. Fair Play & Pro Gaming Recommendations (Zero Guided Access)
                recommendationsCard

                Spacer(minLength: 36)
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
        }
        .background(SensiTheme.voidBlack.ignoresSafeArea())
    }

    // MARK: - 1. Header Section
    private var headerSection: some View {
        VStack(spacing: 6) {
            HStack {
                Image(systemName: "slider.horizontal.below.rectangle")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(SensiTheme.rubyRed)
                Text("FLOATING ASSISTANT")
                    .font(.system(size: 22, weight: .black, design: .monospaced))
                    .foregroundColor(.white)
                Spacer()
                HStack(spacing: 5) {
                    Circle()
                        .fill(SensiTheme.fairPlayGreen)
                        .frame(width: 7, height: 7)
                    Text("ACTIVE")
                        .font(.system(size: 10, weight: .black, design: .monospaced))
                        .foregroundColor(SensiTheme.fairPlayGreen)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(SensiTheme.fairPlayGreen.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 6))
            }

            HStack {
                Text("Targeting Hitbox Calibration • Head, Body & Leg Zones")
                    .font(.system(size: 11))
                    .foregroundColor(SensiTheme.textSecondary)
                Spacer()
            }
        }
    }

    // MARK: - 2. AIM ZONES (Red Criminal Targeting)
    private var aimZonesSection: some View {
        VStack(spacing: 14) {
            // Zone Selector Pills
            HStack(spacing: 8) {
                zonePill(zone: .head, label: "🎯 HEAD", badge: "100% LOCK")
                zonePill(zone: .body, label: "🛡️ BODY", badge: "MAX DPS")
                zonePill(zone: .legs, label: "⚡ LEGS", badge: "SWEEP")
            }

            // Split: Left Silhouette Canvas + Right Live Metrics
            HStack(alignment: .top, spacing: 14) {
                // Red Criminal Silhouette with Laser Scanline Animation
                FloatingTargetingSilhouetteView(
                    targetZone: menuManager.activeZone,
                    isCalculating: menuManager.isCalculating,
                    calculationProgress: menuManager.calculationProgress
                )
                .frame(width: 140, height: 195)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(menuManager.activeZone.color.opacity(0.4), lineWidth: 1)
                )

                // Metrics Column
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text(menuManager.activeZone.title)
                            .font(.system(size: 12, weight: .black, design: .monospaced))
                            .foregroundColor(menuManager.activeZone.color)
                        Spacer()
                    }

                    metricRow(label: "General Sensi", value: "\(Int(menuManager.generalSensi))", bonus: "+8", color: SensiTheme.rubyRed)
                    metricRow(label: "Red Dot Sight", value: "\(Int(menuManager.redDotSensi))", bonus: "+6", color: .white)
                    metricRow(label: "2X Scope", value: "\(Int(menuManager.scope2xSensi))", bonus: "+4", color: .white)
                    metricRow(label: "4X Scope", value: "\(Int(menuManager.scope4xSensi))", bonus: "+2", color: .white)
                    metricRow(label: "Fire Button", value: "\(Int(menuManager.fireButtonSize))%", bonus: "Snap", color: SensiTheme.cyanAccent)
                    metricRow(label: "Drag Flick", value: menuManager.isJDrag ? "J-Drag" : "Linear", bonus: "Fast", color: SensiTheme.goldAccent)

                    Spacer(minLength: 6)

                    // ⚡ APPLY SETTINGS Button
                    Button(action: {
                        menuManager.triggerApplyAnimation()
                    }) {
                        HStack(spacing: 6) {
                            if menuManager.isCalculating {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(0.8)
                                Text("CALCULATING...")
                                    .font(.system(size: 11, weight: .black, design: .monospaced))
                            } else if menuManager.isApplied {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 13, weight: .black))
                                Text("APPLIED!")
                                    .font(.system(size: 11, weight: .black, design: .monospaced))
                            } else {
                                Image(systemName: "bolt.fill")
                                    .font(.system(size: 12))
                                Text("APPLY TO FREE FIRE")
                                    .font(.system(size: 11, weight: .black, design: .monospaced))
                            }
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 11)
                        .background(
                            menuManager.isApplied ? Color.green : (menuManager.isCalculating ? Color.orange : SensiTheme.rubyRed)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .shadow(color: SensiTheme.rubyRed.opacity(0.35), radius: 6, x: 0, y: 3)
                    }
                    .disabled(menuManager.isCalculating)
                }
            }

            // Coaching Advice Pill
            HStack(alignment: .top, spacing: 8) {
                Text("💡")
                    .font(.system(size: 12))
                Text(coachingTipForZone(menuManager.activeZone))
                    .font(.system(size: 11))
                    .foregroundColor(Color(red: 0.8, green: 0.8, blue: 0.9))
                    .lineSpacing(2)
            }
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white.opacity(0.04))
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }

    @ViewBuilder
    private func zonePill(zone: TargetZoneType, label: String, badge: String) -> some View {
        let isSelected = menuManager.activeZone == zone
        Button(action: {
            let haptic = UIImpactFeedbackGenerator(style: .medium)
            haptic.impactOccurred()
            withAnimation(.spring(response: 0.28, dampingFraction: 0.8)) {
                menuManager.activeZone = zone
            }
        }) {
            VStack(spacing: 2) {
                Text(label)
                    .font(.system(size: 11, weight: isSelected ? .black : .bold, design: .monospaced))
                    .foregroundColor(isSelected ? .white : SensiTheme.textMuted)
                Text(badge)
                    .font(.system(size: 8, weight: .bold, design: .monospaced))
                    .foregroundColor(isSelected ? zone.color : SensiTheme.textMuted.opacity(0.6))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(isSelected ? zone.color.opacity(0.2) : Color.white.opacity(0.04))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(isSelected ? zone.color : Color.clear, lineWidth: 1.5)
            )
        }
    }

    @ViewBuilder
    private func metricRow(label: String, value: String, bonus: String, color: Color) -> some View {
        HStack {
            Text(label)
                .font(.system(size: 10, design: .monospaced))
                .foregroundColor(SensiTheme.textMuted)
            Spacer()
            HStack(spacing: 4) {
                Text(value)
                    .font(.system(size: 11, weight: .black, design: .monospaced))
                    .foregroundColor(color)
                Text("(\(bonus))")
                    .font(.system(size: 9, weight: .bold, design: .monospaced))
                    .foregroundColor(SensiTheme.textMuted)
            }
        }
    }

    private func coachingTipForZone(_ zone: TargetZoneType) -> String {
        switch zone {
        case .head:
            return "Head Lock: Fast upward flick, release immediately at apex to let aim magnetism lock onto the head hitbox."
        case .body:
            return "Body Defense: Controlled moderate drag stabilizes bullet spread for continuous chest and armor DPS."
        case .legs:
            return "Leg Sweep: Lower general sensitivity counters jumping, sliding, and crouching opponents."
        }
    }

    // MARK: - 3. Recommendations Card (No Guided Access)
    private var recommendationsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "checkmark.seal.fill")
                    .foregroundColor(SensiTheme.fairPlayGreen)
                Text("100% FAIR PLAY & ANTI-BAN")
                    .font(.system(size: 12, weight: .black, design: .monospaced))
                    .foregroundColor(SensiTheme.fairPlayGreen)
                Spacer()
            }

            Text("Sensi Bot Max uses mathematical sensitivity curves and display scaling. It operates without game memory modification or file injection.")
                .font(.system(size: 11))
                .foregroundColor(SensiTheme.textSecondary)
                .lineSpacing(2)

            Divider().background(Color.white.opacity(0.1))

            Text("PRO GAMING RECOMMENDATIONS")
                .font(.system(size: 11, weight: .black, design: .monospaced))
                .foregroundColor(SensiTheme.goldAccent)

            BulletRow(
                icon: "bolt.slash.fill",
                title: "Disable Low Power Mode",
                detail: "Low Power Mode limits ProMotion 120Hz screens to 60Hz and cuts touch sampling rate in half."
            )

            BulletRow(
                icon: "iphone.smartphone",
                title: "Standard Display Zoom",
                detail: "Settings > Display & Brightness > View: Standard ensures 1:1 physical touch-to-pixel coordinate tracking."
            )
        }
        .gamingCard()
    }
}
