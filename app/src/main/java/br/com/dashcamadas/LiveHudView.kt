package br.com.dashcamadas

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.hardware.*
import android.os.SystemClock
import android.view.View
import androidx.core.graphics.PathParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Popup 1: readings are live; absence of GPS/sensor/camera is never replaced by demo values. */
class LiveHudView(context: Context, private val visuals: PremiumVisuals) : View(context), SensorEventListener {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sensors = context.getSystemService(SensorManager::class.java)
    private var sensorRegistered = false
    private var lastSensorNanos = 0L
    private var animator: ValueAnimator? = null
    private var displayedSpeed = 0f
    private var measuredSpeed: Float? = null
    private var gpsReady = false
    private var resolution = "VÍDEO --"
    private var vision: VisionFrame? = null
    private var recording = false
    fun update(speedKmh: Float?, gps: Boolean, videoLabel: String, rec: Boolean, frame: VisionFrame?) {
        gpsReady=gps; resolution=videoLabel; vision=frame; recording=rec
        if (measuredSpeed != speedKmh) {
            animator?.cancel()
            animator=ValueAnimator.ofFloat(displayedSpeed,speedKmh ?: 0f).apply {
                duration=300L
                addUpdateListener { displayedSpeed=it.animatedValue as Float;invalidate() }
                start()
            }
            measuredSpeed=speedKmh
        }
        contentDescription="Velocidade ${speedKmh?.toInt() ?: "indisponível"} km/h. ${if(gps)"GPS OK" else "GPS indisponível"}. $videoLabel. ${if(rec)"Gravando" else "Gravação parada"}."
        invalidate()
    }
    fun startSensors() {
        if (sensorRegistered) return
        val sensor=sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensorRegistered=sensors?.registerListener(this,sensor,SensorManager.SENSOR_DELAY_NORMAL)==true
    }
    fun stopSensors() { sensors?.unregisterListener(this);sensorRegistered=false;lastSensorNanos=0L;animator?.cancel();invalidate() }
    override fun onSensorChanged(event:SensorEvent){if(event.values.all{it.isFinite()}){lastSensorNanos=event.timestamp;invalidate()}}
    override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}
    override fun onDetachedFromWindow(){stopSensors();super.onDetachedFromWindow()}
    init { setLayerType(LAYER_TYPE_SOFTWARE,null) }
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val scale=minOf(width/514f,height/146f);if(scale<=0f)return
        canvas.save();canvas.translate((width-514f*scale)/2,(height-146f*scale)/2);canvas.scale(scale,scale)
        paint.style=Paint.Style.FILL;paint.shader=LinearGradient(0f,1f,0f,144f,intArrayOf(Color.argb(242,39,57,75),Color.argb(242,9,18,29),Color.argb(245,6,20,34)),floatArrayOf(0f,.38f,1f),Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(8f,8f,506f,138f),24f,24f,paint)
        paint.shader=null;paint.color=PremiumVisuals.RED;paint.style=Paint.Style.STROKE;paint.strokeWidth=2.4f;paint.setShadowLayer(7f,0f,0f,PremiumVisuals.RED)
        canvas.drawRoundRect(RectF(8f,8f,506f,138f),24f,24f,paint);paint.clearShadowLayer()
        val gauge=requireNotNull(PathParser.createPathFromPathData("M35 116 A60 60 0 0 1 124 34"))
        paint.color=Color.rgb(57,67,77);paint.strokeWidth=6f;canvas.drawPath(gauge,paint)
        val measure=PathMeasure(gauge,false);val progress=Path()
        measure.getSegment(0f,measure.length*(if(measuredSpeed!=null)displayedSpeed/100f else 0f).coerceIn(0f,1f),progress,true)
        paint.color=Color.rgb(0,202,250);canvas.drawPath(progress,paint)
        paint.style=Paint.Style.FILL;paint.color=PremiumVisuals.WHITE;paint.textAlign=Paint.Align.CENTER;paint.typeface=Typeface.create("sans-serif-condensed",Typeface.BOLD);paint.textSize=54f
        canvas.drawText(if(measuredSpeed!=null)"%.0f".format(Locale.US,displayedSpeed) else "--",82f,89f,paint)
        paint.typeface=Typeface.create("sans-serif-condensed",Typeface.NORMAL);paint.textSize=24f;canvas.drawText("km/h",82f,119f,paint)
        val frame=vision?.takeIf{SystemClock.elapsedRealtime()-it.timestampMs<=1200}
        fun laneOffset(left:Boolean,y:Float):Float {
            val lane=if(left)frame?.lanes?.left else frame?.lanes?.right
            return if(lane!=null && lane.confidence>=.65f && y in lane.yStart..lane.yEnd)(lane.x(y)-(if(left).3f else .7f)).coerceIn(-.2f,.2f)*60f else 0f
        }
        val left=153f+laneOffset(true,.93f);val right=270f+laneOffset(false,.93f)
        paint.color=Color.rgb(16,25,35)
        canvas.drawPath(Path().apply{moveTo(193f,54f);lineTo(left,126f);lineTo(right,126f);lineTo(235f,54f);close()},paint)
        paint.style=Paint.Style.STROKE;paint.strokeWidth=4f;paint.color=Color.rgb(129,146,163)
        canvas.drawLine(193f,54f,left,126f,paint);canvas.drawLine(235f,54f,right,126f,paint)
        paint.strokeWidth=7f;paint.color=Color.rgb(78,94,109)
        val phase=if(measuredSpeed!=null && displayedSpeed>1f)(SystemClock.uptimeMillis()%1000)/1000f*8f else 0f
        for(y in floatArrayOf(69f,88f,112f)){val t=(y+phase-54f)/72f;canvas.drawLine(193f+(left-193f)*t+4f,y+phase,235f+(right-235f)*t-4f,y+phase,paint)}
        if(frame?.vehicles?.isNotEmpty()==true)visuals.icon(canvas,"car",188f,83f,43f,Color.rgb(140,158,174))
        visuals.icon(canvas,"gps",252f,19f,29f,if(gpsReady)PremiumVisuals.WHITE else Color.rgb(82,98,115))
        visuals.icon(canvas,"sun",300f,22f,24f,Color.rgb(184,197,208))
        paint.style=Paint.Style.FILL;paint.color=PremiumVisuals.WHITE;paint.typeface=Typeface.create("sans-serif-condensed",Typeface.NORMAL);paint.textAlign=Paint.Align.RIGHT;paint.textSize=23f
        canvas.drawText(resolution,485f,44f,paint)
        canvas.drawText(if(gpsReady)"GPS OK" else "GPS --",485f,82f,paint)
        val age=SystemClock.elapsedRealtimeNanos()-lastSensorNanos
        canvas.drawText(if(sensorRegistered && lastSensorNanos>0 && age in 0L..2_000_000_000L)"G-SENSOR ATIVO" else "G-SENSOR --",485f,120f,paint)
        if(recording){paint.color=PremiumVisuals.RED;canvas.drawCircle(329f,34f,4f,paint)}
        canvas.restore()
        if(isShown && measuredSpeed!=null && displayedSpeed>1f)postInvalidateDelayed(100L)
    }
}

class DashboardHeaderView(context:Context,private val visuals:PremiumVisuals):View(context){
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    var usbConnected=false
    var wifiConnected=false
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas);if(width<=0 || height<=0)return
        canvas.save();canvas.scale(width/1679f,height/120f)
        paint.color=PremiumVisuals.WHITE;paint.typeface=Typeface.create("sans-serif",Typeface.NORMAL);paint.textSize=40f
        val title="DashCam ADAS ";canvas.drawText(title,119f,70f,paint);val end=119f+paint.measureText(title);paint.color=PremiumVisuals.CYAN;canvas.drawText("PRO",end,70f,paint)
        visuals.icon(canvas,"device",1304f,37f,40f,if(usbConnected)PremiumVisuals.WHITE else Color.rgb(96,116,137))
        // Capture is currently silent: microphone status must not imply audio recording.
        visuals.icon(canvas,"mic",1367f,39f,38f,Color.rgb(96,116,137))
        visuals.icon(canvas,"wifi",1437f,37f,40f,if(wifiConnected)PremiumVisuals.WHITE else Color.rgb(96,116,137))
        paint.color=PremiumVisuals.WHITE;paint.textSize=34f;canvas.drawText(SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date()),1556f,70f,paint)
        canvas.restore()
    }
}
