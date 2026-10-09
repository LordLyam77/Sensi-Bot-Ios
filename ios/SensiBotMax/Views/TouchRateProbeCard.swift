import SwiftUI

public struct TouchRateProbeCard: View {
    @Binding public var measuredTouchHz: Double?
    @State private var liveVelocity: Double = 0.0
    @State private var dragDistance: Double = 0.0
    @State private var dragClassification: String = "READY"
    @State private var isTouching: Bool = false
    @State private var dragPath: [CGPoint] = []
    
    @State private var touchStartTime: Date = Date()
    @State private var touchPoints: [(point: CGPoint, time: Date)] = []

    private let profile = DeviceProbe.current()

    public init(measuredTouchHz: Binding<Double?>) {
        self._measuredTouchHz = measuredTouchHz
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Header
            HStack {
                Image(systemName: "hand.draw.fill")
                    .foregroundColor(SensiTheme.cyanAccent)
                Text("TOUCH VELOCITY & POLLING LAB")
                    .font(.system(size: 13, weight: .bold, design: .rounded))
                    .foregroundColor(SensiTheme.textPrimary)
                Spacer()
                if let hz = measuredTouchHz {
                    HStack(spacing: 4) {
                        Text("\(Int(hz.rounded())) HZ")
                            .font(.system(size: 12, weight: .black, design: .monospaced))
                            .foregroundColor(SensiTheme.cyanAccent)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(SensiTheme.cyanAccent.opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                }
            }

            Text("Swipe upward in the pad below. Measures your real-time drag velocity, touch friction, and hardware polling rate.")
                .font(.system(size: 12))
                .foregroundColor(SensiTheme.textSecondary)

            // Interactive Drag Area with Real-Time Gesture Tracking
            ZStack {
                Color.black.opacity(0.5)
                    .clipShape(RoundedRectangle(cornerRadius: 14))

                // Canvas showing the user's touch trajectory glow
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

                // Prompt Overlay
                VStack(spacing: 6) {
                    if isTouching {
                        VStack(spacing: 2) {
                            Text("\(Int(liveVelocity)) px/s")
                                .font(.system(size: 22, weight: .black, design: .monospaced))
                                .foregroundColor(SensiTheme.cyanAccent)
                            Text("DRAGGING")
                                .font(.system(size: 10, weight: .bold, design: .monospaced))
                                .foregroundColor(SensiTheme.rubyRed)
                        }
                    } else if liveVelocity > 0 {
                        VStack(spacing: 2) {
                            Text("\(Int(liveVelocity)) px/s")
                                .font(.system(size: 20, weight: .black, design: .monospaced))
                                .foregroundColor(SensiTheme.cyanAccent)
                            Text("\(dragClassification) • \(Int(dragDistance))px")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(SensiTheme.goldAccent)
                        }
                    } else {
                        VStack(spacing: 4) {
                            Image(systemName: "arrow.up.circle.fill")
                                .font(.system(size: 26))
                                .foregroundColor(SensiTheme.rubyRed)
                            Text("SWIPE UPWARD HERE")
                                .font(.system(size: 13, weight: .black, design: .monospaced))
                                .foregroundColor(.white)
                            Text("Detects Flick Velocity & Screen Friction")
                                .font(.system(size: 10))
                                .foregroundColor(SensiTheme.textMuted)
                        }
                    }
                }
            }
            .frame(height: 120)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(
                        isTouching ? SensiTheme.cyanAccent : (liveVelocity > 0 ? SensiTheme.cyanAccent.opacity(0.5) : SensiTheme.rubyRed.opacity(0.4)),
                        style: StrokeStyle(lineWidth: 1.5, dash: isTouching ? [] : [6, 4])
                    )
            )
            .contentShape(Rectangle())
            // High priority gesture prevents ScrollView from stealing vertical swipes!
            .highPriorityGesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        let now = Date()
                        if !isTouching {
                            isTouching = true
                            touchStartTime = now
                            touchPoints = [(value.location, now)]
                            dragPath = [value.location]
                        } else {
                            touchPoints.append((value.location, now))
                            dragPath.append(value.location)
                        }

                        // Calculate instantaneous velocity over recent samples
                        if touchPoints.count >= 2 {
                            let last = touchPoints.last!
                            let prev = touchPoints[touchPoints.count - 2]
                            let dt = last.time.timeIntervalSince(prev.time)
                            if dt > 0.001 {
                                let dx = Double(last.point.x - prev.point.x)
                                let dy = Double(last.point.y - prev.point.y)
                                let dist = sqrt(dx * dx + dy * dy)
                                self.liveVelocity = dist / dt
                            }
                        }
                    }
                    .onEnded { value in
                        self.isTouching = false
                        let now = Date()
                        let totalDuration = now.timeIntervalSince(touchStartTime)

                        if totalDuration > 0.02 && touchPoints.count >= 2 {
                            // Calculate total distance
                            let first = touchPoints.first!.point
                            let last = touchPoints.last!.point
                            let dx = Double(last.x - first.x)
                            let dy = Double(last.y - first.y)
                            self.dragDistance = sqrt(dx * dx + dy * dy)

                            // Polling Rate Hz
                            let hz = Double(touchPoints.count) / totalDuration
                            let clampedHz = min(max(hz, 60.0), Double(profile.maxFPS))
                            self.measuredTouchHz = clampedHz

                            // Average velocity
                            if self.dragDistance > 10 {
                                self.liveVelocity = self.dragDistance / totalDuration
                            }

                            // Classify drag
                            if dy < -40 && abs(dx) < 35 {
                                self.dragClassification = "ONE-TAP STRAIGHT DRAG"
                            } else if dx > 25 && dy < -30 {
                                self.dragClassification = "J-DRAG ARC"
                            } else {
                                self.dragClassification = "ROTATION FLICK"
                            }
                        }
                    }
            )

            // Metrics readout row
            if liveVelocity > 0 {
                HStack(spacing: 8) {
                    MetricBadge(title: "VELOCITY", value: "\(Int(liveVelocity)) px/s", color: SensiTheme.cyanAccent)
                    MetricBadge(title: "DISTANCE", value: "\(Int(dragDistance)) px", color: SensiTheme.rubyRed)
                    MetricBadge(title: "POLLING", value: "\(Int((measuredTouchHz ?? 60).rounded())) Hz", color: SensiTheme.goldAccent)
                }
            }
        }
        .gamingCard(borderColor: liveVelocity > 0 ? SensiTheme.cyanAccent.opacity(0.4) : SensiTheme.glassBorder)
    }
}

struct MetricBadge: View {
    let title: String
    let value: String
    let color: Color

    var body: some View {
        VStack(spacing: 2) {
            Text(title)
                .font(.system(size: 9, weight: .bold, design: .monospaced))
                .foregroundColor(SensiTheme.textMuted)
            Text(value)
                .font(.system(size: 12, weight: .black, design: .monospaced))
                .foregroundColor(color)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 6)
        .background(Color.black.opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}
