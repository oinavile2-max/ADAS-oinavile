package br.com.dashcamadas

import android.content.Context
import android.content.SharedPreferences

/** One cached immutable palette; never read preferences once per pixel/frame. */
object NeonTheme {
    @Volatile private var preferences: SharedPreferences? = null
    @Volatile private var palette = NeonThemeSettings()
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == null || key.startsWith("neon_")) palette = load(prefs)
    }
    fun read(context: Context): NeonThemeSettings {
        if (preferences == null) synchronized(this) {
            if (preferences == null) {
                val prefs = context.applicationContext.getSharedPreferences("ui_appearance", Context.MODE_PRIVATE)
                palette = load(prefs); prefs.registerOnSharedPreferenceChangeListener(listener); preferences = prefs
            }
        }
        return palette
    }
    fun save(context: Context, value: NeonThemeSettings) {
        read(context)
        val clean = value.normalized()
        palette = clean
        preferences!!.edit().putString("neon_color", clean.color.name).putInt("neon_tone", clean.tone)
            .putInt("neon_button_transparency", clean.buttonTransparency).putInt("neon_popup_transparency", clean.popupTransparency)
            .putBoolean("neon_popup_follows_theme", clean.popupFollowsTheme).apply()
    }
    private fun load(prefs: SharedPreferences) = NeonThemeSettings(
        NeonColor.fromId(prefs.getString("neon_color", null)), prefs.getInt("neon_tone", 0),
        prefs.getInt("neon_button_transparency", 0), prefs.getInt("neon_popup_transparency", 0),
        prefs.getBoolean("neon_popup_follows_theme", false)
    ).normalized()
}
