package com.sensibotpro.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.*
import com.sensibotpro.R
import com.sensibotpro.data.local.ProfileDatabaseHelper
import kotlin.math.hypot

/**
 * Target shot zones supported by the Floating In-Game Assistant
 */
enum class TargetZone(val title: String, val badge: String, val colorHex: String) {
    HEAD("HEAD SHOTS", "100% LOCK", "#FF2A4D"),
    BODY("BODY SHOTS", "MAX DPS", "#00E5FF"),
    LEGS("LEG SHOTS", "SWEEP HIT", "#FFB300")
}

/**
 * Floating In-Game Assistant Service
 *
 * Features:
 * - Persistent draggable floating overlay button (no permanent kill button on overlay)
 * - Remembers last position with edge snapping
 * - Half-screen horizontal tactical drawer menu that keeps the game visible
 * - Interactive 2-phase character animation:
 *   1. Calculating / Analyzing scan effect (~1.4s)
 *   2. Target highlighted lock (Head / Body / Legs) over the Free Fire Red Criminal character
 * - Live sensitivity metrics & one-tap profile apply
 * - Controlled exclusively from the dedicated Floating Assistant page in SENSI BOT Pro
 */
class FloatingSensiService : Service() {

    private var windowManager: WindowManager? = null
    private var rootContainer: FrameLayout? = null
    private var bubbleView: FrameLayout? = null
    private var menuCard: LinearLayout? = null
    private var silhouetteView: TargetingSilhouetteView? = null

    private var isExpanded = false
    private var activeZone = TargetZone.HEAD

    private lateinit var prefs: SharedPreferences
    private lateinit var dbHelper: ProfileDatabaseHelper

    private var screenWidth = 1080
    private var screenHeight = 2400
    private var density = 2.75f

    companion object {
        const val PREFS_NAME = "sensi_floating_assistant"
        const val KEY_POS_X = "pos_x"
        const val KEY_POS_Y = "pos_y"
        const val KEY_ZONE = "active_zone"

        var isRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        dbHelper = ProfileDatabaseHelper(this)

        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        density = displayMetrics.density

        // Restore active zone
        val savedZoneName = prefs.getString(KEY_ZONE, TargetZone.HEAD.name)
        activeZone = try {
            TargetZone.valueOf(savedZoneName ?: TargetZone.HEAD.name)
        } catch (_: Exception) {
            TargetZone.HEAD
        }

        // Layout params for system overlay
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val bubbleSize = (54 * density).toInt()
        val defaultEdge = prefs.getString("default_edge", "RIGHT") ?: "RIGHT"
        val defaultX = if (defaultEdge == "LEFT") (12 * density).toInt() else screenWidth - bubbleSize - (12 * density).toInt()
        val defaultY = (screenHeight * 0.35f).toInt()

        val savedX = prefs.getInt(KEY_POS_X, defaultX)
        val savedY = prefs.getInt(KEY_POS_Y, defaultY)

        val params = WindowManager.LayoutParams(
            bubbleSize,
            bubbleSize,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }

        // Root container
        rootContainer = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }

        // 1. Sleek Draggable Floating Bubble (No permanent kill/close button!)
        bubbleView = createBubbleView(bubbleSize)

        // 2. Horizontal Half-Screen Overlay Menu
        menuCard = createHorizontalMenuCard()

        rootContainer?.addView(bubbleView)
        rootContainer?.addView(menuCard)

        // Dragging & Click Handling with Edge Snapping
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        val touchSlop = 10 * density

        bubbleView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && hypot(dx, dy) > touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x = (initialX + dx).toInt()
                        params.y = (initialY + dy).toInt().coerceIn(40, screenHeight - bubbleSize - 80)
                        windowManager?.updateViewLayout(rootContainer, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Click / Tap -> Open or Collapse Horizontal Menu
                        if (isExpanded) {
                            collapseMenu()
                        } else {
                            openHorizontalMenu(params)
                        }
                    } else {
                        // Drag completed -> Snap smoothly to nearest edge (Left or Right)
                        val snapMargin = (12 * density).toInt()
                        val targetX = if (params.x + bubbleSize / 2 < screenWidth / 2) {
                            snapMargin
                        } else {
                            screenWidth - bubbleSize - snapMargin
                        }
                        params.x = targetX
                        windowManager?.updateViewLayout(rootContainer, params)

                        // Persist position so user doesn't have to reposition
                        prefs.edit()
                            .putInt(KEY_POS_X, params.x)
                            .putInt(KEY_POS_Y, params.y)
                            .apply()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(rootContainer, params)
    }

    /**
     * Creates the glowing floating bubble launcher.
     * Note: NO permanent X or kill button is placed on this launcher.
     */
    private fun createBubbleView(bubbleSize: Int): FrameLayout {
        return FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#F012131D")) // Deep dark charcoal
                setStroke((2.5f * density).toInt(), Color.parseColor("#FF2A4D")) // Ruby red glow border
            }
            background = bg

            // Inner icon: Stylized crosshair/target emblem
            val innerEmblem = View(this@FloatingSensiService).apply {
                val emblemSize = (28 * density).toInt()
                layoutParams = FrameLayout.LayoutParams(emblemSize, emblemSize, Gravity.CENTER)
                background = createBubbleEmblemDrawable()
            }
            addView(innerEmblem)

            // Small glowing green live indicator on the top-right
            val statusDot = View(this@FloatingSensiService).apply {
                val dotSize = (8 * density).toInt()
                val lp = FrameLayout.LayoutParams(dotSize, dotSize, Gravity.TOP or Gravity.END).apply {
                    topMargin = (4 * density).toInt()
                    rightMargin = (4 * density).toInt()
                }
                layoutParams = lp
                val dotBg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#00E676")) // Neon green
                    setStroke(1, Color.WHITE)
                }
                background = dotBg
            }
            addView(statusDot)
        }
    }

    private fun createBubbleEmblemDrawable(): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF2A4D")
                style = Paint.Style.STROKE
                strokeWidth = 2.5f * density
            }
            private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }

            override fun draw(canvas: Canvas) {
                val bounds = bounds
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val radius = bounds.width() * 0.38f

                // Outer circle
                canvas.drawCircle(cx, cy, radius, paint)

                // Reticle cross lines
                val tick = radius * 0.4f
                canvas.drawLine(cx - radius - tick, cy, cx - radius + tick, cy, paint)
                canvas.drawLine(cx + radius - tick, cy, cx + radius + tick, cy, paint)
                canvas.drawLine(cx, cy - radius - tick, cx, cy - radius + tick, paint)
                canvas.drawLine(cx, cy + radius - tick, cx, cy + radius + tick, paint)

                // Center red dot
                canvas.drawCircle(cx, cy, 2.5f * density, centerPaint)
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
            @Suppress("DEPRECATION")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }
    }

    /**
     * Builds the horizontal overlay drawer menu (~half screen width)
     */
    private fun createHorizontalMenuCard(): LinearLayout {
        // Width: roughly 50-55% of screen width in landscape, or 88% in portrait, capped between 360dp and 460dp
        val isLandscape = screenWidth > screenHeight
        val menuWidthPx = if (isLandscape) {
            (screenWidth * 0.52f).toInt().coerceIn((360 * density).toInt(), (460 * density).toInt())
        } else {
            (screenWidth * 0.90f).toInt().coerceAtMost((390 * density).toInt())
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * density
                setColor(Color.parseColor("#F50D0D14")) // Dark sci-fi glassmorphism
                setStroke((1.5f * density).toInt(), Color.parseColor("#FF2A4D")) // Glowing ruby border
            }
            background = bg
            setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
            layoutParams = FrameLayout.LayoutParams(menuWidthPx, FrameLayout.LayoutParams.WRAP_CONTENT)

            // ── Row 1: Top Header ───────────────────────────────────────────
            val headerRow = LinearLayout(this@FloatingSensiService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            val badgeTag = TextView(this@FloatingSensiService).apply {
                text = "LYAM FF"
                textSize = 10f
                paint.isFakeBoldText = true
                setTextColor(Color.parseColor("#FF2A4D"))
                val tagBg = GradientDrawable().apply {
                    cornerRadius = 4f * density
                    setColor(Color.parseColor("#26FF2A4D"))
                    setStroke(1, Color.parseColor("#FF2A4D"))
                }
                background = tagBg
                setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())
            }

            val titleTv = TextView(this@FloatingSensiService).apply {
                text = " IN-GAME ASSISTANT"
                textSize = 13f
                paint.isFakeBoldText = true
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = (6 * density).toInt()
                }
            }

            val closeBtn = TextView(this@FloatingSensiService).apply {
                text = "✕"
                textSize = 16f
                paint.isFakeBoldText = true
                setTextColor(Color.parseColor("#FF5572"))
                setPadding((10 * density).toInt(), 0, (4 * density).toInt(), 0)
                setOnClickListener {
                    collapseMenu()
                }
            }

            headerRow.addView(badgeTag)
            headerRow.addView(titleTv)
            headerRow.addView(closeBtn)
            addView(headerRow)

            // ── Row 2: Optimization Tabs (HEAD / BODY / LEGS) ────────────────
            val tabsContainer = LinearLayout(this@FloatingSensiService).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (8 * density).toInt()
                    bottomMargin = (8 * density).toInt()
                }
            }

            val tabViews = mutableListOf<TextView>()

            fun updateTabVisuals() {
                TargetZone.values().forEachIndexed { index, zone ->
                    val tv = tabViews.getOrNull(index) ?: return@forEachIndexed
                    val isSelected = zone == activeZone
                    val zoneColor = Color.parseColor(zone.colorHex)

                    val tabBg = GradientDrawable().apply {
                        cornerRadius = 8f * density
                        if (isSelected) {
                            setColor(Color.parseColor(if (zone == TargetZone.HEAD) "#33FF2A4D" else if (zone == TargetZone.BODY) "#2600E5FF" else "#26FFB300"))
                            setStroke((1.5f * density).toInt(), zoneColor)
                        } else {
                            setColor(Color.parseColor("#1A1A26"))
                            setStroke(1, Color.parseColor("#2A2A3E"))
                        }
                    }
                    tv.background = tabBg
                    tv.setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#8E8E9E"))
                    tv.paint.isFakeBoldText = isSelected
                }
            }

            // User configuration for options visibility
            val optHead = prefs.getBoolean("opt_head_enabled", true)
            val optBody = prefs.getBoolean("opt_body_enabled", true)
            val optLegs = prefs.getBoolean("opt_legs_enabled", true)

            TargetZone.values().forEach { zone ->
                val isVisible = when (zone) {
                    TargetZone.HEAD -> optHead
                    TargetZone.BODY -> optBody
                    TargetZone.LEGS -> optLegs
                }
                if (!isVisible) return@forEach

                val tabTv = TextView(this@FloatingSensiService).apply {
                    text = when (zone) {
                        TargetZone.HEAD -> "🎯 HEAD"
                        TargetZone.BODY -> "🛡️ BODY"
                        TargetZone.LEGS -> "⚡ LEGS"
                    }
                    textSize = 11f
                    gravity = Gravity.CENTER
                    setPadding(0, (6 * density).toInt(), 0, (6 * density).toInt())
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = (4 * density).toInt()
                    }
                    setOnClickListener {
                        activeZone = zone
                        prefs.edit().putString(KEY_ZONE, zone.name).apply()
                        updateTabVisuals()
                        // Update target zone display immediately; calculation animation triggers when clicking APPLY
                        silhouetteView?.setTargetZoneImmediate(zone)
                        updateMetricsForZone(zone)
                    }
                }
                tabViews.add(tabTv)
                tabsContainer.addView(tabTv)
            }
            addView(tabsContainer)

            // ── Row 3: Split Content Area ───────────────────────────────────
            // Left: Interactive Character Silhouette View (with 2-phase calculating animation)
            // Right: Tactical Sensi Metrics & Apply Button
            val contentSplitRow = LinearLayout(this@FloatingSensiService).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            // A. Silhouette View
            val silWidth = (125 * density).toInt()
            val silHeight = (180 * density).toInt()
            silhouetteView = TargetingSilhouetteView(this@FloatingSensiService).apply {
                layoutParams = LinearLayout.LayoutParams(silWidth, silHeight).apply {
                    rightMargin = (10 * density).toInt()
                }
                setTargetZoneImmediate(activeZone)
            }
            contentSplitRow.addView(silhouetteView)

            // B. Metrics Column
            val metricsCol = createMetricsColumn()
            contentSplitRow.addView(metricsCol)

            addView(contentSplitRow)

            // ── Row 4: Bottom Coaching Tip ───────────────────────────────────
            val tipRow = TextView(this@FloatingSensiService).apply {
                tag = "tip_text"
                text = getCoachingTipForZone(activeZone)
                setTextColor(Color.parseColor("#B0B0C0"))
                textSize = 10f
                setPadding((4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt(), (2 * density).toInt())
            }
            addView(tipRow)

            // ── Row 5: Tactical Action Footer with Disable Overlay Button ────
            val footerRow = LinearLayout(this@FloatingSensiService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (6 * density).toInt()
                }

                val hintTv = TextView(this@FloatingSensiService).apply {
                    text = "Tap ✕ to collapse"
                    setTextColor(Color.parseColor("#6E6E82"))
                    textSize = 9.5f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val disableOverlayBtn = TextView(this@FloatingSensiService).apply {
                    text = "⏹ DISABLE OVERLAY"
                    textSize = 10f
                    paint.isFakeBoldText = true
                    setTextColor(Color.parseColor("#FF5572"))
                    gravity = Gravity.CENTER
                    setPadding((10 * density).toInt(), (5 * density).toInt(), (10 * density).toInt(), (5 * density).toInt())
                    val bg = GradientDrawable().apply {
                        cornerRadius = 6f * density
                        setColor(Color.parseColor("#26FF5572"))
                        setStroke((1 * density).toInt(), Color.parseColor("#FF5572"))
                    }
                    background = bg
                    setOnClickListener {
                        Toast.makeText(this@FloatingSensiService, "Overlay assistant disabled", Toast.LENGTH_SHORT).show()
                        stopSelf()
                    }
                }

                addView(hintTv)
                addView(disableOverlayBtn)
            }
            addView(footerRow)

            // Initialize tabs
            updateTabVisuals()
            updateMetricsForZone(activeZone)
        }
    }

    private fun createMetricsColumn(): LinearLayout {
        return LinearLayout(this).apply {
            tag = "metrics_column"
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val zoneDescTv = TextView(this@FloatingSensiService).apply {
                tag = "zone_desc_tv"
                text = "CRITICAL HEADSHOT LOCK"
                setTextColor(Color.parseColor("#FF2A4D"))
                textSize = 11f
                paint.isFakeBoldText = true
            }
            addView(zoneDescTv)

            val genRow = createMetricRow("General Sensi", "196 (+8)", "#FF2A4D")
            genRow.tag = "metric_gen"
            addView(genRow)

            val redRow = createMetricRow("Red Dot", "188 (+6)", "#FFFFFF")
            redRow.tag = "metric_red"
            addView(redRow)

            val btnRow = createMetricRow("Fire Button", "42% (Snap)", "#FFFFFF")
            btnRow.tag = "metric_btn"
            addView(btnRow)

            val dragRow = createMetricRow("Drag Speed", "High / J-Drag", "#FFB300")
            dragRow.tag = "metric_drag"
            addView(dragRow)

            val applyBtn = TextView(this@FloatingSensiService).apply {
                tag = "apply_btn"
                text = "⚡ APPLY"
                textSize = 11f
                paint.isFakeBoldText = true
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                val btnBg = GradientDrawable().apply {
                    cornerRadius = 6f * density
                    setColor(Color.parseColor("#FF2A4D"))
                }
                background = btnBg
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (34 * density).toInt()).apply {
                    topMargin = (8 * density).toInt()
                }
                layoutParams = lp

                setOnClickListener {
                    if (!isEnabled) return@setOnClickListener
                    isEnabled = false
                    text = "⚡ APPLYING..."
                    val activeBg = background as? GradientDrawable
                    activeBg?.setColor(Color.parseColor("#FFB300"))

                    // Trigger the 2-phase calculation scanning animation on the Red Criminal character!
                    silhouetteView?.triggerCalculation(activeZone)

                    // Show toast notification
                    Toast.makeText(this@FloatingSensiService, "Applied", Toast.LENGTH_SHORT).show()

                    // After calculation animation completes (~1.4s), apply settings into profile & show success
                    postDelayed({
                        applyZoneSettings(activeZone)
                        text = "✓ APPLIED!"
                        activeBg?.setColor(Color.parseColor("#00C853"))

                        postDelayed({
                            text = "⚡ APPLY"
                            activeBg?.setColor(Color.parseColor("#FF2A4D"))
                            isEnabled = true
                        }, 1500)
                    }, 1400)
                }
            }
            addView(applyBtn)
        }
    }

    private fun createMetricRow(label: String, initialVal: String, valColorHex: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (2 * density).toInt(), 0, (2 * density).toInt())

            val labelTv = TextView(this@FloatingSensiService).apply {
                text = label
                setTextColor(Color.parseColor("#8E8E9E"))
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val valTv = TextView(this@FloatingSensiService).apply {
                tag = "val_tv"
                text = initialVal
                setTextColor(Color.parseColor(valColorHex))
                textSize = 11f
                paint.isFakeBoldText = true
            }

            addView(labelTv)
            addView(valTv)
        }
    }

    private fun updateMetricsForZone(zone: TargetZone) {
        val menu = menuCard ?: return
        val metricsCol = menu.findViewWithTag<LinearLayout>("metrics_column") ?: return

        val zoneDescTv = metricsCol.findViewWithTag<TextView>("zone_desc_tv")
        val genRow = metricsCol.findViewWithTag<LinearLayout>("metric_gen")
        val redRow = metricsCol.findViewWithTag<LinearLayout>("metric_red")
        val btnRow = metricsCol.findViewWithTag<LinearLayout>("metric_btn")
        val dragRow = metricsCol.findViewWithTag<LinearLayout>("metric_drag")
        val tipTv = menu.findViewWithTag<TextView>("tip_text")

        when (zone) {
            TargetZone.HEAD -> {
                zoneDescTv?.text = "CRITICAL HEADSHOT LOCK"
                zoneDescTv?.setTextColor(Color.parseColor("#FF2A4D"))
                genRow?.findViewWithTag<TextView>("val_tv")?.apply {
                    text = "196 (+8)"
                    setTextColor(Color.parseColor("#FF2A4D"))
                }
                redRow?.findViewWithTag<TextView>("val_tv")?.apply {
                    text = "188 (+6)"
                }
                btnRow?.findViewWithTag<TextView>("val_tv")?.apply {
                    text = "42% (Snap)"
                }
                dragRow?.findViewWithTag<TextView>("val_tv")?.apply {
                    text = "High / J-Drag"
                    setTextColor(Color.parseColor("#FF2A4D"))
                }
                tipTv?.text = "💡 Drag upward quickly and immediately release thumb to let aim lock on head."
            }
            TargetZone.BODY -> {
                zoneDescTv?.text = "MAX DPS CHEST SPRAY"
                zoneDescTv?.setTextColor(Color.parseColor("#00E5FF"))
                genRow?.findViewWithTag<TextView>("val_tv")?.apply {
                    text = "182 (-6)"
                    setTextColor(Color.parseColor("#00E5FF"))
                }
                redRow?.findViewWithTag<TextView>("val_tv")?.apply {
                    text = "175 (Stable)"
                }
                btnRow?.findViewWithTag<TextView>("val_tv")?.apply {
                    text = "48% (Steady)"
                }
                dragRow?.findViewWithTag<TextView>("val_tv")?.apply {
                    text = "Moderate / Track"
                    setTextColor(Color.parseColor("#00E5FF"))
                }
                tipTv?.text = "💡 Moderate drag speed prevents crosshair from jumping above the chest."
            }
            TargetZone.LEGS -> {
                zoneDescTv?.text = "SWEEP & CROUCH RECOVERY"
                zoneDescTv?.setTextColor(Color.parseColor("#FFB300"))
                genRow?.findViewWithTag<TextView>("val_tv")?.apply {
                    text = "174 (-14)"
                    setTextColor(Color.parseColor("#FFB300"))
                }
                redRow?.findViewWithTag<TextView>("val_tv")?.apply {
                    text = "168 (Control)"
                }
                btnRow?.findViewWithTag<TextView>("val_tv")?.apply {
                    text = "52% (Large)"
                }
                dragRow?.findViewWithTag<TextView>("val_tv")?.apply {
                    text = "Low / Sweep"
                    setTextColor(Color.parseColor("#FFB300"))
                }
                tipTv?.text = "💡 Lower general sensitivity recovers aim against jumping/crouching enemies."
            }
        }
    }

    private fun getCoachingTipForZone(zone: TargetZone): String {
        return when (zone) {
            TargetZone.HEAD -> "💡 Drag upward quickly and immediately release thumb to let aim lock on head."
            TargetZone.BODY -> "💡 Moderate drag speed prevents crosshair from jumping above the chest."
            TargetZone.LEGS -> "💡 Lower general sensitivity recovers aim against jumping/crouching enemies."
        }
    }

    private fun applyZoneSettings(zone: TargetZone) {
        val active = dbHelper.getActiveProfile() ?: return
        val (newGen, newRed, newBtn) = when (zone) {
            TargetZone.HEAD -> Triple(196, 188, 42)
            TargetZone.BODY -> Triple(182, 175, 48)
            TargetZone.LEGS -> Triple(174, 168, 52)
        }
        val updated = active.copy(
            general = newGen,
            redDot = newRed,
            fireButtonSize = newBtn
        )
        dbHelper.insertOrUpdateProfile(updated)
    }

    private fun openHorizontalMenu(params: WindowManager.LayoutParams) {
        bubbleView?.visibility = View.GONE
        menuCard?.visibility = View.VISIBLE
        isExpanded = true

        silhouetteView?.startAnimation()
        silhouetteView?.setTargetZoneImmediate(activeZone)

        val isLandscape = screenWidth > screenHeight
        val menuWidthPx = if (isLandscape) {
            (screenWidth * 0.52f).toInt().coerceIn((360 * density).toInt(), (460 * density).toInt())
        } else {
            (screenWidth * 0.90f).toInt().coerceAtMost((390 * density).toInt())
        }

        params.width = menuWidthPx
        params.height = WindowManager.LayoutParams.WRAP_CONTENT

        if (params.x + menuWidthPx > screenWidth) {
            params.x = screenWidth - menuWidthPx - (12 * density).toInt()
        }
        if (params.x < 0) params.x = (12 * density).toInt()

        windowManager?.updateViewLayout(rootContainer, params)
    }

    private fun collapseMenu() {
        silhouetteView?.stopAnimation()
        menuCard?.visibility = View.GONE
        bubbleView?.visibility = View.VISIBLE
        isExpanded = false

        val bubbleSize = (54 * density).toInt()
        val lp = rootContainer?.layoutParams as? WindowManager.LayoutParams ?: return
        lp.width = bubbleSize
        lp.height = bubbleSize

        val snapMargin = (12 * density).toInt()
        lp.x = if (lp.x + bubbleSize / 2 < screenWidth / 2) {
            snapMargin
        } else {
            screenWidth - bubbleSize - snapMargin
        }
        windowManager?.updateViewLayout(rootContainer, lp)

        prefs.edit()
            .putInt(KEY_POS_X, lp.x)
            .putInt(KEY_POS_Y, lp.y)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        silhouetteView?.stopAnimation()
        rootContainer?.let {
            windowManager?.removeView(it)
        }
    }
}

/**
 * 2-Phase Interactive Character Targeting View:
 * Phase 1: CALCULATING / ANALYZING (~1.4s) — laser scanning lines, rotating reticle, computing progress.
 * Phase 2: LOCKED — specific target zone (Head / Body / Legs) highlighted with glowing aura & HUD brackets
 * on the provided Free Fire Red Criminal character!
 */
class TargetingSilhouetteView(context: Context) : View(context) {

    enum class AnimPhase {
        CALCULATING,
        LOCKED
    }

    private var targetZone = TargetZone.HEAD
    private var phase = AnimPhase.LOCKED
    private var calculationProgress = 0f

    private var pulsePhase = 0f
    private var scanPhase = 0f
    private var arrowOffset = 0f

    private var animator: ValueAnimator? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val density = resources.displayMetrics.density

    // Character Bitmap (Red Criminal bundle PNG)
    private val characterBitmap: Bitmap? = try {
        BitmapFactory.decodeResource(resources, R.drawable.character_criminal)
    } catch (_: Exception) {
        null
    }

    // Paints
    private val reticlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val hudTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 8.5f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val dashedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        pathEffect = DashPathEffect(floatArrayOf(6f * density, 4f * density), 0f)
    }

    private val scanLaserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun setTargetZoneImmediate(zone: TargetZone) {
        this.targetZone = zone
        this.phase = AnimPhase.LOCKED
        invalidate()
    }

    /**
     * Triggers the 2-phase calculation animation sequence:
     * 1. Displays the character undergoing scanning / analysis (~1.4 seconds)
     * 2. Smoothly locks onto the selected target zone (Head, Body, or Legs)
     */
    fun triggerCalculation(zone: TargetZone) {
        this.targetZone = zone
        this.phase = AnimPhase.CALCULATING
        this.calculationProgress = 0f
        invalidate()

        // 1.4s calculation simulation
        val totalSteps = 16
        val stepDelayMs = 85L
        for (i in 1..totalSteps) {
            mainHandler.postDelayed({
                calculationProgress = i.toFloat() / totalSteps
                if (i == totalSteps) {
                    phase = AnimPhase.LOCKED
                }
                invalidate()
            }, i * stepDelayMs)
        }
    }

    fun startAnimation() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1300
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                pulsePhase = f
                scanPhase = f
                arrowOffset = f
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
        animator = null
        mainHandler.removeCallbacksAndMessages(null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w * 0.5f

        // 1. Draw tactical grid background
        drawTacticalGrid(canvas, w, h)

        // 2. Draw the provided Free Fire Red Criminal character image
        drawCharacter(canvas, w, h)

        // 3. Render 2-phase animation overlay
        if (phase == AnimPhase.CALCULATING) {
            drawCalculatingScanOverlay(canvas, w, h, cx)
        } else {
            when (targetZone) {
                TargetZone.HEAD -> drawHeadTargetingOverlay(canvas, w, h, cx)
                TargetZone.BODY -> drawBodyTargetingOverlay(canvas, w, h, cx)
                TargetZone.LEGS -> drawLegsTargetingOverlay(canvas, w, h, cx)
            }
        }
    }

    private fun drawTacticalGrid(canvas: Canvas, w: Float, h: Float) {
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#151722")
            strokeWidth = 1f
        }
        val step = 18f * density
        var x = 0f
        while (x < w) {
            canvas.drawLine(x, 0f, x, h, gridPaint)
            x += step
        }
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w, y, gridPaint)
            y += step
        }
    }

    private fun drawCharacter(canvas: Canvas, w: Float, h: Float) {
        val bmp = characterBitmap ?: return

        // Preserve aspect ratio and scale to fit view height with small padding
        val targetH = h * 0.94f
        val scale = targetH / bmp.height.toFloat()
        val targetW = bmp.width.toFloat() * scale

        val left = (w - targetW) / 2f
        val top = h * 0.03f
        val dst = RectF(left, top, left + targetW, top + targetH)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        // If in calculating phase, slightly tint character for hologram scan effect
        if (phase == AnimPhase.CALCULATING) {
            paint.alpha = 230
        } else {
            paint.alpha = 255
        }
        canvas.drawBitmap(bmp, null, dst, paint)
    }

    /**
     * PHASE 1: CALCULATING / ANALYZING ANIMATION
     * Sweeps a laser beam from top to bottom with a glowing gradient trail,
     * drawing telemetry text and scanning reticle.
     */
    private fun drawCalculatingScanOverlay(canvas: Canvas, w: Float, h: Float, cx: Float) {
        val beamY = h * calculationProgress

        // 1. Glowing vertical gradient trail
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx, beamY - 30f * density,
                cx, beamY + 10f * density,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#66FFB300"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, beamY - 30f * density, w, beamY + 10f * density, glowPaint)

        // 2. Horizontal laser beam line
        scanLaserPaint.color = Color.parseColor("#FFB300")
        scanLaserPaint.strokeWidth = 3f * density
        canvas.drawLine(0f, beamY, w, beamY, scanLaserPaint)

        // 3. Rotating target circle in the center of the beam
        val reticleRadius = 14f * density
        reticlePaint.color = Color.parseColor("#FFB300")
        reticlePaint.strokeWidth = 1.5f * density
        canvas.drawCircle(cx, beamY, reticleRadius, reticlePaint)
        canvas.drawLine(cx - reticleRadius - 4f, beamY, cx + reticleRadius + 4f, beamY, reticlePaint)

        // 4. Telemetry Text
        hudTextPaint.color = Color.parseColor("#FFB300")
        val pct = (calculationProgress * 100).toInt()
        canvas.drawText("ANALYZING HITBOX... $pct%", cx, h * 0.95f, hudTextPaint)
    }

    /**
     * PHASE 2 (HEAD): Highlight Clown Mask & Head with Ruby Red Spotlight, Lock Brackets, and Upward Drag Arrow
     */
    private fun drawHeadTargetingOverlay(canvas: Canvas, w: Float, h: Float, cx: Float) {
        val glowColor = Color.parseColor("#FF2A4D")

        // Head position on character: ~13% from top
        val headY = h * 0.14f
        val pulse = 1f + (pulsePhase * 0.18f)
        val headRadius = (18f * density) * pulse

        // Glowing radial spotlight on head
        val radialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, headY, headRadius * 1.5f,
                intArrayOf(Color.parseColor("#99FF2A4D"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, headY, headRadius * 1.5f, radialPaint)

        // 4 lock corner brackets [ + ]
        val bSize = headRadius * 1.15f
        val cLen = 7f * density
        reticlePaint.color = glowColor
        reticlePaint.strokeWidth = 2.2f * density

        val bl = cx - bSize
        val br = cx + bSize
        val bt = headY - bSize
        val bb = headY + bSize

        canvas.drawLine(bl, bt, bl + cLen, bt, reticlePaint)
        canvas.drawLine(bl, bt, bl, bt + cLen, reticlePaint)
        canvas.drawLine(br, bt, br - cLen, bt, reticlePaint)
        canvas.drawLine(br, bt, br, bt + cLen, reticlePaint)
        canvas.drawLine(bl, bb, bl + cLen, bb, reticlePaint)
        canvas.drawLine(bl, bb, bl, bb - cLen, reticlePaint)
        canvas.drawLine(br, bb, br - cLen, bb, reticlePaint)
        canvas.drawLine(br, bb, br, bb - cLen, reticlePaint)

        // Upward Drag Vector Arrow
        val arrowStartY = headY + (36f * density)
        val arrowEndY = headY - (6f * density)
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = glowColor
            strokeWidth = 2.2f * density
        }
        canvas.drawLine(cx, arrowStartY, cx, arrowEndY, arrowPaint)

        // Arrow tip ^
        val arrowTip = Path().apply {
            moveTo(cx, arrowEndY)
            lineTo(cx - 4f * density, arrowEndY + 7f * density)
            lineTo(cx + 4f * density, arrowEndY + 7f * density)
            close()
        }
        val tipFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = glowColor
            style = Paint.Style.FILL
        }
        canvas.drawPath(arrowTip, tipFill)

        // Traveling particle
        val partY = arrowStartY - (arrowStartY - arrowEndY) * arrowOffset
        val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, partY, 3f * density, particlePaint)

        // HUD Text
        hudTextPaint.color = glowColor
        canvas.drawText("🎯 100% HEAD LOCK", cx, headY - bSize - 4f * density, hudTextPaint)
    }

    /**
     * PHASE 2 (BODY): Highlight Jumpsuit Torso with Neon Cyan Spotlight, Recoil Box, and Impact Pings
     */
    private fun drawBodyTargetingOverlay(canvas: Canvas, w: Float, h: Float, cx: Float) {
        val cyanColor = Color.parseColor("#00E5FF")

        // Chest position on character: ~34% from top
        val chestY = h * 0.35f
        val pulse = 1f + (pulsePhase * 0.12f)
        val rx = (26f * density) * pulse
        val ry = (22f * density) * pulse

        // Glowing radial spotlight on chest
        val radialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, chestY, rx * 1.4f,
                intArrayOf(Color.parseColor("#8000E5FF"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, chestY, rx * 1.4f, radialPaint)

        // Recoil grouping box
        dashedLinePaint.color = cyanColor
        val rect = RectF(cx - rx, chestY - ry, cx + rx, chestY + ry)
        canvas.drawRoundRect(rect, 4f * density, 4f * density, dashedLinePaint)

        // 6 bullet impact pings
        val impactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val impactGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8000E5FF")
            style = Paint.Style.FILL
        }

        val offsets = listOf(
            Pair(-0.25f, -0.3f),
            Pair(0.20f, -0.2f),
            Pair(0.0f, 0.0f),
            Pair(-0.15f, 0.25f),
            Pair(0.25f, 0.2f),
            Pair(0.05f, -0.35f)
        )

        offsets.forEachIndexed { i, (ox, oy) ->
            val ix = cx + ox * rx
            val iy = chestY + oy * ry
            val stagger = ((pulsePhase + (i * 0.16f)) % 1f)
            val rad = (1.5f + stagger * 2.8f) * density
            canvas.drawCircle(ix, iy, rad * 1.5f, impactGlow)
            canvas.drawCircle(ix, iy, 1.8f * density, impactPaint)
        }

        // Horizontal stabilization lines
        val stabPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cyanColor
            strokeWidth = 1.5f * density
        }
        canvas.drawLine(cx - rx - 8f * density, chestY, cx - rx, chestY, stabPaint)
        canvas.drawLine(cx + rx, chestY, cx + rx + 8f * density, chestY, stabPaint)

        // HUD Text
        hudTextPaint.color = cyanColor
        canvas.drawText("🛡️ MAX DPS SPRAY", cx, chestY - ry - 4f * density, hudTextPaint)
    }

    /**
     * PHASE 2 (LEGS): Highlight Boots and Legs with Amber Aura and Sweeping Radar Line
     */
    private fun drawLegsTargetingOverlay(canvas: Canvas, w: Float, h: Float, cx: Float) {
        val amberColor = Color.parseColor("#FFB300")

        val top = h * 0.54f
        val bottom = h * 0.90f
        val legSpan = w * 0.36f

        // Glowing vertical gradient
        val legGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx, top, cx, bottom,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#4DFFB300"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(cx - legSpan, top, cx + legSpan, bottom, legGlow)

        // Sweeping horizontal radar line
        val scanY = top + (bottom - top) * scanPhase
        val scanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = amberColor
            strokeWidth = 2.5f * density
        }
        canvas.drawLine(cx - legSpan, scanY, cx + legSpan, scanY, scanPaint)

        // HUD Text
        hudTextPaint.color = amberColor
        canvas.drawText("⚡ SWEEP CROUCH RECOVERY", cx, bottom + 10f * density, hudTextPaint)
    }
}
