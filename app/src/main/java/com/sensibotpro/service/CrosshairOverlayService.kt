package com.sensibotpro.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class CrosshairOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var crosshairView: CrosshairCanvasView? = null
    private lateinit var prefs: SharedPreferences

    companion object {
        var isRunning = false
            private set
        const val PREFS_NAME = "sensi_crosshair_prefs"
        const val KEY_STYLE = "style"
        const val KEY_SIZE = "size"
        const val KEY_COLOR = "color"
        const val KEY_THICKNESS = "thickness"
        const val KEY_OPACITY = "opacity"

        const val ACTION_UPDATE = "com.sensibotpro.ACTION_UPDATE_CROSSHAIR"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val sizePx = (120 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        crosshairView = CrosshairCanvasView(this, prefs)
        windowManager?.addView(crosshairView, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE) {
            crosshairView?.invalidate()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        crosshairView?.let {
            windowManager?.removeView(it)
        }
    }

    class CrosshairCanvasView(context: Context, private val prefs: SharedPreferences) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val style = prefs.getString(KEY_STYLE, "Classic Cross") ?: "Classic Cross"
            val sizeDp = prefs.getInt(KEY_SIZE, 24).toFloat()
            val colorHex = prefs.getString(KEY_COLOR, "#FF2A4D") ?: "#FF2A4D"
            val thicknessDp = prefs.getInt(KEY_THICKNESS, 3).toFloat()
            val opacity = prefs.getFloat(KEY_OPACITY, 0.9f)

            val density = resources.displayMetrics.density
            val sizePx = sizeDp * density
            val thicknessPx = thicknessDp * density

            val parsedColor = try {
                Color.parseColor(colorHex)
            } catch (e: Exception) {
                Color.parseColor("#FF2A4D")
            }

            val alphaInt = (opacity * 255).toInt().coerceIn(20, 255)
            val finalColor = Color.argb(alphaInt, Color.red(parsedColor), Color.green(parsedColor), Color.blue(parsedColor))

            paint.color = finalColor
            paint.strokeWidth = thicknessPx

            val cx = width / 2f
            val cy = height / 2f
            val half = sizePx / 2f
            val gap = half * 0.35f

            when (style) {
                "Classic Cross" -> {
                    // Left, Right, Top, Bottom bars with center gap
                    canvas.drawLine(cx - half, cy, cx - gap, cy, paint)
                    canvas.drawLine(cx + gap, cy, cx + half, cy, paint)
                    canvas.drawLine(cx, cy - half, cx, cy - gap, paint)
                    canvas.drawLine(cx, cy + gap, cx, cy + half, paint)
                    // Center dot
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, thicknessPx * 0.8f, paint)
                    paint.style = Paint.Style.STROKE
                }
                "Center Dot" -> {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, half * 0.4f, paint)
                    paint.style = Paint.Style.STROKE
                }
                "Circle Dot" -> {
                    canvas.drawCircle(cx, cy, half * 0.75f, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, thicknessPx * 0.9f, paint)
                    paint.style = Paint.Style.STROKE
                }
                "T-Style" -> {
                    // Left, Right, Bottom bars, no top bar
                    canvas.drawLine(cx - half, cy, cx - gap, cy, paint)
                    canvas.drawLine(cx + gap, cy, cx + half, cy, paint)
                    canvas.drawLine(cx, cy + gap, cx, cy + half, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, thicknessPx * 0.8f, paint)
                    paint.style = Paint.Style.STROKE
                }
                "Diamond" -> {
                    val path = android.graphics.Path().apply {
                        moveTo(cx, cy - half * 0.7f)
                        lineTo(cx + half * 0.7f, cy)
                        lineTo(cx, cy + half * 0.7f)
                        lineTo(cx - half * 0.7f, cy)
                        close()
                    }
                    canvas.drawPath(path, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, thicknessPx * 0.7f, paint)
                    paint.style = Paint.Style.STROKE
                }
            }
        }
    }
}
