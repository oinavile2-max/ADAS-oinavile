package br.com.dashcamadas

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** Native presentation of the approved commercial concept. No browser or simulated service data. */
class CommercialPage(val activity: Activity, title: String, subtitle: String) {
    val root = LinearLayout(activity)
    val body = LinearLayout(activity)
    val scroll = ScrollView(activity)
    val footer = LinearLayout(activity)
    val visuals = PremiumVisuals(activity)
    private val tabButtons = mutableListOf<Pair<Button, () -> Unit>>()
    private val tabColumn = LinearLayout(activity)
    private val tabHost = FrameLayout(activity)
    private var verticalTabs: Boolean? = null
    private val workspace = object : LinearLayout(activity) {
        override fun onMeasure(w: Int, h: Int) {
            val wide = MeasureSpec.getSize(w) >= dp(900)
            if (tabButtons.isNotEmpty() && verticalTabs != wide) {
                verticalTabs = wide
                orientation = if (wide) HORIZONTAL else VERTICAL
                (tabColumn.parent as? ViewGroup)?.removeView(tabColumn)
                tabHost.removeAllViews()
                tabColumn.orientation = if (wide) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                val container: ViewGroup = if (wide) ScrollView(activity) else HorizontalScrollView(activity).apply { isHorizontalScrollBarEnabled = false }
                container.addView(tabColumn)
                tabHost.addView(container, FrameLayout.LayoutParams(-1, -1))
                tabHost.layoutParams = LayoutParams(if (wide) dp(196) else -1, if (wide) -1 else dp(68))
                scroll.layoutParams = LayoutParams(if (wide) 0 else -1, if (wide) -1 else 0, 1f)
                tabButtons.forEach { (button, _) ->
                    button.layoutParams = LayoutParams(if (wide) -1 else -2, -2).apply {
                        setMargins(dp(2), dp(4), dp(8), dp(4))
                    }
                }
            }
            super.onMeasure(w, h)
        }
    }
    init {
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(18), dp(10), dp(18), dp(10))
        root.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(12, 30, 47), Color.rgb(5, 13, 24)))
        val header = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(ImageView(activity).apply { setImageResource(R.mipmap.ic_launcher); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }, LinearLayout.LayoutParams(dp(44), dp(44)))
        val name = "DashCam ADAS PRO"
        val brand = SpannableString(name).apply { setSpan(ForegroundColorSpan(PremiumVisuals.CYAN), name.length - 3, name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        header.addView(label(brand, 20f).apply { setPadding(dp(10), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(action("‹ Voltar") { activity.finish() })
        root.addView(header)
        root.addView(label(title, 25f).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); setPadding(0, dp(14), 0, dp(4)) })
        root.addView(label(subtitle, 13f, MUTED).apply { setPadding(0, 0, 0, dp(12)) })
        workspace.orientation = LinearLayout.HORIZONTAL
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(4), dp(4), dp(4), dp(8))
        scroll.isFillViewport = true
        scroll.addView(body)
        workspace.addView(tabHost, LinearLayout.LayoutParams(0, -1))
        workspace.addView(scroll, LinearLayout.LayoutParams(-1, -1))
        root.addView(workspace, LinearLayout.LayoutParams(-1, 0, 1f))
        footer.orientation = LinearLayout.HORIZONTAL
        footer.gravity = Gravity.CENTER_VERTICAL
        footer.setPadding(0, dp(8), 0, 0)
        root.addView(footer)
    }
    fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density + .5f).toInt()
    fun label(value: CharSequence, size: Float = 15f, color: Int = PremiumVisuals.WHITE): TextView = TextView(activity).apply {
        text = value; textSize = size; setTextColor(color); setLineSpacing(0f, 1.12f)
    }
    fun action(value: String, primary: Boolean = false, click: () -> Unit): Button = Button(activity).apply {
        text = value; CommercialUi.button(this)
        if (primary) backgroundTintList = ColorStateList.valueOf(Color.rgb(7, 95, 142))
        setOnClickListener { click() }
    }
    fun tab(value: String, icon: String, click: () -> Unit): Button {
        val button = action(value, click = click)
        val drawable = PremiumIconDrawable(visuals, icon).apply { setBounds(0, 0, dp(22), dp(22)) }
        button.setCompoundDrawables(drawable, null, null, null)
        button.compoundDrawablePadding = dp(10)
        button.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        button.textSize = 13f
        tabColumn.addView(button)
        tabButtons += button to click
        workspace.requestLayout()
        return button
    }
    fun selected(button: Button) {
        tabButtons.forEach { (item, _) ->
            item.isSelected = item === button
            item.backgroundTintList = if (item === button) ColorStateList.valueOf(Color.rgb(7, 71, 105)) else null
            item.setTextColor(if (item === button) PremiumVisuals.WHITE else MUTED)
        }
    }
    fun card(title: String, detail: String = "", icon: String = "gear"): LinearLayout {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(35, 53, 73), Color.rgb(12, 23, 39))).apply {
                cornerRadius = dp(16).toFloat(); setStroke(dp(1).coerceAtLeast(1), Color.rgb(48, 70, 92))
            }
            val heading = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
            heading.addView(ImageView(activity).apply { setImageDrawable(PremiumIconDrawable(visuals, icon)); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }, LinearLayout.LayoutParams(dp(23), dp(23)))
            heading.addView(label(title, 17f).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); setPadding(dp(10), 0, 0, 0) })
            addView(heading)
            if (detail.isNotEmpty()) addView(label(detail, 13f, MUTED).apply { setPadding(0, dp(8), 0, dp(12)) })
        }
    }
    fun addCard(parent: LinearLayout, card: View) {
        parent.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
    }
    fun pair(parent: LinearLayout, left: View, right: View) {
        val row = object : LinearLayout(activity) {
            private var wide: Boolean? = null
            override fun onMeasure(w: Int, h: Int) {
                val next = MeasureSpec.getSize(w) >= dp(580)
                if (wide != next) {
                    wide = next; orientation = if (next) HORIZONTAL else VERTICAL
                    left.layoutParams = LayoutParams(if (next) 0 else -1, -2, if (next) 1f else 0f).apply { if (next) rightMargin = dp(7) else bottomMargin = dp(14) }
                    right.layoutParams = LayoutParams(if (next) 0 else -1, -2, if (next) 1f else 0f).apply { if (next) leftMargin = dp(7) }
                }
                super.onMeasure(w, h)
            }
        }
        row.addView(left); row.addView(right); addCard(parent, row)
    }
    fun note(parent: LinearLayout, value: String, neon: Boolean = false): TextView {
        val view = label(value, 13f, if (neon) Color.rgb(255, 219, 224) else MUTED)
        view.setPadding(dp(14), dp(12), dp(14), dp(12))
        if (neon) view.background = NeonSurface(activity)
        addCard(parent, view)
        return view
    }
    fun show() { activity.setContentView(root); SafeInsets.apply(activity); CommercialUi.applyAppearance(activity) }
    companion object { val MUTED: Int = Color.rgb(158, 179, 204) }
}
