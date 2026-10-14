package br.com.dashcamadas

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.View
import br.com.dashcamadas.vision.*

data class VisionFrame(val bitmap:Bitmap,val lanes:Lanes,val vehicles:List<Vehicle>,val risk:RiskResult,val timestampMs:Long,val laneNetworkActive:Boolean=false,val vehicleNetworkActive:Boolean=false,val inferenceMs:Long=0,val lanePixels:Int=0,val roadPixels:Int=0,val pedestrians:List<Pedestrian> = emptyList())

/** Reference appearance; every lane, actor and subject belongs to the actual analyzed frame. */
class RiskOverlayView(context:Context):View(context) {
    var showLanes=true
    var showCaption=true
    var preview:CameraPreviewView?=null
    var result:VisionFrame?=null
        set(value){field=value;invalidate()}
    private val tuning=context.getSharedPreferences("adas_tuning",Context.MODE_PRIVATE)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND }
    private var lineWidth=4f
    private fun glow(canvas:Canvas,path:Path,color:Int,pulse:Int=255,width:Float=lineWidth){
        paint.style=Paint.Style.STROKE
        for((factor,alpha) in listOf(3.5f to 26,2f to 68,1f to pulse)){
            paint.strokeWidth=width*factor;paint.color=Color.argb(alpha,Color.red(color),Color.green(color),Color.blue(color));canvas.drawPath(path,paint)
        }
        if(color==PremiumVisuals.RED){paint.strokeWidth=(width*.25f).coerceAtLeast(.7f);paint.color=Color.argb(pulse,255,207,215);canvas.drawPath(path,paint)}
    }
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val frame=result ?: return
        if(SystemClock.elapsedRealtime()-frame.timestampMs>1200)return
        if(preview?.timestampMs!=frame.timestampMs)return
        val transform=preview?.mapping() ?: return
        val period=tuning.getLong("blink_ms",500).coerceIn(250,1000)
        val pulse=if(SystemClock.uptimeMillis()%period<period/2)255 else 160
        lineWidth=tuning.getFloat("lane_width_dp",4f).takeIf{it.isFinite()}?.coerceIn(1f,12f)?.times(resources.displayMetrics.density) ?: 4f
        val cyan=Color.rgb(0,218,255)
        fun point(x:Float,y:Float)=transform.project(x*frame.bitmap.width,y*frame.bitmap.height)
        fun add(path:Path,x:Float,y:Float,first:Boolean=false){val p=point(x,y);if(first)path.moveTo(p.first,p.second) else path.lineTo(p.first,p.second)}
        val left=frame.lanes.left?.takeIf{it.confidence>=.65f};val right=frame.lanes.right?.takeIf{it.confidence>=.65f}
        if(showLanes && left!=null && right!=null){
            val start=maxOf(left.yStart,right.yStart);val end=minOf(left.yEnd,right.yEnd)
            if(end>start){
                val corridor=Path()
                for(i in 0..24){val y=start+(end-start)*i/24;add(corridor,left.x(y),y,i==0)}
                for(i in 24 downTo 0){val y=start+(end-start)*i/24;add(corridor,right.x(y),y)}
                corridor.close();paint.style=Paint.Style.FILL;paint.color=Color.argb(22,0,218,255);canvas.drawPath(corridor,paint)
                // Perspective arrow never extends outside the jointly observed lane region.
                val top=start+(end-start)*.57f;val bottom=start+(end-start)*.92f;val shoulder=top+(bottom-top)*.5f
                fun centre(y:Float)=(left.x(y)+right.x(y))/2f
                fun laneWidth(y:Float)=right.x(y)-left.x(y)
                val arrow=Path()
                add(arrow,centre(top),top,true)
                add(arrow,centre(shoulder)+laneWidth(shoulder)*.14f,shoulder)
                add(arrow,centre(shoulder)+laneWidth(shoulder)*.055f,shoulder)
                add(arrow,centre(bottom)+laneWidth(bottom)*.055f,bottom)
                add(arrow,centre(bottom)-laneWidth(bottom)*.055f,bottom)
                add(arrow,centre(shoulder)-laneWidth(shoulder)*.055f,shoulder)
                add(arrow,centre(shoulder)-laneWidth(shoulder)*.14f,shoulder)
                arrow.close();paint.color=Color.argb(95,0,218,255);canvas.drawPath(arrow,paint);glow(canvas,arrow,cyan)
            }
        }
        for((lane,risk) in listOf(left to Risk.LEFT_DEPARTURE,right to Risk.RIGHT_DEPARTURE)){
            if(lane==null || (!showLanes && risk !in frame.risk.risks))continue
            val path=Path()
            for(i in 0..24){val y=lane.yStart+(lane.yEnd-lane.yStart)*i/24;add(path,lane.x(y),y,i==0)}
            val dangerous=risk in frame.risk.risks
            glow(canvas,path,if(dangerous)PremiumVisuals.RED else cyan,if(dangerous)pulse else 255)
        }
        fun actor(box:Box,dangerous:Boolean){
            val a=point(box.left,box.top);val b=point(box.right,box.bottom)
            val rect=RectF(minOf(a.first,b.first),minOf(a.second,b.second),maxOf(a.first,b.first),maxOf(a.second,b.second))
            if(dangerous){paint.style=Paint.Style.FILL;paint.color=Color.argb(20,255,35,62);canvas.drawRoundRect(rect,5f,5f,paint)}
            val path=Path().apply { addRoundRect(rect,5f,5f,Path.Direction.CW) }
            glow(canvas,path,if(dangerous)PremiumVisuals.RED else cyan,if(dangerous)pulse else 180,width=3f*resources.displayMetrics.density)
        }
        for(vehicle in frame.vehicles){
            val dangerous=Risk.COLLISION in frame.risk.risks && frame.risk.target==vehicle
            actor(vehicle.box,dangerous)
            if(vehicle==frame.risk.target){
                val box=vehicle.box;val marker=Path()
                add(marker,box.left+box.width*.05f,box.bottom-box.height*.02f,true)
                add(marker,box.right-box.width*.05f,box.bottom-box.height*.02f)
                add(marker,box.right,box.bottom);add(marker,box.left,box.bottom);marker.close()
                val color=if(dangerous)PremiumVisuals.RED else cyan
                paint.style=Paint.Style.FILL;paint.color=Color.argb(110,Color.red(color),Color.green(color),Color.blue(color));canvas.drawPath(marker,paint);glow(canvas,marker,color,if(dangerous)pulse else 255)
            }
        }
        for(person in frame.pedestrians)actor(person.box,Risk.PEDESTRIAN in frame.risk.risks && frame.risk.pedestrianTarget==person)
        if(showCaption){
            paint.style=Paint.Style.FILL;paint.typeface=Typeface.create("sans-serif",Typeface.NORMAL);paint.textSize=12*resources.displayMetrics.scaledDensity;paint.color=Color.WHITE
            val label=when { Risk.PEDESTRIAN in frame.risk.risks->"PEDESTRE À FRENTE";Risk.COLLISION in frame.risk.risks->"RISCO DE COLISÃO";frame.inferenceMs>500->"ALERTAS SUSPENSOS";else->"ASSISTENTE DE FAIXA" }
            canvas.drawText(label,8*resources.displayMetrics.density,height-12*resources.displayMetrics.density,paint)
        }
        if(frame.risk.risks.isNotEmpty())postInvalidateDelayed(minOf(100,period/4))
    }
}
