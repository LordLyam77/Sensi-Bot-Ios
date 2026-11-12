import SwiftUI

public struct HeadlockGateView: View {
    public init() {}

    public var body: some View {
        if HeadlockModule.isFeatureEnabled {
            HeadlockScreenView()
        }
    }
}

