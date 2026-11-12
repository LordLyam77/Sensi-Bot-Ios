import SwiftUI
import UIKit

public struct HeadlockScreenView: View {
    @StateObject private var controller = HeadlockController(bridge: HeadlockChannel.shared)
    @State private var showWizard = false
    @State private var shareUrl: URL? = nil
    @State private var showShareSheet = false

    public init() {}

    public var body: some View {
        if showWizard {
            HeadlockWizardView(
                initialConfig: controller.snapshot.config,
                onCommit: { cfg in
                    controller.updateConfig(cfg)
                    showWizard = false
                },
                onCancel: {
                    showWizard = false
                }
            )
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("HEADLOCK GUIDE")
                        .font(.system(size: 22, weight: .black, design: .monospaced))
                        .foregroundColor(SensiTheme.rubyRed)

                    Text("Calibrated Touch Accommodations & drag profile. Zero game injection.")
                        .font(.system(size: 13))
                        .foregroundColor(SensiTheme.textMuted)

                    // Enable / Recommended Card
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(controller.snapshot.state == .enabled ? "HEADLOCK ARMED" : "HEADLOCK IDLE")
                                    .font(.system(size: 14, weight: .bold, design: .monospaced))
                                    .foregroundColor(controller.snapshot.state == .enabled ? SensiTheme.fairPlayGreen : .white)
                                Text(controller.snapshot.statusMessage)
                                    .font(.system(size: 12))
                                    .foregroundColor(SensiTheme.textMuted)
                            }
                            Spacer()
                            Toggle("", isOn: Binding(
                                get: { controller.snapshot.state == .enabled },
                                set: { wantOn in
                                    if wantOn { controller.enable() } else { controller.disable() }
                                }
                            ))
                            .labelsHidden()
                            .tint(SensiTheme.rubyRed)
                        }

                        HStack(spacing: 10) {
                            Button(action: { showWizard = true }) {
                                Text("Calibration Wizard")
                                    .font(.system(size: 12, weight: .bold))
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 10)
                                    .background(SensiTheme.rubyRed)
                                    .foregroundColor(.white)
                                    .cornerRadius(8)
                            }

                            Button(action: {
                                controller.applyRecommendation(HeadlockProfile.collectMetrics())
                            }) {
                                Text("Auto Recommend")
                                    .font(.system(size: 12, weight: .bold))
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 10)
                                    .background(Color.white.opacity(0.08))
                                    .foregroundColor(.white)
                                    .cornerRadius(8)
                            }
                        }
                    }
                    .padding(14)
                    .background(SensiTheme.cardBg)
                    .cornerRadius(12)

                    // Primary Setup Checklist (HeadlockGuide)
                    VStack(alignment: .leading, spacing: 10) {
                        Text("MANUAL SETUP CHECKLIST")
                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                            .foregroundColor(SensiTheme.cyanAccent)

                        ForEach(HeadlockGuide.checklist(for: controller.snapshot.config)) { step in
                            VStack(alignment: .leading, spacing: 3) {
                                Text("\(step.id). \(step.title)")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(.white)
                                Text(step.detail)
                                    .font(.system(size: 12))
                                    .foregroundColor(SensiTheme.textMuted)
                            }
                            .padding(.vertical, 4)
                        }

                        Button(action: { _ = controller.openSystemSettings() }) {
                            Label("Open Touch Accommodations", systemImage: "gearshape.fill")
                                .font(.system(size: 13, weight: .bold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 11)
                                .background(SensiTheme.rubyRed)
                                .foregroundColor(.white)
                                .cornerRadius(8)
                        }

                        Button(action: {
                            let res = controller.installTouchProfile()
                            if res.ok, let path = res.path {
                                shareUrl = URL(fileURLWithPath: path)
                                showShareSheet = true
                            }
                        }) {
                            Label("Optional: Export .mobileconfig Profile", systemImage: "square.and.arrow.up")
                                .font(.system(size: 12, weight: .semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(Color.white.opacity(0.08))
                                .foregroundColor(.white)
                                .cornerRadius(8)
                        }
                    }
                    .padding(14)
                    .background(SensiTheme.cardBg)
                    .cornerRadius(12)
                }
                .padding(16)
            }
            .background(SensiTheme.deepCarbon.ignoresSafeArea())
            .sheet(isPresented: $showShareSheet) {
                if let url = shareUrl {
                    HeadlockShareSheet(items: [url])
                }
            }
        }
    }
}

private struct HeadlockShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

