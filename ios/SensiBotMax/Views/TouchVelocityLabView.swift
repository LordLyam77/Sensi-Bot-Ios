import SwiftUI

public struct DragSample: Identifiable {
    public let id = UUID()
    public let sampleNumber: Int
    public let velocity: Double // px/s
    public let distance: Double // px
    public let durationMs: Double
}

public struct TouchVelocityLabView: View {
    @Environment(\.presentationMode) var presentationMode
    @Binding public var measuredTouchHz: Double?

    @State private var samples: [DragSample] = []
    @State private var isTouching: Bool = false
    @State private var liveVelocity: Double = 0.0
    @State private var dragPath: [CGPoint] = []
    @State private var touchStartTime: Date = Date()
    @State private var lastDragClassification: String = "READY"
    @State private var isCompleted: Bool = false
    @State private var isSaved: Bool = false

    public init(measuredTouchHz: Binding<Double?> = .constant(nil)) {
        self._measuredTouchHz = measuredTouchHz
    }

    private let maxSamples = 5
    private let profile = DeviceProbe.current()

    public var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 18) {
                // Header Bar
                headerBar

                // 5-Sample Progress Indicator
                progressPillsBar

                if !isCompleted {
                    // Interactive Swipe Testing Pad
                    dragTestPad

                    // Live Drag Metrics Card
                    currentSampleStatusCard
                } else {
                    // 5-Sample Complete: Results & Recommendations Card
                    completedResultsCard
                }

                // Sample History Table
                if !samples.isEmpty {
                    sampleHistoryTable
                }

                Spacer(minLength: 32)
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
        }
        .background(SensiTheme.voidBlack.ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - 1. Header Bar
    private var headerBar: some View {
        VStack(spacing: 6) {
            HStack {
                Button(action: {
                    presentationMode.wrappedValue.dismiss()
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 14, weight: .bold))
                        Text("BACK")
                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                    }
                    .foregroundColor(SensiTheme.cyanAccent)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(SensiTheme.cyanAccent.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                }

                Spacer()

                HStack(spacing: 6) {
                    Image(systemName: "speedometer")
                        .foregroundColor(SensiTheme.rubyRed)
                    Text("TOUCH VELOCITY LAB")
                        .font(.system(size: 16, weight: .black, design: .monospaced))
                        .foregroundColor(.white)
                }

                Spacer()

                // Balance layout
                Text("     ")
            }

            Text("5-Drag Sampling Test • Custom Headshot Speed Calibration")
                .font(.system(size: 11))
                .foregroundColor(SensiTheme.textSecondary)
        }
    }

    // MARK: - 2. Progress Pills Bar (1 to 5)
    private var progressPillsBar: some View {
        HStack(spacing: 8) {
            ForEach(1...maxSamples, id: \.self) { num in
                let done = samples.count >= num
                let current = samples.count == (num - 1) && !isCompleted

                HStack(spacing: 4) {
                    if done {
                        Image(systemName: "checkmark")
                            .font(.system(size: 9, weight: .black))
                            .foregroundColor(.white)
                    } else {
                        Text("\(num)")
                            .font(.system(size: 10, weight: .black, design: .monospaced))
                            .foregroundColor(current ? SensiTheme.rubyRed : SensiTheme.textMuted)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 7)
                .background(
                    done ? SensiTheme.fairPlayGreen :
                    (current ? SensiTheme.rubyRed.opacity(0.25) : Color.white.opacity(0.05))
                )
                .clipShape(RoundedRectangle(cornerRadius: 6))
                .overlay(
                    RoundedRectangle(cornerRadius: 6)
                        .stroke(current ? SensiTheme.rubyRed : Color.clear, lineWidth: 1.5)
                )
            }
        }
        .padding(6)
        .background(Color.white.opacity(0.03))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - 3. Interactive Drag Testing Pad
    private var dragTestPad: some View {
        VStack(spacing: 10) {
            ZStack {
                Color.black.opacity(0.65)
                    .frame(height: 200)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(
                                LinearGradient(
                                    colors: [SensiTheme.rubyRed.opacity(0.8), SensiTheme.cyanAccent.opacity(0.4)],
                                    startPoint: .top,
                                    endPoint: .bottom
                                ),
                                lineWidth: 1.5
                            )
                    )

                // Drag Gesture Path
                Path { path in
                    guard dragPath.count > 1 else { return }
                    path.move(to: dragPath[0])
                    for pt in dragPath.dropFirst() {
                        path.addLine(to: pt)
                    }
                }
                .stroke(
                    LinearGradient(
                        colors: [SensiTheme.rubyRed, SensiTheme.cyanAccent],
                        startPoint: .bottom,
                        endPoint: .top
                    ),
                    style: StrokeStyle(lineWidth: 4, lineCap: .round, lineJoin: .round)
                )

                // Center Guide / Feedback
                VStack(spacing: 6) {
                    if isTouching {
                        Text("\(Int(liveVelocity)) px/s")
                            .font(.system(size: 26, weight: .black, design: .monospaced))
                            .foregroundColor(SensiTheme.cyanAccent)
                        Text("MEASURING FLICK...")
                            .font(.system(size: 11, weight: .black, design: .monospaced))
                            .foregroundColor(SensiTheme.rubyRed)
                    } else {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.system(size: 32))
                            .foregroundColor(SensiTheme.rubyRed)
                        Text("FLICK UPWARD (SAMPLE \(samples.count + 1) OF 5)")
                            .font(.system(size: 13, weight: .black, design: .monospaced))
                            .foregroundColor(.white)
                        Text("Perform a quick upward headshot flick with your thumb")
                            .font(.system(size: 10.5))
                            .foregroundColor(SensiTheme.textSecondary)
                    }
                }
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 5)
                    .onChanged { val in
                        if !isTouching {
                            isTouching = true
                            touchStartTime = Date()
                            dragPath = [val.location]
                        } else {
                            dragPath.append(val.location)
                        }

                        let elapsed = Date().timeIntervalSince(touchStartTime)
                        let dist = abs(val.translation.height)
                        if elapsed > 0.01 {
                            liveVelocity = (Double(dist) / elapsed)
                        }
                    }
                    .onEnded { val in
                        isTouching = false
                        let elapsed = Date().timeIntervalSince(touchStartTime)
                        let dist = abs(val.translation.height)
                        let durationMs = elapsed * 1000.0

                        if dist > 35 && elapsed > 0.02 {
                            let computedVelocity = (Double(dist) / elapsed)
                            recordSample(velocity: computedVelocity, distance: Double(dist), durationMs: durationMs)
                        } else {
                            // Too short
                            let haptic = UINotificationFeedbackGenerator()
                            haptic.notificationOccurred(.warning)
                            lastDragClassification = "Swipe was too short. Please flick upward firmly."
                        }

                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                            dragPath = []
                        }
                    }
            )
        }
    }

    private func recordSample(velocity: Double, distance: Double, durationMs: Double) {
        let sampleNum = samples.count + 1
        let newSample = DragSample(sampleNumber: sampleNum, velocity: velocity, distance: distance, durationMs: durationMs)
        samples.append(newSample)

        let haptic = UIImpactFeedbackGenerator(style: .medium)
        haptic.impactOccurred()

        if samples.count >= maxSamples {
            isCompleted = true
            let notif = UINotificationFeedbackGenerator()
            notif.notificationOccurred(.success)
        }
    }

    // MARK: - 4. Current Sample Status Card
    private var currentSampleStatusCard: some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) {
                Text("CURRENT SAMPLE")
                    .font(.system(size: 9, weight: .bold, design: .monospaced))
                    .foregroundColor(SensiTheme.textMuted)
                Text("Sample \(samples.count + 1) of \(maxSamples)")
                    .font(.system(size: 13, weight: .black, design: .monospaced))
                    .foregroundColor(SensiTheme.cyanAccent)
            }
            Spacer()
            if let last = samples.last {
                VStack(alignment: .trailing, spacing: 3) {
                    Text("PREVIOUS FLICK")
                        .font(.system(size: 9, weight: .bold, design: .monospaced))
                        .foregroundColor(SensiTheme.textMuted)
                    Text("\(Int(last.velocity)) px/s (\(Int(last.durationMs))ms)")
                        .font(.system(size: 12, weight: .black, design: .monospaced))
                        .foregroundColor(SensiTheme.goldAccent)
                }
            }
        }
        .padding(12)
        .gamingCard(borderColor: Color.white.opacity(0.1))
    }

    // MARK: - 5. Completed Results & Recommendations Card
    private var completedResultsCard: some View {
        let avgVelocity = samples.isEmpty ? 0.0 : (samples.map(\.velocity).reduce(0, +) / Double(samples.count))
        let classification = classifyUserVelocity(avgVelocity)

        return VStack(alignment: .leading, spacing: 14) {
            HStack {
                Image(systemName: "checkmark.seal.fill")
                    .font(.system(size: 18))
                    .foregroundColor(SensiTheme.fairPlayGreen)
                Text("5-DRAG ANALYSIS COMPLETE")
                    .font(.system(size: 13, weight: .black, design: .monospaced))
                    .foregroundColor(SensiTheme.fairPlayGreen)
                Spacer()
                Text("\(Int(avgVelocity)) PX/S")
                    .font(.system(size: 13, weight: .black, design: .monospaced))
                    .foregroundColor(SensiTheme.cyanAccent)
            }

            VStack(alignment: .leading, spacing: 6) {
                Text(classification.title)
                    .font(.system(size: 16, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                Text(classification.description)
                    .font(.system(size: 11))
                    .foregroundColor(SensiTheme.textSecondary)
                    .lineSpacing(2)
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.black.opacity(0.4))
            .clipShape(RoundedRectangle(cornerRadius: 10))

            Divider().background(Color.white.opacity(0.1))

            Text("RECOMMENDED SETTINGS ADJUSTMENT")
                .font(.system(size: 11, weight: .black, design: .monospaced))
                .foregroundColor(SensiTheme.goldAccent)

            HStack(spacing: 10) {
                recPill(label: "GENERAL", val: "\(classification.recGeneral)")
                recPill(label: "RED DOT", val: "\(classification.recRedDot)")
                recPill(label: "2X SCOPE", val: "\(classification.rec2x)")
                recPill(label: "4X SCOPE", val: "\(classification.rec4x)")
                recPill(label: "BUTTON", val: "\(classification.recButton)%")
            }

            HStack(spacing: 10) {
                // Apply Button
                Button(action: {
                    let menu = FloatingMenuManager.shared
                    menu.generalSensi = Double(classification.recGeneral)
                    menu.redDotSensi = Double(classification.recRedDot)
                    menu.scope2xSensi = Double(classification.rec2x)
                    menu.scope4xSensi = Double(classification.rec4x)
                    menu.fireButtonSize = Double(classification.recButton)
                    isSaved = true

                    let haptic = UINotificationFeedbackGenerator()
                    haptic.notificationOccurred(.success)
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: isSaved ? "checkmark" : "bolt.fill")
                        Text(isSaved ? "SAVED TO PROFILE!" : "APPLY TO SETTINGS")
                            .font(.system(size: 11, weight: .black, design: .monospaced))
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(isSaved ? Color.green : SensiTheme.rubyRed)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }

                // Restart Button
                Button(action: {
                    samples = []
                    isCompleted = false
                    isSaved = false
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: "arrow.counterclockwise")
                        Text("RETEST")
                            .font(.system(size: 11, weight: .bold, design: .monospaced))
                    }
                    .foregroundColor(SensiTheme.cyanAccent)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color.white.opacity(0.08))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }
        }
        .gamingCard(borderColor: SensiTheme.fairPlayGreen.opacity(0.5))
    }

    @ViewBuilder
    private func recPill(label: String, val: String) -> some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.system(size: 8, weight: .bold, design: .monospaced))
                .foregroundColor(SensiTheme.textMuted)
            Text(val)
                .font(.system(size: 12, weight: .black, design: .monospaced))
                .foregroundColor(.white)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 6)
        .background(Color.white.opacity(0.06))
        .clipShape(RoundedRectangle(cornerRadius: 6))
    }

    // MARK: - 6. Sample History Table
    private var sampleHistoryTable: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("CAPTURED SAMPLES")
                .font(.system(size: 11, weight: .black, design: .monospaced))
                .foregroundColor(SensiTheme.textMuted)

            VStack(spacing: 4) {
                ForEach(samples) { s in
                    HStack {
                        Text("Flick #\(s.sampleNumber)")
                            .font(.system(size: 10, weight: .bold, design: .monospaced))
                            .foregroundColor(.white)
                        Spacer()
                        Text("\(Int(s.distance)) px")
                            .font(.system(size: 10, design: .monospaced))
                            .foregroundColor(SensiTheme.textMuted)
                        Spacer()
                        Text("\(Int(s.durationMs)) ms")
                            .font(.system(size: 10, design: .monospaced))
                            .foregroundColor(SensiTheme.textMuted)
                        Spacer()
                        Text("\(Int(s.velocity)) px/s")
                            .font(.system(size: 11, weight: .black, design: .monospaced))
                            .foregroundColor(SensiTheme.cyanAccent)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.white.opacity(0.03))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                }
            }
        }
        .padding(12)
        .gamingCard(borderColor: Color.white.opacity(0.08))
    }

    private struct VelocityClassification {
        let title: String
        let description: String
        let recGeneral: Int
        let recRedDot: Int
        let rec2x: Int
        let rec4x: Int
        let recButton: Int
    }

    private func classifyUserVelocity(_ v: Double) -> VelocityClassification {
        if v >= 1600 {
            return VelocityClassification(
                title: "⚡ ULTRA-FAST AGGRESSIVE FLICKER",
                description: "Your drag speed is exceptionally rapid. Slightly lower General sensitivity stabilizes the crosshair from overshooting the head, while a compact 40% fire button enables instant snap flicking.",
                recGeneral: 192,
                recRedDot: 184,
                rec2x: 170,
                rec4x: 158,
                recButton: 40
            )
        } else if v >= 1200 {
            return VelocityClassification(
                title: "🎯 OPTIMAL HEADSHOT FLICKER",
                description: "Your swipe velocity is in the ideal Free Fire competitive range. Maximized General and Red Dot values provide magnetic hitbox pull at medium and close ranges.",
                recGeneral: 196,
                recRedDot: 188,
                rec2x: 175,
                rec4x: 162,
                recButton: 42
            )
        } else if v >= 800 {
            return VelocityClassification(
                title: "🛡️ BALANCED STEADY FLICKER",
                description: "Your drag flick is controlled and deliberate. Higher General sensitivity (+8) and a 48% fire button compensate for physical screen friction and deliver clean headshot recovery.",
                recGeneral: 188,
                recRedDot: 180,
                rec2x: 172,
                rec4x: 156,
                recButton: 48
            )
        } else {
            return VelocityClassification(
                title: "📐 SMOOTH CONTROLLED FLICKER",
                description: "Your drag motion is gentle. Maximize General sensitivity (200) and enlarge the fire button to 52% to trigger rapid upward drag acceleration without hand strain.",
                recGeneral: 200,
                recRedDot: 194,
                rec2x: 180,
                rec4x: 168,
                recButton: 52
            )
        }
    }
}
