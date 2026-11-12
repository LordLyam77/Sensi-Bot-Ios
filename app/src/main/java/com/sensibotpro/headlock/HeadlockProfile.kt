package com.sensibotpro.headlock

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

object HeadlockProfile {

    fun collectMetrics(context: Context): HeadlockDeviceMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)

        val widthInches = dm.widthPixels / dm.xdpi.coerceAtLeast(160f)
        val heightInches = dm.heightPixels / dm.ydpi.coerceAtLeast(160f)
        val diagonalInches = kotlin.math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())

        val deviceClass = when {
            diagonalInches >= 7.8 -> HeadlockDeviceClass.TABLET
            diagonalInches >= 6.55 -> HeadlockDeviceClass.LARGE
            diagonalInches < 5.95 -> HeadlockDeviceClass.COMPACT
            else -> HeadlockDeviceClass.PHONE
        }

        val refreshHz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.refreshRate?.toInt() ?: 60
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.refreshRate.toInt()
        }

        return HeadlockDeviceMetrics(
            deviceClass = deviceClass,
            screenHeightPx = dm.heightPixels.coerceAtLeast(1000),
            refreshRateHz = refreshHz.coerceIn(60, 240)
        )
    }

    fun recommend(context: Context): HeadlockConfig {
        return HeadlockCalibration.recommend(collectMetrics(context))
    }
}

