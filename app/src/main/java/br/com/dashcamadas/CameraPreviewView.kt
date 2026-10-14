package br.com.dashcamadas

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout

/** Camera2 can use the GPU directly; immutable bitmap snapshots remain the native/ADAS fallback. */
class CameraPreviewView(context:Context,allowDirect:Boolean=false):FrameLayout(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val gpu=if(allowDirect)TextureView(context) else null
    var onSurfaceChanged:((SurfaceTexture?)->Unit)?=null
    var onFrameDrawn:((Long)->Unit)?=null
    var directActive=false;private set
    private var directWanted=false
    private var rawWidth=0
    private var rawHeight=0
    private var rotation=0
    var frame:Bitmap?=null
        set(value){field=value;invalidate()}
    var timestampMs=0L
    private var drawnTimestamp=-1L
    private val source=FloatArray(6)
    private val destination=FloatArray(6)
    private val matrix=Matrix()
    private val gpuMatrix=Matrix()
    private val producerMatrix=FloatArray(16)
    private var producerOrientation=TextureBufferOrientation()
    var fill=false
        set(value){if(field==value)return;field=value;updateDirectTransform();invalidate()}
    var mirror=false
        set(value){if(field==value)return;field=value;updateDirectTransform();invalidate()}
    var focusBox:br.com.dashcamadas.vision.Box?=null
        set(value){field=value;invalidate()}
    init {
        setWillNotDraw(false)
        gpu?.let { view ->
            view.isOpaque=false;view.visibility=View.INVISIBLE
            view.surfaceTextureListener=object:TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(texture:SurfaceTexture,w:Int,h:Int){producerOrientation=TextureBufferOrientation();if(directWanted)onSurfaceChanged?.invoke(texture);updateDirectTransform()}
                override fun onSurfaceTextureSizeChanged(texture:SurfaceTexture,w:Int,h:Int){if(rawWidth>0 && rawHeight>0)texture.setDefaultBufferSize(rawWidth,rawHeight);updateDirectTransform()}
                override fun onSurfaceTextureDestroyed(texture:SurfaceTexture):Boolean {onSurfaceChanged?.invoke(null);return true}
                override fun onSurfaceTextureUpdated(texture:SurfaceTexture){
                    texture.getTransformMatrix(producerMatrix)
                    val actual=TextureBufferOrientation.fromSurfaceMatrix(producerMatrix)
                    if(actual!=null && actual!=producerOrientation){producerOrientation=actual;updateDirectTransform()}
                    if(directActive){timestampMs=SystemClock.elapsedRealtime();onFrameDrawn?.invoke(timestampMs)}}
            }
            addView(view,LayoutParams(-1,-1))
        }
    }
    fun directTexture():SurfaceTexture?=gpu?.takeIf{directWanted && it.isAvailable}?.surfaceTexture
    fun requestDirectPreview(wanted:Boolean) {
        if(directWanted==wanted)return
        directWanted=wanted
        gpu?.visibility=if(wanted)View.VISIBLE else View.INVISIBLE
        gpu?.alpha=if(directActive)1f else .01f
        onSurfaceChanged?.invoke(directTexture())
    }
    fun showDirectPreview(active:Boolean,w:Int,h:Int,angle:Int) {
        directActive=active && gpu!=null
        rawWidth=w;rawHeight=h;rotation=angle
        gpu?.alpha=if(directActive)1f else .01f
        updateDirectTransform();invalidate()
    }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){super.onSizeChanged(w,h,oldw,oldh);updateDirectTransform()}
    private fun updateDirectTransform() {
        val view=gpu ?: return
        if(width<=0 || height<=0 || rawWidth<=0 || rawHeight<=0)return
        val transform=CameraTransform(rawWidth,rawHeight,width,height,rotation,mirror,fill)
        val points=floatArrayOf(0f,0f,rawWidth.toFloat(),0f,0f,rawHeight.toFloat())
        val target=FloatArray(6)
        for(i in 0..2){val point=transform.project(points[i*2],points[i*2+1]);target[i*2]=point.first;target[i*2+1]=point.second}
        // Map the corners as TextureView actually displays them, compensating the producer once.
        val displayed=producerOrientation.displayTriangle(width,height)
        gpuMatrix.setPolyToPoly(displayed,0,target,0,3)
        view.setTransform(gpuMatrix)
    }
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
        if(directActive)return
        val bitmap=frame ?: return
        val transform=mapping() ?: return
        source[0]=0f;source[1]=0f;source[2]=bitmap.width.toFloat();source[3]=0f;source[4]=0f;source[5]=bitmap.height.toFloat()
        for(i in 0..2) {val point=transform.project(source[i*2],source[i*2+1]);destination[i*2]=point.first;destination[i*2+1]=point.second}
        matrix.setPolyToPoly(source,0,destination,0,3)
        canvas.drawBitmap(bitmap,matrix,paint)
        if(timestampMs!=drawnTimestamp){drawnTimestamp=timestampMs;onFrameDrawn?.invoke(timestampMs)}
    }
}
