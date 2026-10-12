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
    fun processNeural(mask:NeuralMasks,vehicles:List<Vehicle>,nowMs:Long,speedMs:Float?,calibration:Calibration):Analysis {
        val lanes=observeNeural(mask,if(calibration.valid)calibration.egoX else .5f)
        return evaluateNeural(mask,lanes,vehicles,nowMs,speedMs,calibration)
    }
    fun observeNeural(mask:NeuralMasks,egoX:Float=.5f):Lanes {
        val observed=laneDetector.detect(mask.lanes,mask.width,mask.height,null,true,egoX)
        return Lanes(observed.left?.takeIf{mask.besideLane(it,true)},observed.right?.takeIf{mask.besideLane(it,false)})
    }
    @JvmOverloads fun evaluateNeural(mask:NeuralMasks,lanes:Lanes,vehicles:List<Vehicle>,nowMs:Long,speedMs:Float?,calibration:Calibration,pedestrians:List<Pedestrian> = emptyList()):Analysis {
        val onRoad=vehicles.filter{mask.roadUnder(it.box)}
        val onRoadPeople=pedestrians.filter{mask.roadUnder(it.box)}
        return Analysis(lanes,riskEngine.update(nowMs,lanes,onRoad,speedMs,calibration,onRoadPeople))
    }
}
