import Foundation

public enum HeadlockMode: String, Codable, CaseIterable {
    case hold = "hold"
    case burst = "burst"
}

public enum HeadlockCurve: String, Codable, CaseIterable {
    case linear = "linear"
    case easeOut = "easeOut"
}

public enum HeadlockDeviceClass: String, Codable, CaseIterable {
    case compact = "compact"
    case phone = "phone"
    case large = "large"
    case tablet = "tablet"

    public var label: String {
        switch self {
        case .compact: return "Compact (<6.0\")"
        case .phone: return "Standard Phone (6.0\"-6.5\")"
        case .large: return "Large Phone (>6.5\")"
        case .tablet: return "Tablet / iPad"
        }
    }
}

public struct HeadlockDeviceMetrics {
    public let deviceClass: HeadlockDeviceClass
    public let screenHeightPx: Int
    public let refreshRateHz: Int

    public init(deviceClass: HeadlockDeviceClass, screenHeightPx: Int, refreshRateHz: Int) {
        self.deviceClass = deviceClass
        self.screenHeightPx = screenHeightPx
        self.refreshRateHz = refreshRateHz
    }
}

public struct HeadlockConfig: Codable, Equatable {
    public static let currentVersion = 1
    public static let storageKeyPrefix = "headlock."
    public static let storageConfigKey = "\(storageKeyPrefix)config"
    public static let minVerticalBias = 0.6
    public static let maxVerticalBias = 1.6

    public var v: Int
    public var mode: HeadlockMode
    public var startX: Double
    public var startY: Double
    public var distancePx: Int
    public var durationMs: Int
    public var repeatIntervalMs: Int
    public var burstCount: Int
    public var curve: HeadlockCurve
    public var verticalBias: Double
    public var deviceClass: HeadlockDeviceClass

    public init(
        v: Int = HeadlockConfig.currentVersion,
        mode: HeadlockMode = .hold,
        startX: Double = 0.82,
        startY: Double = 0.78,
        distancePx: Int = 42,
        durationMs: Int = 90,
        repeatIntervalMs: Int = 55,
        burstCount: Int = 3,
        curve: HeadlockCurve = .linear,
        verticalBias: Double = 1.0,
        deviceClass: HeadlockDeviceClass = .phone
    ) {
        self.v = v
        self.mode = mode
        self.startX = startX
        self.startY = startY
        self.distancePx = distancePx
        self.durationMs = durationMs
        self.repeatIntervalMs = repeatIntervalMs
        self.burstCount = burstCount
        self.curve = curve
        self.verticalBias = verticalBias
        self.deviceClass = deviceClass
    }

    public func clamped() -> HeadlockConfig {
        let clampedBias = min(max(verticalBias, Self.minVerticalBias), Self.maxVerticalBias)
        let roundedBias = (clampedBias * 10.0).rounded() / 10.0
        return HeadlockConfig(
            v: max(1, v),
            mode: mode,
            startX: min(max(startX, 0.05), 0.95),
            startY: min(max(startY, 0.05), 0.95),
            distancePx: min(max(distancePx, 10), 400),
            durationMs: min(max(durationMs, 20), 500),
            repeatIntervalMs: min(max(repeatIntervalMs, 15), 500),
            burstCount: min(max(burstCount, 1), 10),
            curve: curve,
            verticalBias: roundedBias,
            deviceClass: deviceClass
        )
    }

    public func toJson() -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        guard let data = try? encoder.encode(clamped()),
              let str = String(data: data, encoding: .utf8) else {
            return "{}"
        }
        return str
    }

    public static func fromJson(_ json: String?) -> HeadlockConfig {
        guard let json = json, !json.isEmpty,
              let data = json.data(using: .utf8),
              let decoded = try? JSONDecoder().decode(HeadlockConfig.self, from: data) else {
            return HeadlockConfig()
        }
        return decoded.clamped()
    }
}

