package com.sensibotpro.license

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MockLicenseRepository(context: Context) : LicenseRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("sensi_bot_license_prefs", Context.MODE_PRIVATE)

    private val installationId: String
    private val _licenseState: MutableStateFlow<LicenseInfo>
    override val licenseState: StateFlow<LicenseInfo>

    init {
        var id = prefs.getString("key_installation_id", null)
        if (id == null) {
            id = "INST-" + UUID.randomUUID().toString().take(12).uppercase()
            prefs.edit().putString("key_installation_id", id).apply()
        }
        installationId = id

        val isLicensed = prefs.getBoolean("key_is_licensed", false)
        val key = prefs.getString("key_license_key", null)
        val activatedAt = if (isLicensed) prefs.getLong("key_activated_at", System.currentTimeMillis()) else null

        _licenseState = MutableStateFlow(
            LicenseInfo(
                isLicensed = isLicensed,
                licenseKey = key,
                boundInstallationId = installationId,
                activatedAt = activatedAt,
                statusDescription = if (isLicensed) "Active • Bound to this Device" else "Inactive • License Key Required"
            )
        )
        licenseState = _licenseState.asStateFlow()
    }

    override suspend fun getInstallationId(): String = installationId

    override suspend fun activateLicense(key: String): ActivationResult {
        val cleanKey = key.trim().uppercase()

        if (cleanKey.isEmpty()) {
            return ActivationResult.Error("Please enter a valid license key.")
        }

        // Development key: DEV-TEST-299
        val isValidDevKey = cleanKey == "DEV-TEST-299"
        // Standard pattern: SENSI-PRO-XXXX-XXXX
        val isValidStandardPattern = cleanKey.startsWith("SENSI-PRO-") && cleanKey.length >= 15

        if (!isValidDevKey && !isValidStandardPattern) {
            return ActivationResult.Error("Invalid license key. For testing, use the developer key 'DEV-TEST-299'.")
        }

        val now = System.currentTimeMillis()
        prefs.edit()
            .putBoolean("key_is_licensed", true)
            .putString("key_license_key", cleanKey)
            .putLong("key_activated_at", now)
            .apply()

        val updated = LicenseInfo(
            isLicensed = true,
            licenseKey = cleanKey,
            boundInstallationId = installationId,
            activatedAt = now,
            statusDescription = "Active • Bound to this Device"
        )
        _licenseState.value = updated
        return ActivationResult.Success(updated)
    }

    override suspend fun checkLicenseStatus(): LicenseInfo {
        return _licenseState.value
    }

    override suspend fun requestDeviceTransfer(reason: String): Result<String> {
        val ticketId = "REQ-" + UUID.randomUUID().toString().take(8).uppercase()
        // In real backend, this creates a device reset ticket on server
        return Result.success("Transfer request submitted (Ticket: $ticketId). Support will review your installation ID: $installationId within 24 hours.")
    }

    override fun isDevModeEnabled(): Boolean = true
}
