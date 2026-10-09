import SwiftUI

struct LicenseView: View {
    @ObservedObject var licenseManager = LicenseManager.shared
    @State private var inputKey: String = ""
    @State private var copiedHwid: Bool = false
    @State private var showingErrorAlert = false
    @State private var errorMessage = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header
                VStack(spacing: 8) {
                    HStack {
                        Image(systemName: "shield.lefthalf.filled.badge.checkmark")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(ColorTheme.rubyRed)
                        Text("HARDWARE VIP ACCESS")
                            .font(.system(size: 20, weight: .black, design: .monospaced))
                            .foregroundColor(ColorTheme.pureWhite)
                    }
                    Text("Device-Locked Anti-Tamper Licensing")
                        .font(.caption)
                        .foregroundColor(ColorTheme.textSecondary)
                }
                .padding(.top, 16)

                // Current License Status Card
                VStack(spacing: 16) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("LICENSE TIER")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(ColorTheme.textMuted)
                            Text(licenseManager.isVipActive ? "VIP LIFETIME ACCESS" : "STANDARD FREE PASS")
                                .font(.system(size: 16, weight: .black, design: .monospaced))
                                .foregroundColor(licenseManager.isVipActive ? ColorTheme.goldAccent : ColorTheme.pureWhite)
                        }
                        Spacer()
                        Text(licenseManager.isVipActive ? "ACTIVE" : "FREE")
                            .font(.system(size: 11, weight: .bold, design: .monospaced))
                            .foregroundColor(licenseManager.isVipActive ? ColorTheme.fairPlayGreen : ColorTheme.textMuted)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background((licenseManager.isVipActive ? ColorTheme.fairPlayGreen : ColorTheme.textMuted).opacity(0.15))
                            .cornerRadius(6)
                    }

                    Divider().background(ColorTheme.cardBorder)

                    // Hardware ID Section
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("YOUR HARDWARE ID (HWID)")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(ColorTheme.textMuted)
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
                                .foregroundColor(copiedHwid ? ColorTheme.fairPlayGreen : ColorTheme.cyanAccent)
                            }
                        }

                        Text(licenseManager.hardwareId)
                            .font(.system(size: 12, weight: .semibold, design: .monospaced))
                            .foregroundColor(ColorTheme.pureWhite)
                            .padding(10)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(ColorTheme.obsidianBlack)
                            .cornerRadius(8)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(ColorTheme.cardBorder, lineWidth: 1)
                            )
                    }
                }
                .modifier(ColorTheme.CardModifier(borderColor: licenseManager.isVipActive ? ColorTheme.goldAccent.opacity(0.4) : ColorTheme.cardBorder))

                // Key Activation Box
                VStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("ENTER VIP ACCESS KEY")
                            .font(.system(size: 11, weight: .bold, design: .monospaced))
                            .foregroundColor(ColorTheme.rubyRed)

                        TextField("SENSI-XXXX-XXXX-XXXX", text: $inputKey)
                            .font(.system(size: 15, weight: .bold, design: .monospaced))
                            .foregroundColor(.white)
                            .padding(12)
                            .background(ColorTheme.obsidianBlack)
                            .cornerRadius(8)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(ColorTheme.cardBorder, lineWidth: 1)
                            )
                            .autocapitalization(.allCharacters)
                            .disableAutocorrection(true)
                    }

                    Button(action: {
                        guard !inputKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
                        Task {
                            let (success, message) = await licenseManager.activateKey(inputKey)
                            if !success {
                                errorMessage = message
                                showingErrorAlert = true
                            }
                        }
                    }) {
                        HStack(spacing: 8) {
                            if licenseManager.isValidating {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Image(systemName: "key.fill")
                                Text("ACTIVATE LICENSE")
                                    .font(.system(size: 14, weight: .bold, design: .monospaced))
                            }
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
                    }
                    .disabled(licenseManager.isValidating || inputKey.isEmpty)
                }
                .modifier(ColorTheme.CardModifier())

                // Discord Purchase Card
                VStack(spacing: 12) {
                    HStack {
                        Image(systemName: "cart.fill")
                            .foregroundColor(ColorTheme.cyanAccent)
                        Text("GET A VIP LICENSE")
                            .font(.system(size: 13, weight: .bold, design: .monospaced))
                            .foregroundColor(ColorTheme.pureWhite)
                        Spacer()
                    }

                    Text("Get instant activation keys directly from our official Discord store. Keys are single-device locked for lifetime use.")
                        .font(.system(size: 12))
                        .foregroundColor(ColorTheme.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Button(action: {
                        if let url = URL(string: "https://discord.gg/6X8fUjeE2D") {
                            UIApplication.shared.open(url)
                        }
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "bubble.left.and.bubble.right.fill")
                            Text("JOIN DISCORD STORE")
                                .font(.system(size: 13, weight: .bold, design: .monospaced))
                        }
                        .foregroundColor(ColorTheme.obsidianBlack)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(ColorTheme.cyanAccent)
                        .cornerRadius(8)
                    }
                }
                .modifier(ColorTheme.CardModifier(borderColor: ColorTheme.cyanAccent.opacity(0.3)))

                Spacer(minLength: 30)
            }
            .padding(.horizontal, 16)
        }
        .background(ColorTheme.obsidianBlack.ignoresSafeArea())
        .alert(isPresented: $showingErrorAlert) {
            Alert(title: Text("Activation Failed"), message: Text(errorMessage), dismissButton: .default(Text("OK")))
        }
    }
}
