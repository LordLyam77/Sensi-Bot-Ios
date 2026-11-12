package com.sensibotpro.headlock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Android HeadlockBridge implementation.
 * Persists config under "headlock." prefix in dedicated preferences so existing sensi storage is untouched.
 */
class HeadlockChannel(private val context: Context) : HeadlockBridge {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun enable(): HeadlockEnableResult {
        if (!HeadlockService.isServiceConnected()) {
            return HeadlockEnableResult(ok = false, needsAccessibilityPermission = true)
        }
        if (!Settings.canDrawOverlays(context)) {
            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            runCatching { context.startActivity(overlayIntent) }
            return HeadlockEnableResult(ok = false, needsAccessibilityPermission = true)
        }

        val bubbleIntent = Intent(context, HeadlockBubble::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(bubbleIntent)
        } else {
            context.startService(bubbleIntent)
        }
        prefs.edit().putBoolean(KEY_ENABLED, true).apply()
        return HeadlockEnableResult(ok = true, needsAccessibilityPermission = false)
    }

    override fun disable() {
        HeadlockService.instance?.abortInFlight()
        context.stopService(Intent(context, HeadlockBubble::class.java))
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()
    }

    override fun isEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLED, false) && HeadlockService.isServiceConnected()
    }

    override fun setConfig(cfg: HeadlockConfig): Boolean {
        val clamped = cfg.clamped()
        return prefs.edit()
            .putString(HeadlockConfig.STORAGE_CONFIG_KEY, clamped.toJson())
            .commit()
    }

    override fun getConfig(): HeadlockConfig {
        val raw = prefs.getString(HeadlockConfig.STORAGE_CONFIG_KEY, null)
        return HeadlockConfig.fromJson(raw).clamped()
    }

    override fun recommend(deviceMetrics: HeadlockDeviceMetrics): HeadlockConfig {
        return HeadlockCalibration.recommend(deviceMetrics)
    }

    override fun installTouchProfile(): HeadlockProfileInstallResult {
        return HeadlockProfileInstallResult(ok = false, path = null, unsupported = true)
    }

    override fun openSystemSettings(): Boolean {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    companion object {
        private const val PREFS_NAME = "headlock_isolated_prefs"
        private const val KEY_ENABLED = "${HeadlockConfig.STORAGE_KEY_PREFIX}enabled"
    }
}

