package com.sensibotpro.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.sensibotpro.domain.model.SensitivityProfile
import java.util.UUID

class ProfileDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "sensi_bot_profiles.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_PROFILES = "profiles"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_GENERAL = "general"
        const val COL_RED_DOT = "red_dot"
        const val COL_SCOPE_2X = "scope_2x"
        const val COL_SCOPE_4X = "scope_4x"
        const val COL_SNIPER = "sniper"
        const val COL_FREE_LOOK = "free_look"
        const val COL_FIRE_BUTTON = "fire_button"
        const val COL_RECOMMENDED_DPI = "recommended_dpi"
        const val COL_DEFAULT_DPI = "default_dpi"
        const val COL_DPI_ADVICE = "dpi_advice"
        const val COL_PLAYSTYLE = "playstyle"
        const val COL_WEAPON = "weapon_focus"
        const val COL_DRAG_STYLE = "drag_style"
        const val COL_HUD_NOTES = "hud_notes"
        const val COL_EXPLANATION = "explanation"
        const val COL_COACHING_TIP = "coaching_tip"
        const val COL_IS_CUSTOM = "is_custom"
        const val COL_IS_ACTIVE = "is_active"
        const val COL_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createQuery = """
            CREATE TABLE $TABLE_PROFILES (
                $COL_ID TEXT PRIMARY KEY,
                $COL_NAME TEXT NOT NULL,
                $COL_GENERAL INTEGER NOT NULL,
                $COL_RED_DOT INTEGER NOT NULL,
                $COL_SCOPE_2X INTEGER NOT NULL,
                $COL_SCOPE_4X INTEGER NOT NULL,
                $COL_SNIPER INTEGER NOT NULL,
                $COL_FREE_LOOK INTEGER NOT NULL,
                $COL_FIRE_BUTTON INTEGER NOT NULL,
                $COL_RECOMMENDED_DPI INTEGER NOT NULL DEFAULT 440,
                $COL_DEFAULT_DPI INTEGER NOT NULL DEFAULT 411,
                $COL_DPI_ADVICE TEXT NOT NULL DEFAULT '',
                $COL_PLAYSTYLE TEXT NOT NULL,
                $COL_WEAPON TEXT NOT NULL,
                $COL_DRAG_STYLE TEXT NOT NULL,
                $COL_HUD_NOTES TEXT NOT NULL,
                $COL_EXPLANATION TEXT NOT NULL,
                $COL_COACHING_TIP TEXT NOT NULL,
                $COL_IS_CUSTOM INTEGER NOT NULL,
                $COL_IS_ACTIVE INTEGER NOT NULL,
                $COL_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createQuery)

        // Seed default starting profiles
        seedDefaultProfiles(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROFILES")
        onCreate(db)
    }

    private fun seedDefaultProfiles(db: SQLiteDatabase) {
        val defaults = listOf(
            SensitivityProfile(
                id = "default_lyam_ff",
                name = "Lyam FF • Official Sensi",
                general = 196,
                redDot = 188,
                scope2x = 182,
                scope4x = 174,
                sniper = 90,
                freeLook = 155,
                fireButtonSize = 44,
                recommendedDpi = 450,
                defaultDpi = 411,
                dpiAdvice = "Safe +39 boost over stock for maximum M1887 headshot drag angle.",
                playstyle = "Aggressive",
                weaponFocus = "Shotgun",
                dragStyle = "Curved J-Drag for M1887 shotgun headshots & MP40 spray tracking",
                hudNotes = "Fire button at 44% placed low on HUD for long upward drag travel.",
                explanation = "Official signature Free Fire sensitivity profile calibrated by Lyam FF for the 200-scale OB update.",
                coachingTip = "Whip your fire button up with authority right as the red target dot hits the enemy's shoulder.",
                isCustom = false,
                isActive = true
            ),
            SensitivityProfile(
                id = "default_white_ff",
                name = "White FF • One-Tap Pro",
                general = 195,
                redDot = 188,
                scope2x = 175,
                scope4x = 165,
                sniper = 85,
                freeLook = 150,
                fireButtonSize = 44,
                recommendedDpi = 460,
                defaultDpi = 411,
                dpiAdvice = "DPI 460 provides the iconic White444 flick velocity without losing horizontal control.",
                playstyle = "Precise",
                weaponFocus = "Mixed",
                dragStyle = "Fast upward J-flick with instantaneous weapon switch and un-crouch reset",
                hudNotes = "Place Quick Weapon Switch right beside left index/thumb for millisecond resets.",
                explanation = "Inspired by legendary mobile headshot king White444 (White FF) for M1887 & Desert Eagle one-taps.",
                coachingTip = "Pause for a split-second to lock initial reticle before snapping upward explosively.",
                isCustom = false,
                isActive = false
            ),
            SensitivityProfile(
                id = "default_raistar",
                name = "Raistar • Speed Movement",
                general = 198,
                redDot = 190,
                scope2x = 180,
                scope4x = 170,
                sniper = 95,
                freeLook = 160,
                fireButtonSize = 42,
                recommendedDpi = 510,
                defaultDpi = 411,
                dpiAdvice = "DPI 510 unlocks ultra-fluid 360-degree camera turning for fast gloo wall deployment.",
                playstyle = "Rush",
                weaponFocus = "SMG",
                dragStyle = "Rotation drag with 360° Sit-up Gloo Wall follow-through",
                hudNotes = "Small fire button (42%) for instant swipe initiation.",
                explanation = "Modeled after Raistar's lightning mobile movement, fast camera flicks, and close-quarters MP40 tracking.",
                coachingTip = "Drag into a continuous upward arc while simultaneously executing your jump-turn.",
                isCustom = false,
                isActive = false
            ),
            SensitivityProfile(
                id = "default_balanced",
                name = "Balanced AR (200 Scale)",
                general = 180,
                redDot = 170,
                scope2x = 160,
                scope4x = 150,
                sniper = 90,
                freeLook = 140,
                fireButtonSize = 48,
                recommendedDpi = 440,
                defaultDpi = 411,
                dpiAdvice = "DPI 440 is the golden competitive standard for Android devices.",
                playstyle = "Balanced",
                weaponFocus = "AR",
                dragStyle = "Smooth vertical drag",
                hudNotes = "Fire button centered lower right, 48% scale.",
                explanation = "All-rounder 200-scale profile for steady mid-range recoil control and consistent camera turning.",
                coachingTip = "Test 10 shots with Woodpecker or AK in Training Ground.",
                isCustom = false,
                isActive = false
            ),
            SensitivityProfile(
                id = "default_long_range",
                name = "Long Range Sniper",
                general = 168,
                redDot = 155,
                scope2x = 150,
                scope4x = 145,
                sniper = 80,
                freeLook = 130,
                fireButtonSize = 52,
                recommendedDpi = 420,
                defaultDpi = 411,
                dpiAdvice = "Moderate DPI maintains optical crosshair stability over 40+ meters.",
                playstyle = "Long Range",
                weaponFocus = "Sniper",
                dragStyle = "Gentle micro-flick and scope alignment",
                hudNotes = "Larger fire button (52%) prevents accidental slip during long-range aim.",
                explanation = "Stable optical control for 4x scope sprays and sniper switch timing on the modern 200 scale.",
                coachingTip = "Lead the moving target slightly ahead before releasing the shot.",
                isCustom = false,
                isActive = false
            )
        )

        for (profile in defaults) {
            val cv = ContentValues().apply {
                put(COL_ID, profile.id)
                put(COL_NAME, profile.name)
                put(COL_GENERAL, profile.general)
                put(COL_RED_DOT, profile.redDot)
                put(COL_SCOPE_2X, profile.scope2x)
                put(COL_SCOPE_4X, profile.scope4x)
                put(COL_SNIPER, profile.sniper)
                put(COL_FREE_LOOK, profile.freeLook)
                put(COL_FIRE_BUTTON, profile.fireButtonSize)
                put(COL_RECOMMENDED_DPI, profile.recommendedDpi)
                put(COL_DEFAULT_DPI, profile.defaultDpi)
                put(COL_DPI_ADVICE, profile.dpiAdvice)
                put(COL_PLAYSTYLE, profile.playstyle)
                put(COL_WEAPON, profile.weaponFocus)
                put(COL_DRAG_STYLE, profile.dragStyle)
                put(COL_HUD_NOTES, profile.hudNotes)
                put(COL_EXPLANATION, profile.explanation)
                put(COL_COACHING_TIP, profile.coachingTip)
                put(COL_IS_CUSTOM, if (profile.isCustom) 1 else 0)
                put(COL_IS_ACTIVE, if (profile.isActive) 1 else 0)
                put(COL_CREATED_AT, profile.createdAt)
            }
            db.insert(TABLE_PROFILES, null, cv)
        }
    }

    fun getAllProfiles(): List<SensitivityProfile> {
        val list = mutableListOf<SensitivityProfile>()
        val db = readableDatabase
        val cursor = db.query(TABLE_PROFILES, null, null, null, null, null, "$COL_CREATED_AT DESC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToProfile(c))
            }
        }
        return list
    }

    fun insertOrUpdateProfile(profile: SensitivityProfile) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_ID, profile.id)
            put(COL_NAME, profile.name)
            put(COL_GENERAL, profile.general)
            put(COL_RED_DOT, profile.redDot)
            put(COL_SCOPE_2X, profile.scope2x)
            put(COL_SCOPE_4X, profile.scope4x)
            put(COL_SNIPER, profile.sniper)
            put(COL_FREE_LOOK, profile.freeLook)
            put(COL_FIRE_BUTTON, profile.fireButtonSize)
            put(COL_RECOMMENDED_DPI, profile.recommendedDpi)
            put(COL_DEFAULT_DPI, profile.defaultDpi)
            put(COL_DPI_ADVICE, profile.dpiAdvice)
            put(COL_PLAYSTYLE, profile.playstyle)
            put(COL_WEAPON, profile.weaponFocus)
            put(COL_DRAG_STYLE, profile.dragStyle)
            put(COL_HUD_NOTES, profile.hudNotes)
            put(COL_EXPLANATION, profile.explanation)
            put(COL_COACHING_TIP, profile.coachingTip)
            put(COL_IS_CUSTOM, if (profile.isCustom) 1 else 0)
            put(COL_IS_ACTIVE, if (profile.isActive) 1 else 0)
            put(COL_CREATED_AT, profile.createdAt)
        }
        db.insertWithOnConflict(TABLE_PROFILES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteProfile(id: String) {
        val db = writableDatabase
        db.delete(TABLE_PROFILES, "$COL_ID = ?", arrayOf(id))
    }

    fun setActiveProfile(id: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Deactivate all
            val cvDeactivate = ContentValues().apply { put(COL_IS_ACTIVE, 0) }
            db.update(TABLE_PROFILES, cvDeactivate, null, null)

            // Activate specific
            val cvActivate = ContentValues().apply { put(COL_IS_ACTIVE, 1) }
            db.update(TABLE_PROFILES, cvActivate, "$COL_ID = ?", arrayOf(id))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getActiveProfile(): SensitivityProfile? {
        val db = readableDatabase
        val cursor = db.query(TABLE_PROFILES, null, "$COL_IS_ACTIVE = 1", null, null, null, null, "1")
        cursor.use { c ->
            if (c.moveToFirst()) {
                return cursorToProfile(c)
            }
        }
        return null
    }

    private fun cursorToProfile(c: Cursor): SensitivityProfile {
        return SensitivityProfile(
            id = c.getString(c.getColumnIndexOrThrow(COL_ID)),
            name = c.getString(c.getColumnIndexOrThrow(COL_NAME)),
            general = c.getInt(c.getColumnIndexOrThrow(COL_GENERAL)),
            redDot = c.getInt(c.getColumnIndexOrThrow(COL_RED_DOT)),
            scope2x = c.getInt(c.getColumnIndexOrThrow(COL_SCOPE_2X)),
            scope4x = c.getInt(c.getColumnIndexOrThrow(COL_SCOPE_4X)),
            sniper = c.getInt(c.getColumnIndexOrThrow(COL_SNIPER)),
            freeLook = c.getInt(c.getColumnIndexOrThrow(COL_FREE_LOOK)),
            fireButtonSize = c.getInt(c.getColumnIndexOrThrow(COL_FIRE_BUTTON)),
            recommendedDpi = if (c.getColumnIndex(COL_RECOMMENDED_DPI) != -1) c.getInt(c.getColumnIndexOrThrow(COL_RECOMMENDED_DPI)) else 440,
            defaultDpi = if (c.getColumnIndex(COL_DEFAULT_DPI) != -1) c.getInt(c.getColumnIndexOrThrow(COL_DEFAULT_DPI)) else 411,
            dpiAdvice = if (c.getColumnIndex(COL_DPI_ADVICE) != -1) c.getString(c.getColumnIndexOrThrow(COL_DPI_ADVICE)) else "",
            playstyle = c.getString(c.getColumnIndexOrThrow(COL_PLAYSTYLE)),
            weaponFocus = c.getString(c.getColumnIndexOrThrow(COL_WEAPON)),
            dragStyle = c.getString(c.getColumnIndexOrThrow(COL_DRAG_STYLE)),
            hudNotes = c.getString(c.getColumnIndexOrThrow(COL_HUD_NOTES)),
            explanation = c.getString(c.getColumnIndexOrThrow(COL_EXPLANATION)),
            coachingTip = c.getString(c.getColumnIndexOrThrow(COL_COACHING_TIP)),
            isCustom = c.getInt(c.getColumnIndexOrThrow(COL_IS_CUSTOM)) == 1,
            isActive = c.getInt(c.getColumnIndexOrThrow(COL_IS_ACTIVE)) == 1,
            createdAt = c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT))
        )
    }
}
