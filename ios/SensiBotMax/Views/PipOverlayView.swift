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

                // 2. Navigation Tabs (ZONES | TUNER | RETICLE | DEVICE)
                navigationTabsBar

                // 3. Main Content Panel
                VStack(spacing: 14) {
                    switch menuManager.selectedTab {
                    case 0:
                        aimZonesSection
                    case 1:
                        tunerSlidersSection
                    case 2:
                        crosshairSection
                    default:
                        deviceTelemetrySection
                    }
                }
                .gamingCard(borderColor: SensiTheme.rubyRed.opacity(0.45))

                // 4. Tactical Recommendations & Fair Play Card
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
                Text("Tactical Aim Zones • Sensitivity Tuner • Reticle Calibration")
                    .font(.system(size: 11))
                    .foregroundColor(SensiTheme.textSecondary)
                Spacer()
            }
        }
    }

    // MARK: - 2. Navigation Tabs Bar
    private var navigationTabsBar: some View {
        HStack(spacing: 6) {
            tabButton(title: "🎯 ZONES", index: 0)
            tabButton(title: "🎚️ TUNER", index: 1)
            tabButton(title: "✨ RETICLE", index: 2)
            tabButton(title: "📊 DEVICE", index: 3)
        }
        .padding(4)
        .background(Color.white.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    @ViewBuilder
    private func tabButton(title: String, index: Int) -> some View {
        let isSelected = menuManager.selectedTab == index
        Button(action: {
            let haptic = UIImpactFeedbackGenerator(style: .light)
            haptic.impactOccurred()
            withAnimation(.spring(response: 0.28, dampingFraction: 0.8)) {
                menuManager.selectedTab = index
            }
        }) {
            Text(title)
                .font(.system(size: 11, weight: isSelected ? .black : .bold, design: .monospaced))
                .foregroundColor(isSelected ? .white : SensiTheme.textMuted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(isSelected ? SensiTheme.rubyRed : Color.clear)
                .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }

    // MARK: - 3. Tab 1: AIM ZONES (Red Criminal Targeting)
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
                .frame(width: 140, height: 185)
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

                    Spacer(minLength: 4)

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
                        .padding(.vertical, 10)
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

    // MARK: - 4. Tab 2: TUNER & SLIDERS
    private var tunerSlidersSection: some View {
        VStack(spacing: 12) {
            // Quick Presets
            HStack(spacing: 6) {
                presetButton(name: "RUSH HEADSHOT", gen: 196, red: 188, s2x: 175, s4x: 162, btn: 42)
                presetButton(name: "BALANCED", gen: 182, red: 175, s2x: 168, s4x: 154, btn: 48)
                presetButton(name: "SNIPER 4X", gen: 174, red: 168, s2x: 160, s4x: 148, btn: 52)
            }

            sliderCard(title: "General Sensitivity", value: $menuManager.generalSensi, range: 0...200, unit: "")
            sliderCard(title: "Red Dot Sight", value: $menuManager.redDotSensi, range: 0...200, unit: "")
            sliderCard(title: "2X Scope", value: $menuManager.scope2xSensi, range: 0...200, unit: "")
            sliderCard(title: "4X Scope", value: $menuManager.scope4xSensi, range: 0...200, unit: "")
            sliderCard(title: "Fire Button Size", value: $menuManager.fireButtonSize, range: 30...70, unit: "%")

            // J-Drag Toggle
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("J-DRAG FLICK ACCELERATION")
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundColor(.white)
                    Text(menuManager.isJDrag ? "Curved 'J' flick curve (Maximum headshot snap)" : "Linear straight drag flick")
                        .font(.system(size: 10))
                        .foregroundColor(SensiTheme.textSecondary)
                }
                Spacer()
                Toggle("", isOn: $menuManager.isJDrag)
                    .labelsHidden()
                    .toggleStyle(SwitchToggleStyle(tint: SensiTheme.rubyRed))
            }
            .padding(10)
            .background(Color.white.opacity(0.04))
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }

    @ViewBuilder
    private func presetButton(name: String, gen: Double, red: Double, s2x: Double, s4x: Double, btn: Double) -> some View {
        Button(action: {
            let haptic = UIImpactFeedbackGenerator(style: .light)
            haptic.impactOccurred()
            menuManager.generalSensi = gen
            menuManager.redDotSensi = red
            menuManager.scope2xSensi = s2x
            menuManager.scope4xSensi = s4x
            menuManager.fireButtonSize = btn
        }) {
            Text(name)
                .font(.system(size: 9.5, weight: .black, design: .monospaced))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 7)
                .background(Color.white.opacity(0.08))
                .clipShape(RoundedRectangle(cornerRadius: 6))
        }
    }

    @ViewBuilder
    private func sliderCard(title: String, value: Binding<Double>, range: ClosedRange<Double>, unit: String) -> some View {
        VStack(spacing: 4) {
            HStack {
                Text(title)
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundColor(SensiTheme.textMuted)
                Spacer()
                Text("\(Int(value.wrappedValue))\(unit)")
                    .font(.system(size: 12, weight: .black, design: .monospaced))
                    .foregroundColor(SensiTheme.cyanAccent)
            }
            Slider(value: value, in: range, step: 1)
                .accentColor(SensiTheme.rubyRed)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(Color.white.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - 5. Tab 3: RETICLE & CROSSHAIR
    private var crosshairSection: some View {
        VStack(spacing: 12) {
            // Style Selector
            HStack(spacing: 6) {
                ForEach(CrosshairStyleType.allCases, id: \.self) { style in
                    let isSel = menuManager.crosshairStyle == style
                    Button(action: {
                        let haptic = UIImpactFeedbackGenerator(style: .light)
                        haptic.impactOccurred()
                        menuManager.crosshairStyle = style
                    }) {
                        Text(style.rawValue)
                            .font(.system(size: 10, weight: isSel ? .black : .bold, design: .monospaced))
                            .foregroundColor(isSel ? .white : SensiTheme.textMuted)
                            .padding(.vertical, 7)
                            .frame(maxWidth: .infinity)
                            .background(isSel ? SensiTheme.rubyRed : Color.white.opacity(0.06))
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                }
            }

            // Live Crosshair Preview Canvas
            ZStack {
                Color.black.opacity(0.6)
                    .frame(height: 120)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.1), lineWidth: 1))

                // Subtle Grid Background
                GeometryReader { geo in
                    Path { path in
                        let w = geo.size.width
                        let h = geo.size.height
                        path.move(to: CGPoint(x: w / 2, y: 0))
                        path.addLine(to: CGPoint(x: w / 2, y: h))
                        path.move(to: CGPoint(x: 0, y: h / 2))
                        path.addLine(to: CGPoint(x: w, y: h / 2))
                    }
                    .stroke(Color.white.opacity(0.08), lineWidth: 1)
                }
                .frame(height: 120)

                renderCrosshairPreview()
            }

            // Color Selector Pills
            HStack(spacing: 12) {
                colorPill(hex: "#FF2A4D", color: SensiTheme.rubyRed)
                colorPill(hex: "#00E5FF", color: SensiTheme.cyanAccent)
                colorPill(hex: "#00E676", color: SensiTheme.fairPlayGreen)
                colorPill(hex: "#FFB300", color: SensiTheme.goldAccent)
                colorPill(hex: "#FFFFFF", color: .white)
            }
            .padding(.vertical, 4)

            sliderCard(title: "Reticle Size", value: $menuManager.crosshairSize, range: 14...44, unit: "pt")
            sliderCard(title: "Line Thickness", value: $menuManager.crosshairThickness, range: 1...5, unit: "pt")
        }
    }

    @ViewBuilder
    private func colorPill(hex: String, color: Color) -> some View {
        let isSel = menuManager.crosshairColorHex == hex
        Button(action: {
            let haptic = UIImpactFeedbackGenerator(style: .light)
            haptic.impactOccurred()
            menuManager.crosshairColorHex = hex
        }) {
            Circle()
                .fill(color)
                .frame(width: 26, height: 26)
                .overlay(
                    Circle().stroke(Color.white, lineWidth: isSel ? 3 : 0)
                )
        }
    }

    @ViewBuilder
    private func renderCrosshairPreview() -> some View {
        let col = Color(hex: menuManager.crosshairColorHex)
        let sz = CGFloat(menuManager.crosshairSize)
        let th = CGFloat(menuManager.crosshairThickness)

        ZStack {
            switch menuManager.crosshairStyle {
            case .classicCross:
                Rectangle().fill(col).frame(width: th, height: sz)
                Rectangle().fill(col).frame(width: sz, height: th)
                Circle().fill(col).frame(width: th * 1.5, height: th * 1.5)
            case .centerDot:
                Circle().fill(col).frame(width: sz * 0.45, height: sz * 0.45)
            case .circleDot:
                Circle().stroke(col, lineWidth: th).frame(width: sz * 0.8, height: sz * 0.8)
                Circle().fill(col).frame(width: th * 1.5, height: th * 1.5)
            case .tStyle:
                Rectangle().fill(col).frame(width: sz, height: th)
                Rectangle().fill(col).frame(width: th, height: sz / 2).offset(y: sz / 4)
                Circle().fill(col).frame(width: th * 1.5, height: th * 1.5)
            }
        }
    }

    // MARK: - 6. Tab 4: DEVICE TELEMETRY
    private var deviceTelemetrySection: some View {
        let profile = DeviceProbe.current()
        return VStack(spacing: 10) {
            telemetryRow(label: "Device Model", value: profile.modelMarketingName, color: .white)
            telemetryRow(label: "Screen PPI", value: "\(Int(profile.ppi)) PPI", color: SensiTheme.cyanAccent)
            telemetryRow(label: "ProMotion Refresh Rate", value: "\(profile.maxFPS)Hz", color: profile.maxFPS >= 120 ? SensiTheme.cyanAccent : SensiTheme.textMuted)
            telemetryRow(label: "Screen Width", value: String(format: "%.2f inches", profile.widthInches), color: .white)
            telemetryRow(label: "Touch Sampling Rate", value: "240Hz Ultra-Fast", color: SensiTheme.fairPlayGreen)
            telemetryRow(label: "Low Power Mode", value: profile.lowPowerMode ? "THROTTLED (60Hz)" : "OPTIMAL (120Hz)", color: profile.lowPowerMode ? SensiTheme.goldAccent : SensiTheme.fairPlayGreen)
            telemetryRow(label: "Gyroscope Hardware", value: profile.hasGyro ? "AVAILABLE" : "UNAVAILABLE", color: SensiTheme.fairPlayGreen)
        }
        .padding(6)
    }

    @ViewBuilder
    private func telemetryRow(label: String, value: String, color: Color) -> some View {
        HStack {
            Text(label)
                .font(.system(size: 11, design: .monospaced))
                .foregroundColor(SensiTheme.textMuted)
            Spacer()
            Text(value)
                .font(.system(size: 11, weight: .bold, design: .monospaced))
                .foregroundColor(color)
        }
        .padding(.vertical, 3)
    }

    // MARK: - 7. Recommendations Card
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
                icon: "lock.shield.fill",
                title: "Enable Guided Access",
                detail: "Settings > Accessibility > Guided Access. Prevents accidental swipes on the home indicator during upward drag flicks."
            )

            BulletRow(
                icon: "bolt.slash.fill",
                title: "Disable Low Power Mode",
                detail: "Low Power Mode limits ProMotion 120Hz screens to 60Hz and cuts touch sampling rate in half."
            )
        }
        .gamingCard()
    }
}

struct BulletRow: View {
    let icon: String
    let title: String
    let detail: String

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: icon)
                .foregroundColor(SensiTheme.goldAccent)
                .font(.system(size: 13))
                .frame(width: 18)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.white)
                Text(detail)
                    .font(.system(size: 10.5))
                    .foregroundColor(SensiTheme.textSecondary)
            }
        }
    }
}
