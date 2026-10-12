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
    fun processRgb(pixels:IntArray,width:Int,height:Int,vehicles:List<Vehicle>,nowMs:Long,speedMs:Float?,calibration:Calibration):Analysis {
        val gray=IntArray(pixels.size){i->val c=pixels[i];((c shr 16 and 255)*30+(c shr 8 and 255)*59+(c and 255)*11)/100}
        val lanes=laneDetector.detect(gray,width,height,pixels)
        return Analysis(lanes,riskEngine.update(nowMs,lanes,vehicles,speedMs,calibration))
    }
    fun reset(){laneDetector.reset();riskEngine.reset()}
}
