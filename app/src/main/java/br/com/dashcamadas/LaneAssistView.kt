package br.com.dashcamadas

import android.content.Context
import android.graphics.*
import android.view.View

/** Visualiza SOMENTE linhas detectadas nos pixels da câmera. Sem detecção = sem linhas. */
class LaneAssistView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(51, 236, 174); strokeWidth = 5f; style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND; setShadowLayer(5f, 0f, 0f, Color.rgb(18, 90, 80))
    }
    var transform: CameraTransform? = null
        set(value) { field = value; invalidate() }
    private var left: FloatArray? = null
    private var right: FloatArray? = null
    fun update(result: LaneDetection.Result?) {
        left = result?.left; right = result?.right; invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (lane in listOfNotNull(left, right)) {
            val path = Path()
            val mapping = transform ?: continue
            val start = mapping.project(lane[0]*mapping.frameWidth, lane[1]*mapping.frameHeight)
            val end = mapping.project(lane[2]*mapping.frameWidth, lane[3]*mapping.frameHeight)
            path.moveTo(start.first, start.second)
            path.lineTo(end.first, end.second)
            canvas.drawPath(path, paint)
        }
    }
}

/** Baseline de faixas: extração por contraste e ajuste linear com rejeição por confiança.
 * Não é ADAS de segurança certificado. Funciona somente para marcações visíveis.
 */
object LaneDetection {
    data class Result(val left: FloatArray?, val right: FloatArray?)
    fun detect(bitmap: Bitmap): Result? {
        val pixels=IntArray(bitmap.width*bitmap.height)
        bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        val gray=IntArray(pixels.size) { i ->
            val c=pixels[i]
            (Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100
        }
        return LaneGeometry.detect(gray,bitmap.width,bitmap.height)?.let { Result(it.left,it.right) }
    }
}
