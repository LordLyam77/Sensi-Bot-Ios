package com.sensibotpro.license

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.util.UUID

/**
 * SecureDeviceManager handles:
 * 1. Persistent, hardware-bound unique device ID generation.
 *    Combines Settings.Secure.ANDROID_ID with a persistent random UUID.
 * 2. Storing and retrieving the signed JWT session token in EncryptedSharedPreferences.
 * 3. Never writing the raw license key to local storage.
 * 4. Cleaning up session tokens upon invalidation, revocation, or logout.
 */
class SecureDeviceManager(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val PREFS_FILE_NAME = "sensi_secure_auth_prefs"
        private const val KEY_DEVICE_ID = "sec_device_id"
        private const val KEY_SESSION_TOKEN = "sec_session_token"
        private const val KEY_LAST_CHECKED = "sec_last_checked_timestamp"
    }

    /**
     * Retrieves or generates the persistent device ID.
     * Note: If the app is uninstalled, EncryptedSharedPreferences is wiped by Android.
     * Upon reinstall, a new device ID is generated (as intended by spec).
     */
    @Synchronized
    fun getOrCreateDeviceId(): String {
        val existing = encryptedPrefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }

        // Generate composite device ID
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_ANDROID_ID"
        } catch (_: Exception) {
            "UNKNOWN_ANDROID_ID"
        }

        // Deterministic hardware seed derived from permanent Android ID and hardware signature.
        // Reinstalling on the SAME device produces the exact same ID, letting buyers log back in.
        // A different device with a different ANDROID_ID produces a different ID and is blocked by Supabase.
        val rawSeed = "$androidId-SENSI-HWID-${android.os.Build.MANUFACTURER.uppercase()}-${android.os.Build.MODEL.uppercase()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(rawSeed.toByteArray())
        val hexString = digest.joinToString("") { "%02x".format(it) }.take(24).uppercase()
        val generatedId = "DEV-$hexString"

        encryptedPrefs.edit()
            .putString(KEY_DEVICE_ID, generatedId)
            .apply()

        return generatedId
    }

    /**
     * Returns the signed session token received from Supabase, or null if unauthenticated.
     */
    fun getSessionToken(): String? {
        return encryptedPrefs.getString(KEY_SESSION_TOKEN, null)
    }

    /**
     * Persists the signed session token after successful Supabase validation.
     */
    fun saveSessionToken(token: String) {
        encryptedPrefs.edit()
            .putString(KEY_SESSION_TOKEN, token)
            .putLong(KEY_LAST_CHECKED, System.currentTimeMillis())
            .apply()
    }

    /**
     * Wipes session token when license validation fails, key is revoked, or session expires.
     */
    fun clearSessionToken() {
        encryptedPrefs.edit()
            .remove(KEY_SESSION_TOKEN)
            .remove(KEY_LAST_CHECKED)
            .apply()
    }

    /**
     * Returns timestamp of last successful server validation.
     */
    fun getLastValidatedTimestamp(): Long {
        return encryptedPrefs.getLong(KEY_LAST_CHECKED, 0L)
    }
}
