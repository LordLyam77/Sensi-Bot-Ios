import SwiftUI

@main
struct SensiBotMaxApp: App {
    @ObservedObject var licenseManager = LicenseManager.shared

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
