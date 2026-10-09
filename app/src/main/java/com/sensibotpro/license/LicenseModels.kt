package com.sensibotpro.license

data class LicenseInfo(
    val isLicensed: Boolean,
    val licenseKey: String?,
    val boundInstallationId: String,
    val activatedAt: Long?,
    val planName: String = "Pro Lifetime Access (₹299)",
    val statusDescription: String
)

sealed class ActivationResult {
    data class Success(val info: LicenseInfo) : ActivationResult()
    data class Error(val message: String) : ActivationResult()
}
