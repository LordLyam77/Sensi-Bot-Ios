import SwiftUI

struct MainTabView: View {
    @State private var selectedTab: Int = 0

    init() {
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(red: 0.043, green: 0.043, blue: 0.055, alpha: 1.0) // #0B0B0E
        
        appearance.stackedLayoutAppearance.normal.iconColor = UIColor(red: 0.53, green: 0.53, blue: 0.58, alpha: 1.0)
        appearance.stackedLayoutAppearance.normal.titleTextAttributes = [
            .foregroundColor: UIColor(red: 0.53, green: 0.53, blue: 0.58, alpha: 1.0),
            .font: UIFont.monospacedSystemFont(ofSize: 10, weight: .semibold)
        ]
        
        appearance.stackedLayoutAppearance.selected.iconColor = UIColor(red: 1.0, green: 0.165, blue: 0.302, alpha: 1.0) // #FF2A4D
        appearance.stackedLayoutAppearance.selected.titleTextAttributes = [
            .foregroundColor: UIColor(red: 1.0, green: 0.165, blue: 0.302, alpha: 1.0),
            .font: UIFont.monospacedSystemFont(ofSize: 10, weight: .bold)
        ]
        
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some View {
        ZStack {
            TabView(selection: $selectedTab) {
                HomeView()
                    .tabItem {
                        Label("CALIBRATOR", systemImage: "target")
                    }
                    .tag(0)

                SensiFinderView()
                    .tabItem {
                        Label("FINDER", systemImage: "slider.horizontal.3")
                    }
                    .tag(1)

                PipOverlayView()
                    .tabItem {
                        Label("FLOAT HUD", systemImage: "pip.enter")
                    }
                    .tag(2)

                SensiBotView()
                    .tabItem {
                        Label("SENSI BOT", systemImage: "cpu.fill")
                    }
                    .tag(3)

                ProfileView()
                    .tabItem {
                        Label("PROFILE", systemImage: "person.crop.circle")
                    }
                    .tag(4)
            }
            .accentColor(SensiTheme.rubyRed)
            .preferredColorScheme(.dark)

            // iOSGods-style Floating Mod Menu Overlay (Floats over all tabs with edge-snapping)
            FloatingModMenuOverlayView()
        }
    }
}
