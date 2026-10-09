package com.sensibotpro.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.sensibotpro.domain.model.DevicePerformanceClass
import com.sensibotpro.domain.model.DeviceSpecs
import kotlin.math.roundToInt

class DeviceInfoProvider(private val context: Context) {

    fun getDeviceSpecs(): DeviceSpecs {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val androidVersion = "Android ${Build.VERSION.RELEASE}"
        val apiLevel = Build.VERSION.SDK_INT

        // Memory info (Use Locale.US so commas/dots never crash Double parsing on European/international locales)
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamGb = String.format(java.util.Locale.US, "%.1f", memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)).toDoubleOrNull() ?: 4.0
        val availableRamGb = String.format(java.util.Locale.US, "%.1f", memoryInfo.availMem / (1024.0 * 1024.0 * 1024.0)).toDoubleOrNull() ?: 2.0

        // Display info
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        
        val display = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Try context.display if available (Activity), else windowManager?.defaultDisplay
                try {
                    context.display
                } catch (_: Throwable) {
                    @Suppress("DEPRECATION")
                    windowManager?.defaultDisplay
                }
            } else {
                @Suppress("DEPRECATION")
                windowManager?.defaultDisplay
            }
        } catch (_: Throwable) {
            null
        }

        val refreshRate = try {
            display?.refreshRate?.roundToInt() ?: 60
        } catch (_: Throwable) {
            60
        }

        var displayDensityDpi = 411
        var resolution = "1080x2400"

        try {
            val metrics = DisplayMetrics()
            val (widthPx, heightPx) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
                val bounds = windowManager.currentWindowMetrics.bounds
                @Suppress("DEPRECATION")
                display?.getRealMetrics(metrics) ?: context.resources.displayMetrics
                bounds.width().toDouble() to bounds.height().toDouble()
            } else {
                @Suppress("DEPRECATION")
                windowManager?.defaultDisplay?.getRealMetrics(metrics) ?: run {
                    val dm = context.resources.displayMetrics
                    metrics.setTo(dm)
                }
                (if (metrics.widthPixels > 0) metrics.widthPixels.toDouble() else 1080.0) to
                        (if (metrics.heightPixels > 0) metrics.heightPixels.toDouble() else 2400.0)
            }

            resolution = "${widthPx.toInt()}x${heightPx.toInt()}"

            // Pull physical XDPI & YDPI from DisplayMetrics to calculate actual physical screen diagonal in inches
            val xdpi = if (metrics.xdpi > 0f) metrics.xdpi.toDouble() else context.resources.configuration.densityDpi.toDouble()
            val ydpi = if (metrics.ydpi > 0f) metrics.ydpi.toDouble() else context.resources.configuration.densityDpi.toDouble()

            val widthInches = widthPx / xdpi
            val heightInches = heightPx / ydpi
            val screenSizeInches = kotlin.math.sqrt(widthInches * widthInches + heightInches * heightInches)

            // PPI = sqrt(width_px² + height_px²) / screen_size_inches
            val diagonalPx = kotlin.math.sqrt(widthPx * widthPx + heightPx * heightPx)
            val computedPpi = if (screenSizeInches > 0.0) {
                diagonalPx / screenSizeInches
            } else {
                metrics.densityDpi.toDouble()
            }

            displayDensityDpi = computedPpi.roundToInt().coerceIn(160, 800)
        } catch (_: Throwable) {
            displayDensityDpi = context.resources.configuration.densityDpi
        }

        // Performance classification
        val performanceClass = when {
            totalRamGb >= 11.0 || (refreshRate >= 120 && totalRamGb >= 7.5) -> DevicePerformanceClass.FLAGSHIP
            totalRamGb >= 6.0 || refreshRate >= 90 -> DevicePerformanceClass.HIGH_PERFORMANCE
            totalRamGb >= 3.5 -> DevicePerformanceClass.MID_RANGE
            else -> DevicePerformanceClass.LOW_END
        }

        return DeviceSpecs(
            manufacturer = manufacturer,
            model = model,
            androidVersion = androidVersion,
            apiLevel = apiLevel,
            totalRamGb = totalRamGb,
            availableRamGb = availableRamGb,
            refreshRate = refreshRate,
            displayDensityDpi = displayDensityDpi,
            resolution = resolution,
            performanceClass = performanceClass
        )
    }
}
