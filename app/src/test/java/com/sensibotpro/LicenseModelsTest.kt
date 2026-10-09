package com.sensibotpro

import com.sensibotpro.license.LicenseValidationResult
import com.sensibotpro.license.SessionCheckResult
import org.junit.Assert.*
import org.junit.Test

class LicenseModelsTest {

    @Test
    fun testLicenseValidationResultTypes() {
        val success = LicenseValidationResult.Success("jwt-sample-token-12345")
        assertEquals("jwt-sample-token-12345", success.token)

        val errorGeneric = LicenseValidationResult.Error("Invalid or inactive key")
        assertEquals("Invalid or inactive key", errorGeneric.message)
        assertFalse(errorGeneric.isRateLimited)

        val errorRateLimit = LicenseValidationResult.Error("Too many attempts, try again later", isRateLimited = true)
        assertTrue(errorRateLimit.isRateLimited)

        val errorMismatch = LicenseValidationResult.Error("This key is already active on another device.")
        assertEquals("This key is already active on another device.", errorMismatch.message)
    }

    @Test
    fun testSessionCheckResultTypes() {
        val valid = SessionCheckResult.Valid
        assertNotNull(valid)

        val invalid = SessionCheckResult.Invalid("Device identity mismatch.")
        assertEquals("Device identity mismatch.", invalid.reason)

        val netErr = SessionCheckResult.NetworkError("Unable to connect to license server.")
        assertEquals("Unable to connect to license server.", netErr.message)
    }
}
