package com.sensibotpro.headlock

import kotlin.math.roundToInt

enum class HeadlockMode(val wireValue: String) {
    HOLD("hold"),
    BURST("burst");

    companion object {
        fun fromWire(value: String?): HeadlockMode =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: HOLD
    }
}

enum class HeadlockCurve(val wireValue: String) {
    LINEAR("linear"),
    EASE_OUT("easeOut");

    companion object {
        fun fromWire(value: String?): HeadlockCurve =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: LINEAR
    }
}

enum class HeadlockDeviceClass(val wireValue: String, val label: String) {
    COMPACT("compact", "Compact (<6.0\")"),
    PHONE("phone", "Standard Phone (6.0\"-6.5\")"),
    LARGE("large", "Large Phone (>6.5\")"),
    TABLET("tablet", "Tablet / iPad");

    companion object {
        fun fromWire(value: String?): HeadlockDeviceClass =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: PHONE
    }
}

data class HeadlockDeviceMetrics(
    val deviceClass: HeadlockDeviceClass,
    val screenHeightPx: Int,
    val refreshRateHz: Int
)

data class HeadlockConfig(
    val v: Int = CURRENT_VERSION,
    val mode: HeadlockMode = HeadlockMode.HOLD,
    val startX: Double = 0.82,
    val startY: Double = 0.78,
    val distancePx: Int = 42,
    val durationMs: Int = 90,
    val repeatIntervalMs: Int = 55,
    val burstCount: Int = 3,
    val curve: HeadlockCurve = HeadlockCurve.LINEAR,
    val verticalBias: Double = 1.0,
    val deviceClass: HeadlockDeviceClass = HeadlockDeviceClass.PHONE
) {
    fun clamped(): HeadlockConfig {
        val roundedBias = ((verticalBias.coerceIn(MIN_VERTICAL_BIAS, MAX_VERTICAL_BIAS)) * 10.0).roundToInt() / 10.0
        return copy(
            v = if (v < 1) CURRENT_VERSION else v,
            startX = startX.coerceIn(0.05, 0.95),
            startY = startY.coerceIn(0.05, 0.95),
            distancePx = distancePx.coerceIn(10, 400),
            durationMs = durationMs.coerceIn(20, 500),
            repeatIntervalMs = repeatIntervalMs.coerceIn(15, 500),
            burstCount = burstCount.coerceIn(1, 10),
            verticalBias = roundedBias
        )
    }

    fun toJson(): String {
        val c = clamped()
        return buildString {
            append("{")
            append("\"v\":").append(c.v).append(",")
            append("\"mode\":\"").append(c.mode.wireValue).append("\",")
            append("\"startX\":").append(c.startX).append(",")
            append("\"startY\":").append(c.startY).append(",")
            append("\"distancePx\":").append(c.distancePx).append(",")
            append("\"durationMs\":").append(c.durationMs).append(",")
            append("\"repeatIntervalMs\":").append(c.repeatIntervalMs).append(",")
            append("\"burstCount\":").append(c.burstCount).append(",")
            append("\"curve\":\"").append(c.curve.wireValue).append("\",")
            append("\"verticalBias\":").append(c.verticalBias).append(",")
            append("\"deviceClass\":\"").append(c.deviceClass.wireValue).append("\"")
            append("}")
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
        const val STORAGE_KEY_PREFIX = "headlock."
        const val STORAGE_CONFIG_KEY = "${STORAGE_KEY_PREFIX}config"
        const val MIN_VERTICAL_BIAS = 0.6
        const val MAX_VERTICAL_BIAS = 1.6

        fun fromJson(json: String?): HeadlockConfig {
            if (json.isNullOrBlank()) return HeadlockConfig()
            val map = parseFlatJsonObject(json)
            if (map.isEmpty()) return HeadlockConfig()

            val v = map["v"]?.toIntOrNull() ?: CURRENT_VERSION
            val mode = HeadlockMode.fromWire(map["mode"])
            val startX = map["startX"]?.toDoubleOrNull() ?: 0.82
            val startY = map["startY"]?.toDoubleOrNull() ?: 0.78
            val distancePx = map["distancePx"]?.toIntOrNull() ?: 42
            val durationMs = map["durationMs"]?.toIntOrNull() ?: 90
            val repeatIntervalMs = map["repeatIntervalMs"]?.toIntOrNull() ?: 55
            val burstCount = map["burstCount"]?.toIntOrNull() ?: 3
            val curve = HeadlockCurve.fromWire(map["curve"])
            val verticalBias = map["verticalBias"]?.toDoubleOrNull() ?: 1.0
            val deviceClass = HeadlockDeviceClass.fromWire(map["deviceClass"])

            return HeadlockConfig(
                v = v,
                mode = mode,
                startX = startX,
                startY = startY,
                distancePx = distancePx,
                durationMs = durationMs,
                repeatIntervalMs = repeatIntervalMs,
                burstCount = burstCount,
                curve = curve,
                verticalBias = verticalBias,
                deviceClass = deviceClass
            ).clamped()
        }

        private fun parseFlatJsonObject(raw: String): Map<String, String> {
            val trimmed = raw.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return emptyMap()
            val body = trimmed.substring(1, trimmed.length - 1).trim()
            if (body.isEmpty()) return emptyMap()

            val result = mutableMapOf<String, String>()
            val pairs = body.split(",")
            for (pair in pairs) {
                val idx = pair.indexOf(':')
                if (idx <= 0) continue
                val key = pair.substring(0, idx).trim().trim('"')
                val value = pair.substring(idx + 1).trim().trim('"')
                if (key.isNotEmpty()) {
                    result[key] = value
                }
            }
            return result
        }
    }
}

