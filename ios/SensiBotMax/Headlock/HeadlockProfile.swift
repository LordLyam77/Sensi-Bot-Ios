import UIKit

public enum HeadlockProfile {

    public static func collectMetrics() -> HeadlockDeviceMetrics {
        let screen = UIScreen.main
        let nativeHeight = Int(max(screen.nativeBounds.width, screen.nativeBounds.height))
        let refreshHz = screen.maximumFramesPerSecond
        let id = DeviceProbe.machineIdentifier()

        let deviceClass: HeadlockDeviceClass
        if UIDevice.current.userInterfaceIdiom == .pad || id.hasPrefix("iPad") {
            deviceClass = .tablet
        } else if nativeHeight >= 2688 {
            deviceClass = .large
        } else if nativeHeight <= 1920 {
            deviceClass = .compact
        } else {
            deviceClass = .phone
        }

        return HeadlockDeviceMetrics(
            deviceClass: deviceClass,
            screenHeightPx: max(nativeHeight, 1000),
            refreshRateHz: min(max(refreshHz, 60), 240)
        )
    }

    public static func recommend() -> HeadlockConfig {
        HeadlockCalibration.recommend(collectMetrics())
    }
}

