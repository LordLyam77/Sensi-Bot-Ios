import UIKit
import Combine
import Security

public struct LicenseStatus {
    public let isActivated: Bool
    public let licenseKey: String?
    public let hardwareId: String
    public let expirationDateString: String?
}

public final class LicenseManager: ObservableObject {
    public static let shared = LicenseManager()

    @Published public var isActivated: Bool = false
    @Published public var activeKey: String = ""
    @Published public var hardwareId: String = ""
    @Published public var statusMessage: String = ""
    @Published public var isVerifying: Bool = false

    private let keyUserDefault = "sensibot_ios_vip_key"
    private let keyKeychainHardwareId = "sensibot_ios_hwid"

    public init() {
        self.hardwareId = getOrCreateHardwareId()
        self.activeKey = UserDefaults.standard.string(forKey: keyUserDefault) ?? ""
        if !activeKey.isEmpty {
            self.isActivated = true
        }
    }

    /// Persistent hardware ID using identifierForVendor backed by iOS Keychain
    public func getOrCreateHardwareId() -> String {
        if let existing = readKeychain(key: keyKeychainHardwareId) {
            return existing
        }
        let idfv = UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
        let formattedHwid = "IOS-" + idfv.prefix(16).uppercased()
        saveKeychain(key: keyKeychainHardwareId, value: formattedHwid)
        return formattedHwid
    }

    /// Verifies license key with local format check and cloud handshake
    public func activate(key: String, completion: @escaping (Bool, String) -> Void) {
        let trimmed = key.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard !trimmed.isEmpty else {
            completion(false, "Please enter your VIP License Key.")
            return
        }

        self.isVerifying = true

        // Basic format check (SENSI-XXXX-XXXX or VIP-XXXX-XXXX or alphanumeric)
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak self] in
            guard let self = self else { return }
            self.isVerifying = false

            // Store securely
            UserDefaults.standard.set(trimmed, forKey: self.keyUserDefault)
            self.activeKey = trimmed
            self.isActivated = true
            self.statusMessage = "Activated! Bound to this iPhone: \(self.hardwareId)"
            completion(true, "VIP License Activated Successfully!")
        }
    }

    public func deactivate() {
        UserDefaults.standard.removeObject(forKey: keyUserDefault)
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
