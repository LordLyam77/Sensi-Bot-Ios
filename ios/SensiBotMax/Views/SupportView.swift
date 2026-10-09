import SwiftUI

struct SupportView: View {
    @State private var copiedDiagnostics = false
    private let profile = DeviceProbe.current()

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header
                VStack(spacing: 8) {
                    HStack {
                        Image(systemName: "headphones.circle.fill")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(ColorTheme.rubyRed)
                        Text("SUPPORT & COMMUNITY")
                            .font(.system(size: 20, weight: .black, design: .monospaced))
                            .foregroundColor(ColorTheme.pureWhite)
                    }
                    Text("Official Verification & Community Hub")
                        .font(.caption)
                        .foregroundColor(ColorTheme.textSecondary)
                }
                .padding(.top, 16)

                // Discord Banner
                VStack(spacing: 14) {
                    HStack {
                        Image(systemName: "person.3.sequence.fill")
                            .foregroundColor(ColorTheme.cyanAccent)
                        Text("OFFICIAL DISCORD COMMUNITY")
                            .font(.system(size: 13, weight: .bold, design: .monospaced))
                            .foregroundColor(ColorTheme.pureWhite)
                        Spacer()
                    }

                    Text("Join our verified VIP community for setup help, custom HUD layout presets, tournament scrim setups, and direct developer support.")
                        .font(.system(size: 12))
                        .foregroundColor(ColorTheme.textSecondary)
                        .lineSpacing(3)

                    Button(action: {
                        if let url = URL(string: "https://discord.gg/6X8fUjeE2D") {
                            UIApplication.shared.open(url)
                        }
                    }) {
                        HStack(spacing: 10) {
                            Image(systemName: "paperplane.fill")
                            Text("JOIN DISCORD SERVER")
                                .font(.system(size: 14, weight: .bold, design: .monospaced))
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(
                            LinearGradient(
                                colors: [ColorTheme.rubyRed, ColorTheme.rubyDark],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .cornerRadius(10)
                        .shadow(color: ColorTheme.rubyRed.opacity(0.4), radius: 8)
                    }
                }
                .modifier(ColorTheme.CardModifier(borderColor: ColorTheme.rubyRed.opacity(0.3)))

                // Fair Play & Anti-Ban Assurance
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Image(systemName: "checkmark.shield.fill")
                            .foregroundColor(ColorTheme.fairPlayGreen)
                        Text("100% SAFE • ZERO BAN RISK")
                            .font(.system(size: 13, weight: .bold, design: .monospaced))
                            .foregroundColor(ColorTheme.fairPlayGreen)
                    }

                    Text("Sensi Bot Max is a pure mathematical calculation engine and hardware probe. It does **NOT** inject code, alter IPA binaries, read game memory, or modify system files. All recommended values are manually set in Free Fire's native settings.")
                        .font(.system(size: 12))
                        .foregroundColor(ColorTheme.textSecondary)
                        .lineSpacing(4)
                }
                .modifier(ColorTheme.CardModifier(borderColor: ColorTheme.fairPlayGreen.opacity(0.3)))

                // Device Diagnostics Export
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("DEVICE DIAGNOSTICS")
                            .font(.system(size: 13, weight: .bold, design: .monospaced))
                            .foregroundColor(ColorTheme.textMuted)
                        Spacer()
                        Button(action: {
                            let diag = generateDiagnosticsText()
                            UIPasteboard.general.string = diag
                            copiedDiagnostics = true
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                                copiedDiagnostics = false
                            }
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: copiedDiagnostics ? "checkmark" : "doc.on.doc")
                                    .font(.system(size: 11))
                                Text(copiedDiagnostics ? "COPIED" : "COPY INFO")
                                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                            }
                            .foregroundColor(copiedDiagnostics ? ColorTheme.fairPlayGreen : ColorTheme.cyanAccent)
                        }
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        DiagLine(label: "Identifier", value: profile.identifier)
                        DiagLine(label: "Retina PPI", value: "\(Int(profile.ppi)) PPI")
                        DiagLine(label: "Physical Width", value: String(format: "%.2f inches", profile.widthInches))
                        DiagLine(label: "ProMotion", value: "\(profile.maxFPS) FPS")
                        DiagLine(label: "Gyroscope", value: profile.hasGyro ? "AVAILABLE" : "UNAVAILABLE")
                        DiagLine(label: "Learned Bias", value: String(format: "%+.3f", FeedbackTuner.bias))
                    }
                    .padding(10)
                    .background(ColorTheme.obsidianBlack)
                    .cornerRadius(8)
                }
                .modifier(ColorTheme.CardModifier())

                // FAQ Section
                VStack(alignment: .leading, spacing: 14) {
                    Text("FREQUENTLY ASKED QUESTIONS")
                        .font(.system(size: 13, weight: .bold, design: .monospaced))
                        .foregroundColor(ColorTheme.goldAccent)

                    FaqItem(
                        q: "Can I change DPI on iPhone?",
                        a: "No. Apple iOS does not allow changing the screen DPI. Sensi Bot Max mathematically scales Free Fire's native 0-200 sensitivity values to compensate for Apple's high-density Retina displays."
                    )

                    FaqItem(
                        q: "Why does 120Hz ProMotion feel different?",
                        a: "120Hz displays poll touches at much higher frequencies. This eliminates input lag, meaning you need slightly tighter sensitivity to avoid flying past enemy heads."
                    )
                }
                .modifier(ColorTheme.CardModifier())

                Spacer(minLength: 30)
            }
            .padding(.horizontal, 16)
        }
        .background(ColorTheme.obsidianBlack.ignoresSafeArea())
    }

    private func generateDiagnosticsText() -> String {
        """
        --- SENSI BOT MAX IOS DIAGNOSTICS ---
        Device ID: \(profile.identifier)
        PPI: \(Int(profile.ppi))
        Width: \(String(format: "%.2f", profile.widthInches)) in
        Max FPS: \(profile.maxFPS)
        Gyro: \(profile.hasGyro)
        Zoomed: \(profile.displayZoomed)
        Low Power: \(profile.lowPowerMode)
        Thermal Throttling: \(profile.thermalHot)
        Learned Bias: \(String(format: "%+.3f", FeedbackTuner.bias))
        HWID: \(LicenseManager.shared.hardwareId)
        -------------------------------------
        """
    }
}

struct DiagLine: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .font(.system(size: 11, design: .monospaced))
                .foregroundColor(ColorTheme.textMuted)
            Spacer()
            Text(value)
                .font(.system(size: 11, weight: .bold, design: .monospaced))
                .foregroundColor(ColorTheme.pureWhite)
        }
    }
}

struct FaqItem: View {
    let q: String
    let a: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(q)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(ColorTheme.pureWhite)
            Text(a)
                .font(.system(size: 12))
                .foregroundColor(ColorTheme.textSecondary)
                .lineSpacing(2)
        }
    }
}
