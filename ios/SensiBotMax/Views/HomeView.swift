import SwiftUI

public struct HomeView: View {
    @Binding public var selectedTab: Int

    @State private var deviceProfile: IOSDeviceProfile = DeviceProbe.current()
    @State private var measuredTouchHz: Double? = nil
    @State private var signals: PlayerSignals = PlayerSignals()
    @State private var fingers: Int = 2
    @State private var currentBias: Double = FeedbackTuner.bias
    @State private var recommendation: IOSRecommendation? = nil
    @State private var isCalculating: Bool = false
    @State private var calculationStep: String = "Optimizing..."

    public init(selectedTab: Binding<Int> = .constant(0)) {
        self._selectedTab = selectedTab
    }

    public var body: some View {
        NavigationView {
            ScrollViewReader { proxy in
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 18) {
                        // 1. App Branding Header
                        brandingHeader

                        // 2. Hardware Device Profile Card
                        hardwareDeviceCard

                        // 3. "GET MY SETTINGS" Primary Action Button
                        if isCalculating {
                            calculatingCard
                        } else {
                            getMySettingsButton(proxy: proxy)
                        }

                        // 4. Recommended Settings Display (Shown after clicking Get My Settings)
                        if let rec = recommendation {
                            VStack(alignment: .leading, spacing: 16) {
                                SensitivityDisplayCard(recommendation: rec, deviceProfile: deviceProfile)
                                    .id("recommended_settings_section")

                                // Post-Practice Feedback Tuner
                                FeedbackTunerCard(currentBias: $currentBias) {
                                    self.recommendation = IOSSensitivityEngine.recommend(
                                        profile: deviceProfile,
                                        touchHz: measuredTouchHz,
                                        signals: signals,
                                        fingers: fingers,
                                        learnedBias: FeedbackTuner.bias
                                    )
                                }
                            }
                        }

                        // 5. Tactical Hub: Links to Touch Velocity Calculator, Sensi Bot, and Assistant
                        tacticalFeaturesHub

                        Spacer(minLength: 32)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                }
                .background(SensiTheme.voidBlack.ignoresSafeArea())
            }
            .navigationBarHidden(true)
        }
        .navigationViewStyle(.stack)
    }

    // MARK: - 1. Branding Header
    private var brandingHeader: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text("SENSI BOT")
                        .font(.system(size: 24, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                    Text("PRO")
                        .font(.system(size: 11, weight: .black, design: .rounded))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 3)
                        .background(SensiTheme.rubyRed)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 6))
                    Text("iOS")
                        .font(.system(size: 11, weight: .black, design: .rounded))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 3)
                        .background(SensiTheme.cyanAccent.opacity(0.18))
                        .foregroundColor(SensiTheme.cyanAccent)
                        .overlay(
                            RoundedRectangle(cornerRadius: 6)
                                .stroke(SensiTheme.cyanAccent.opacity(0.5), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 6))
                }
                Text("Lyam FF Official iOS Edition • Physical PPI Calibrator")
                    .font(.system(size: 12))
                    .foregroundColor(SensiTheme.textSecondary)
            }
            Spacer()
            Image(systemName: "shield.checkered")
                .font(.system(size: 22))
                .foregroundColor(SensiTheme.greenFairPlay)
        }
        .padding(.top, 4)
    }

    // MARK: - 2. Hardware Device Card
    private var hardwareDeviceCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "iphone.gen3")
                    .foregroundColor(SensiTheme.rubyRed)
                Text("DETECTED APPLE HARDWARE")
                    .font(.system(size: 13, weight: .bold, design: .rounded))
                    .foregroundColor(SensiTheme.textPrimary)
                Spacer()
                Text(deviceProfile.identifier)
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundColor(SensiTheme.textMuted)
            }

            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(deviceProfile.modelMarketingName)
                        .font(.system(size: 18, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                    Text("\(Int(deviceProfile.ppi)) PPI • \(deviceProfile.maxFPS)Hz • \(String(format: "%.2f\"", deviceProfile.widthInches)) Short Width")
                        .font(.system(size: 12))
                        .foregroundColor(SensiTheme.textSecondary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 4) {
                    Text(deviceProfile.maxFPS >= 120 ? "ProMotion 120Hz" : "60Hz Display")
                        .font(.system(size: 11, weight: .black, design: .monospaced))
                        .foregroundColor(deviceProfile.maxFPS >= 120 ? SensiTheme.cyanAccent : SensiTheme.textMuted)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background((deviceProfile.maxFPS >= 120 ? SensiTheme.cyanAccent : SensiTheme.textMuted).opacity(0.12))
                        .clipShape(RoundedRectangle(cornerRadius: 6))

                    if deviceProfile.lowPowerMode {
                        Text("Low Power: Throttled")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(SensiTheme.goldAccent)
                    }
                }
            }
        }
        .gamingCard(borderColor: SensiTheme.rubyRed.opacity(0.35))
    }

    // MARK: - 3. "GET MY SETTINGS" Button
    private func getMySettingsButton(proxy: ScrollViewProxy) -> some View {
        Button(action: {
            runCalculation(proxy: proxy)
        }) {
            HStack(spacing: 8) {
                Image(systemName: "bolt.fill")
                    .font(.system(size: 16, weight: .black))
                Text("GET MY SETTINGS")
                    .font(.system(size: 15, weight: .black, design: .rounded))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(
                LinearGradient(
                    colors: [SensiTheme.rubyRed, SensiTheme.rubyRedDark],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .foregroundColor(.white)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .shadow(color: SensiTheme.rubyRed.opacity(0.4), radius: 12, x: 0, y: 4)
        }
    }

    private var calculatingCard: some View {
        VStack(spacing: 10) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: SensiTheme.rubyRed))
                .scaleEffect(1.2)
            Text(calculationStep)
                .font(.system(size: 14, weight: .black, design: .monospaced))
                .foregroundColor(.white)
            Text("Calculating device PPI curve and finger sampling rate...")
                .font(.system(size: 11))
                .foregroundColor(SensiTheme.textMuted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
        .gamingCard(borderColor: SensiTheme.rubyRed)
    }

    // MARK: - 4. Tactical Features Hub (Links to Touch Calculator, Chat Bot, Assistant)
    private var tacticalFeaturesHub: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("TACTICAL LABS & TOOLS")
                .font(.system(size: 12, weight: .black, design: .monospaced))
                .foregroundColor(SensiTheme.goldAccent)

            // Link 1: Touch Velocity Calculator (Separate Page with 5-Drag Sampling)
            NavigationLink(destination: TouchVelocityLabView(measuredTouchHz: $measuredTouchHz)) {
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(SensiTheme.cyanAccent.opacity(0.18))
                            .frame(width: 44, height: 44)
                        Image(systemName: "speedometer")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(SensiTheme.cyanAccent)
                    }

                    VStack(alignment: .leading, spacing: 3) {
                        HStack {
                            Text("TOUCH VELOCITY LAB")
                                .font(.system(size: 13, weight: .black, design: .monospaced))
                                .foregroundColor(.white)
                            Spacer()
                            Text("5-DRAG TEST")
                                .font(.system(size: 9, weight: .bold, design: .monospaced))
                                .foregroundColor(SensiTheme.cyanAccent)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(SensiTheme.cyanAccent.opacity(0.15))
                                .clipShape(RoundedRectangle(cornerRadius: 4))
                        }
                        Text("Measure your 5-drag flick velocity and get tailored sensitivity & fire button scaling.")
                            .font(.system(size: 11))
                            .foregroundColor(SensiTheme.textSecondary)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                    }
                }
                .padding(14)
                .gamingCard(borderColor: SensiTheme.cyanAccent.opacity(0.4))
            }
            .buttonStyle(PlainButtonStyle())

            // Link 2: Sensi Bot Chat Bot
            Button(action: {
                let haptic = UIImpactFeedbackGenerator(style: .medium)
                haptic.impactOccurred()
                selectedTab = 3
            }) {
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(SensiTheme.rubyRed.opacity(0.18))
                            .frame(width: 44, height: 44)
                        Image(systemName: "cpu.fill")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(SensiTheme.rubyRed)
                    }

                    VStack(alignment: .leading, spacing: 3) {
                        HStack {
                            Text("SENSI BOT AI COACH")
                                .font(.system(size: 13, weight: .black, design: .monospaced))
                                .foregroundColor(.white)
                            Spacer()
                            Image(systemName: "arrow.right")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(SensiTheme.rubyRed)
                        }
                        Text("Chat with our AI weapon coach for custom DPI curves, weapon loadouts & drag flick advice.")
                            .font(.system(size: 11))
                            .foregroundColor(SensiTheme.textSecondary)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                    }
                }
                .padding(14)
                .gamingCard(borderColor: SensiTheme.rubyRed.opacity(0.35))
            }
            .buttonStyle(PlainButtonStyle())

            // Link 3: Floating Assistant
            Button(action: {
                let haptic = UIImpactFeedbackGenerator(style: .medium)
                haptic.impactOccurred()
                selectedTab = 2
            }) {
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(SensiTheme.fairPlayGreen.opacity(0.18))
                            .frame(width: 44, height: 44)
                        Image(systemName: "slider.horizontal.below.rectangle")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(SensiTheme.fairPlayGreen)
                    }

                    VStack(alignment: .leading, spacing: 3) {
                        HStack {
                            Text("FLOATING ASSISTANT")
                                .font(.system(size: 13, weight: .black, design: .monospaced))
                                .foregroundColor(.white)
                            Spacer()
                            Image(systemName: "arrow.right")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(SensiTheme.fairPlayGreen)
                        }
                        Text("Calibrate Red Criminal hitbox aim zones: Headshot magnetism, chest DPS & leg sweeps.")
                            .font(.system(size: 11))
                            .foregroundColor(SensiTheme.textSecondary)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                    }
                }
                .padding(14)
                .gamingCard(borderColor: SensiTheme.fairPlayGreen.opacity(0.35))
            }
            .buttonStyle(PlainButtonStyle())
        }
    }

    private func runCalculation(proxy: ScrollViewProxy) {
        isCalculating = true
        calculationStep = "Probing Apple Hardware..."

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            self.calculationStep = "Sampling Touch Polling..."
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
            self.calculationStep = "Normalizing \(Int(self.deviceProfile.ppi)) PPI Display..."
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            self.calculationStep = "Calibrating 0-200 Aim Curves..."
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            self.recommendation = IOSSensitivityEngine.recommend(
                profile: self.deviceProfile,
                touchHz: self.measuredTouchHz,
                signals: self.signals,
                fingers: self.fingers,
                learnedBias: FeedbackTuner.bias
            )
            self.isCalculating = false

            let haptic = UINotificationFeedbackGenerator()
            haptic.notificationOccurred(.success)

            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                withAnimation(.spring(response: 0.6, dampingFraction: 0.8)) {
                    proxy.scrollTo("recommended_settings_section", anchor: .top)
                }
            }
        }
    }
}
