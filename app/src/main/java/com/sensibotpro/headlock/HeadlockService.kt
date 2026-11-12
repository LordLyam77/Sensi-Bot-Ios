package com.sensibotpro.headlock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Isolated AccessibilityService for calibrated drag-gesture dispatch.
 * No screen capture, no CV, no target detection, no auto-fire.
 */
class HeadlockService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val gestureInFlight = AtomicBoolean(false)
    private val holdActive = AtomicBoolean(false)
    private var burstRemaining = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally unused — Headlock does not inspect window/screen content.
    }

    override fun onInterrupt() {
        abortInFlight()
    }

    override fun onDestroy() {
        abortInFlight()
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    fun triggerHeadlock(config: HeadlockConfig) {
        val cfg = config.clamped()
        // Guard: if a gesture is already in flight, drop the new one — never queue.
        if (!gestureInFlight.compareAndSet(false, true)) {
            return
        }

        when (cfg.mode) {
            HeadlockMode.HOLD -> {
                holdActive.set(true)
                dispatchSingleStroke(cfg) { completed ->
                    if (completed && holdActive.get()) {
                        handler.post {
                            if (holdActive.get()) {
                                gestureInFlight.set(false)
                                triggerHeadlock(cfg)
                            } else {
                                gestureInFlight.set(false)
                            }
                        }
                    } else {
                        gestureInFlight.set(false)
                    }
                }
            }
            HeadlockMode.BURST -> {
                holdActive.set(false)
                burstRemaining = cfg.burstCount
                dispatchBurstStep(cfg)
            }
        }
    }

    fun stopHold() {
        holdActive.set(false)
    }

    fun abortInFlight() {
        holdActive.set(false)
        burstRemaining = 0
        handler.removeCallbacksAndMessages(null)
        gestureInFlight.set(false)
    }

    private fun dispatchBurstStep(cfg: HeadlockConfig) {
        if (burstRemaining <= 0) {
            gestureInFlight.set(false)
            return
        }
        burstRemaining--
        dispatchSingleStroke(cfg) { completed ->
            if (completed && burstRemaining > 0) {
                handler.postDelayed(
                    { dispatchBurstStep(cfg) },
                    cfg.repeatIntervalMs.toLong()
                )
            } else {
                burstRemaining = 0
                gestureInFlight.set(false)
            }
        }
    }

    private fun dispatchSingleStroke(cfg: HeadlockConfig, onDone: (Boolean) -> Unit) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)

        val startX = (cfg.startX * dm.widthPixels).toFloat().coerceIn(1f, (dm.widthPixels - 1).toFloat())
        val startY = (cfg.startY * dm.heightPixels).toFloat().coerceIn(1f, (dm.heightPixels - 1).toFloat())
        val dragDy = (cfg.distancePx * cfg.verticalBias).toFloat()
        val endY = (startY - dragDy).coerceIn(1f, (dm.heightPixels - 1).toFloat())

        val path = Path().apply {
            moveTo(startX, startY)
            when (cfg.curve) {
                HeadlockCurve.LINEAR -> lineTo(startX, endY)
                HeadlockCurve.EASE_OUT -> {
                    val midY = startY - (dragDy * 0.72f)
                    quadTo(startX, midY.coerceIn(1f, (dm.heightPixels - 1).toFloat()), startX, endY)
                }
            }
        }

        val stroke = GestureDescription.StrokeDescription(path, 0L, cfg.durationMs.toLong())
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onDone(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onDone(false)
            }
        }, handler)

        if (!dispatched) {
            onDone(false)
        }
    }

    companion object {
        @Volatile
        var instance: HeadlockService? = null
            private set

        fun isServiceConnected(): Boolean = instance != null
    }
}

