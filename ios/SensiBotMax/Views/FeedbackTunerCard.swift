import SwiftUI

public struct FeedbackTunerCard: View {
    @Binding public var currentBias: Double
    public let onFeedbackSubmitted: () -> Void

    @State private var feedbackStatus: String? = nil

    public init(currentBias: Binding<Double>, onFeedbackSubmitted: @escaping () -> Void) {
        self._currentBias = currentBias
        self.onFeedbackSubmitted = onFeedbackSubmitted
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Image(systemName: "tuningfork")
                    .foregroundColor(SensiTheme.goldAccent)
                Text("POST-PRACTICE FEEDBACK TUNER")
                    .font(.system(size: 13, weight: .bold, design: .rounded))
                    .foregroundColor(SensiTheme.textPrimary)
                Spacer()
                Text(String(format: "%+.1f%%", currentBias * 100))
                    .font(.system(size: 11, weight: .black, design: .monospaced))
                    .foregroundColor(SensiTheme.goldAccent)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(SensiTheme.goldAccent.opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
            }

            Text("After testing in Free Fire Training Grounds, tap your drag result to let the mathematical feedback loop adapt your profile.")
                .font(.system(size: 12))
                .foregroundColor(SensiTheme.textSecondary)

            HStack(spacing: 8) {
                // Too fast / flew over head
                Button(action: {
                    FeedbackTuner.record(result: 1)
                    currentBias = FeedbackTuner.bias
                    feedbackStatus = "Decreased bias (-1.5%) to stabilize over-drag"
                    onFeedbackSubmitted()
                }) {
                    VStack(spacing: 4) {
                        Image(systemName: "arrow.up.right")
                            .font(.system(size: 14, weight: .bold))
                        Text("OVER HEAD")
                            .font(.system(size: 10, weight: .heavy))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(Color.red.opacity(0.15))
                    .foregroundColor(Color.red)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.red.opacity(0.4), lineWidth: 1))
                }

                // Perfect clean red numbers
                Button(action: {
                    FeedbackTuner.record(result: 0)
                    currentBias = FeedbackTuner.bias
                    feedbackStatus = "Perfect Sweet Spot Locked!"
                    onFeedbackSubmitted()
                }) {
                    VStack(spacing: 4) {
                        Image(systemName: "scope")
                            .font(.system(size: 14, weight: .bold))
                        Text("CLEAN RED")
                            .font(.system(size: 10, weight: .heavy))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(SensiTheme.greenFairPlay.opacity(0.15))
                    .foregroundColor(SensiTheme.greenFairPlay)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(SensiTheme.greenFairPlay.opacity(0.4), lineWidth: 1))
                }

                // Stuck on chest
                Button(action: {
                    FeedbackTuner.record(result: -1)
                    currentBias = FeedbackTuner.bias
                    feedbackStatus = "Increased bias (+1.5%) to lift chest lock"
                    onFeedbackSubmitted()
                }) {
                    VStack(spacing: 4) {
                        Image(systemName: "arrow.down.right")
                            .font(.system(size: 14, weight: .bold))
                        Text("BODY SHOTS")
                            .font(.system(size: 10, weight: .heavy))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(SensiTheme.cyanAccent.opacity(0.15))
                    .foregroundColor(SensiTheme.cyanAccent)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(SensiTheme.cyanAccent.opacity(0.4), lineWidth: 1))
                }
            }

            if let status = feedbackStatus {
                HStack(spacing: 6) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(SensiTheme.greenFairPlay)
                        .font(.system(size: 12))
                    Text(status)
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(SensiTheme.greenFairPlay)
                }
                .transition(.opacity)
            }
        }
        .gamingCard(borderColor: SensiTheme.goldAccent.opacity(0.35))
    }
}
