package br.com.dashcamadas.vision

import br.com.dashcamadas.AdasEngine
import org.junit.Assert.*
import org.junit.Test

class PedestrianRiskTest {
    private val calibration=Calibration(true,.5f,1.8f,3.5f)
    private val lanes=Lanes(LaneCurve(0f,-.15f,.33f,.9f),LaneCurve(0f,.15f,.67f,.9f))
    private val person=Pedestrian(Box(.45f,.45f,.57f,.84f),.9f)
    @Test fun pedestrianNeedsConfirmationAndIsRemovedWhenLost(){
        val engine=RiskEngine()
        for(t in listOf(0L,300L,600L))assertFalse(Risk.PEDESTRIAN in engine.update(t,lanes,emptyList(),12f,calibration,listOf(person)).risks)
        val confirmed=engine.update(900L,lanes,emptyList(),12f,calibration,listOf(person))
        assertTrue(Risk.PEDESTRIAN in confirmed.risks);assertEquals(person,confirmed.pedestrianTarget)
        assertFalse(Risk.PEDESTRIAN in engine.update(1200L,lanes,emptyList(),12f,calibration).risks)
    }
    @Test fun pedestrianCannotWarnWithoutGpsOrCalibration(){
        val noGps=RiskEngine()
        for(i in 0..10){
            assertFalse(Risk.PEDESTRIAN in noGps.update(i*300L,lanes,emptyList(),null,calibration,listOf(person)).risks)
        }
        val engine=RiskEngine()
        for(i in 0..10)assertFalse(Risk.PEDESTRIAN in engine.update(i*300L,lanes,emptyList(),12f,Calibration(),listOf(person)).risks)
    }
    @Test fun anotherPersonDoesNotInheritConfirmation(){
        val engine=RiskEngine()
        for(i in 0..2)engine.update(i*300L,lanes,emptyList(),12f,calibration,listOf(person))
        val other=Pedestrian(Box(.65f,.45f,.75f,.84f),.9f)
        for(i in 3..5)assertFalse(Risk.PEDESTRIAN in engine.update(i*300L,lanes,emptyList(),12f,calibration,listOf(other)).risks)
    }
    @Test fun personOutsideRoadMaskCannotWarn(){
        val engine=AdasEngine();val mask=NeuralMasks(320,180,IntArray(320*180),BooleanArray(320*180),0,0)
        for(i in 0..10)assertFalse(Risk.PEDESTRIAN in engine.evaluateNeural(mask,lanes,emptyList(),i*300L,12f,calibration,listOf(person)).risk.risks)
    }
    @Test fun higherSensitivityChangesProximityMarginWithoutRemovingConfidenceGate(){
        val near=Lanes(LaneCurve(0f,0f,.36f,.9f),LaneCurve(0f,0f,.86f,.9f))
        val normal=RiskEngine();val sensitive=RiskEngine(RiskTuning(departureSensitivity=100))
        for(i in 0..3){normal.update(i*300L,near,emptyList(),15f,calibration);sensitive.update(i*300L,near,emptyList(),15f,calibration)}
        assertFalse(Risk.LEFT_DEPARTURE in normal.update(1200L,near,emptyList(),15f,calibration).risks)
        assertTrue(Risk.LEFT_DEPARTURE in sensitive.update(1200L,near,emptyList(),15f,calibration).risks)
        assertFalse(Risk.LEFT_DEPARTURE in sensitive.update(1500L,near.copy(left=near.left!!.copy(confidence=.6f)),emptyList(),15f,calibration).risks)
    }
}
