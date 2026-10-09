package com.sensibotpro.license

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed class LicenseValidationResult {
    data class Success(val token: String) : LicenseValidationResult()
    data class Error(val message: String, val isRateLimited: Boolean = false) : LicenseValidationResult()
}

sealed class SessionCheckResult {
    object Valid : SessionCheckResult()
    data class Invalid(val reason: String) : SessionCheckResult()
    data class NetworkError(val message: String) : SessionCheckResult()
}

/**
 * SupabaseLicenseClient communicates with the Supabase Edge Functions:
 * - validate_license
 * - check_session
 *
 * Implements strict error mappings adhering to the specification:
 * - Invalid or revoked key -> "Invalid or inactive key"
 * - Device mismatch -> "This key is already active on another device."
 * - Rate limit (HTTP 429) -> "Too many attempts, try again later"
 * - Network errors -> "Unable to connect to license server. Check your internet connection."
 */
class SupabaseLicenseClient {

    /**
     * Gathers device hardware and OS info for auditing in the licenses table.
     */
    fun getDeviceInfoString(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val os = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        return "$manufacturer $model • $os"
    }

    suspend fun validateLicense(
        key: String,
        deviceId: String,
        deviceInfo: String = getDeviceInfoString()
    ): LicenseValidationResult = withContext(Dispatchers.IO) {
        val cleanKey = key.trim().uppercase()
        if (cleanKey.isEmpty()) {
            return@withContext LicenseValidationResult.Error("Please enter your license key.")
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(SupabaseConfig.VALIDATE_LICENSE_ENDPOINT)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 12000
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
            }

            val payload = JSONObject().apply {
                put("key", cleanKey)
                put("device_id", deviceId)
                put("device_info", deviceInfo)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

            if (responseCode == 429) {
                return@withContext LicenseValidationResult.Error(
                    message = "Too many attempts, try again later",
                    isRateLimited = true
                )
            }

            val json = try {
                JSONObject(responseText)
            } catch (_: Exception) {
                JSONObject()
            }

            if (responseCode in 200..299 && json.optBoolean("success", false)) {
                val token = json.optString("token")
                if (token.isNotBlank()) {
                    LicenseValidationResult.Success(token)
                } else {
                    LicenseValidationResult.Error("Invalid server response. Please try again.")
                }
            } else {
                val serverError = json.optString("error", "")
                val userFriendlyMessage = when {
                    responseCode == 403 || serverError.contains("already active", ignoreCase = true) ->
                        "This key is already active on another device."
                    serverError.contains("Too many attempts", ignoreCase = true) ->
                        "Too many attempts, try again later"
                    else ->
                        "Invalid or inactive key"
                }
                LicenseValidationResult.Error(userFriendlyMessage)
            }
        } catch (_: java.net.UnknownHostException) {
            LicenseValidationResult.Error("Unable to connect to license server. Check your internet connection.")
        } catch (_: java.net.SocketTimeoutException) {
            LicenseValidationResult.Error("License server connection timed out. Check your internet connection.")
        } catch (e: Exception) {
            LicenseValidationResult.Error(e.message ?: "Unable to connect to license server.")
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun checkSession(
        token: String,
        deviceId: String
    ): SessionCheckResult = withContext(Dispatchers.IO) {
        if (token.isBlank() || deviceId.isBlank()) {
            return@withContext SessionCheckResult.Invalid("No active session.")
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(SupabaseConfig.CHECK_SESSION_ENDPOINT)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
            }

            val payload = JSONObject().apply {
                put("token", token)
                put("device_id", deviceId)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

            val json = try {
                JSONObject(responseText)
            } catch (_: Exception) {
                JSONObject()
            }

            if (responseCode in 200..299 && json.optBoolean("valid", false)) {
                SessionCheckResult.Valid
            } else {
                val errorReason = json.optString("error", "Session is no longer valid.")
                SessionCheckResult.Invalid(errorReason)
            }
        } catch (_: java.net.UnknownHostException) {
            SessionCheckResult.NetworkError("Unable to connect to license server. Check your internet connection.")
        } catch (_: java.net.SocketTimeoutException) {
            SessionCheckResult.NetworkError("License server connection timed out.")
        } catch (e: Exception) {
            SessionCheckResult.NetworkError(e.message ?: "Connection error.")
        } finally {
            connection?.disconnect()
        }
    }
}
