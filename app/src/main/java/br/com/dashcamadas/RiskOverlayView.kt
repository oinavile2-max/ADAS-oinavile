package br.com.dashcamadas

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.View
import br.com.dashcamadas.vision.*

data class VisionFrame(val bitmap:Bitmap,val lanes:Lanes,val vehicles:List<Vehicle>,val risk:RiskResult,val timestampMs:Long)
class RiskOverlayView(context:Context):View(context) {
    var showLanes=true
    var preview:CameraPreviewView?=null
    var result:VisionFrame?=null
        set(value){field=value;invalidate()}
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeWidth=4f;strokeCap=Paint.Cap.ROUND}
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val frame=result ?: return
        if(SystemClock.elapsedRealtime()-frame.timestampMs>1500) return
        val transform=preview?.mapping() ?: return
        val period=context.getSharedPreferences("adas_tuning",Context.MODE_PRIVATE).getLong("blink_ms",500).coerceIn(250,1000)
        val flashing=SystemClock.uptimeMillis()%period<period/2
        fun point(x:Float,y:Float)=transform.project(x*frame.bitmap.width,y*frame.bitmap.height)
        for((lane,risk) in listOf(frame.lanes.left to Risk.LEFT_DEPARTURE,frame.lanes.right to Risk.RIGHT_DEPARTURE)) {
            if(!showLanes && risk !in frame.risk.risks) continue
            if(lane==null || lane.confidence<.45f) continue
            val dangerous=risk in frame.risk.risks
            if(dangerous && !flashing) continue
            paint.color=if(dangerous) Color.RED else Color.rgb(51,236,174)
            val path=Path()
            for(i in 0..20) {
                val y=.56f+i*.019f;val p=point(lane.x(y),y)
                if(i==0) path.moveTo(p.first,p.second) else path.lineTo(p.first,p.second)
            }
            canvas.drawPath(path,paint)
        }
        for(vehicle in frame.vehicles) {
            val dangerous=Risk.COLLISION in frame.risk.risks && frame.risk.target==vehicle
            if(dangerous && !flashing) continue
            paint.color=if(dangerous) Color.RED else Color.rgb(60,195,255)
            val a=point(vehicle.box.left,vehicle.box.top);val b=point(vehicle.box.right,vehicle.box.bottom)
            canvas.drawRect(minOf(a.first,b.first),minOf(a.second,b.second),maxOf(a.first,b.first),maxOf(a.second,b.second),paint)
        }
        paint.style=Paint.Style.FILL;paint.textSize=12*resources.displayMetrics.scaledDensity
        paint.color=Color.WHITE
        val ldw=frame.risk.phases[Risk.LEFT_DEPARTURE]
        val fcw=frame.risk.phases[Risk.COLLISION]
        fun label(value:Phase?)=when(value){Phase.UNAVAILABLE->"indisponível";Phase.CANDIDATE->"confirmando";Phase.RISK->"RISCO";Phase.RECOVERY->"recuperação";else->"monitorando"}
        canvas.drawText("LDW ${label(ldw)}  |  FCW ${label(fcw)}",12*resources.displayMetrics.density,height-12*resources.displayMetrics.density,paint)
        paint.style=Paint.Style.STROKE
        if(frame.risk.risks.isNotEmpty()) postInvalidateDelayed(minOf(100,period/4))
    }
}
