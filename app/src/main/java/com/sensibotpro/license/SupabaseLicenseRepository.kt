package com.sensibotpro.license

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production implementation of [LicenseRepository] backed by Supabase and [SecureDeviceManager].
 *
 * Implements strict hardware-locking:
 * - Reads/writes encrypted session tokens.
 * - Hardware device identity bound permanently on server.
 * - Local storage wipe on uninstall generates a new device identity on reinstall,
 *   which triggers server-side rejection until manually reset by owner.
 */
class SupabaseLicenseRepository(
    private val context: Context,
    val secureDeviceManager: SecureDeviceManager = SecureDeviceManager(context),
    val client: SupabaseLicenseClient = SupabaseLicenseClient()
) : LicenseRepository {

    private val _licenseState = MutableStateFlow(
        LicenseInfo(
            isLicensed = false,
            licenseKey = null,
            boundInstallationId = secureDeviceManager.getOrCreateDeviceId(),
            activatedAt = null,
            planName = "Pro Lifetime License",
            statusDescription = "Checking license status..."
        )
    )
    override val licenseState: StateFlow<LicenseInfo> = _licenseState.asStateFlow()

    override suspend fun getInstallationId(): String {
        return secureDeviceManager.getOrCreateDeviceId()
    }

    override suspend fun activateLicense(key: String): ActivationResult {
        val deviceId = secureDeviceManager.getOrCreateDeviceId()
        val deviceInfo = client.getDeviceInfoString()

        return when (val res = client.validateLicense(key, deviceId, deviceInfo)) {
            is LicenseValidationResult.Success -> {
                secureDeviceManager.saveSessionToken(res.token)
                val info = LicenseInfo(
                    isLicensed = true,
                    licenseKey = null, // Do not store raw key
                    boundInstallationId = deviceId,
                    activatedAt = System.currentTimeMillis(),
                    planName = "Pro Lifetime License",
                    statusDescription = "Active • Bound to this device"
                )
                _licenseState.value = info
                ActivationResult.Success(info)
            }
            is LicenseValidationResult.Error -> {
                ActivationResult.Error(res.message)
            }
        }
    }

    override suspend fun checkLicenseStatus(): LicenseInfo {
        val token = secureDeviceManager.getSessionToken()
        val deviceId = secureDeviceManager.getOrCreateDeviceId()

        if (token.isNullOrBlank()) {
            val unauth = LicenseInfo(
                isLicensed = false,
                licenseKey = null,
                boundInstallationId = deviceId,
                activatedAt = null,
                statusDescription = "Inactive • License key required"
            )
            _licenseState.value = unauth
            return unauth
        }

        return when (val check = client.checkSession(token, deviceId)) {
            is SessionCheckResult.Valid -> {
                val active = LicenseInfo(
                    isLicensed = true,
                    licenseKey = null,
                    boundInstallationId = deviceId,
                    activatedAt = secureDeviceManager.getLastValidatedTimestamp(),
                    statusDescription = "Active • Verified with server"
                )
                _licenseState.value = active
                active
            }
            is SessionCheckResult.Invalid -> {
                // Wipe local token immediately if server rejects or revokes
                secureDeviceManager.clearSessionToken()
                val invalid = LicenseInfo(
                    isLicensed = false,
                    licenseKey = null,
                    boundInstallationId = deviceId,
                    activatedAt = null,
                    statusDescription = "Revoked or expired • ${check.reason}"
                )
                _licenseState.value = invalid
                invalid
            }
            is SessionCheckResult.NetworkError -> {
                // If offline and token was previously validated, keep status or flag network
                val hasRecentValidation = secureDeviceManager.getLastValidatedTimestamp() > 0
                val netState = LicenseInfo(
                    isLicensed = hasRecentValidation,
                    licenseKey = null,
                    boundInstallationId = deviceId,
                    activatedAt = secureDeviceManager.getLastValidatedTimestamp(),
                    statusDescription = if (hasRecentValidation) "Offline • Using cached session" else "Network error • Cannot verify license"
                )
                _licenseState.value = netState
                netState
            }
        }
    }

    override suspend fun requestDeviceTransfer(reason: String): Result<String> {
        return Result.success(
            "Device resets are performed manually by the administrator. Please contact Lyam FF support with your Device ID: ${secureDeviceManager.getOrCreateDeviceId()}"
        )
    }

    override fun isDevModeEnabled(): Boolean = false
}
