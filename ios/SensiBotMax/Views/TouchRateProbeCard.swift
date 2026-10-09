import SwiftUI
import UIKit

public struct TouchRateProbeCard: View {
    @Binding public var measuredTouchHz: Double?
    @State private var isTouching: Bool = false
    @State private var touchCount: Int = 0

    public init(measuredTouchHz: Binding<Double?>) {
        self._measuredTouchHz = measuredTouchHz
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "hand.tap.fill")
                    .foregroundColor(SensiTheme.cyanAccent)
                Text("HARDWARE TOUCH POLLING PROBE")
                    .font(.system(size: 13, weight: .bold, design: .rounded))
                    .foregroundColor(SensiTheme.textPrimary)
                Spacer()
                if let hz = measuredTouchHz {
                    Text("\(Int(hz.rounded())) HZ")
                        .font(.system(size: 13, weight: .black, design: .monospaced))
                        .foregroundColor(SensiTheme.cyanAccent)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(SensiTheme.cyanAccent.opacity(0.15))
                        .clipShape(RoundedRectangle(cornerRadius: 6))
                }
            }

            Text("Swipe upward rapidly in the zone below like in Free Fire. Measures your iPhone's coalesced sub-frame touch polling rate.")
                .font(.system(size: 12))
                .foregroundColor(SensiTheme.textSecondary)

            ZStack {
                // Interactive probe UIView
                TouchProbeRepresentable { rate in
                    DispatchQueue.main.async {
                        self.measuredTouchHz = rate
                    }
                }
                .frame(height: 110)
                .background(Color.black.opacity(0.4))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(
                            measuredTouchHz != nil ? SensiTheme.cyanAccent.opacity(0.6) : SensiTheme.rubyRed.opacity(0.4),
                            style: StrokeStyle(lineWidth: 1.5, dash: [6, 4])
                        )
                )

                // Visual guide prompt
                VStack(spacing: 4) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(measuredTouchHz != nil ? SensiTheme.cyanAccent : SensiTheme.rubyRed)
                    Text(measuredTouchHz != nil ? "Probe Sampled: \(Int(measuredTouchHz!.rounded())) Hz" : "SWIPE UPWARD HERE")
                        .font(.system(size: 12, weight: .black, design: .monospaced))
                        .foregroundColor(SensiTheme.textPrimary)
                    Text("Detects 60Hz / 120Hz / 240Hz coalesced samples")
                        .font(.system(size: 10))
                        .foregroundColor(SensiTheme.textMuted)
                }
                .allowsHitTesting(false)
            }
        }
        .gamingCard(borderColor: measuredTouchHz != nil ? SensiTheme.cyanAccent.opacity(0.4) : SensiTheme.glassBorder)
    }
}

struct TouchProbeRepresentable: UIViewRepresentable {
    let onResult: (Double) -> Void

    func makeUIView(context: Context) -> TouchRateProbeView {
        let view = TouchRateProbeView()
        view.backgroundColor = .clear
        view.onResult = onResult
        return view
    }

    func updateUIView(_ uiView: TouchRateProbeView, context: Context) {
        uiView.onResult = onResult
    }
}
