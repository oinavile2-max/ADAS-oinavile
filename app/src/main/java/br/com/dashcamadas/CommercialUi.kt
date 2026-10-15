package br.com.dashcamadas

import android.app.Activity
import android.view.WindowManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.Button
import android.widget.TextView

/** Shared finish for controls on the internal pages; event popups retain their red presentation. */
object CommercialUi {
    fun button(view: Button, primary: Boolean = false) {
        view.setTag(R.id.neon_primary, primary)
        buttonSurface(view, NeonTheme.read(view.context))
        view.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        view.textSize = 15f
        view.letterSpacing = .025f
        view.isAllCaps = false
        val density = view.resources.displayMetrics.density
        view.minHeight = (52 * density).toInt(); view.minimumHeight = view.minHeight
        view.setPadding((18 * density).toInt(), (12 * density).toInt(), (18 * density).toInt(), (12 * density).toInt())
    }
    private fun buttonSurface(view: Button, theme: NeonThemeSettings) {
        val density = view.resources.displayMetrics.density
        fun dp(value: Float) = (value * density).toInt()
        view.backgroundTintList = null
        val active = view.getTag(R.id.neon_primary) == true || view.isSelected
        fun surface(focused: Boolean): GradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.rgb(38, 58, 79), Color.rgb(17, 32, 49), Color.rgb(9, 22, 37)).map { theme.buttonFill(it, active) }.toIntArray()
        ).apply {
            cornerRadius = 12f * density
            setStroke(dp(if (focused) 2f else 1f).coerceAtLeast(1),
                if (focused || active) theme.accent else NeonThemeSettings.blend(Color.rgb(56, 95, 126), theme.accent, .35f))
        }
        val states = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), surface(true))
            addState(intArrayOf(), surface(false))
        }
        val mask = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 12f * density }
        view.background = RippleDrawable(ColorStateList.valueOf((theme.accent and 0xffffff) or (55 shl 24)), states, mask)
        view.setTextColor(ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(Color.rgb(120, 141, 161), PremiumVisuals.WHITE)
        ))

    }
    fun buttonsIn(root: View) {
        if (root is Button && root !is android.widget.CompoundButton) button(root)
        if (root is ViewGroup) for (i in 0 until root.childCount) buttonsIn(root.getChildAt(i))
    }
    fun refreshTheme(root: View) {
        val theme = NeonTheme.read(root.context)
        fun visit(view: View) {
            when (view) {
                is Switch -> {
                    view.thumbTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(theme.accent, CommercialPage.MUTED))
                    view.trackTintList = ColorStateList.valueOf(NeonThemeSettings.blend(Color.rgb(49, 66, 88), theme.accent, .25f))
                }
                is Button -> if (view.getTag(R.id.neon_primary) != null) buttonSurface(view, theme)
                is SeekBar -> {
                    view.progressTintList = ColorStateList.valueOf(theme.accent)
                    view.thumbTintList = ColorStateList.valueOf(theme.accent)
                }
                is Spinner -> view.setPopupBackgroundDrawable(NeonSurface(view.context))

            }
            if (view.getTag(R.id.neon_role) == "brand" && view is TextView) {
                val value = view.text.toString()
                view.text = android.text.SpannableString(value).apply {
                    setSpan(android.text.style.ForegroundColorSpan(theme.accent), value.length - 3, value.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            val stroke = view.getTag(R.id.neon_border_width) as? Int
            if (stroke != null) (view.background as? GradientDrawable)?.setStroke(stroke, theme.accent)
            view.invalidate()
            if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
        }
        visit(root)
    }
    fun applyAppearance(activity: Activity) {
        val appearance=activity.getSharedPreferences("ui_appearance",Activity.MODE_PRIVATE)
        val night=appearance.getBoolean("night",false) || (appearance.getBoolean("automatic_brightness",false) &&
            activity.getSharedPreferences("camera_runtime",Activity.MODE_PRIVATE).getBoolean("night",false))
        activity.findViewById<ViewGroup>(android.R.id.content)?.let { refreshTheme(it) }
        activity.window.attributes = activity.window.attributes.apply {
            screenBrightness = if (night) .22f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }
    fun heading(view: TextView) {
        view.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        view.setTextColor(PremiumVisuals.WHITE)
        view.setLineSpacing(0f, 1.12f)
    }
}
