package br.com.dashcamadas

import android.content.Context
import android.graphics.*
import android.view.View

/** Local palette sample; does not issue events or change saved preferences. */
class NeonThemePreviewView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var settings = NeonThemeSettings()
        set(value) { field = value.normalized(); contentDescription = "Prévia ${field.color.title}. Botões ${field.buttonTransparency}% transparentes. Popups ${field.popupTransparency}%."; invalidate() }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize((w * 200f / 560f).toInt(), heightMeasureSpec))
    }
    override fun onDraw(canvas: Canvas) {
        val scale = width / 560f
        if (scale <= 0f) return
        canvas.save(); canvas.scale(scale, scale)
        paint.style = Paint.Style.FILL
        for (x in 0..13) for (y in 0..4) {
            paint.color = if ((x + y) % 2 == 0) Color.rgb(17, 30, 44) else Color.rgb(36, 53, 70)
            canvas.drawRect(x * 40f, y * 40f, (x + 1) * 40f, (y + 1) * 40f, paint)
        }
        fun sample(box: RectF, label: String, color: Int, fill: Int) {
            paint.style = Paint.Style.FILL; paint.color = fill
            canvas.drawRoundRect(box, 12f, 12f, paint)
            paint.style = Paint.Style.STROKE; paint.color = color; paint.strokeWidth = 2f
            canvas.drawRoundRect(box, 12f, 12f, paint)
            paint.style = Paint.Style.FILL; paint.color = PremiumVisuals.WHITE; paint.textSize = 19f
            paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); paint.textAlign = Paint.Align.CENTER
            canvas.drawText(label, box.centerX(), box.centerY() + 6f, paint)
        }
        sample(RectF(14f, 18f, 190f, 94f), "BOTÃO", settings.accent, settings.buttonFill(Color.rgb(17, 32, 49), true))
        sample(RectF(206f, 18f, 546f, 94f), "POPUP COMUM", settings.popupAccent, settings.popupFill(Color.rgb(33, 13, 20)))
        sample(RectF(14f, 112f, 546f, 184f), "BANNER DE EVENTO · VERMELHO", NeonThemeSettings.ALERT_RED, settings.popupFill(Color.rgb(33, 13, 20), true))
        canvas.restore()
    }
}
