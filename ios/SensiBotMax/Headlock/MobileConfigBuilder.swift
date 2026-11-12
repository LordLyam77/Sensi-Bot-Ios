import Foundation

/// Optional unsigned .mobileconfig generator for iOS Touch Accommodations.
/// Note: Apple does not publicly document Touch Accommodations MDM keys, so HeadlockGuide
/// is the primary setup flow and this profile is offered as an optional convenience export.
public enum MobileConfigBuilder {

    public static func buildUnsignedProfile(
        holdDurationSeconds: Double = 0.15,
        ignoreRepeatSeconds: Double = 0.30
    ) -> URL? {
        let profileUUID = UUID().uuidString
        let payloadUUID = UUID().uuidString

        let xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
        <plist version="1.0">
        <dict>
            <key>PayloadContent</key>
            <array>
                <dict>
                    <key>PayloadDescription</key>
                    <string>Optional Touch Accommodations preset for Sensi Bot Max Headlock</string>
                    <key>PayloadDisplayName</key>
                    <string>Headlock Touch Accommodations</string>
                    <key>PayloadIdentifier</key>
                    <string>com.sensibotpro.ios.headlock.touch.\(payloadUUID)</string>
                    <key>PayloadType</key>
                    <string>com.apple.Accessibility</string>
                    <key>PayloadUUID</key>
                    <string>\(payloadUUID)</string>
                    <key>PayloadVersion</key>
                    <integer>1</integer>
                    <key>TouchAccommodationsEnabled</key>
                    <true/>
                    <key>TouchAccommodationsHoldDurationEnabled</key>
                    <true/>
                    <key>TouchAccommodationsHoldDuration</key>
                    <real>\(String(format: "%.2f", holdDurationSeconds))</real>
                    <key>TouchAccommodationsIgnoreRepeatEnabled</key>
                    <true/>
                    <key>TouchAccommodationsIgnoreRepeatInterval</key>
                    <real>\(String(format: "%.2f", ignoreRepeatSeconds))</real>
                </dict>
            </array>
            <key>PayloadDescription</key>
            <string>Calibrated Touch Accommodations helper profile (unsigned).</string>
            <key>PayloadDisplayName</key>
            <string>Sensi Bot Max — Headlock Touch</string>
            <key>PayloadIdentifier</key>
            <string>com.sensibotpro.ios.headlock.profile</string>
            <key>PayloadRemovalDisallowed</key>
            <false/>
            <key>PayloadType</key>
            <string>Configuration</string>
            <key>PayloadUUID</key>
            <string>\(profileUUID)</string>
            <key>PayloadVersion</key>
            <integer>1</integer>
        </dict>
        </plist>
        """

        let tempDir = FileManager.default.temporaryDirectory
        let fileUrl = tempDir.appendingPathComponent("SensiBotMax_Headlock.mobileconfig")
        do {
            try xml.write(to: fileUrl, atomically: true, encoding: .utf8)
            return fileUrl
        } catch {
            return nil
        }
    }
}

