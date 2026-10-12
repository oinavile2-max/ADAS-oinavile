package br.com.dashcamadas.vision

import kotlin.math.*

data class Box(val left: Float,val top: Float,val right: Float,val bottom: Float) {
    val width get()=right-left
    val height get()=bottom-top
    val area get()=width*height
    val centerX get()=(left+right)/2
    fun iou(other:Box):Float {
        val intersection=max(0f,min(right,other.right)-max(left,other.left))*max(0f,min(bottom,other.bottom)-max(top,other.top))
        return intersection/(area+other.area-intersection).coerceAtLeast(.000001f)
    }
}
data class Vehicle(val box:Box,val confidence:Float,val category:Int)
data class Pedestrian(val box:Box,val confidence:Float)
data class SceneDetections(val vehicles:List<Vehicle>,val pedestrians:List<Pedestrian>)
/** x(t)=a*t²+b*t+c, t=(y-.5)/.48; only supported pixel observations are returned. */
data class LaneCurve(val a:Float,val b:Float,val c:Float,val confidence:Float,val yStart:Float=.56f,val yEnd:Float=.94f) {
    fun x(y:Float):Float { val t=(y-.5f)/.48f;return a*t*t+b*t+c }
}
data class Lanes(val left:LaneCurve?,val right:LaneCurve?)
data class Calibration(val enabled:Boolean=false,val egoX:Float=.5f,val vehicleWidthM:Float=1.8f,val laneWidthM:Float=3.5f) {
    val valid get()=enabled && egoX in .15f.. .85f && vehicleWidthM in .8f..3f && laneWidthM in 2.5f..5f && vehicleWidthM<laneWidthM
}
enum class Risk { LEFT_DEPARTURE,RIGHT_DEPARTURE,COLLISION,PEDESTRIAN }
enum class Phase { UNAVAILABLE,IDLE,CANDIDATE,RISK,RECOVERY }
data class RiskResult(val risks:Set<Risk>,val phases:Map<Risk,Phase>,val target:Vehicle?,val ttcSeconds:Float?,val newAlerts:Set<Risk>,val pedestrianTarget:Pedestrian?=null)

/** Development defaults, exposed in settings; not approved definitive product thresholds. */
data class RiskTuning(val departureMinSpeedMs:Float=8.33f,val collisionTtcSeconds:Float=2.5f,val confirmationMs:Long=900,val cooldownMs:Long=10000,val departureSensitivity:Int=50) {
    init {require(departureMinSpeedMs in 2.7f..28f && collisionTtcSeconds in 1f..5f && confirmationMs in 300..2000 && cooldownMs in 5000..60000 && departureSensitivity in 0..100)}
}
