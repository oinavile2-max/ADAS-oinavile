package br.com.dashcamadas

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.graphics.PathParser
import org.json.JSONObject

/** Shared vector paths are also used by the visual HTML preview. No emoji/font substitution. */
class PremiumVisuals(context: Context) {
    private data class Shape(val path: Path, val filled: Boolean, val stroke: Float)
    private val icons = mutableMapOf<String, List<Shape>>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    init {
        val json = JSONObject(context.assets.open("premium_ui_icons.json").bufferedReader().use { it.readText() })
        for (key in json.keys()) {
            val items = json.getJSONArray(key)
            icons[key] = (0 until items.length()).map { index ->
                val item = items.getJSONObject(index)
                Shape(requireNotNull(PathParser.createPathFromPathData(item.getString("d"))), item.optBoolean("fill"), item.optDouble("stroke", 0.0).toFloat())
            }
        }
    }
    fun icon(canvas: Canvas, key: String, x: Float, y: Float, size: Float, color: Int) {
        canvas.save(); canvas.translate(x, y); canvas.scale(size / 100f, size / 100f)
        paint.color = color; paint.shader = null; paint.clearShadowLayer()
        paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        for (shape in icons[key].orEmpty()) {
            paint.style = if (shape.filled) Paint.Style.FILL else Paint.Style.STROKE
            paint.strokeWidth = shape.stroke
            canvas.drawPath(shape.path, paint)
        }
        canvas.restore()
    }
    companion object {
        val CYAN = Color.rgb(0, 174, 255)
        val GOLD = Color.rgb(255, 189, 57)
        val RED = Color.rgb(255, 35, 62)
        val WHITE = Color.rgb(245, 247, 250)
    }
}

data class EventPresentation(val title: String, val detail: String, val icon: String, val critical: Boolean = false) {
    companion object {
        fun radar(alert: RadarAlert): EventPresentation = EventPresentation(
            when (alert.radar.type) {
                "fixo" -> "RADAR FIXO À FRENTE"
                "movel" -> "RADAR MÓVEL À FRENTE"
                "semaforo" -> "RADAR SEMAFÓRICO À FRENTE"
                else -> "RADAR CADASTRADO À FRENTE"
            }, "${alert.radar.limit} km/h", if (alert.radar.type == "semaforo") "traffic" else "radar"
        )
        fun adas(message: String): EventPresentation {
            val normalized = message.uppercase(java.util.Locale.ROOT)
            return when {
                "COLIS" in normalized -> EventPresentation("RISCO DE COLISÃO", "AUMENTE A DISTÂNCIA", "car", true)
                "FAIXA" in normalized -> EventPresentation("SAÍDA DE FAIXA", if ("ESQUERDA" in normalized) "ATENÇÃO À ESQUERDA" else "ATENÇÃO À DIREITA", "lane", true)
                "PEDESTRE" in normalized -> EventPresentation("PEDESTRE À FRENTE", "REDUZA A VELOCIDADE", "pedestrian", true)
                "CICLIST" in normalized -> EventPresentation("CICLISTA À FRENTE", "REDUZA A VELOCIDADE", "cyclist", true)
                else -> EventPresentation("ATENÇÃO", message, "warning", true)
            }
        }
    }
}

/** Popup 2: fixed 752×194 reference canvas, including the glow outside its 720×162 border. */
class EventPopupView(context: Context, private val visuals: PremiumVisuals) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val box = RectF(16f, 16f, 736f, 178f)
    var presentation = EventPresentation("", "", "warning")
        set(value) { field = value; contentDescription = "${value.title}. ${value.detail}"; invalidate() }
    init { setLayerType(LAYER_TYPE_SOFTWARE, null); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = minOf(width / 752f, height / 194f)
        if (scale <= 0f) return
        canvas.save(); canvas.translate((width - 752f * scale) / 2f, (height - 194f * scale) / 2f); canvas.scale(scale, scale)
        val edge = PremiumVisuals.RED
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f; paint.color = edge; paint.shader = null
        paint.setShadowLayer(13f, 0f, 0f, edge)
        canvas.drawRoundRect(box, 19f, 19f, paint)
        paint.clearShadowLayer(); paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, 16f, 0f, 178f,
            intArrayOf(Color.argb(230,145,40,49), Color.argb(242,33,13,20), Color.argb(245,80,32,43)),
            floatArrayOf(0f,.6f,1f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(box, 19f, 19f, paint)
        paint.shader = null; paint.style = Paint.Style.STROKE; paint.color = edge; paint.strokeWidth = 2.4f
        paint.setShadowLayer(3f, 0f, 0f, edge); canvas.drawRoundRect(box, 19f, 19f, paint); paint.clearShadowLayer()
        paint.style = Paint.Style.FILL; paint.color = Color.argb(184,24,7,13)
        canvas.drawRoundRect(RectF(49f,37f,185f,160f),28f,28f,paint)
        visuals.icon(canvas,presentation.icon,61f,32f,113f,edge)
        paint.color = Color.rgb(255,219,224)
        paint.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER; paint.textSize = 38f
        fitText(presentation.title, 525f)
        canvas.drawText(presentation.title,455f,84f,paint)
        paint.color = Color.rgb(255,219,224)
        paint.textSize = if (presentation.detail.endsWith(" km/h")) 40f else 27f
        fitText(presentation.detail, 525f)
        canvas.drawText(presentation.detail,455f,141f,paint)
        canvas.restore()
    }
    private fun fitText(value: String, max: Float) {
        val measured = paint.measureText(value)
        if (measured > max) paint.textSize *= max / measured
    }
}

/** Six glossy navigation tiles; icons and labels remain independent of device emoji fonts. */
class PremiumNavigationView(context: Context, private val visuals: PremiumVisuals, val label: String, private val icon: String, private val active: Boolean = false) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    init { isClickable = true; isFocusable = true; contentDescription = label }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        canvas.save(); canvas.scale(width / 252f, height / 89f)
        val colors = if (active) intArrayOf(Color.rgb(0,184,244),Color.rgb(0,105,172),Color.rgb(0,65,119),Color.rgb(0,34,81),Color.rgb(0,113,202))
            else intArrayOf(Color.rgb(83,97,112),Color.rgb(40,54,68),Color.rgb(23,34,48),Color.rgb(3,11,21),Color.rgb(16,30,44))
        paint.style = Paint.Style.FILL; paint.shader = LinearGradient(0f,0f,0f,89f,colors,floatArrayOf(0f,.25f,.49f,.51f,1f),Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(1f,1f,251f,88f),17f,17f,paint)
        paint.shader=null; paint.style=Paint.Style.STROKE; paint.color=if(isFocused)Color.rgb(140,231,255) else if(active)PremiumVisuals.CYAN else Color.rgb(105,120,138); paint.strokeWidth=if(isFocused)3f else 1.6f
        canvas.drawRoundRect(RectF(1f,1f,251f,88f),17f,17f,paint)
        // A narrow glass highlight preserves the reference finish without a large blurred layer.
        paint.color=Color.argb(if(active)135 else 65,210,239,255);paint.strokeWidth=1f
        canvas.drawLine(22f,3f,230f,3f,paint)
        if(isPressed){paint.style=Paint.Style.FILL;paint.color=Color.argb(45,255,255,255);canvas.drawRoundRect(RectF(1f,1f,251f,88f),17f,17f,paint)}
        visuals.icon(canvas,icon,45f,22f,42f,if(active)Color.rgb(120,222,255) else PremiumVisuals.WHITE)
        paint.style=Paint.Style.FILL;paint.color=PremiumVisuals.WHITE;paint.typeface=Typeface.create("sans-serif-condensed",Typeface.NORMAL);paint.textAlign=Paint.Align.LEFT;paint.textSize=29f
        val textWidth=paint.measureText(label); if(textWidth>139f)paint.textSize*=139f/textWidth
        canvas.drawText(label,112f,56f,paint);canvas.restore()
    }
    override fun drawableStateChanged(){super.drawableStateChanged();invalidate()}
}

class PremiumIconDrawable(private val visuals: PremiumVisuals, private val icon: String, private val shell: Boolean = false, private val active: () -> Boolean = { false }) : Drawable() {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas:Canvas){
        canvas.save();canvas.translate(bounds.left.toFloat(),bounds.top.toFloat());canvas.scale(bounds.width()/100f,bounds.height()/100f)
        if(shell){paint.color=if(active())Color.rgb(96,20,38) else Color.rgb(0,69,120);paint.style=Paint.Style.FILL;canvas.drawRoundRect(RectF(1f,1f,99f,99f),22f,22f,paint);paint.style=Paint.Style.STROKE;paint.strokeWidth=3f;paint.color=if(active())PremiumVisuals.RED else PremiumVisuals.CYAN;canvas.drawRoundRect(RectF(1f,1f,99f,99f),22f,22f,paint)}
        visuals.icon(canvas,icon,19f,19f,62f,PremiumVisuals.WHITE);canvas.restore()
    }
    override fun setAlpha(alpha:Int){}
    override fun setColorFilter(filter:ColorFilter?){}
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
