import UIKit
import Combine
import Security

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

    public init() {
        SupabaseConfig.logStartupConfig()
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
                UserDefaults.standard.set(trimmed, forKey: self.keyUserDefault)
                UserDefaults.standard.set(token, forKey: self.tokenUserDefault)
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

    private struct ValidationError {
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
