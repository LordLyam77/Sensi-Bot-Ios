import Foundation

public enum SupabaseConfig {
    public static let projectUrl = "https://xvmrtmyjnbxpsgpikths.supabase.co"
    public static let anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh2bXJ0bXlqbmJ4cHNncGlrdGhzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2Mzc4MDUsImV4cCI6MjEwNTIxMzgwNX0.xzRhS5vjsclhHQgP4yJNtrZFcWMVRH090im4YyQf1BU"
    
    public static let validateEndpoint = "\(projectUrl)/functions/v1/validate_license"
    public static let checkSessionEndpoint = "\(projectUrl)/functions/v1/check_session"

    public static func logStartupConfig() {
        let urlValid = !projectUrl.isEmpty && projectUrl.hasPrefix("https://")
        let keyValid = !anonKey.isEmpty && anonKey.count > 50
        let maskedKey = anonKey.prefix(6) + "..." + anonKey.suffix(4)
        print("[SupabaseConfig] iOS Target Configured -> URL: \(projectUrl) (valid: \(urlValid)), AnonKey: \(maskedKey) (len: \(anonKey.count), valid: \(keyValid))")
    }
}
