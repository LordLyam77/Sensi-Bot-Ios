package com.sensibotpro.ui.screens.touchlab

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sensibotpro.data.local.ProfileDatabaseHelper
import com.sensibotpro.data.repository.ProfileRepository
import com.sensibotpro.device.DeviceInfoProvider
import com.sensibotpro.domain.model.DeviceSpecs
import com.sensibotpro.domain.touch.SwipeSample
import com.sensibotpro.domain.touch.TouchAnalysisEngine
import com.sensibotpro.domain.touch.TouchLabAnalysisResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.hypot

enum class TouchLabPhase {
    IDLE,
    SWIPING,
    CALCULATING,
    COMPLETED
}

class TouchLabViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application
    private val deviceInfoProvider = DeviceInfoProvider(application)
    val deviceSpecs: DeviceSpecs = deviceInfoProvider.getDeviceSpecs()
    private val profileRepository = ProfileRepository(ProfileDatabaseHelper(application))

    private val _phase = MutableStateFlow(TouchLabPhase.IDLE)
    val phase: StateFlow<TouchLabPhase> = _phase.asStateFlow()

    private val _swipes = MutableStateFlow<List<SwipeSample>>(emptyList())
    val swipes: StateFlow<List<SwipeSample>> = _swipes.asStateFlow()

    private val _currentSwipeVelocity = MutableStateFlow(0f)
    val currentSwipeVelocity: StateFlow<Float> = _currentSwipeVelocity.asStateFlow()

    private val _analysisResult = MutableStateFlow<TouchLabAnalysisResult?>(null)
    val analysisResult: StateFlow<TouchLabAnalysisResult?> = _analysisResult.asStateFlow()

    private val _isApplied = MutableStateFlow(false)
    val isApplied: StateFlow<Boolean> = _isApplied.asStateFlow()

    private val vibrator = ContextCompat.getSystemService(application, Vibrator::class.java)

    fun startSession() {
        _swipes.value = emptyList()
        _analysisResult.value = null
        _isApplied.value = false
        _currentSwipeVelocity.value = 0f
        _phase.value = TouchLabPhase.SWIPING
    }

    fun recordSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long,
        midX: Float = (startX + endX) / 2f
    ) {
        if (_phase.value != TouchLabPhase.SWIPING) return

        val clampedDuration = durationMs.coerceAtLeast(16L) // Minimum 1 frame at 60Hz
        val deltaX = endX - startX
        val deltaY = endY - startY
        val distancePx = hypot(deltaX, deltaY)

        // Calculate velocity in pixels per second
        val velocityPxPerSec = (distancePx / clampedDuration) * 1000f

        // Check curvature (J-drag vs straight flick)
        // If mid point deviates horizontally by > 25px, it's a curved J-drag
        val isCurved = kotlin.math.abs(midX - startX) > 25f

        val angle = Math.toDegrees(atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()

        val sample = SwipeSample(
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            distancePx = distancePx,
            durationMs = clampedDuration,
            velocityPxPerSec = velocityPxPerSec,
            angleDegrees = angle,
            isCurvedJDrag = isCurved
        )

        val updated = _swipes.value.toMutableList().apply { add(sample) }
        _swipes.value = updated
        _currentSwipeVelocity.value = velocityPxPerSec

        triggerHaptic()

        // After 5 swipes, trigger diagnosis
        if (updated.size >= 5) {
            finishAndAnalyze()
        }
    }

    private fun finishAndAnalyze() {
        viewModelScope.launch {
            _phase.value = TouchLabPhase.CALCULATING
            delay(1400) // Sci-fi calculating simulation
            val result = TouchAnalysisEngine.analyzeSwipes(_swipes.value, deviceSpecs)
            _analysisResult.value = result
            _phase.value = TouchLabPhase.COMPLETED
        }
    }

    fun applyCalibratedProfile() {
        val res = _analysisResult.value ?: return
        viewModelScope.launch {
            profileRepository.saveProfile(res.generatedProfile)
            profileRepository.setActiveProfile(res.generatedProfile.id)
            _isApplied.value = true
            triggerHaptic()
        }
    }

    private fun triggerHaptic() {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(35)
                }
            }
        } catch (_: Exception) {}
    }
}
