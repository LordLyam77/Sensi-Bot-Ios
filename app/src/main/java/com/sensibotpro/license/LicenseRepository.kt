package com.sensibotpro.license

import kotlinx.coroutines.flow.StateFlow

interface LicenseRepository {
    val licenseState: StateFlow<LicenseInfo>

    suspend fun getInstallationId(): String
    suspend fun activateLicense(key: String): ActivationResult
    suspend fun checkLicenseStatus(): LicenseInfo
    suspend fun requestDeviceTransfer(reason: String): Result<String>
    fun isDevModeEnabled(): Boolean
}
