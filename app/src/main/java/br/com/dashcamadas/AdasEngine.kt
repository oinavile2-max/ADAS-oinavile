package br.com.dashcamadas

import br.com.dashcamadas.vision.*

/** Shared real-frame processing core: curved lane observations, vehicle tracking and risk state. */
class AdasEngine {
    data class Analysis(val lanes:Lanes,val risk:RiskResult)
    private val laneDetector=CurveLaneDetector()
    private var riskEngine=RiskEngine()
    private var tuning=RiskTuning()
    fun configure(value:RiskTuning) {if(value!=tuning){tuning=value;riskEngine=RiskEngine(value);laneDetector.reset()}}
    fun process(gray:IntArray,width:Int,height:Int,vehicles:List<Vehicle>,nowMs:Long,speedMs:Float?,calibration:Calibration):Analysis {
        val lanes=laneDetector.detect(gray,width,height)
        return Analysis(lanes,riskEngine.update(nowMs,lanes,vehicles,speedMs,calibration))
    }
    fun reset(){laneDetector.reset();riskEngine.reset()}
}
