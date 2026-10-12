package br.com.dashcamadas

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

/** Shared finish for controls on the internal pages; event popups retain their red presentation. */
object CommercialUi {
    fun button(view: Button) {
        val density = view.resources.displayMetrics.density
        fun dp(value: Float) = (value * density).toInt()
        fun surface(focused: Boolean): GradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.rgb(38, 58, 79), Color.rgb(17, 32, 49), Color.rgb(9, 22, 37))
        ).apply {
            cornerRadius = 12f * density
            setStroke(dp(if (focused) 2f else 1f).coerceAtLeast(1),
                if (focused) Color.rgb(111, 222, 255) else Color.rgb(56, 95, 126))
        }
        val states = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), surface(true))
            addState(intArrayOf(), surface(false))
        }
        val mask = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 12f * density }
        view.background = RippleDrawable(ColorStateList.valueOf(Color.argb(55, 85, 209, 255)), states, mask)
        view.setTextColor(ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(Color.rgb(120, 141, 161), PremiumVisuals.WHITE)
        ))
        view.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        view.textSize = 15f
        view.letterSpacing = .025f
        view.isAllCaps = false
        view.minHeight = dp(52f)
        view.minimumHeight = dp(52f)
        view.setPadding(dp(18f), dp(12f), dp(18f), dp(12f))
    }
    fun buttonsIn(root: View) {
        if (root is Button) button(root)
        if (root is ViewGroup) for (i in 0 until root.childCount) buttonsIn(root.getChildAt(i))
    }
    fun heading(view: TextView) {
        view.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        view.setTextColor(PremiumVisuals.WHITE)
        view.setLineSpacing(0f, 1.12f)
    }
}
