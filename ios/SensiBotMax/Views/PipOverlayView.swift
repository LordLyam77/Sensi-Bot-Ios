import SwiftUI
import AVKit

struct PipOverlayView: View {
    @ObservedObject var pipManager = PipOverlayManager.shared
    @ObservedObject var menuManager = FloatingMenuManager.shared

    var body: some View {
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
                    Text("iOSGods-Style In-Game Tactical Mod Menu & Floating HUD")
                        .font(.system(size: 12))
                        .foregroundColor(SensiTheme.textSecondary)
                }
                .padding(.top, 16)

                // ═════════════════════════════════════════════════════════════════════
                //  1. FLOATING MOD MENU LAUNCHER (iOSGods-style Draggable Menu)
                // ═════════════════════════════════════════════════════════════════════
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        Image(systemName: "app.badge.checkmark")
                            .foregroundColor(SensiTheme.rubyRed)
                        Text("FLOATING MOD MENU")
                            .font(.system(size: 13, weight: .black, design: .monospaced))
                            .foregroundColor(.white)
                        Spacer()
                        Toggle("", isOn: $menuManager.isMenuEnabled)
                            .labelsHidden()
                            .toggleStyle(SwitchToggleStyle(tint: SensiTheme.rubyRed))
                    }

                    Text("A draggable floating launcher that snaps smoothly to your screen edges. Tap it anytime to expand the custom iOSGods-style mod menu panel with live sensitivity sliders, character target zones, and crosshairs.")
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
                                Text("OPEN MOD MENU")
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
                        Text(menuManager.isMenuEnabled ? "LAUNCHER FLOATING ON SCREEN" : "LAUNCHER HIDDEN")
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
                //  2. IN-GAME PiP FLOATING OVERLAY (Picture-in-Picture)
                // ═════════════════════════════════════════════════════════════════════
                VStack(spacing: 12) {
                    HStack {
                        Image(systemName: "pip.enter")
                            .foregroundColor(SensiTheme.cyanAccent)
                        Text("PICTURE-IN-PICTURE (OVER FREE FIRE)")
                            .font(.system(size: 11, weight: .black, design: .monospaced))
                            .foregroundColor(SensiTheme.cyanAccent)
                        Spacer()
                        Text(pipManager.statusMessage.uppercased())
                            .font(.system(size: 10, weight: .bold, design: .monospaced))
                            .foregroundColor(pipManager.isPipActive ? SensiTheme.fairPlayGreen : SensiTheme.goldAccent)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background((pipManager.isPipActive ? SensiTheme.fairPlayGreen : SensiTheme.goldAccent).opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: 4))
                    }

                    ZStack {
                        PipPlayerPreviewView()
                            .frame(height: 160)
                            .clipShape(RoundedRectangle(cornerRadius: 14))

                        if pipManager.isPipActive {
                            VStack(spacing: 6) {
                                Image(systemName: "pip.swap")
                                    .font(.system(size: 28))
                                    .foregroundColor(SensiTheme.fairPlayGreen)
                                Text("HUD FLOATING OVER GAME")
                                    .font(.system(size: 12, weight: .black, design: .monospaced))
                                    .foregroundColor(.white)
                                Text("Switch to Free Fire now — widget stays on screen")
                                    .font(.system(size: 11))
                                    .foregroundColor(SensiTheme.textSecondary)
                            }
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .background(Color.black.opacity(0.78))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                    }

                    // Launch / Stop Button
                    Button(action: {
                        pipManager.togglePip()
                    }) {
                        HStack(spacing: 10) {
                            Image(systemName: pipManager.isPipActive ? "stop.circle.fill" : "play.circle.fill")
                                .font(.system(size: 18))
                            Text(pipManager.isPipActive ? "STOP IN-GAME HUD" : "LAUNCH HUD OVER GAME")
                                .font(.system(size: 14, weight: .black, design: .monospaced))
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
                .gamingCard(borderColor: pipManager.isPipActive ? SensiTheme.fairPlayGreen.opacity(0.5) : SensiTheme.cyanAccent.opacity(0.4))

                // ═════════════════════════════════════════════════════════════════════
                //  3. TACTICAL RECOMMENDATIONS
                // ═════════════════════════════════════════════════════════════════════
                VStack(alignment: .leading, spacing: 12) {
                    Text("TACTICAL IOS RECOMMENDATIONS")
                        .font(.system(size: 13, weight: .black, design: .monospaced))
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

struct PipPlayerPreviewView: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let container = UIView()
        container.backgroundColor = .clear

        let hud = FloatingHudView(frame: .zero)
        container.addSubview(hud)
        hud.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            hud.topAnchor.constraint(equalTo: container.topAnchor),
            hud.bottomAnchor.constraint(equalTo: container.bottomAnchor),
            hud.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            hud.trailingAnchor.constraint(equalTo: container.trailingAnchor)
        ])

        DispatchQueue.main.async {
            PipOverlayManager.shared.attachSourceView(container)
        }

        return container
    }

    func updateUIView(_ uiView: UIView, context: Context) {}
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
