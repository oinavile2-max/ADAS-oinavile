package br.com.dashcamadas

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/** O menu é desenhado usando a referência visual APROVADA, não ícones substitutos.
 * Hitboxes são escaladas junto com a arte; cada tile abre sua seção real.
 */
class PremiumMenuActivity : Activity() {
 override fun onResume() { super.onResume(); CommercialUi.applyAppearance(this) }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.decorView.systemUiVisibility = 5894 or 1024 or 512
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  setContentView(android.widget.FrameLayout(this).apply { addView(MenuSurface(), android.widget.FrameLayout.LayoutParams(-1,-1)) })
  SafeInsets.apply(this)
 }
 private inner class MenuSurface : View(this@PremiumMenuActivity) {
  private val bitmap = BitmapFactory.decodeResource(resources, R.drawable.premium_menu)
  private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
  private val imageBounds = RectF()
  private fun updateBounds() {
   if (width <= 0 || height <= 0) return
   val scale = minOf(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
   val drawWidth = bitmap.width * scale
   val drawHeight = bitmap.height * scale
   val left = (width - drawWidth) / 2f
   val top = (height - drawHeight) / 2f
   imageBounds.set(left, top, left + drawWidth, top + drawHeight)
  }
  override fun onDraw(canvas: Canvas) {
   canvas.drawColor(android.graphics.Color.rgb(5, 10, 20))
   updateBounds()
   canvas.drawBitmap(bitmap,null,imageBounds,paint)
  }
  override fun onTouchEvent(event: MotionEvent): Boolean {
   if(event.action != MotionEvent.ACTION_UP) return true
   updateBounds()
   if (!imageBounds.contains(event.x, event.y)) return true
   val x = (event.x - imageBounds.left) / imageBounds.width()
   val y = (event.y - imageBounds.top) / imageBounds.height()
   val tile = when {
    y in .13f.. .49f -> when { x < .285f -> "LIVE"; x < .51f -> "ADAS"; x < .735f -> "RADARES"; else -> "PLACAS" }
    y in .49f.. .85f -> when { x < .285f -> "SEMAFOROS"; x < .51f -> "EVENTOS"; x < .735f -> "AUDIO"; else -> "CONFIG" }
    y >= .85f -> when { x < .145f -> "INICIO"; x < .255f -> "LIVE"; x < .37f -> "ADAS"; x < .49f -> "RADARES"; x < .61f -> "EVENTOS"; x < .73f -> "MIDIA"; else -> "CONFIG" }
    else -> "INICIO"
   }
   if(tile == "INICIO") return true
   if(tile == "LIVE") startActivity(Intent(this@PremiumMenuActivity,MainActivity::class.java))
   else if(tile == "CONFIG") startActivity(Intent(this@PremiumMenuActivity,SettingsActivity::class.java))
   else if(tile == "AUDIO") startActivity(Intent(this@PremiumMenuActivity,SettingsActivity::class.java).putExtra("section","AUDIO"))
   else startActivity(Intent(this@PremiumMenuActivity,PremiumSectionActivity::class.java).putExtra("section",tile))
   return true
  }
 }
}
