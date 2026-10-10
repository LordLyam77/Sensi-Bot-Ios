import SwiftUI

public struct ProfileView: View {
    @ObservedObject var licenseManager = LicenseManager.shared
    @State private var copiedHwid = false
    @State private var showingDeactivateAlert = false
    private let profile = DeviceProbe.current()

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header
                VStack(spacing: 6) {
                    HStack {
                        Image(systemName: "person.crop.circle.fill")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(SensiTheme.rubyRed)
                        Text("PROFILE & VIP PASS")
                            .font(.system(size: 20, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                    }
                    Text("Device Licensing, Fair Play & System Status")
                        .font(.caption)
                        .foregroundColor(SensiTheme.textSecondary)
                }
                .padding(.top, 16)

                // VIP License Badge Card
                VStack(spacing: 16) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("LICENSE TIER")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(SensiTheme.textMuted)
                            Text(licenseManager.isActivated ? "VIP LIFETIME ACCESS" : "AUTHENTICATING")
                                .font(.system(size: 16, weight: .black, design: .monospaced))
                                .foregroundColor(licenseManager.isActivated ? SensiTheme.goldAccent : .white)
                        }
                        Spacer()
                        Text(licenseManager.isActivated ? "ACTIVE" : "FREE")
                            .font(.system(size: 11, weight: .bold, design: .monospaced))
                            .foregroundColor(licenseManager.isActivated ? SensiTheme.fairPlayGreen : SensiTheme.textMuted)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background((licenseManager.isActivated ? SensiTheme.fairPlayGreen : SensiTheme.textMuted).opacity(0.15))
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                    }

                    Divider().background(SensiTheme.glassBorder)

                    // Active Key Display
                    if !licenseManager.activeKey.isEmpty {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("BOUND VIP KEY")
                                .font(.system(size: 10, weight: .bold, design: .monospaced))
                                .foregroundColor(SensiTheme.textMuted)
                            Text(licenseManager.activeKey)
                                .font(.system(size: 13, weight: .bold, design: .monospaced))
                                .foregroundColor(SensiTheme.cyanAccent)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    // Hardware ID Section
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("DEVICE HWID (AUTHENTICATED)")
                                .font(.system(size: 10, weight: .bold, design: .monospaced))
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
                            .font(.system(size: 12, weight: .semibold, design: .monospaced))
                            .foregroundColor(.white)
                            .padding(10)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.black.opacity(0.4))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(SensiTheme.glassBorder, lineWidth: 1)
                            )
                    }
                }
                .gamingCard(borderColor: licenseManager.isActivated ? SensiTheme.goldAccent.opacity(0.4) : SensiTheme.glassBorder)

                // Fair Play & 100% Anti-Ban Section
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Image(systemName: "checkmark.shield.fill")
                            .foregroundColor(SensiTheme.fairPlayGreen)
                        Text("FAIR PLAY & ZERO BAN GUARANTEE")
                            .font(.system(size: 12, weight: .black, design: .monospaced))
                            .foregroundColor(SensiTheme.fairPlayGreen)
                    }

                    Text("Sensi Bot Max uses **pure mathematical screen physics** and standard iOS Picture-in-Picture. It does **not** inject game code, read memory, alter IPA packages, or modify system files. 100% safe for rank, custom rooms, and official tournament play.")
                        .font(.system(size: 12))
                        .foregroundColor(SensiTheme.textSecondary)
                        .lineSpacing(3)
                }
                .gamingCard(borderColor: SensiTheme.fairPlayGreen.opacity(0.3))

                // Device Hardware Info
                VStack(alignment: .leading, spacing: 10) {
                    Text("DEVICE HARDWARE PROFILE")
                        .font(.system(size: 12, weight: .black, design: .monospaced))
                        .foregroundColor(SensiTheme.textMuted)

                    VStack(spacing: 8) {
                        ProfileRow(label: "Device Model", value: profile.modelMarketingName)
                        ProfileRow(label: "App Version", value: "v1.0.4 (Build 5)")
                        ProfileRow(label: "Hardware ID", value: profile.identifier)
                        ProfileRow(label: "Retina Density", value: "\(Int(profile.ppi)) PPI")
                        ProfileRow(label: "Refresh Rate", value: "\(profile.maxFPS) Hz")
                        ProfileRow(label: "Screen Short Width", value: String(format: "%.2f inches", profile.widthInches))
                        ProfileRow(label: "Gyroscope", value: profile.hasGyro ? "AVAILABLE" : "UNAVAILABLE")
                    }
                    .padding(12)
                    .background(Color.black.opacity(0.3))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                .gamingCard()

                // Discord Community Store
                VStack(spacing: 12) {
                    HStack {
                        Image(systemName: "cart.fill")
                            .foregroundColor(SensiTheme.cyanAccent)
                        Text("OFFICIAL DISCORD STORE")
                            .font(.system(size: 13, weight: .bold, design: .monospaced))
                            .foregroundColor(.white)
                        Spacer()
                    }

                    Text("Join our verified VIP community for tournament HUD setups, device migrations, and direct 24/7 developer assistance.")
                        .font(.system(size: 12))
                        .foregroundColor(SensiTheme.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Button(action: {
                        if let url = URL(string: "https://discord.gg/6X8fUjeE2D") {
                            UIApplication.shared.open(url)
                        }
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "bubble.left.and.bubble.right.fill")
                            Text("OPEN DISCORD COMMUNITY")
                                .font(.system(size: 13, weight: .black, design: .monospaced))
                        }
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(SensiTheme.cyanAccent)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
                .gamingCard(borderColor: SensiTheme.cyanAccent.opacity(0.3))

                // Deactivate Option
                Button(action: {
                    showingDeactivateAlert = true
                }) {
                    Text("DEACTIVATE CURRENT LICENSE")
                        .font(.system(size: 12, weight: .bold, design: .monospaced))
                        .foregroundColor(Color.red.opacity(0.8))
                        .padding(.vertical, 8)
                }
                .alert(isPresented: $showingDeactivateAlert) {
                    Alert(
                        title: Text("Deactivate License?"),
                        message: Text("This will remove your saved license key and lock the app until re-entered."),
                        primaryButton: .destructive(Text("Deactivate")) {
                            licenseManager.deactivate()
                        },
                        secondaryButton: .cancel()
                    )
                }

                Spacer(minLength: 30)
            }
            .padding(.horizontal, 16)
        }
        .background(SensiTheme.voidBlack.ignoresSafeArea())
    }
}

struct ProfileRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .font(.system(size: 11, design: .monospaced))
                .foregroundColor(SensiTheme.textMuted)
            Spacer()
            Text(value)
                .font(.system(size: 11, weight: .bold, design: .monospaced))
                .foregroundColor(.white)
        }
    }
}
