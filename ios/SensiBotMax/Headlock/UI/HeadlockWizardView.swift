import SwiftUI

public struct HeadlockWizardView: View {
    @State private var step: CalibrationStep = .deviceClass
    @State private var workingConfig: HeadlockConfig
    @State private var sweepState = BiasSweepState()

    let onCommit: (HeadlockConfig) -> Void
    let onCancel: () -> Void

    public init(
        initialConfig: HeadlockConfig,
        onCommit: @escaping (HeadlockConfig) -> Void,
        onCancel: @escaping () -> Void
    ) {
        _workingConfig = State(initialValue: initialConfig.clamped())
        self.onCommit = onCommit
        self.onCancel = onCancel
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text("HEADLOCK CALIBRATION (\(step.rawValue + 1)/4)")
                    .font(.system(size: 18, weight: .black, design: .monospaced))
                    .foregroundColor(SensiTheme.rubyRed)
                Text(step.title)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)

                switch step {
                case .deviceClass:
                    ForEach(HeadlockDeviceClass.allCases, id: \.rawValue) { dc in
                        let selected = workingConfig.deviceClass == dc
                        Button(action: {
                            let base = HeadlockProfile.collectMetrics()
                            let metrics = HeadlockDeviceMetrics(
                                deviceClass: dc,
                                screenHeightPx: base.screenHeightPx,
                                refreshRateHz: base.refreshRateHz
                            )
                            workingConfig = HeadlockCalibration.recommend(metrics)
                        }) {
                            HStack {
                                Text(dc.label)
                                    .foregroundColor(.white)
                                    .fontWeight(selected ? .bold : .regular)
                                Spacer()
                                if selected {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(SensiTheme.rubyRed)
                                }
                            }
                            .padding(14)
                            .background(selected ? SensiTheme.rubyRed.opacity(0.2) : SensiTheme.surface)
                            .cornerRadius(10)
                        }
                    }

                case .fireAnchor:
                    Text("Tap inside the pad where your Free Fire fire button sits.")
                        .font(.system(size: 13))
                        .foregroundColor(SensiTheme.textMuted)
                    GeometryReader { geo in
                        ZStack(alignment: .topLeading) {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(SensiTheme.surface)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(SensiTheme.rubyRed.opacity(0.4), lineWidth: 1)
                                )
                            Circle()
                                .fill(SensiTheme.rubyRed)
                                .overlay(Circle().stroke(Color.white, lineWidth: 2))
                                .frame(width: 28, height: 28)
                                .position(
                                    x: geo.size.width * workingConfig.startX,
                                    y: geo.size.height * workingConfig.startY
                                )
                        }
                        .contentShape(Rectangle())
                        .gesture(
                            DragGesture(minimumDistance: 0)
                                .onEnded { value in
                                    workingConfig.startX = min(max(value.location.x / max(geo.size.width, 1), 0.05), 0.95)
                                    workingConfig.startY = min(max(value.location.y / max(geo.size.height, 1), 0.05), 0.95)
                                    workingConfig = workingConfig.clamped()
                                }
                        )
                    }
                    .frame(height: 240)
                    Text("Anchor: (\(Int(workingConfig.startX * 100))%, \(Int(workingConfig.startY * 100))%)")
                        .font(.system(size: 13))
                        .foregroundColor(.white)

                case .verticalBiasSweep:
                    Text("Sweep vertical bias 0.6x → 1.6x (0.1 steps, 20 reps each). Keep the bias hitting ~6/10 (12/20).")
                        .font(.system(size: 13))
                        .foregroundColor(SensiTheme.textMuted)
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Current Bias: \(String(format: "%.1fx", sweepState.currentBias))")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(SensiTheme.rubyRed)
                        Text("Reps: \(sweepState.repsCompleted)/\(sweepState.targetRepsPerStep) • Hits: \(sweepState.hitsRecorded) • Best: \(String(format: "%.1fx", sweepState.bestBias))")
                            .font(.system(size: 13))
                            .foregroundColor(.white)
                        HStack(spacing: 10) {
                            Button("Record Hit") {
                                sweepState = HeadlockCalibration.recordSweepRep(sweepState, hit: true)
                            }
                            .disabled(sweepState.repsCompleted >= sweepState.targetRepsPerStep)
                            .padding(.horizontal, 14).padding(.vertical, 8)
                            .background(SensiTheme.fairPlayGreen)
                            .foregroundColor(.black)
                            .cornerRadius(8)

                            Button("Miss") {
                                sweepState = HeadlockCalibration.recordSweepRep(sweepState, hit: false)
                            }
                            .disabled(sweepState.repsCompleted >= sweepState.targetRepsPerStep)
                            .padding(.horizontal, 14).padding(.vertical, 8)
                            .background(Color.white.opacity(0.12))
                            .foregroundColor(.white)
                            .cornerRadius(8)
                        }
                        if sweepState.currentBias < HeadlockConfig.maxVerticalBias {
                            Button("Next Bias Step (+0.1)") {
                                sweepState = HeadlockCalibration.advanceSweepBias(sweepState)
                                workingConfig.verticalBias = sweepState.bestBias
                            }
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(SensiTheme.rubyRed)
                        }
                    }
                    .padding(14)
                    .background(SensiTheme.surface)
                    .cornerRadius(12)

                case .commit:
                    let finalCfg = { () -> HeadlockConfig in
                        var c = workingConfig
                        c.verticalBias = sweepState.bestBias
                        return c.clamped()
                    }()
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Device: \(finalCfg.deviceClass.label)").foregroundColor(.white)
                        Text("Anchor: (\(Int(finalCfg.startX * 100))%, \(Int(finalCfg.startY * 100))%)").foregroundColor(.white)
                        Text("Distance: \(finalCfg.distancePx)px • Duration: \(finalCfg.durationMs)ms").foregroundColor(.white)
                        Text("Vertical Bias: \(String(format: "%.1fx", finalCfg.verticalBias))")
                            .foregroundColor(SensiTheme.rubyRed)
                            .fontWeight(.bold)
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(SensiTheme.surface)
                    .cornerRadius(12)
                }

                HStack(spacing: 10) {
                    Button(step == .deviceClass ? "Cancel" : "Back") {
                        if let prev = CalibrationStep(rawValue: step.rawValue - 1) {
                            step = prev
                        } else {
                            onCancel()
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(Color.white.opacity(0.1))
                    .foregroundColor(.white)
                    .cornerRadius(8)

                    Button(step == .commit ? "Commit Profile" : "Next") {
                        if let next = CalibrationStep(rawValue: step.rawValue + 1) {
                            if step == .verticalBiasSweep {
                                workingConfig.verticalBias = sweepState.bestBias
                            }
                            step = next
                        } else {
                            var finalCfg = workingConfig
                            finalCfg.verticalBias = sweepState.bestBias
                            onCommit(finalCfg.clamped())
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(SensiTheme.rubyRed)
                    .foregroundColor(.white)
                    .cornerRadius(8)
                }
            }
            .padding(16)
        }
        .background(SensiTheme.voidBlack.ignoresSafeArea())
    }
}

