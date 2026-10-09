import SwiftUI
import AVKit

struct PipOverlayView: View {
    @ObservedObject var pipManager = PipOverlayManager.shared
    @State private var isPipActive = false
    @State private var showingFreeFireReminder = false

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header
                VStack(spacing: 8) {
                    HStack {
                        Image(systemName: "pip.enter")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(ColorTheme.rubyRed)
                        Text("FLOATING HUD")
                            .font(.system(size: 22, weight: .black, design: .monospaced))
                            .foregroundColor(ColorTheme.pureWhite)
                    }
                    Text("Picture-in-Picture Tactical In-Game Overlay")
                        .font(.caption)
                        .foregroundColor(ColorTheme.textSecondary)
                }
                .padding(.top, 16)

                // Explanation Banner
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Image(systemName: "info.circle.fill")
                            .foregroundColor(ColorTheme.cyanAccent)
                        Text("HOW FLOATING HUD WORKS ON IOS")
                            .font(.system(size: 13, weight: .bold, design: .monospaced))
                            .foregroundColor(ColorTheme.cyanAccent)
                    }
                    Text("Apple iOS does not allow raw screen overlay drawing over other apps. Sensi Bot Max uses the native **Picture-in-Picture (PiP)** engine to project a floating tactical HUD widget on top of Free Fire.")
                        .font(.system(size: 13))
                        .foregroundColor(ColorTheme.textSecondary)
                        .lineSpacing(4)
                }
                .modifier(ColorTheme.CardModifier(borderColor: ColorTheme.cyanAccent.opacity(0.3)))

                // PiP Status Card
                VStack(spacing: 16) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("OVERLAY STATUS")
                                .font(.system(size: 12, weight: .bold, design: .monospaced))
                                .foregroundColor(ColorTheme.textMuted)
                            Text(pipManager.isPipActive ? "RUNNING IN BACKGROUND" : "IDLE / STOPPED")
                                .font(.system(size: 16, weight: .black, design: .monospaced))
                                .foregroundColor(pipManager.isPipActive ? ColorTheme.fairPlayGreen : ColorTheme.textSecondary)
                        }
                        Spacer()
                        Circle()
                            .fill(pipManager.isPipActive ? ColorTheme.fairPlayGreen : ColorTheme.rubyRed)
                            .frame(width: 14, height: 14)
                            .shadow(color: pipManager.isPipActive ? ColorTheme.fairPlayGreen : ColorTheme.rubyRed, radius: 6)
                    }

                    Divider().background(ColorTheme.cardBorder)

                    Button(action: {
                        if pipManager.isPipActive {
                            pipManager.stopPip()
                        } else {
                            pipManager.startPip()
                        }
                    }) {
                        HStack(spacing: 12) {
                            Image(systemName: pipManager.isPipActive ? "stop.circle.fill" : "play.circle.fill")
                                .font(.title3)
                            Text(pipManager.isPipActive ? "STOP FLOATING HUD" : "LAUNCH IN-GAME HUD")
                                .font(.system(size: 15, weight: .bold, design: .monospaced))
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(
                            LinearGradient(
                                colors: pipManager.isPipActive ? [Color.gray, Color.black] : [ColorTheme.rubyRed, ColorTheme.rubyDark],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .cornerRadius(12)
                        .shadow(color: pipManager.isPipActive ? Color.clear : ColorTheme.rubyRed.opacity(0.5), radius: 8)
                    }
                }
                .modifier(ColorTheme.CardModifier(borderColor: pipManager.isPipActive ? ColorTheme.fairPlayGreen.opacity(0.4) : ColorTheme.cardBorder))

                // Tactical Tips for Free Fire on iOS
                VStack(alignment: .leading, spacing: 14) {
                    Text("TACTICAL IOS RECOMMENDATIONS")
                        .font(.system(size: 14, weight: .bold, design: .monospaced))
                        .foregroundColor(ColorTheme.goldAccent)

                    BulletRow(
                        icon: "lock.shield.fill",
                        title: "Enable Guided Access",
                        detail: "Settings > Accessibility > Guided Access. Prevents accidental Control Center & Home Bar gestures during intensive swipe drags."
                    )

                    BulletRow(
                        icon: "aspectratio.fill",
                        title: "Disable Display Zoom",
                        detail: "Settings > Display & Brightness > View: Standard. Keeps physical PPI touch coordinates 1:1."
                    )

                    BulletRow(
                        icon: "bolt.slash.fill",
                        title: "Keep Low Power Mode OFF",
                        detail: "Low Power Mode limits 120Hz ProMotion screens to 60Hz and cuts touch sampling rates."
                    )
                }
                .modifier(ColorTheme.CardModifier())

                Spacer(minLength: 30)
            }
            .padding(.horizontal, 16)
        }
        .background(ColorTheme.obsidianBlack.ignoresSafeArea())
    }
}

struct BulletRow: View {
    let icon: String
    let title: String
    let detail: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(ColorTheme.goldAccent)
                .frame(width: 20)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(ColorTheme.pureWhite)
                Text(detail)
                    .font(.system(size: 12))
                    .foregroundColor(ColorTheme.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}
