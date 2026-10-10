import SwiftUI
import AVKit

public typealias PipOverlayView = FloatingAssistantView

public struct FloatingAssistantView: View {
    @ObservedObject var pipManager = PipOverlayManager.shared
    @ObservedObject var menuManager = FloatingMenuManager.shared

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header
                VStack(spacing: 6) {
                    HStack {
                        Image(systemName: "slider.horizontal.below.rectangle")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(SensiTheme.rubyRed)
                        Text("FLOATING ASSISTANT")
                            .font(.system(size: 22, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                    }
                    Text("Real-Time In-Game Tactical Sensitivity Assistant • Fair Play")
                        .font(.system(size: 12))
                        .foregroundColor(SensiTheme.textSecondary)
                }
                .padding(.top, 16)

                // ═════════════════════════════════════════════════════════════════════
                //  1. FLOAT OVER FREE FIRE (IN-GAME FLOATING ASSISTANT)
                // ═════════════════════════════════════════════════════════════════════
                VStack(spacing: 14) {
                    HStack {
                        Image(systemName: "scope")
                            .foregroundColor(SensiTheme.rubyRed)
                        Text("FLOAT OVER FREE FIRE (IN-GAME)")
                            .font(.system(size: 12, weight: .black, design: .monospaced))
                            .foregroundColor(.white)
                        Spacer()
                        HStack(spacing: 4) {
                            Circle()
                                .fill(pipManager.isPipActive ? SensiTheme.fairPlayGreen : SensiTheme.cyanAccent)
                                .frame(width: 7, height: 7)
                            Text(pipManager.isPipActive ? "ACTIVE OVER GAME" : "READY - STANDBY")
                                .font(.system(size: 10, weight: .bold, design: .monospaced))
                                .foregroundColor(pipManager.isPipActive ? SensiTheme.fairPlayGreen : SensiTheme.cyanAccent)
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background((pipManager.isPipActive ? SensiTheme.fairPlayGreen : SensiTheme.cyanAccent).opacity(0.12))
                        .clipShape(RoundedRectangle(cornerRadius: 6))
                    }

                    // Tactical HUD Status Card
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            VStack(alignment: .leading, spacing: 3) {
                                Text("AIM LOCK TARGET")
                                    .font(.system(size: 9, weight: .bold, design: .monospaced))
                                    .foregroundColor(SensiTheme.textMuted)
                                Text(menuManager.activeZone.title)
                                    .font(.system(size: 13, weight: .black, design: .monospaced))
                                    .foregroundColor(menuManager.activeZone.color)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 3) {
                                Text("REFRESH RATE")
                                    .font(.system(size: 9, weight: .bold, design: .monospaced))
                                    .foregroundColor(SensiTheme.textMuted)
                                Text("\(Int(DeviceProbe.current().maxFPS))Hz PROMOTION")
                                    .font(.system(size: 13, weight: .black, design: .monospaced))
                                    .foregroundColor(SensiTheme.cyanAccent)
                            }
                        }

                        Divider().background(Color.white.opacity(0.1))

                        HStack(spacing: 12) {
                            HudMetricChip(label: "GEN", val: "\(Int(menuManager.generalSensi))")
                            HudMetricChip(label: "RED", val: "\(Int(menuManager.redDotSensi))")
                            HudMetricChip(label: "2X", val: "\(Int(menuManager.scope2xSensi))")
                            HudMetricChip(label: "4X", val: "\(Int(menuManager.scope4xSensi))")
                            HudMetricChip(label: "BTN", val: "\(Int(menuManager.fireButtonSize))%")
                        }

                        Text("⚡ When active, your Floating Assistant stays smoothly pinned on screen over Free Fire whenever you minimize or switch outside this app.")
                            .font(.system(size: 10.5))
                            .foregroundColor(SensiTheme.textSecondary)
                            .lineSpacing(2)
                    }
                    .padding(12)
                    .background(Color.black.opacity(0.35))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.08), lineWidth: 1))

                    // Launch / Stop In-Game Assistant Button
                    Button(action: {
                        let haptic = UIImpactFeedbackGenerator(style: .medium)
                        haptic.impactOccurred()
                        pipManager.togglePip()
                    }) {
                        HStack(spacing: 10) {
                            Image(systemName: pipManager.isPipActive ? "stop.circle.fill" : "play.circle.fill")
                                .font(.system(size: 18))
                            Text(pipManager.isPipActive ? "STOP IN-GAME ASSISTANT" : "ACTIVATE IN-GAME FLOATING ASSISTANT")
                                .font(.system(size: 12, weight: .black, design: .monospaced))
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(
                            LinearGradient(
                                colors: pipManager.isPipActive ? [Color.gray, Color.black] : [SensiTheme.rubyRed, SensiTheme.rubyDark],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .shadow(color: (pipManager.isPipActive ? Color.clear : SensiTheme.rubyRed.opacity(0.4)), radius: 8, x: 0, y: 3)
                    }
                }
                .gamingCard(borderColor: pipManager.isPipActive ? SensiTheme.fairPlayGreen.opacity(0.6) : SensiTheme.rubyRed.opacity(0.5))

                // ═════════════════════════════════════════════════════════════════════
                //  2. FLOATING ASSISTANT LAUNCHER (In-App Draggable Widget)
                // ═════════════════════════════════════════════════════════════════════
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        Image(systemName: "app.badge.checkmark")
                            .foregroundColor(SensiTheme.rubyRed)
                        Text("ON-SCREEN FLOATING LAUNCHER")
                            .font(.system(size: 13, weight: .black, design: .monospaced))
                            .foregroundColor(.white)
                        Spacer()
                        Toggle("", isOn: $menuManager.isMenuEnabled)
                            .labelsHidden()
                            .toggleStyle(SwitchToggleStyle(tint: SensiTheme.rubyRed))
                    }

                    Text("Draggable floating launcher with edge-snapping physics. Tap anytime inside the app to expand live sensitivity sliders, character target zones, and reticle customizers.")
                        .font(.system(size: 11))
                        .foregroundColor(SensiTheme.textSecondary)
                        .lineSpacing(2)

                    HStack(spacing: 10) {
                        Button(action: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                                menuManager.isMenuEnabled = true
                                menuManager.isExpanded = true
                            }
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "arrow.up.left.and.arrow.down.right")
                                Text("OPEN ASSISTANT PANEL")
                                    .font(.system(size: 11, weight: .black, design: .monospaced))
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(SensiTheme.rubyRed)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        }

                        Button(action: {
                            menuManager.savePosition(x: 320, y: 220)
                            let haptic = UIImpactFeedbackGenerator(style: .light)
                            haptic.impactOccurred()
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "arrow.counterclockwise")
                                Text("RESET POSITION")
                                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                            }
                            .foregroundColor(SensiTheme.cyanAccent)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color.white.opacity(0.08))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                    }

                    // Status pill
                    HStack {
                        Circle()
                            .fill(menuManager.isMenuEnabled ? Color(red: 0.0, green: 0.9, blue: 0.46) : Color.gray)
                            .frame(width: 8, height: 8)
                        Text(menuManager.isMenuEnabled ? "LAUNCHER PINNED ON SCREEN" : "LAUNCHER HIDDEN")
                            .font(.system(size: 10, weight: .bold, design: .monospaced))
                            .foregroundColor(menuManager.isMenuEnabled ? Color(red: 0.0, green: 0.9, blue: 0.46) : SensiTheme.textMuted)
                        Spacer()
                        Text("DRAG & SNAP TO EDGE")
                            .font(.system(size: 9, weight: .bold, design: .monospaced))
                            .foregroundColor(SensiTheme.goldAccent)
                    }
                    .padding(8)
                    .background(Color.black.opacity(0.3))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                }
                .gamingCard(borderColor: menuManager.isMenuEnabled ? SensiTheme.rubyRed.opacity(0.6) : SensiTheme.glassBorder)

                // ═════════════════════════════════════════════════════════════════════
                //  3. TACTICAL RECOMMENDATIONS (FAIR PLAY & REACTION)
                // ═════════════════════════════════════════════════════════════════════
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Image(systemName: "checkmark.seal.fill")
                            .foregroundColor(SensiTheme.fairPlayGreen)
                        Text("100% FAIR PLAY • ANTI-BAN COMPLIANT")
                            .font(.system(size: 12, weight: .black, design: .monospaced))
                            .foregroundColor(SensiTheme.fairPlayGreen)
                    }

                    Text("Sensi Bot Max uses zero game file injection or memory alteration. All sensitivity curves and crosshairs operate strictly as an assistive tactical HUD.")
                        .font(.system(size: 10.5))
                        .foregroundColor(SensiTheme.textSecondary)

                    Divider().background(Color.white.opacity(0.1))

                    Text("TACTICAL IOS RECOMMENDATIONS")
                        .font(.system(size: 12, weight: .black, design: .monospaced))
                        .foregroundColor(SensiTheme.goldAccent)

                    BulletRow(
                        icon: "lock.shield.fill",
                        title: "Enable Guided Access",
                        detail: "Settings > Accessibility > Guided Access. Blocks home bar / control center swipes during upward drag flicks."
                    )

                    BulletRow(
                        icon: "aspectratio.fill",
                        title: "Standard Display Zoom",
                        detail: "Settings > Display & Brightness > View: Standard for 1:1 physical PPI coordinate mapping."
                    )

                    BulletRow(
                        icon: "bolt.slash.fill",
                        title: "Turn Off Low Power Mode",
                        detail: "Low Power Mode limits ProMotion 120Hz screens to 60Hz and slows touch sampling."
                    )
                }
                .gamingCard()

                Spacer(minLength: 30)
            }
            .padding(.horizontal, 16)
        }
        .background(SensiTheme.voidBlack.ignoresSafeArea())
    }
}

struct HudMetricChip: View {
    let label: String
    let val: String

    var body: some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.system(size: 8, weight: .bold, design: .monospaced))
                .foregroundColor(SensiTheme.textMuted)
            Text(val)
                .font(.system(size: 11, weight: .black, design: .monospaced))
                .foregroundColor(.white)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 6)
        .background(Color.white.opacity(0.06))
        .clipShape(RoundedRectangle(cornerRadius: 6))
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
                .font(.system(size: 14))
                .frame(width: 20)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.white)
                Text(detail)
                    .font(.system(size: 11))
                    .foregroundColor(SensiTheme.textSecondary)
            }
        }
    }
}
