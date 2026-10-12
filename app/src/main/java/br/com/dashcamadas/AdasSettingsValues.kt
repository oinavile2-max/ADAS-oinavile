package br.com.dashcamadas

/** Validate all advanced values before the settings screen writes any ADAS preference. */
data class AdasSettingsValues(val speed: Float, val ttc: Float, val confirmation: Long, val cooldown: Long, val blink: Long) {
    companion object {
        fun parse(speed: String, ttc: String, confirmation: String, cooldown: String, blink: String): AdasSettingsValues? {
            val a = speed.replace(',', '.').toFloatOrNull() ?: return null
            val b = ttc.replace(',', '.').toFloatOrNull() ?: return null
            val c = confirmation.toLongOrNull() ?: return null
            val d = cooldown.toLongOrNull() ?: return null
            val e = blink.toLongOrNull() ?: return null
            if (a !in 10f..100f || b !in 1f..5f || c !in 300L..2000L || d !in 5000L..60000L || e !in 250L..1000L) return null
            return AdasSettingsValues(a, b, c, d, e)
        }
    }
}
