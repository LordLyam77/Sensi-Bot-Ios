package com.sensibotpro

import com.sensibotpro.domain.model.*
import com.sensibotpro.domain.recommendation.CalibrationEngine
import com.sensibotpro.domain.recommendation.CalibrationFeedback
import com.sensibotpro.domain.recommendation.RecommendationEngine
import org.junit.Assert.*
import org.junit.Test

class RecommendationEngineTest {

    private val sampleDevice = DeviceSpecs(
        manufacturer = "Samsung",
        model = "Galaxy S23",
        androidVersion = "Android 15",
        apiLevel = 35,
        totalRamGb = 8.0,
        availableRamGb = 4.5,
        refreshRate = 120,
        displayDensityDpi = 420,
        resolution = "1080x2340",
        performanceClass = DevicePerformanceClass.HIGH_PERFORMANCE
    )

    @Test
    fun testDeterministicRecommendation() {
        val rec1 = RecommendationEngine.generateRecommendation(
            deviceSpecs = sampleDevice,
            playstyle = Playstyle.AGGRESSIVE,
            fingerSetup = FingerSetup.TWO_FINGER,
            weaponType = WeaponType.SHOTGUN,
            problem = AimProblem.OVERSHOOTING_HEAD
        )

        val rec2 = RecommendationEngine.generateRecommendation(
            deviceSpecs = sampleDevice,
            playstyle = Playstyle.AGGRESSIVE,
            fingerSetup = FingerSetup.TWO_FINGER,
            weaponType = WeaponType.SHOTGUN,
            problem = AimProblem.OVERSHOOTING_HEAD
        )

        // Must be deterministic
        assertEquals(rec1.profile.general, rec2.profile.general)
        assertEquals(rec1.profile.redDot, rec2.profile.redDot)
        assertEquals(rec1.profile.scope2x, rec2.profile.scope2x)
        assertEquals(rec1.profile.fireButtonSize, rec2.profile.fireButtonSize)
        assertTrue(rec1.profile.general in 50..200)
    }

    @Test
    fun testCalibrationOvershootingReducesSensitivity() {
        val initialRec = RecommendationEngine.generateRecommendation(
            deviceSpecs = sampleDevice,
            playstyle = Playstyle.BALANCED,
            fingerSetup = FingerSetup.TWO_FINGER,
            weaponType = WeaponType.AR,
            problem = AimProblem.AIM_TOO_SLOW
        )

        val initialGeneral = initialRec.profile.general
        val calibResult = CalibrationEngine.calibrate(
            profile = initialRec.profile,
            feedback = CalibrationFeedback.OVERSHOOTING
        )

        assertTrue(calibResult.updatedProfile.general < initialGeneral)
        assertFalse(calibResult.isOptimal)
    }
}
