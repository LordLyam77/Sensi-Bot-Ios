import SwiftUI

public struct LicenseGateView: View {
    @ObservedObject var licenseManager = LicenseManager.shared
    @State private var inputKey: String = ""
    @State private var errorMessage: String? = nil
    @State private var copiedHwid: Bool = false

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Spacer(minLength: 40)

                // 1. App Logo Badge
                ZStack {
                    Circle()
                        .fill(SensiTheme.rubyRed.opacity(0.12))
                        .frame(width: 86, height: 86)
                    Circle()
                        .stroke(SensiTheme.rubyRed.opacity(0.5), lineWidth: 1.5)
                        .frame(width: 86, height: 86)
                    SensiLogoView(size: 62)
                }

                // 2. Titles
                VStack(spacing: 8) {
                    HStack(spacing: 8) {
                        Text("SENSI BOT")
                            .font(.system(size: 26, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                        Text("PRO")
                            .font(.system(size: 12, weight: .black, design: .rounded))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 3)
                            .background(SensiTheme.rubyRed)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                        Text("iOS")
                            .font(.system(size: 12, weight: .black, design: .rounded))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 3)
                            .background(SensiTheme.cyanAccent.opacity(0.18))
                            .foregroundColor(SensiTheme.cyanAccent)
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                    Text("Device-Locked Anti-Tamper Licensing")
                        .font(.system(size: 13))
                        .foregroundColor(SensiTheme.textSecondary)
                }

                // 3. Error Banner
                if let err = errorMessage {
                    HStack(spacing: 10) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                        Text(err)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.white)
                        Spacer()
                    }
                    .padding(14)
                    .background(Color.red.opacity(0.18))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.red.opacity(0.5), lineWidth: 1)
                    )
                }

                // 4. Input Card
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("ENTER VIP ACCESS KEY")
                            .font(.system(size: 12, weight: .black, design: .monospaced))
                            .foregroundColor(SensiTheme.rubyRed)

                        TextField("SENSI-XXXX-XXXX-XXXX", text: $inputKey)
                            .font(.system(size: 16, weight: .bold, design: .monospaced))
                            .foregroundColor(.white)
                            .padding(14)
                            .background(Color.black.opacity(0.4))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(SensiTheme.glassBorder, lineWidth: 1)
                            )
                            .autocapitalization(.allCharacters)
                            .disableAutocorrection(true)
                    }

                    Button(action: handleActivation) {
                        HStack(spacing: 8) {
                            if licenseManager.isVerifying {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Image(systemName: "lock.open.fill")
                                Text("VALIDATE & UNLOCK")
                                    .font(.system(size: 14, weight: .black, design: .monospaced))
                            }
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(
                            LinearGradient(
                                colors: [SensiTheme.rubyRed, SensiTheme.rubyDark],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .disabled(licenseManager.isVerifying || inputKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
                .gamingCard(borderColor: SensiTheme.rubyRed.opacity(0.4))

                // 5. Hardware ID Display Card
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text("YOUR HARDWARE ID (HWID)")
                            .font(.system(size: 11, weight: .bold, design: .monospaced))
                            .foregroundColor(SensiTheme.textMuted)
                        Spacer()
                        Button(action: {
                            UIPasteboard.general.string = licenseManager.hardwareId
                            copiedHwid = true
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                                copiedHwid = false
                            }
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: copiedHwid ? "checkmark" : "doc.on.doc")
                                    .font(.system(size: 11))
                                Text(copiedHwid ? "COPIED" : "COPY HWID")
                                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                            }
                            .foregroundColor(copiedHwid ? SensiTheme.fairPlayGreen : SensiTheme.cyanAccent)
                        }
                    }

                    Text(licenseManager.hardwareId)
                        .font(.system(size: 13, weight: .semibold, design: .monospaced))
                        .foregroundColor(.white)
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.black.opacity(0.4))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(SensiTheme.glassBorder, lineWidth: 1)
                        )

                    Text("Keys are cryptographically locked to this device. For device migration or key purchases, copy your HWID.")
                        .font(.system(size: 11))
                        .foregroundColor(SensiTheme.textMuted)
                        .lineSpacing(2)
                }
                .gamingCard()

                // 6. Buy VIP Key Discord Link
                Button(action: {
                    if let url = URL(string: "https://discord.gg/6X8fUjeE2D") {
                        UIApplication.shared.open(url)
                    }
                }) {
                    HStack(spacing: 8) {
                        Image(systemName: "bubble.left.and.bubble.right.fill")
                        Text("NEED A VIP KEY? JOIN OUR DISCORD")
                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                    }
                    .foregroundColor(SensiTheme.cyanAccent)
                    .padding(.vertical, 10)
                }

                Spacer(minLength: 40)
            }
            .padding(.horizontal, 20)
        }
        .background(SensiTheme.voidBlack.ignoresSafeArea())
    }

    private func handleActivation() {
        errorMessage = nil
        Task {
            let (success, message) = await licenseManager.activateKey(inputKey)
            if !success {
                errorMessage = message
            }
        }
    }
}
