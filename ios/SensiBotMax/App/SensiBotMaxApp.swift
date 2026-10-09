import SwiftUI
import AVFoundation

@main
struct SensiBotMaxApp: App {
    @ObservedObject var licenseManager = LicenseManager.shared

    init() {
        // Configure background audio playback session for PiP floating HUD
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .moviePlayback, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("Failed to set audio session for PiP: \(error.localizedDescription)")
        }
    }

    var body: some Scene {
        WindowGroup {
            if licenseManager.isActivated {
                MainTabView()
            } else {
                LicenseGateView()
            }
        }
    }
}
