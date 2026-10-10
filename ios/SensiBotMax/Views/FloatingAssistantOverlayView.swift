import SwiftUI

public struct FloatingAssistantOverlayView: View {
    @ObservedObject var menuManager = FloatingMenuManager.shared
    @ObservedObject var pipManager = PipOverlayManager.shared

    @State private var dragOffset: CGSize = .zero
    @State private var isDraggingLauncher: Bool = false
    @State private var panelOffset: CGSize = .zero
    @State private var isDraggingPanel: Bool = false

    public init() {}

    public var body: some View {
        GeometryReader { screen in
            ZStack {
                if menuManager.isMenuEnabled {
                    if menuManager.isExpanded {
                        // 1. Semi-translucent backdrop to dismiss on tap
                        Color.black.opacity(0.4)
                            .ignoresSafeArea()
                            .onTapGesture {
                                withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                                    menuManager.isExpanded = false
                                }
                            }

                        // 2. Expanded Floating Assistant Panel
                        expandedAssistantPanel(screen: screen)
                            .offset(panelOffset)
                            .transition(.asymmetric(
                                insertion: .scale(scale: 0.85).combined(with: .opacity),
                                removal: .scale(scale: 0.85).combined(with: .opacity)
                            ))
                    } else {
                        // 3. Compact Custom Floating Launcher (Collapsed State)
                        compactFloatingLauncher(screen: screen)
                            .position(
                                x: menuManager.launcherX + dragOffset.width + 24,
                                y: menuManager.launcherY + dragOffset.height + 24
                            )
                            .transition(.scale.combined(with: .opacity))
                    }
                }
            }
        }
    }

    // MARK: - 1. Compact Floating Launcher (Collapsed State)
    @ViewBuilder
    private func compactFloatingLauncher(screen: GeometryProxy) -> some View {
        ZStack {
            // Dark Obsidian Base
            Circle()
                .fill(Color(red: 0.05, green: 0.05, blue: 0.08).opacity(0.94))
                .frame(width: 48, height: 48)
                .shadow(color: SensiTheme.rubyRed.opacity(0.5), radius: isDraggingLauncher ? 12 : 6, x: 0, y: 3)

            // Outer Neon Glow Border
            Circle()
                .stroke(
                    LinearGradient(
                        colors: [SensiTheme.rubyRed, SensiTheme.rubyRedDark],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 2
                )
                .frame(width: 48, height: 48)

            // Center Target Crosshair Emblem
            ZStack {
                Circle()
                    .stroke(SensiTheme.cyanAccent.opacity(0.6), lineWidth: 1)
                    .frame(width: 22, height: 22)

                Rectangle()
                    .fill(SensiTheme.rubyRed)
                    .frame(width: 2, height: 28)

                Rectangle()
                    .fill(SensiTheme.rubyRed)
                    .frame(width: 28, height: 2)

                Circle()
                    .fill(SensiTheme.cyanAccent)
                    .frame(width: 5, height: 5)
            }

            // Top-Right Live Status Indicator (Neon Green)
            Circle()
                .fill(Color(red: 0.0, green: 0.9, blue: 0.46))
                .frame(width: 8, height: 8)
                .overlay(Circle().stroke(Color.white, lineWidth: 1))
                .offset(x: 15, y: -15)
        }
        .scaleEffect(isDraggingLauncher ? 1.1 : 1.0)
        .contentShape(Circle())
        .onTapGesture {
            let haptic = UIImpactFeedbackGenerator(style: .medium)
            haptic.impactOccurred()
            withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                menuManager.isExpanded = true
                panelOffset = .zero
            }
        }
        .gesture(
            DragGesture()
                .onChanged { val in
                    isDraggingLauncher = true
                    dragOffset = val.translation
                }
                .onEnded { val in
                    isDraggingLauncher = false
                    menuManager.launcherX += val.translation.width
                    menuManager.launcherY += val.translation.height
                    dragOffset = .zero
                    menuManager.snapToNearestEdge(
                        screenWidth: screen.size.width,
                        safeTop: screen.safeAreaInsets.top,
                        safeBottom: screen.size.height - screen.safeAreaInsets.bottom
                    )
                }
        )
    }

    // MARK: - 2. Expanded Floating Assistant Panel
    @ViewBuilder
    private func expandedAssistantPanel(screen: GeometryProxy) -> some View {
        let panelWidth = min(screen.size.width - 24, 370)

        VStack(spacing: 0) {
            // A. Panel Header Bar (Draggable)
            HStack(spacing: 8) {
                Text("LYAM FF")
                    .font(.system(size: 10, weight: .black, design: .rounded))
                    .foregroundColor(SensiTheme.rubyRed)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(SensiTheme.rubyRed.opacity(0.18))
                    .clipShape(RoundedRectangle(cornerRadius: 4))
                    .overlay(RoundedRectangle(cornerRadius: 4).stroke(SensiTheme.rubyRed.opacity(0.6), lineWidth: 1))

                Text("FLOATING ASSISTANT")
                    .font(.system(size: 13, weight: .black, design: .monospaced))
                    .foregroundColor(.white)

                // Drag Handle Pill
                Capsule()
                    .fill(Color.white.opacity(0.25))
                    .frame(width: 32, height: 4)
                    .padding(.horizontal, 4)

                Spacer()

                HStack(spacing: 4) {
                    Circle()
                        .fill(Color(red: 0.0, green: 0.9, blue: 0.46))
                        .frame(width: 6, height: 6)
                    Text("ACTIVE")
                        .font(.system(size: 9, weight: .bold, design: .monospaced))
                        .foregroundColor(Color(red: 0.0, green: 0.9, blue: 0.46))
                }

                Button(action: {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                        menuManager.isExpanded = false
                    }
                }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(.white)
                        .padding(6)
                        .background(Color.white.opacity(0.1))
                        .clipShape(Circle())
                }
            }
            .padding(.horizontal, 14)
            .padding(.top, 12)
            .padding(.bottom, 8)
            .background(Color(red: 0.07, green: 0.07, blue: 0.10))
            .gesture(
                DragGesture()
                    .onChanged { val in
                        isDraggingPanel = true
                        panelOffset = val.translation
                    }
                    .onEnded { val in
                        isDraggingPanel = false
                        panelOffset = val.translation
                    }
            )

            Divider().background(Color.white.opacity(0.12))

            // B. Navigation Tabs Bar
            HStack(spacing: 4) {
                tabButton(title: "🎯 ZONES", index: 0)
                tabButton(title: "🎚️ TUNER", index: 1)
                tabButton(title: "✨ RETICLE", index: 2)
                tabButton(title: "📊 DEVICE", index: 3)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(Color(red: 0.05, green: 0.05, blue: 0.08))

            // C. Tab Content Area
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 12) {
                    switch menuManager.selectedTab {
                    case 0:
                        aimZonesTab()
                    case 1:
                        tunerSlidersTab()
                    case 2:
                        crosshairTab()
                    default:
                        deviceTelemetryTab()
                    }
                }
                .padding(12)
            }
            .frame(maxHeight: 380)

            // D. In-Game Float Trigger Banner
            Button(action: {
                pipManager.startPip()
            }) {
                HStack(spacing: 8) {
                    Image(systemName: "scope")
                        .font(.system(size: 12, weight: .bold))
                    Text(pipManager.isPipActive ? "ASSISTANT ACTIVE OVER GAME" : "FLOAT OVER FREE FIRE (IN-GAME)")
                        .font(.system(size: 11, weight: .black, design: .monospaced))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(pipManager.isPipActive ? Color(red: 0.0, green: 0.6, blue: 0.3) : SensiTheme.rubyRed)
                .clipShape(RoundedRectangle(cornerRadius: 6))
                .padding(.horizontal, 12)
                .padding(.top, 4)
            }

            // E. Panel Footer
            HStack {
                Text("100% Fair Play • No Game Files Altered")
                    .font(.system(size: 9.5, design: .monospaced))
                    .foregroundColor(SensiTheme.textMuted)
                Spacer()
                Button(action: {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                        menuManager.isExpanded = false
                    }
                }) {
                    Text("MINIMIZE")
                        .font(.system(size: 10, weight: .bold, design: .monospaced))
                        .foregroundColor(SensiTheme.cyanAccent)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(Color(red: 0.04, green: 0.04, blue: 0.06))
        }
        .frame(width: panelWidth)
        .background(
            Color(red: 0.05, green: 0.05, blue: 0.08).opacity(0.96)
        )
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(
                    LinearGradient(
                        colors: [SensiTheme.rubyRed.opacity(0.8), SensiTheme.cyanAccent.opacity(0.3)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 1.5
                )
        )
        .shadow(color: Color.black.opacity(0.7), radius: 25, x: 0, y: 10)
    }

    @ViewBuilder
    private func tabButton(title: String, index: Int) -> some View {
        let isSelected = menuManager.selectedTab == index
        Button(action: {
            menuManager.selectedTab = index
        }) {
            Text(title)
                .font(.system(size: 11, weight: isSelected ? .black : .bold, design: .monospaced))
                .foregroundColor(isSelected ? .white : SensiTheme.textMuted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 6)
                .background(isSelected ? SensiTheme.rubyRed : Color.clear)
                .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }

    // MARK: - Tab 1: AIM ZONES (Matching Android In-Game Assistant)
    @ViewBuilder
    private func aimZonesTab() -> some View {
        VStack(spacing: 12) {
            // Zone Selector Pills
            HStack(spacing: 6) {
                zonePill(zone: .head, label: "🎯 HEAD")
                zonePill(zone: .body, label: "🛡️ BODY")
                zonePill(zone: .legs, label: "⚡ LEGS")
            }

            // Split: Left Silhouette Preview + Right Metrics
            HStack(alignment: .top, spacing: 10) {
                // Character Targeting Silhouette Canvas
                FloatingTargetingSilhouetteView(
                    targetZone: menuManager.activeZone,
                    isCalculating: menuManager.isCalculating,
                    calculationProgress: menuManager.calculationProgress
                )
                .frame(width: 120, height: 165)

                // Metrics Column
                VStack(alignment: .leading, spacing: 6) {
                    Text(menuManager.activeZone.title)
                        .font(.system(size: 11, weight: .black, design: .monospaced))
                        .foregroundColor(menuManager.activeZone.color)

                    metricRow(label: "General Sensi", value: "\(Int(menuManager.generalSensi)) (+8)", color: SensiTheme.rubyRed)
                    metricRow(label: "Red Dot", value: "\(Int(menuManager.redDotSensi)) (+6)", color: .white)
                    metricRow(label: "Fire Button", value: "\(Int(menuManager.fireButtonSize))% (Snap)", color: .white)
                    metricRow(label: "Drag Speed", value: menuManager.isJDrag ? "High / J-Drag" : "Moderate", color: SensiTheme.goldAccent)

                    Spacer(minLength: 4)

                    // ⚡ APPLY SETTINGS Button
                    Button(action: {
                        menuManager.triggerApplyAnimation()
                        pipManager.updateHudMetrics(
                            general: Int(menuManager.generalSensi),
                            redDot: Int(menuManager.redDotSensi),
                            scope2x: Int(menuManager.scope2xSensi),
                            scope4x: Int(menuManager.scope4xSensi),
                            hz: Int(DeviceProbe.current().maxFPS)
                        )
                        pipManager.updateHudZone(name: menuManager.activeZone.rawValue)
                    }) {
                        HStack(spacing: 4) {
                            if menuManager.isCalculating {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(0.8)
                                Text("APPLYING...")
                                    .font(.system(size: 10, weight: .black, design: .monospaced))
                            } else if menuManager.isApplied {
                                Image(systemName: "checkmark")
                                    .font(.system(size: 11, weight: .black))
                                Text("APPLIED!")
                                    .font(.system(size: 10, weight: .black, design: .monospaced))
                            } else {
                                Text("⚡ APPLY SETTINGS")
                                    .font(.system(size: 10, weight: .black, design: .monospaced))
                            }
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(
                            menuManager.isApplied ? Color.green : (menuManager.isCalculating ? Color.orange : SensiTheme.rubyRed)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                    .disabled(menuManager.isCalculating)
                }
            }

            // Coaching Tip Box
            HStack(alignment: .top, spacing: 6) {
                Text("💡")
                    .font(.system(size: 11))
                Text(coachingTipForZone(menuManager.activeZone))
                    .font(.system(size: 10))
                    .foregroundColor(Color(red: 0.75, green: 0.75, blue: 0.85))
                    .lineSpacing(2)
            }
            .padding(8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white.opacity(0.05))
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }

    @ViewBuilder
    private func zonePill(zone: TargetZoneType, label: String) -> some View {
        let isSelected = menuManager.activeZone == zone
        Button(action: {
            menuManager.activeZone = zone
        }) {
            Text(label)
                .font(.system(size: 11, weight: isSelected ? .black : .bold, design: .monospaced))
                .foregroundColor(isSelected ? .white : SensiTheme.textMuted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 7)
                .background(isSelected ? zone.color.opacity(0.3) : Color(red: 0.1, green: 0.1, blue: 0.14))
                .clipShape(RoundedRectangle(cornerRadius: 6))
                .overlay(
                    RoundedRectangle(cornerRadius: 6)
                        .stroke(isSelected ? zone.color : Color.clear, lineWidth: 1.5)
                )
        }
    }

    @ViewBuilder
    private func metricRow(label: String, value: String, color: Color) -> some View {
        HStack {
            Text(label)
                .font(.system(size: 10, design: .monospaced))
                .foregroundColor(SensiTheme.textMuted)
            Spacer()
            Text(value)
                .font(.system(size: 10, weight: .bold, design: .monospaced))
                .foregroundColor(color)
        }
    }

    private func coachingTipForZone(_ zone: TargetZoneType) -> String {
        switch zone {
        case .head:
            return "Drag upward quickly and immediately release thumb to let aim lock on head."
        case .body:
            return "Moderate drag speed prevents crosshair from jumping above the chest."
        case .legs:
            return "Lower general sensitivity recovers aim against jumping/crouching enemies."
        }
    }

    // MARK: - Tab 2: TUNER & SLIDERS
    @ViewBuilder
    private func tunerSlidersTab() -> some View {
        VStack(spacing: 12) {
            sliderCard(title: "General Sensitivity", value: $menuManager.generalSensi, range: 0...200, unit: "")
            sliderCard(title: "Red Dot Sight", value: $menuManager.redDotSensi, range: 0...200, unit: "")
            sliderCard(title: "2X Scope", value: $menuManager.scope2xSensi, range: 0...200, unit: "")
            sliderCard(title: "4X Scope", value: $menuManager.scope4xSensi, range: 0...200, unit: "")
            sliderCard(title: "Fire Button Size", value: $menuManager.fireButtonSize, range: 30...70, unit: "%")

            // J-Drag Toggle
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("DRAG FLICK TECHNIQUE")
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundColor(.white)
                    Text(menuManager.isJDrag ? "J-Drag Curve (Curved Flick)" : "Straight Drag (Direct Lift)")
                        .font(.system(size: 10))
                        .foregroundColor(SensiTheme.textSecondary)
                }
                Spacer()
                Toggle("", isOn: $menuManager.isJDrag)
                    .labelsHidden()
                    .toggleStyle(SwitchToggleStyle(tint: SensiTheme.rubyRed))
            }
            .padding(8)
            .background(Color.white.opacity(0.05))
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }

    @ViewBuilder
    private func sliderCard(title: String, value: Binding<Double>, range: ClosedRange<Double>, unit: String) -> some View {
        VStack(spacing: 4) {
            HStack {
                Text(title)
                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                    .foregroundColor(SensiTheme.textMuted)
                Spacer()
                Text("\(Int(value.wrappedValue))\(unit)")
                    .font(.system(size: 11, weight: .black, design: .monospaced))
                    .foregroundColor(SensiTheme.cyanAccent)
            }
            Slider(value: value, in: range, step: 1)
                .accentColor(SensiTheme.rubyRed)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color.white.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 6))
    }

    // MARK: - Tab 3: CROSSHAIR OVERLAY (Matching Android Crosshair)
    @ViewBuilder
    private func crosshairTab() -> some View {
        VStack(spacing: 12) {
            // Style Selector
            HStack(spacing: 4) {
                ForEach(CrosshairStyleType.allCases, id: \.self) { style in
                    let isSel = menuManager.crosshairStyle == style
                    Button(action: { menuManager.crosshairStyle = style }) {
                        Text(style.rawValue)
                            .font(.system(size: 9, weight: isSel ? .black : .bold, design: .monospaced))
                            .foregroundColor(isSel ? .white : SensiTheme.textMuted)
                            .padding(.vertical, 5)
                            .frame(maxWidth: .infinity)
                            .background(isSel ? SensiTheme.rubyRed : Color.white.opacity(0.06))
                            .clipShape(RoundedRectangle(cornerRadius: 4))
                    }
                }
            }

            // Live Crosshair Preview
            ZStack {
                Color.black.opacity(0.5)
                    .frame(height: 80)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.white.opacity(0.1), lineWidth: 1))

                renderCrosshairPreview()
            }

            // Color Selector Pills
            HStack(spacing: 8) {
                colorPill(hex: "#FF2A4D", color: SensiTheme.rubyRed)
                colorPill(hex: "#00E5FF", color: SensiTheme.cyanAccent)
                colorPill(hex: "#00E676", color: SensiTheme.fairPlayGreen)
                colorPill(hex: "#FFB300", color: SensiTheme.goldAccent)
                colorPill(hex: "#FFFFFF", color: .white)
            }

            sliderCard(title: "Crosshair Size", value: $menuManager.crosshairSize, range: 14...44, unit: "pt")
            sliderCard(title: "Thickness", value: $menuManager.crosshairThickness, range: 1...5, unit: "pt")
        }
    }

    @ViewBuilder
    private func colorPill(hex: String, color: Color) -> some View {
        let isSel = menuManager.crosshairColorHex == hex
        Button(action: { menuManager.crosshairColorHex = hex }) {
            Circle()
                .fill(color)
                .frame(width: 22, height: 22)
                .overlay(
                    Circle().stroke(Color.white, lineWidth: isSel ? 2.5 : 0)
                )
        }
    }

    @ViewBuilder
    private func renderCrosshairPreview() -> some View {
        let col = Color(hex: menuManager.crosshairColorHex) ?? SensiTheme.rubyRed
        let sz = CGFloat(menuManager.crosshairSize)
        let th = CGFloat(menuManager.crosshairThickness)

        ZStack {
            switch menuManager.crosshairStyle {
            case .classicCross:
                Rectangle().fill(col).frame(width: th, height: sz)
                Rectangle().fill(col).frame(width: sz, height: th)
                Circle().fill(col).frame(width: th * 1.5, height: th * 1.5)
            case .centerDot:
                Circle().fill(col).frame(width: sz * 0.4, height: sz * 0.4)
            case .circleDot:
                Circle().stroke(col, lineWidth: th).frame(width: sz * 0.75, height: sz * 0.75)
                Circle().fill(col).frame(width: th * 1.5, height: th * 1.5)
            case .tStyle:
                Rectangle().fill(col).frame(width: sz, height: th)
                Rectangle().fill(col).frame(width: th, height: sz / 2).offset(y: sz / 4)
                Circle().fill(col).frame(width: th * 1.5, height: th * 1.5)
            }
        }
    }

    // MARK: - Tab 4: TELEMETRY (Hardware Specs)
    @ViewBuilder
    private func deviceTelemetryTab() -> some View {
        let profile = DeviceProbe.current()
        VStack(spacing: 8) {
            telemetryRow(label: "Device Model", value: profile.modelMarketingName, color: .white)
            telemetryRow(label: "Screen PPI", value: "\(Int(profile.ppi)) PPI", color: SensiTheme.cyanAccent)
            telemetryRow(label: "Refresh Rate", value: "\(profile.maxFPS)Hz", color: profile.maxFPS >= 120 ? SensiTheme.cyanAccent : SensiTheme.textMuted)
            telemetryRow(label: "Screen Short Width", value: String(format: "%.2f\"", profile.widthInches), color: .white)
            telemetryRow(label: "Low Power Mode", value: profile.lowPowerMode ? "THROTTLED" : "NORMAL", color: profile.lowPowerMode ? SensiTheme.goldAccent : SensiTheme.fairPlayGreen)
            telemetryRow(label: "Gyroscope", value: profile.hasGyro ? "AVAILABLE" : "NONE", color: SensiTheme.fairPlayGreen)
        }
        .padding(8)
        .background(Color.white.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    @ViewBuilder
    private func telemetryRow(label: String, value: String, color: Color) -> some View {
        HStack {
            Text(label)
                .font(.system(size: 10, design: .monospaced))
                .foregroundColor(SensiTheme.textMuted)
            Spacer()
            Text(value)
                .font(.system(size: 10, weight: .bold, design: .monospaced))
                .foregroundColor(color)
        }
    }
}
