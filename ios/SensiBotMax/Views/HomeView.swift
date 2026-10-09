import SwiftUI

public struct HomeView: View {
    @State private var deviceProfile: IOSDeviceProfile = DeviceProbe.current()
    @State private var measuredTouchHz: Double? = nil
    @State private var signals: PlayerSignals = PlayerSignals()
    @State private var fingers: Int = 2
    @State private var currentBias: Double = FeedbackTuner.bias
    @State private var recommendation: IOSRecommendation? = nil
    @State private var isCalculating: Bool = false
    @State private var calculationStep: String = "Optimizing..."

    public init() {}

    public var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(spacing: 18) {
                    // 1. App Branding Header
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

                    // 2. Hardware Device Profile Card
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

                    // 3. Hardware Touch Rate Probe
                    TouchRateProbeCard(measuredTouchHz: $measuredTouchHz)

                    // 4. "GET MY SETTINGS" Action Button
                    if isCalculating {
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
                    } else {
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
                            .padding(.vertical, 15)
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

                    // 5. Recommended Settings Display (With Anchor ID for Auto-Scroll)
                    if let rec = recommendation {
                        VStack(alignment: .leading, spacing: 16) {
                            SensitivityDisplayCard(recommendation: rec, deviceProfile: deviceProfile)
                                .id("recommended_settings_section")

                            // 6. Post-Practice Feedback Tuner
                            FeedbackTunerCard(currentBias: $currentBias) {
                                // Re-run calculation with updated learned bias
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

                    Spacer(minLength: 30)
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }
            .background(SensiTheme.voidBlack.ignoresSafeArea())
        }
    }

    private func runCalculation(proxy: ScrollViewProxy) {
        isCalculating = true
        calculationStep = "Probing Apple Hardware..."

        // Step animation simulation matching the Android UX
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
            // Compute real output
            self.recommendation = IOSSensitivityEngine.recommend(
                profile: self.deviceProfile,
                touchHz: self.measuredTouchHz,
                signals: self.signals,
                fingers: self.fingers,
                learnedBias: FeedbackTuner.bias
            )
            self.isCalculating = false

            // Auto-scroll directly to the generated settings
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                withAnimation(.spring(response: 0.6, dampingFraction: 0.8)) {
                    proxy.scrollTo("recommended_settings_section", anchor: .top)
                }
            }
        }
    }
}
