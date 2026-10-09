package com.sensibotpro.license

/**
 * Supabase project configuration for license validation.
 *
 * IMPORTANT SECURITY NOTE:
 * - Only the public Anon Key is used here.
 * - The service_role key is NEVER placed in Android code.
 * - All elevated table operations and JWT signing occur inside Supabase Edge Functions.
 */
object SupabaseConfig {
    var PROJECT_URL: String = "https://xvmrtmyjnbxpsgpikths.supabase.co"
    var ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh2bXJ0bXlqbmJ4cHNncGlrdGhzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2Mzc4MDUsImV4cCI6MjEwNTIxMzgwNX0.xzRhS5vjsclhHQgP4yJNtrZFcWMVRH090im4YyQf1BU"

    val VALIDATE_LICENSE_ENDPOINT: String
        get() = "$PROJECT_URL/functions/v1/validate_license"

    val CHECK_SESSION_ENDPOINT: String
        get() = "$PROJECT_URL/functions/v1/check_session"
}
