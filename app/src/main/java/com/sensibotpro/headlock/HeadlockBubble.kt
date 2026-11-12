package com.sensibotpro.headlock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.abs

/**
 * 56dp draggable overlay toggle for Headlock.
 * Single tap toggles active/dispatch state; long-press opens Headlock config.
 */
class HeadlockBubble : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: TextView? = null
    private var activeToggle = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
        if (Settings.canDrawOverlays(this)) {
            attachBubble()
        } else {
            stopSelf()
        }
    }

    override fun onDestroy() {
        HeadlockService.instance?.abortInFlight()
        bubbleView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        bubbleView = null
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val channelId = "headlock_bubble_channel"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Headlock Overlay",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Headlock Overlay")
            .setContentText("Tap bubble to toggle drag macro; long-press for config")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun attachBubble() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val density = resources.displayMetrics.density
        val sizePx = (56 * density).toInt()

        val view = TextView(this).apply {
            text = "HL"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            background = createCircleDrawable(active = false)
        }

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (24 * density).toInt()
            y = (220 * density).toInt()
        }

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var initialX = 0
        var initialY = 0
        var downX = 0f
        var downY = 0f
        var downTime = 0L
        var longPressed = false

        val longPressRunnable = Runnable {
            longPressed = true
            val launch = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_OPEN_HEADLOCK_CONFIG, true)
            }
            if (launch != null) startActivity(launch)
        }

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    downX = event.rawX
                    downY = event.rawY
                    downTime = System.currentTimeMillis()
                    longPressed = false
                    view.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        view.removeCallbacks(longPressRunnable)
                        params.x = initialX + dx
                        params.y = initialY + dy
                        wm.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.removeCallbacks(longPressRunnable)
                    val dx = abs(event.rawX - downX)
                    val dy = abs(event.rawY - downY)
                    if (!longPressed && dx <= touchSlop && dy <= touchSlop) {
                        onBubbleTapped(view)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.removeCallbacks(longPressRunnable)
                    true
                }
                else -> false
            }
        }

        wm.addView(view, params)
        bubbleView = view
    }

    private fun onBubbleTapped(view: TextView) {
        val svc = HeadlockService.instance
        if (svc == null) {
            activeToggle = false
            view.background = createCircleDrawable(active = false)
            return
        }
        activeToggle = !activeToggle
        view.background = createCircleDrawable(active = activeToggle)
        if (activeToggle) {
            val cfg = HeadlockChannel(this).getConfig()
            svc.triggerHeadlock(cfg)
            if (cfg.mode == HeadlockMode.BURST) {
                activeToggle = false
                view.background = createCircleDrawable(active = false)
            }
        } else {
            svc.stopHold()
        }
    }

    private fun createCircleDrawable(active: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (active) Color.parseColor("#FF2A4D") else Color.parseColor("#CC141418"))
            setStroke(4, if (active) Color.WHITE else Color.parseColor("#FF2A4D"))
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 7741
        const val EXTRA_OPEN_HEADLOCK_CONFIG = "headlock.open_config"
    }
}

