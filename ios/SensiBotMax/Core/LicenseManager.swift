import UIKit
import Combine
import Security
import CryptoKit

public enum LicenseValidationResult {
    case success(token: String)
    case error(message: String, isRateLimited: Bool = false)
}

public final class LicenseManager: ObservableObject {
    public static let shared = LicenseManager()

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
    private let keyKeychainVipKey = "sensibot_ios_vip_key"
    private let keyKeychainJwtToken = "sensibot_ios_jwt_token"
    private static let keychainService = "com.sensibotpro.ios.keychain"

    public init() {
        SupabaseConfig.logStartupConfig()
        self.hardwareId = getOrCreateHardwareId()

        // 1. Restore VIP key: Check Keychain first (persists across app deletion/reinstalls), then UserDefaults
        var savedKey = readKeychain(key: keyKeychainVipKey) ?? ""
        if savedKey.isEmpty {
            savedKey = UserDefaults.standard.string(forKey: keyUserDefault) ?? ""
            if !savedKey.isEmpty {
                saveKeychain(key: keyKeychainVipKey, value: savedKey)
            }
        }
        self.activeKey = savedKey

        // 2. Restore JWT token: Check Keychain first, then UserDefaults
        var savedToken = readKeychain(key: keyKeychainJwtToken) ?? ""
        if savedToken.isEmpty {
            savedToken = UserDefaults.standard.string(forKey: tokenUserDefault) ?? ""
            if !savedToken.isEmpty {
                saveKeychain(key: keyKeychainJwtToken, value: savedToken)
            }
        }

        if !activeKey.isEmpty && !savedToken.isEmpty {
            self.isActivated = true
            self.statusMessage = "VIP License Active! Bound to \(self.hardwareId)"
            // Keep UserDefaults in sync
            UserDefaults.standard.set(activeKey, forKey: keyUserDefault)
            UserDefaults.standard.set(savedToken, forKey: tokenUserDefault)
        }
        self.isInitialCheckComplete = true
    }

    /// Persistent hardware ID guaranteed to stay identical across app deletes and reinstalls on the same phone.
    /// 1. Reads from iOS Keychain (which persists across app uninstall/reinstall).
    /// 2. If Keychain was ever wiped, generates a deterministic hardware-locked device ID
    ///    from the immutable physical specs of this exact iPhone (zero random UUIDs).
    public func getOrCreateHardwareId() -> String {
        // Step 1: Check existing Keychain entry
        if let existing = readKeychain(key: keyKeychainHardwareId), !existing.isEmpty {
            return existing
        }

        // Step 2: Deterministic hardware-locked device ID derived from immutable physical hardware properties.
        // Even if the user deletes the app or changes certificates, the same physical phone will always
        // recompute the exact same device ID, preventing accidental lockouts on reinstall.
        let machine = DeviceProbe.machineIdentifier()
        let hwModel = getSysctlString("hw.model")
        let screen = UIScreen.main
        let screenBounds = "\(Int(screen.nativeBounds.width))x\(Int(screen.nativeBounds.height))@\(Int(screen.nativeScale))"
        let cpuCores = ProcessInfo.processInfo.activeProcessorCount
        let ramBytes = ProcessInfo.processInfo.physicalMemory
        let diskGB = totalDiskCapacityGB()

        let rawSeed = "IOS-HWID|\(machine)|\(hwModel)|\(screenBounds)|\(cpuCores)|\(ramBytes)|\(diskGB)"

        let hash = SHA256.hash(data: Data(rawSeed.utf8))
        let hex = hash.map { String(format: "%02X", $0) }.joined()
        let formattedHwid = "IOS-" + hex.prefix(16)

        // Save into Keychain with ThisDeviceOnly persistence
        saveKeychain(key: keyKeychainHardwareId, value: formattedHwid)
        return formattedHwid
    }

    /// Gathering device hardware and OS info for auditing in Supabase
    public func getDeviceInfoString() -> String {
        let profile = DeviceProbe.current()
        let sysVersion = UIDevice.current.systemVersion
        return "Apple \(profile.modelMarketingName) (\(profile.identifier)) • iOS \(sysVersion)"
    }

    /// Validates the license key against Supabase Edge Function with 1 automatic retry
    @MainActor
    public func activateKey(_ key: String) async -> (Bool, String) {
        let trimmed = key.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard !trimmed.isEmpty else {
            return (false, "Please enter your license key.")
        }

        self.isVerifying = true
        defer { self.isVerifying = false }

        // Attempt 1, followed by 1 retry on transient failure
        for attempt in 1...2 {
            print("[LicenseManager] Starting validation attempt \(attempt)/2 for key: \(trimmed.prefix(4))****")
            let result = await executeValidationRequest(key: trimmed)

            switch result {
            case .success(let token):
                // Persist to both UserDefaults and Keychain (Keychain survives app uninstall)
                UserDefaults.standard.set(trimmed, forKey: self.keyUserDefault)
                UserDefaults.standard.set(token, forKey: self.tokenUserDefault)
                self.saveKeychain(key: self.keyKeychainVipKey, value: trimmed)
                self.saveKeychain(key: self.keyKeychainJwtToken, value: token)

                self.activeKey = trimmed
                self.isActivated = true
                self.statusMessage = "VIP License Active! Bound to \(self.hardwareId)"
                return (true, "VIP License Activated Successfully!")

            case .failure(let error):
                // If it's a non-transient error (e.g. invalid key or device mismatch), don't retry
                if error.isFatal || attempt == 2 {
                    self.statusMessage = error.userMessage
                    return (false, error.userMessage)
                }

                print("[LicenseManager] Transient failure on attempt \(attempt): \(error.userMessage). Retrying in 1s...")
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
        }

        return (false, "Unable to validate license. Please check your network and try again.")
    }

    private struct ValidationError: Error {
        let userMessage: String
        let isFatal: Bool
    }

    private func executeValidationRequest(key: String) async -> Result<String, ValidationError> {
        guard let url = URL(string: SupabaseConfig.validateEndpoint) else {
            return .failure(ValidationError(userMessage: "Invalid server endpoint configuration.", isFatal: true))
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 12.0
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(SupabaseConfig.anonKey, forHTTPHeaderField: "apikey")
        request.setValue("Bearer \(SupabaseConfig.anonKey)", forHTTPHeaderField: "Authorization")

        let payload: [String: Any] = [
            "key": key,
            "device_id": self.hardwareId,
            "device_info": getDeviceInfoString()
        ]

        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: payload)
            let (data, response) = try await URLSession.shared.data(for: request)

            guard let httpResponse = response as? HTTPURLResponse else {
                return .failure(ValidationError(userMessage: "Invalid server response.", isFatal: false))
            }

            let responseBodyString = String(data: data, encoding: .utf8) ?? ""
            print("[LicenseManager] HTTP \(httpResponse.statusCode) -> Body: \(responseBodyString)")

            // Parse JSON response
            let json = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
            let serverError = (json["error"] as? String) ?? (json["message"] as? String)

            switch httpResponse.statusCode {
            case 200...299:
                if let token = json["token"] as? String {
                    return .success(token)
                } else if json["success"] as? Bool == true {
                    return .success("active_session")
                } else {
                    return .failure(ValidationError(userMessage: serverError ?? "Invalid license token.", isFatal: true))
                }

            case 400:
                let msg = serverError ?? "Invalid or inactive license key."
                return .failure(ValidationError(userMessage: msg, isFatal: true))

            case 403:
                let msg = serverError ?? "This key is already active on another device."
                return .failure(ValidationError(userMessage: msg, isFatal: true))

            case 429:
                let msg = serverError ?? "Too many attempts, try again later"
                return .failure(ValidationError(userMessage: msg, isFatal: true))

            case 500...599:
                let msg = "Server error (\(httpResponse.statusCode)). Please try again shortly."
                return .failure(ValidationError(userMessage: msg, isFatal: false))

            default:
                let msg = serverError ?? "Validation rejected (HTTP \(httpResponse.statusCode))."
                return .failure(ValidationError(userMessage: msg, isFatal: true))
            }

        } catch let urlError as URLError {
            print("[LicenseManager] URLError: \(urlError.code) - \(urlError.localizedDescription)")
            switch urlError.code {
            case .notConnectedToInternet:
                return .failure(ValidationError(userMessage: "You appear to be offline. Check your internet connection.", isFatal: false))
            case .timedOut:
                return .failure(ValidationError(userMessage: "License server timed out. Retrying...", isFatal: false))
            case .cannotFindHost, .cannotConnectToHost:
                return .failure(ValidationError(userMessage: "Cannot reach license server. Check your network or DNS.", isFatal: false))
            default:
                return .failure(ValidationError(userMessage: "Network error (\(urlError.code.rawValue)). Please check your connection.", isFatal: false))
            }
        } catch {
            print("[LicenseManager] General error: \(error.localizedDescription)")
            return .failure(ValidationError(userMessage: "Connection failed: \(error.localizedDescription)", isFatal: false))
        }
    }

    public func deactivate() {
        UserDefaults.standard.removeObject(forKey: keyUserDefault)
        UserDefaults.standard.removeObject(forKey: tokenUserDefault)
        deleteKeychain(key: keyKeychainVipKey)
        deleteKeychain(key: keyKeychainJwtToken)
        self.activeKey = ""
        self.isActivated = false
        self.statusMessage = "License cleared."
    }

    // MARK: - Hardware Introspection Helpers

    private func getSysctlString(_ name: String) -> String {
        var size: Int = 0
        sysctlbyname(name, nil, &size, nil, 0)
        guard size > 0 else { return "UNKNOWN" }
        var buffer = [CChar](repeating: 0, count: size)
        sysctlbyname(name, &buffer, &size, nil, 0)
        return String(cString: buffer)
    }

    private func totalDiskCapacityGB() -> Int64 {
        do {
            let attrs = try FileManager.default.attributesOfFileSystem(forPath: NSHomeDirectory())
            if let totalBytes = attrs[.systemSize] as? Int64 {
                // Round to nearest GB to stay stable across minor OS file-system adjustments
                return (totalBytes + (500 * 1024 * 1024)) / (1024 * 1024 * 1024)
            }
        } catch {}
        return 0
    }

    // MARK: - Keychain Helper
    // Uses kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly so:
    // 1. Data persists even when the app is uninstalled and reinstalled.
    // 2. Data is hardware-bound to THIS device and will never migrate to other devices via backups.

    private func saveKeychain(key: String, value: String) {
        guard let data = value.data(using: .utf8) else { return }

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Self.keychainService,
            kSecAttrAccount as String: key
        ]

        let attributesToUpdate: [String: Any] = [
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]

        let status = SecItemUpdate(query as CFDictionary, attributesToUpdate as CFDictionary)
        if status == errSecItemNotFound {
            var newItem = query
            newItem[kSecValueData as String] = data
            newItem[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            let addStatus = SecItemAdd(newItem as CFDictionary, nil)
            if addStatus != errSecSuccess {
                print("[Keychain] SecItemAdd failed with status: \(addStatus)")
            }
        } else if status != errSecSuccess {
            print("[Keychain] SecItemUpdate failed with status: \(status)")
        }
    }

    private func readKeychain(key: String) -> String? {
        // 1. Try modern query with explicit service
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Self.keychainService,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var item: CFTypeRef?
        var status = SecItemCopyMatching(query as CFDictionary, &item)
        if status == errSecSuccess, let data = item as? Data, let str = String(data: data, encoding: .utf8), !str.isEmpty {
            return str
        }

        // 2. Fallback: Check legacy query (without service) for backwards compatibility with earlier installs
        let legacyQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        item = nil
        status = SecItemCopyMatching(legacyQuery as CFDictionary, &item)
        if status == errSecSuccess, let data = item as? Data, let str = String(data: data, encoding: .utf8), !str.isEmpty {
            // Upgrade to modern keychain with ThisDeviceOnly attribute
            saveKeychain(key: key, value: str)
            return str
        }

        return nil
    }

    private func deleteKeychain(key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Self.keychainService,
            kSecAttrAccount as String: key
        ]
        SecItemDelete(query as CFDictionary)

        let legacyQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key
        ]
        SecItemDelete(legacyQuery as CFDictionary)
    }
}
