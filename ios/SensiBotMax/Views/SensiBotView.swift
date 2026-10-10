import SwiftUI

public struct SensiBotView: View {
    @State private var messages: [ChatMessage] = []
    @State private var inputText: String = ""
    private let deviceProfile = DeviceProbe.current()

    private let suggestions = [
        "M1887 One-Tap",
        "White444 Settings",
        "Best iPhone Settings",
        "iPhone DPI Guide",
        "Aim Flies Over Head",
        "Aim Stuck on Chest",
        "Raistar Speed",
        "Best Fire Button Size"
    ]

    public init() {}

    public var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack(spacing: 12) {
                SensiLogoView(size: 30)

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text("SENSI BOT")
                            .font(.system(size: 20, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                        Text("AI COACH")
                            .font(.system(size: 10, weight: .black, design: .rounded))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(SensiTheme.rubyRed)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 4))
                    }
                    Text("Free Fire Neural Drag & Weapon Assistant")
                        .font(.system(size: 11))
                        .foregroundColor(SensiTheme.textSecondary)
                }
                Spacer()
                Image(systemName: "cpu.fill")
                    .font(.system(size: 20))
                    .foregroundColor(SensiTheme.cyanAccent)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(SensiTheme.surfaceElevated)

            Divider().background(SensiTheme.glassBorder)

            // Suggestions Carousel
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(suggestions, id: \.self) { sug in
                        Button(action: {
                            sendMessage(text: sug)
                        }) {
                            Text(sug)
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(SensiTheme.textPrimary)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(SensiTheme.surface)
                                .clipShape(Capsule())
                                .overlay(Capsule().stroke(SensiTheme.glassBorder, lineWidth: 1))
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
            .background(Color.black.opacity(0.3))

            // Messages List
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 14) {
                        if messages.isEmpty {
                            VStack(spacing: 12) {
                                Image(systemName: "bubble.left.and.bubble.right.fill")
                                    .font(.system(size: 40))
                                    .foregroundColor(SensiTheme.rubyRed.opacity(0.6))
                                Text("Ask SensiBot anything about Free Fire!")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(.white)
                                Text("Ask for weapon sensitivities, one-tap drag techniques, or tap a suggestion above.")
                                    .font(.system(size: 12))
                                    .foregroundColor(SensiTheme.textSecondary)
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 24)
                            }
                            .padding(.top, 40)
                        }

                        ForEach(messages) { msg in
                            MessageRow(message: msg)
                                .id(msg.id)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                }
                .onChange(of: messages.count) { _ in
                    if let last = messages.last {
                        withAnimation {
                            proxy.scrollTo(last.id, anchor: .bottom)
                        }
                    }
                }
            }

            // Input Bar
            HStack(spacing: 10) {
                TextField("Ask about weapons, drag, headshots...", text: $inputText)
                    .font(.system(size: 14))
                    .foregroundColor(.white)
                    .padding(12)
                    .background(SensiTheme.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(SensiTheme.glassBorder, lineWidth: 1)
                    )

                Button(action: {
                    guard !inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
                    sendMessage(text: inputText)
                    inputText = ""
                }) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.system(size: 32))
                        .foregroundColor(inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? SensiTheme.textMuted : SensiTheme.rubyRed)
                }
                .disabled(inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(SensiTheme.surfaceElevated)
        }
        .background(SensiTheme.voidBlack.ignoresSafeArea())
    }

    private func sendMessage(text: String) {
        let userMsg = ChatMessage(isUser: true, text: text)
        messages.append(userMsg)

        // Bot response
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            let botReply = SensiChatEngine.shared.respond(to: text, deviceProfile: deviceProfile)
            messages.append(botReply)
        }
    }
}

struct MessageRow: View {
    let message: ChatMessage

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            if message.isUser {
                Spacer()
                Text(message.text)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(.white)
                    .padding(12)
                    .background(
                        LinearGradient(
                            colors: [SensiTheme.rubyRed, SensiTheme.rubyDark],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            } else {
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 6) {
                        Image(systemName: "cpu.fill")
                            .font(.system(size: 12))
                            .foregroundColor(SensiTheme.rubyRed)
                        Text("SENSI BOT")
                            .font(.system(size: 10, weight: .black, design: .monospaced))
                            .foregroundColor(SensiTheme.rubyRed)
                    }

                    Text(message.text)
                        .font(.system(size: 13))
                        .foregroundColor(SensiTheme.textPrimary)
                        .lineSpacing(3)

                    if let rec = message.recommendation {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(rec.weapon.uppercased())
                                .font(.system(size: 11, weight: .black, design: .monospaced))
                                .foregroundColor(SensiTheme.cyanAccent)

                            HStack(spacing: 12) {
                                MiniStat(title: "GENERAL", value: "\(rec.general)")
                                MiniStat(title: "RED DOT", value: "\(rec.redDot)")
                                MiniStat(title: "2X", value: "\(rec.scope2x)")
                                MiniStat(title: "4X", value: "\(rec.scope4x)")
                                MiniStat(title: "FIRE BTN", value: "\(rec.fireButton)%")
                            }

                            Divider().background(Color.white.opacity(0.1))

                            Text("🎯 Drag: \(rec.dragTechnique)")
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(SensiTheme.goldAccent)

                            Text("💡 Advice: \(rec.advice)")
                                .font(.system(size: 11))
                                .foregroundColor(SensiTheme.textSecondary)
                        }
                        .padding(12)
                        .background(Color.black.opacity(0.4))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(SensiTheme.cyanAccent.opacity(0.3), lineWidth: 1)
                        )
                    }
                }
                .padding(14)
                .background(SensiTheme.surface)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(SensiTheme.glassBorder, lineWidth: 1)
                )
                Spacer()
            }
        }
    }
}

struct MiniStat: View {
    let title: String
    let value: String

    var body: some View {
        VStack(spacing: 2) {
            Text(title)
                .font(.system(size: 9, weight: .bold, design: .monospaced))
                .foregroundColor(SensiTheme.textMuted)
            Text(value)
                .font(.system(size: 13, weight: .black, design: .monospaced))
                .foregroundColor(.white)
        }
    }
}
