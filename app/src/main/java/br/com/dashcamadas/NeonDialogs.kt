package br.com.dashcamadas

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** A single red-neon presentation for application-owned dialogs, dropdowns and notices. */
object NeonDialogs {
    fun themed(context:Context):Context = ContextThemeWrapper(context,android.R.style.Theme_Material_Dialog_Alert)
    class Builder(context:Context):AlertDialog.Builder(NeonDialogs.themed(context)) {
        override fun create():AlertDialog = super.create().also { dialog->dialog.setOnShowListener { NeonDialogs.decorate(dialog) } }
    }
    fun decorate(dialog:AlertDialog){
        val window=dialog.window ?: return
        window.setBackgroundDrawable(NeonSurface(dialog.context))
        window.decorView.setLayerType(View.LAYER_TYPE_SOFTWARE,null)
        styleContent(window.decorView)
        listOf(AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL).forEach { dialog.getButton(it)?.setTextColor(PremiumVisuals.WHITE) }
    }
    fun styleContent(view:View){
        if(view is TextView)view.setTextColor(PremiumVisuals.WHITE)
        if(view is Spinner)view.setPopupBackgroundDrawable(NeonSurface(view.context))
        if(view is ViewGroup)for(i in 0 until view.childCount)styleContent(view.getChildAt(i))
    }
    @Suppress("DEPRECATION")
    fun notice(activity:Activity,message:String){
        val label=TextView(activity).apply { text=message;setTextColor(PremiumVisuals.WHITE);textSize=16f;gravity=Gravity.CENTER;setPadding(32,24,32,24);background=NeonSurface(activity);setLayerType(View.LAYER_TYPE_SOFTWARE,null) }
        Toast(activity).apply { view=label;duration=Toast.LENGTH_SHORT;setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,0,40);show() }
    }
}

class NeonSurface(context:Context):Drawable(){
    private val density=context.resources.displayMetrics.density
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas:Canvas){
        val margin=5f*density
        val box=RectF(bounds.left+margin,bounds.top+margin,bounds.right-margin,bounds.bottom-margin)
        if(box.width()<=0 || box.height()<=0)return
        paint.style=Paint.Style.FILL;paint.shader=LinearGradient(0f,box.top,0f,box.bottom,intArrayOf(Color.rgb(68,19,33),Color.rgb(13,10,19),Color.rgb(34,13,24)),null,Shader.TileMode.CLAMP)
        canvas.drawRoundRect(box,15f*density,15f*density,paint)
        paint.shader=null;paint.style=Paint.Style.STROKE
        for((width,alpha) in listOf(8f to 30,4f to 80,1.6f to 255)){
            paint.strokeWidth=width*density;paint.color=Color.argb(alpha,255,35,62);canvas.drawRoundRect(box,15f*density,15f*density,paint)
        }
    }
    override fun setAlpha(alpha:Int){}
    override fun setColorFilter(filter:ColorFilter?){}
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
