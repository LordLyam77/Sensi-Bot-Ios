import SwiftUI

public struct SensiBotView: View {
    @State private var messages: [ChatMessage] = []
    @State private var inputText: String = ""
    @ObservedObject private var qwenManager = LocalQwenModelManager.shared
    private let deviceProfile = DeviceProbe.current()

    private let suggestions = [
        "M1887 One-Tap",
        "White444 Settings",
        "Aim Flies Over Head",
        "Aim Stuck on Chest",
        "Raistar Speed",
        "Best Fire Button Size"
    ]

    public init() {}

    public var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text("SENSI BOT")
                            .font(.system(size: 20, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                        Text(qwenManager.isModelInstalled ? "QWEN OFFLINE" : "AI COACH")
                            .font(.system(size: 10, weight: .black, design: .rounded))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(qwenManager.isModelInstalled ? Color.green.opacity(0.8) : SensiTheme.rubyRed)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 4))
                    }
                    Text(qwenManager.isModelInstalled ? "On-Device Neural Engine Active (100% Offline)" : "Free Fire Neural Drag & Weapon Assistant")
                        .font(.system(size: 11))
                        .foregroundColor(SensiTheme.textSecondary)
                }
                Spacer()
                Image(systemName: qwenManager.isModelInstalled ? "bolt.shield.fill" : "cpu.fill")
                    .font(.system(size: 20))
                    .foregroundColor(qwenManager.isModelInstalled ? .green : SensiTheme.cyanAccent)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(SensiTheme.surfaceElevated)

            Divider().background(SensiTheme.glassBorder)

            // On-Demand Qwen Download Card
            QwenModelCard(manager: qwenManager)

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
                        Image(systemName: message.isLocalQwen ? "bolt.shield.fill" : "cpu.fill")
                            .font(.system(size: 12))
                            .foregroundColor(message.isLocalQwen ? Color.green : SensiTheme.rubyRed)
                        Text(message.isLocalQwen ? "QWEN 0.6B • LOCAL" : "SENSI BOT")
                            .font(.system(size: 10, weight: .black, design: .monospaced))
                            .foregroundColor(message.isLocalQwen ? Color.green : SensiTheme.rubyRed)
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

struct QwenModelCard: View {
    @ObservedObject var manager: LocalQwenModelManager

    var body: some View {
        VStack(spacing: 8) {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(manager.isModelInstalled ? Color.green.opacity(0.15) : SensiTheme.rubyRed.opacity(0.15))
                        .frame(width: 36, height: 36)
                    Image(systemName: manager.isModelInstalled ? "bolt.shield.fill" : "arrow.down.circle.fill")
                        .font(.system(size: 18))
                        .foregroundColor(manager.isModelInstalled ? Color.green : SensiTheme.rubyRed)
                }

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text(QwenPromptConfig.modelName)
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.white)
                        Text(manager.isModelInstalled ? "OFFLINE READY" : "ON-DEMAND")
                            .font(.system(size: 8, weight: .black, design: .monospaced))
                            .padding(.horizontal, 5)
                            .padding(.vertical, 2)
                            .background(manager.isModelInstalled ? Color.green.opacity(0.2) : SensiTheme.rubyRed.opacity(0.2))
                            .foregroundColor(manager.isModelInstalled ? Color.green : SensiTheme.rubyRed)
                            .clipShape(Capsule())
                    }

                    switch manager.status {
                    case .notInstalled:
                        Text("Download ~\(QwenPromptConfig.estimatedModelSizeMb)MB for 100% offline Neural Engine chat")
                            .font(.system(size: 10))
                            .foregroundColor(SensiTheme.textSecondary)
                    case .downloading:
                        Text(manager.downloadedBytesText.isEmpty ? "Downloading weights..." : manager.downloadedBytesText)
                            .font(.system(size: 10, weight: .medium, design: .monospaced))
                            .foregroundColor(SensiTheme.cyanAccent)
                    case .ready, .loaded:
                        Text("Active on device • Neural Engine inference enabled")
                            .font(.system(size: 10))
                            .foregroundColor(.green.opacity(0.9))
                    case .error(let msg):
                        Text(msg)
                            .font(.system(size: 10))
                            .foregroundColor(.red)
                            .lineLimit(1)
                    }
                }

                Spacer()

                // Action Buttons
                switch manager.status {
                case .notInstalled:
                    Button(action: {
                        manager.startDownload()
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "icloud.and.arrow.down")
                                .font(.system(size: 11, weight: .bold))
                            Text("Download")
                                .font(.system(size: 11, weight: .bold))
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(
                            LinearGradient(
                                colors: [SensiTheme.rubyRed, SensiTheme.rubyDark],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .clipShape(Capsule())
                    }
                case .downloading:
                    Button(action: {
                        manager.cancelDownload()
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 22))
                            .foregroundColor(SensiTheme.textMuted)
                    }
                case .ready, .loaded:
                    Button(action: {
                        manager.deleteModel()
                    }) {
                        Image(systemName: "trash.fill")
                            .font(.system(size: 14))
                            .foregroundColor(SensiTheme.textMuted)
                            .padding(8)
                            .background(Color.white.opacity(0.06))
                            .clipShape(Circle())
                    }
                case .error:
                    Button(action: {
                        manager.startDownload()
                    }) {
                        Text("Retry")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(SensiTheme.rubyRed)
                            .clipShape(Capsule())
                    }
                }
            }

            // Progress Bar when downloading
            if case .downloading(let progress) = manager.status {
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(Color.white.opacity(0.1))
                            .frame(height: 4)
                        Capsule()
                            .fill(
                                LinearGradient(
                                    colors: [SensiTheme.rubyRed, SensiTheme.cyanAccent],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(width: max(0, min(geo.size.width * CGFloat(progress), geo.size.width)), height: 4)
                    }
                }
                .frame(height: 4)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(Color(white: 0.08))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(manager.isModelInstalled ? Color.green.opacity(0.3) : SensiTheme.glassBorder, lineWidth: 1)
        )
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
}
