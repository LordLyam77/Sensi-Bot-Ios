package com.sensibotpro

import com.sensibotpro.domain.model.*
import com.sensibotpro.domain.recommendation.RecommendationEngine
import com.sensibotpro.headlock.*
import org.junit.Assert.*
import org.junit.Test

class HeadlockModuleTest {

    private class FakeHeadlockBridge(
        var permissionGranted: Boolean = true
    ) : HeadlockBridge {
        var enableCalls = 0
        var disableCalls = 0
        var enabled = false
        var storedConfig = HeadlockConfig()

        override fun enable(): HeadlockEnableResult {
            enableCalls++
            if (!permissionGranted) {
                return HeadlockEnableResult(ok = false, needsAccessibilityPermission = true)
            }
            enabled = true
            return HeadlockEnableResult(ok = true, needsAccessibilityPermission = false)
        }

        override fun disable() {
            disableCalls++
            enabled = false
        }

        override fun isEnabled(): Boolean = enabled

        override fun setConfig(cfg: HeadlockConfig): Boolean {
            storedConfig = cfg.clamped()
            return true
        }

        override fun getConfig(): HeadlockConfig = storedConfig

        override fun recommend(deviceMetrics: HeadlockDeviceMetrics): HeadlockConfig =
            HeadlockCalibration.recommend(deviceMetrics)

        override fun installTouchProfile(): HeadlockProfileInstallResult =
            HeadlockProfileInstallResult(ok = false, unsupported = true)

        override fun openSystemSettings(): Boolean = true
    }

    @Test
    fun recommendReturnsClampedInRangeForAllFourDeviceClasses() {
        val cases = listOf(
            HeadlockDeviceMetrics(HeadlockDeviceClass.COMPACT, screenHeightPx = 1334, refreshRateHz = 60),
            HeadlockDeviceMetrics(HeadlockDeviceClass.PHONE, screenHeightPx = 2400, refreshRateHz = 90),
            HeadlockDeviceMetrics(HeadlockDeviceClass.LARGE, screenHeightPx = 2796, refreshRateHz = 120),
            HeadlockDeviceMetrics(HeadlockDeviceClass.TABLET, screenHeightPx = 3200, refreshRateHz = 120)
        )

        for (metrics in cases) {
            val rec = HeadlockCalibration.recommend(metrics)
            assertEquals(1, rec.v)
            assertEquals(metrics.deviceClass, rec.deviceClass)
            assertTrue(rec.startX in 0.05..0.95)
            assertTrue(rec.startY in 0.05..0.95)
            assertTrue(rec.distancePx in 10..400)
            assertTrue(rec.durationMs in 20..500)
            assertTrue(rec.repeatIntervalMs in 15..500)
            assertTrue(rec.burstCount in 1..10)
            assertTrue(rec.verticalBias in 0.6..1.6)
        }
    }

    @Test
    fun configRoundTripsWithoutDriftAndClampsOnRead() {
        val original = HeadlockConfig(
            v = 1,
            mode = HeadlockMode.BURST,
            startX = 0.77,
            startY = 0.81,
            distancePx = 58,
            durationMs = 85,
            repeatIntervalMs = 45,
            burstCount = 4,
            curve = HeadlockCurve.EASE_OUT,
            verticalBias = 1.3,
            deviceClass = HeadlockDeviceClass.LARGE
        )

        val json = original.toJson()
        val decoded = HeadlockConfig.fromJson(json)
        assertEquals(original, decoded)

        val outOfBoundsJson = """
            {"v":0,"mode":"hold","startX":1.9,"startY":-0.2,"distancePx":999,"durationMs":5,"repeatIntervalMs":1,"burstCount":50,"curve":"linear","verticalBias":3.5,"deviceClass":"phone"}
        """.trimIndent()
        val clamped = HeadlockConfig.fromJson(outOfBoundsJson)
        assertEquals(1, clamped.v)
        assertEquals(0.95, clamped.startX, 0.0001)
        assertEquals(0.05, clamped.startY, 0.0001)
        assertEquals(400, clamped.distancePx)
        assertEquals(20, clamped.durationMs)
        assertEquals(15, clamped.repeatIntervalMs)
        assertEquals(10, clamped.burstCount)
        assertEquals(1.6, clamped.verticalBias, 0.0001)
    }

    @Test
    fun stateMachineDoubleEnableIsIdempotentAndDisableClearsInFlight() {
        val bridge = FakeHeadlockBridge(permissionGranted = true)
        val controller = HeadlockController(bridge)

        val first = controller.enable()
        assertTrue(first.ok)
        assertEquals(HeadlockState.ENABLED, controller.snapshot.state)
        assertEquals(1, bridge.enableCalls)

        // Double enable must be idempotent
        val second = controller.enable()
        assertTrue(second.ok)
        assertEquals(1, bridge.enableCalls)

        // In-flight guard + disable clearing
        assertTrue(controller.markGestureInFlight(true))
        assertFalse(controller.markGestureInFlight(true))
        controller.disable()
        assertEquals(HeadlockState.DISABLED, controller.snapshot.state)
        assertFalse(controller.snapshot.inFlightGesture)
    }

    @Test
    fun permissionDeniedMovesToRecoverablePromptState() {
        val bridge = FakeHeadlockBridge(permissionGranted = false)
        val controller = HeadlockController(bridge)

        val res = controller.enable()
        assertFalse(res.ok)
        assertTrue(res.needsAccessibilityPermission)
        assertEquals(HeadlockState.NEEDS_PERMISSION, controller.snapshot.state)
    }

    @Test
    fun goldenExistingSensiOutputUnchangedWithFlagOffAndOn() {
        val sampleDevice = DeviceSpecs(
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

        val previousFlag = HeadlockModule.isFeatureEnabled
        try {
            HeadlockModule.isFeatureEnabled = false
            val offRec = RecommendationEngine.generateRecommendation(
                deviceSpecs = sampleDevice,
                playstyle = Playstyle.AGGRESSIVE,
                fingerSetup = FingerSetup.TWO_FINGER,
                weaponType = WeaponType.SHOTGUN,
                problem = AimProblem.OVERSHOOTING_HEAD
            )

            HeadlockModule.isFeatureEnabled = true
            val onRec = RecommendationEngine.generateRecommendation(
                deviceSpecs = sampleDevice,
                playstyle = Playstyle.AGGRESSIVE,
                fingerSetup = FingerSetup.TWO_FINGER,
                weaponType = WeaponType.SHOTGUN,
                problem = AimProblem.OVERSHOOTING_HEAD
            )

            assertEquals(offRec.profile, onRec.profile)
            assertEquals(offRec.recommendedDpi, onRec.recommendedDpi)
            assertEquals(offRec.dragTechnique, onRec.dragTechnique)
            assertEquals(offRec.explanation, onRec.explanation)
        } finally {
            HeadlockModule.isFeatureEnabled = previousFlag
        }
    }
}

