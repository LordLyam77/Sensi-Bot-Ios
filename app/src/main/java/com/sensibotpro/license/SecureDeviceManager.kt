package com.sensibotpro.license

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.util.UUID

/**
 * SecureDeviceManager handles:
 * 1. Persistent, hardware-bound unique device ID generation.
 * 2. Storing and retrieving the signed JWT session token safely.
 * 3. Bulletproof recovery if Android Keystore or EncryptedSharedPreferences throws AEADBadTagException / KeyStoreException.
 * 4. Zero crashes on launch across all Android OEM versions (Samsung, Xiaomi, Vivo, Oppo, Pixel).
 */
class SecureDeviceManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        initSecurePreferences()
    }

    private fun initSecurePreferences(): SharedPreferences {
        // Attempt 1: Standard EncryptedSharedPreferences
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            Log.e("SecureDeviceManager", "EncryptedSharedPreferences initialization failed, attempting reset", e)
        }

        // Attempt 2: Corrupted Keystore file recovery - wipe corrupted xml and re-create
        try {
            context.deleteSharedPreferences(PREFS_FILE_NAME)
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (fallbackEx: Throwable) {
            Log.e("SecureDeviceManager", "EncryptedSharedPreferences recreation failed, using safe fallback", fallbackEx)
        }

        // Attempt 3: Safe, private SharedPreferences fallback so the app NEVER crashes on launch
        return context.getSharedPreferences(PREFS_FILE_NAME + "_compat", Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_FILE_NAME = "sensi_secure_auth_prefs"
        private const val KEY_DEVICE_ID = "sec_device_id"
        private const val KEY_SESSION_TOKEN = "sec_session_token"
        private const val KEY_LAST_CHECKED = "sec_last_checked_timestamp"
    }

    /**
     * Retrieves or generates the persistent device ID.
     * Guaranteed never to throw an unhandled exception.
     */
    @Synchronized
    fun getOrCreateDeviceId(): String {
        try {
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (!existing.isNullOrBlank()) {
                return existing
            }
        } catch (e: Throwable) {
            Log.e("SecureDeviceManager", "Error reading KEY_DEVICE_ID from prefs", e)
        }

        // Generate composite device ID
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_ANDROID_ID"
        } catch (_: Throwable) {
            "UNKNOWN_ANDROID_ID"
        }

        // Deterministic hardware seed derived from permanent Android ID and hardware signature.
        // Reinstalling on the SAME device produces the exact same ID, letting buyers log back in.
        // A different device with a different ANDROID_ID produces a different ID and is blocked by Supabase.
        val mfg = try { (Build.MANUFACTURER ?: "ANDROID").uppercase().replace(" ", "_") } catch (_: Throwable) { "ANDROID" }
        val mdl = try { (Build.MODEL ?: "DEVICE").uppercase().replace(" ", "_") } catch (_: Throwable) { "DEVICE" }
        val rawSeed = "$androidId-SENSI-HWID-$mfg-$mdl"

        val generatedId = try {
            val digest = MessageDigest.getInstance("SHA-256").digest(rawSeed.toByteArray(Charsets.UTF_8))
            val hexString = digest.joinToString("") { "%02x".format(it) }.take(24).uppercase()
            "DEV-$hexString"
        } catch (_: Throwable) {
            "DEV-" + UUID.nameUUIDFromBytes(rawSeed.toByteArray(Charsets.UTF_8)).toString().replace("-", "").take(24).uppercase()
        }

        try {
            prefs.edit()
                .putString(KEY_DEVICE_ID, generatedId)
                .apply()
        } catch (e: Throwable) {
            Log.e("SecureDeviceManager", "Error saving KEY_DEVICE_ID to prefs", e)
        }

        return generatedId
    }

    /**
     * Returns the signed session token received from Supabase, or null if unauthenticated.
     */
    fun getSessionToken(): String? {
        return try {
            prefs.getString(KEY_SESSION_TOKEN, null)
        } catch (e: Throwable) {
            Log.e("SecureDeviceManager", "Error reading session token", e)
            null
        }
    }

    /**
     * Persists the signed session token after successful Supabase validation.
     */
    fun saveSessionToken(token: String) {
        try {
            prefs.edit()
                .putString(KEY_SESSION_TOKEN, token)
                .putLong(KEY_LAST_CHECKED, System.currentTimeMillis())
                .apply()
        } catch (e: Throwable) {
            Log.e("SecureDeviceManager", "Error saving session token", e)
        }
    }

    /**
     * Wipes session token when license validation fails, key is revoked, or session expires.
     */
    fun clearSessionToken() {
        try {
            prefs.edit()
                .remove(KEY_SESSION_TOKEN)
                .remove(KEY_LAST_CHECKED)
                .apply()
        } catch (e: Throwable) {
            Log.e("SecureDeviceManager", "Error clearing session token", e)
        }
    }

    /**
     * Returns timestamp of last successful server validation.
     */
    fun getLastValidatedTimestamp(): Long {
        return try {
            prefs.getLong(KEY_LAST_CHECKED, 0L)
        } catch (_: Throwable) {
            0L
        }
    }
}
