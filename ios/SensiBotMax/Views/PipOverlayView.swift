import SwiftUI
import AVKit

struct PipOverlayView: View {
    @ObservedObject var pipManager = PipOverlayManager.shared

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header
                VStack(spacing: 6) {
                    HStack {
                        Image(systemName: "pip.enter")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(SensiTheme.rubyRed)
                        Text("FLOATING HUD")
                            .font(.system(size: 22, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                    }
                    Text("Picture-in-Picture In-Game Crosshair & Stat Overlay")
                        .font(.system(size: 12))
                        .foregroundColor(SensiTheme.textSecondary)
                }
                .padding(.top, 16)

                // Inline Player View (Required by iOS to anchor PiP)
                VStack(spacing: 12) {
                    Text("TACTICAL HUD PREVIEW")
                        .font(.system(size: 11, weight: .black, design: .monospaced))
                        .foregroundColor(SensiTheme.cyanAccent)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    ZStack {
                        PipPlayerPreviewView()
                            .frame(height: 160)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(pipManager.isPipActive ? SensiTheme.fairPlayGreen : SensiTheme.rubyRed.opacity(0.6), lineWidth: 1.5)
                            )

                        if pipManager.isPipActive {
                            VStack(spacing: 4) {
                                Image(systemName: "pip.swap")
                                    .font(.system(size: 28))
                                    .foregroundColor(SensiTheme.fairPlayGreen)
                                Text("FLOATING OVER GAME")
                                    .font(.system(size: 12, weight: .black, design: .monospaced))
                                    .foregroundColor(.white)
                                Text("Switch to Free Fire now")
                                    .font(.system(size: 11))
                                    .foregroundColor(SensiTheme.textSecondary)
                            }
                            .padding(12)
                            .background(Color.black.opacity(0.75))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                        }
                    }

                    // Launch / Stop Button
                    Button(action: {
                        pipManager.togglePip()
                    }) {
                        HStack(spacing: 10) {
                            Image(systemName: pipManager.isPipActive ? "stop.circle.fill" : "play.circle.fill")
                                .font(.system(size: 18))
                            Text(pipManager.isPipActive ? "STOP FLOATING HUD" : "LAUNCH IN-GAME HUD")
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
                    }
                }
                .gamingCard(borderColor: pipManager.isPipActive ? SensiTheme.fairPlayGreen.opacity(0.5) : SensiTheme.glassBorder)

                // How it works advice
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Image(systemName: "info.circle.fill")
                            .foregroundColor(SensiTheme.cyanAccent)
                        Text("HOW FLOATING HUD WORKS ON IOS")
                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                            .foregroundColor(SensiTheme.cyanAccent)
                    }

                    Text("1. Tap **LAUNCH IN-GAME HUD** above.\n2. Switch into **Free Fire**.\n3. The HUD widget will remain floating in the corner of your screen throughout your match.\n4. You can drag and reposition the floating HUD anywhere on your display.")
                        .font(.system(size: 12))
                        .foregroundColor(SensiTheme.textSecondary)
                        .lineSpacing(4)
                }
                .gamingCard()

                // Guided Access & Pro Tips
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
        container.backgroundColor = .black
        if let playerLayer = PipOverlayManager.shared.playerLayer {
            playerLayer.frame = CGRect(x: 0, y: 0, width: 320, height: 160)
            container.layer.addSublayer(playerLayer)
        }
        return container
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        if let playerLayer = PipOverlayManager.shared.playerLayer {
            playerLayer.frame = uiView.bounds
        }
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
