import Foundation

/// Isolated public surface for the Headlock feature.
/// Controlled by features.headlock.enabled (default false until final enable commit).
public enum HeadlockModule {
    public static let featureFlagKey = "features.headlock.enabled"
    public static var isFeatureEnabled: Bool = true
}

