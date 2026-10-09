import UIKit
import Combine
import Security

public enum LicenseValidationResult {
    case success(token: String)
    case error(message: String, isRateLimited: Bool = false)
}

public final class LicenseManager: ObservableObject {
    public static let shared = LicenseManager()

    // Supabase Credentials (Identical to Android client)
    private static let projectUrl = "https://xvmrtmyjnbxpsgpikths.supabase.co"
    private static let anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh2bXJ0bXlqbmJ4cHNncGlrdGhzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2Mzc4MDUsImV4cCI6MjEwNTIxMzgwNX0.xzRhS5vjsclhHQgP4yJNtrZFcWMVRH090im4YyQf1BU"
    private static let validateEndpoint = "\(projectUrl)/functions/v1/validate_license"
    private static let checkSessionEndpoint = "\(projectUrl)/functions/v1/check_session"

    @Published public var isActivated: Bool = false
    @Published public var activeKey: String = ""
    @Published public var hardwareId: String = ""
    @Published public var statusMessage: String = ""
    @Published public var isVerifying: Bool = false
    @Published public var isInitialCheckComplete: Bool = false

    public var isVipActive: Bool { isActivated }
    public var isValidating: Bool { isVerifying }

    private let keyUserDefault = "sensibot_ios_vip_key"
    private let tokenUserDefault = "sensibot_ios_jwt_token"
    private let keyKeychainHardwareId = "sensibot_ios_hwid"

    public init() {
        self.hardwareId = getOrCreateHardwareId()
        self.activeKey = UserDefaults.standard.string(forKey: keyUserDefault) ?? ""
        let savedToken = UserDefaults.standard.string(forKey: tokenUserDefault) ?? ""

        if !activeKey.isEmpty && !savedToken.isEmpty {
            self.isActivated = true
        }
        self.isInitialCheckComplete = true
    }

    /// Persistent hardware ID using identifierForVendor backed by iOS Keychain
    public func getOrCreateHardwareId() -> String {
        if let existing = readKeychain(key: keyKeychainHardwareId), !existing.isEmpty {
            return existing
        }
        let idfv = UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
        let formattedHwid = "IOS-" + idfv.prefix(16).uppercased()
        saveKeychain(key: keyKeychainHardwareId, value: formattedHwid)
        return formattedHwid
    }

    /// Gathering device hardware and OS info for auditing in Supabase
    public func getDeviceInfoString() -> String {
        let profile = DeviceProbe.current()
        let sysVersion = UIDevice.current.systemVersion
        return "Apple \(profile.modelMarketingName) (\(profile.identifier)) • iOS \(sysVersion)"
    }

    /// Validates the license key against Supabase Edge Function
    @MainActor
    public func activateKey(_ key: String) async -> (Bool, String) {
        let trimmed = key.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard !trimmed.isEmpty else {
            return (false, "Please enter your license key.")
        }

        self.isVerifying = true
        defer { self.isVerifying = false }

        guard let url = URL(string: Self.validateEndpoint) else {
            return (false, "Invalid endpoint configuration.")
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 12.0
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(Self.anonKey, forHTTPHeaderField: "apikey")
        request.setValue("Bearer \(Self.anonKey)", forHTTPHeaderField: "Authorization")

        let payload: [String: Any] = [
            "key": trimmed,
            "device_id": self.hardwareId,
            "device_info": getDeviceInfoString()
        ]

        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: payload)
            let (data, response) = try await URLSession.shared.data(for: request)

            guard let httpResponse = response as? HTTPURLResponse else {
                return (false, "Unable to connect to license server.")
            }

            if httpResponse.statusCode == 429 {
                let msg = "Too many attempts, try again later"
                self.statusMessage = msg
                return (false, msg)
            }

            guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                return (false, "Invalid server response.")
            }

            let success = json["success"] as? Bool ?? false

            if success && (200...299).contains(httpResponse.statusCode) {
                let token = json["token"] as? String ?? "active_session"
                UserDefaults.standard.set(trimmed, forKey: self.keyUserDefault)
                UserDefaults.standard.set(token, forKey: self.tokenUserDefault)
                self.activeKey = trimmed
                self.isActivated = true
                self.statusMessage = "VIP License Active! Bound to \(self.hardwareId)"
                return (true, "VIP License Activated Successfully!")
            } else {
                let serverMsg = json["message"] as? String ?? "Invalid or inactive key"
                self.statusMessage = serverMsg
                return (false, serverMsg)
            }
        } catch {
            let errorMsg = "Unable to connect to license server. Check your internet connection."
            self.statusMessage = errorMsg
            return (false, errorMsg)
        }
    }

    public func deactivate() {
        UserDefaults.standard.removeObject(forKey: keyUserDefault)
        UserDefaults.standard.removeObject(forKey: tokenUserDefault)
        self.activeKey = ""
        self.isActivated = false
        self.statusMessage = "License cleared."
    }

    // MARK: - Keychain Helper
    private func saveKeychain(key: String, value: String) {
        guard let data = value.data(using: .utf8) else { return }
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecValueData as String: data
        ]
        SecItemDelete(query as CFDictionary)
        SecItemAdd(query as CFDictionary, nil)
    }

    private func readKeychain(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var item: CFTypeRef?
        if SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
           let data = item as? Data,
           let result = String(data: data, encoding: .utf8) {
            return result
        }
        return nil
    }
}
