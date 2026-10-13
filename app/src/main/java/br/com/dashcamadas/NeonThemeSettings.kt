package br.com.dashcamadas

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class NeonColor(val title: String, val argb: Int) {
    CYAN("Ciano neon", 0xff00aeff.toInt()), VIOLET("Violeta neon", 0xffa855ff.toInt()),
    PINK("Rosa neon", 0xffff4fd8.toInt()), GREEN("Verde neon", 0xff39ff88.toInt()),
    ORANGE("Laranja neon", 0xffff9a32.toInt()), RED("Vermelho neon", 0xffff233e.toInt());
    companion object { fun fromId(id: String?) = entries.firstOrNull { it.name == id } ?: CYAN }
}

/** Pure palette rules: only surface fills fade; critical event colors remain fixed. */
data class NeonThemeSettings(
    val color: NeonColor = NeonColor.CYAN,
    val tone: Int = 0,
    val buttonTransparency: Int = 0,
    val popupTransparency: Int = 0,
    val popupFollowsTheme: Boolean = false
) {
    fun normalized() = copy(tone = tone.coerceIn(-30, 30), buttonTransparency = buttonTransparency.coerceIn(0, 80), popupTransparency = popupTransparency.coerceIn(0, 80))
    val accent: Int = hueShift(color.argb, tone.coerceIn(-30, 30))
    val popupAccent: Int = if (popupFollowsTheme) accent else ALERT_RED
    fun buttonFill(base: Int, active: Boolean = false): Int = fade(blend(base, accent, if (active) .35f else .13f), buttonTransparency)
    fun popupFill(base: Int, critical: Boolean = false): Int {
        val edge = if (critical) ALERT_RED else popupAccent
        val fill = fade(blend(base, edge, .12f), if (critical) popupTransparency.coerceAtMost(25) else popupTransparency)
        return if (critical) (fill and 0xffffff) or (max(191, (fill ushr 24) and 255) shl 24) else fill
    }
    companion object {
        const val ALERT_RED: Int = -56514 // #FFFF233E
        fun fade(argb: Int, transparency: Int): Int {
            val alpha = (((argb ushr 24) and 255) * (1f - transparency.coerceIn(0, 80) / 100f)).roundToInt()
            return (argb and 0xffffff) or (alpha shl 24)
        }
        fun blend(base: Int, tint: Int, amount: Float): Int {
            val weight = amount.coerceIn(0f, 1f)
            fun channel(shift: Int) = ((((base ushr shift) and 255) * (1f - weight)) + (((tint ushr shift) and 255) * weight)).roundToInt()
            return (base and -0x1000000) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
        }
        private fun hueShift(argb: Int, offset: Int): Int {
            if (offset == 0) return argb
            val r = ((argb ushr 16) and 255) / 255f; val g = ((argb ushr 8) and 255) / 255f; val b = (argb and 255) / 255f
            val high = max(r, max(g, b)); val low = min(r, min(g, b)); val chroma = high - low
            if (chroma == 0f) return argb
            val raw = when (high) { r -> (g - b) / chroma; g -> (b - r) / chroma + 2f; else -> (r - g) / chroma + 4f }
            val hue = ((raw * 60f + offset) % 360f + 360f) % 360f
            val x = chroma * (1f - abs((hue / 60f) % 2f - 1f))
            val rgb = when {
                hue < 60f -> floatArrayOf(chroma, x, 0f)
                hue < 120f -> floatArrayOf(x, chroma, 0f)
                hue < 180f -> floatArrayOf(0f, chroma, x)
                hue < 240f -> floatArrayOf(0f, x, chroma)
                hue < 300f -> floatArrayOf(x, 0f, chroma)
                else -> floatArrayOf(chroma, 0f, x)
            }
            fun channel(i: Int) = ((rgb[i] + low) * 255f).roundToInt().coerceIn(0, 255)
            return (argb and -0x1000000) or (channel(0) shl 16) or (channel(1) shl 8) or channel(2)
        }
    }
}
