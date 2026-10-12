package br.com.dashcamadas

import android.content.Context
import android.graphics.*
import android.view.View

/** Draws actual YUV camera frames published by the owning foreground service. */
class CameraPreviewView(context:Context):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    var frame:Bitmap?=null
        set(value){field=value;invalidate()}
    var timestampMs=0L
    var fill=false
    var mirror=false
    var focusBox:br.com.dashcamadas.vision.Box?=null
        set(value){field=value;invalidate()}
    fun mapping():CameraTransform? = frame?.let { bitmap->
        if(width<=0 || height<=0)return@let null
        val base=CameraTransform(bitmap.width,bitmap.height,width,height,0,mirror,fill)
        val focus=focusBox ?: return@let base
        if(!fill)return@let base
        val center=base.project(focus.centerX*bitmap.width,(focus.top+focus.bottom)/2*bitmap.height)
        val horizontal=((bitmap.width*base.scale-width)/2).coerceAtLeast(0f)
        val vertical=((bitmap.height*base.scale-height)/2).coerceAtLeast(0f)
        base.copy(offsetX=(width/2f-center.first).coerceIn(-horizontal,horizontal),offsetY=(height/2f-center.second).coerceIn(-vertical,vertical))
    }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val bitmap=frame ?: return
        val transform=mapping() ?: return
        val source=floatArrayOf(0f,0f,bitmap.width.toFloat(),0f,0f,bitmap.height.toFloat())
        val destination=FloatArray(6)
        for(i in 0..2) {val point=transform.project(source[i*2],source[i*2+1]);destination[i*2]=point.first;destination[i*2+1]=point.second}
        val matrix=Matrix().apply{setPolyToPoly(source,0,destination,0,3)}
        canvas.drawBitmap(bitmap,matrix,paint)
    }
}
