package br.com.dashcamadas.vision

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.exp

class RiskEngineTest {
    private val calibration=Calibration(true,.5f,1.8f,3.5f)
    private val normal=Lanes(LaneCurve(0f,-.15f,.33f,.9f),LaneCurve(0f,.15f,.67f,.9f))
    private val leftDeparture=Lanes(LaneCurve(0f,0f,.42f,.9f),LaneCurve(0f,0f,.82f,.9f))
    @Test fun departureNeedsTemporalConfirmationAndStopsOnRecovery() {
        val engine=RiskEngine()
        for(t in listOf(0L,300L,600L)) assertTrue(engine.update(t,leftDeparture,emptyList(),15f,calibration).risks.isEmpty())
        val danger=engine.update(900,leftDeparture,emptyList(),15f,calibration)
        assertTrue(Risk.LEFT_DEPARTURE in danger.risks);assertEquals(setOf(Risk.LEFT_DEPARTURE),danger.newAlerts)
        val safe=engine.update(1200,normal,emptyList(),15f,calibration)
        assertTrue(safe.risks.isEmpty());assertEquals(Phase.RECOVERY,safe.phases[Risk.LEFT_DEPARTURE])
    }
    @Test fun confidenceLossImmediatelyCancelsCriticalOutput() {
        val e=RiskEngine();for(t in listOf(0L,300L,600L,900L))e.update(t,leftDeparture,emptyList(),15f,calibration)
        val lost=e.update(1200,Lanes(null,null),emptyList(),15f,calibration)
        assertTrue(lost.risks.isEmpty());assertEquals(Phase.UNAVAILABLE,lost.phases[Risk.LEFT_DEPARTURE])
    }
    @Test fun missingGpsAndUncalibratedCameraCannotAlarm() {
        val e=RiskEngine()
        for(i in 0..10) assertTrue(e.update(i*300L,leftDeparture,emptyList(),null,calibration).risks.isEmpty())
        for(i in 11..20) assertTrue(e.update(i*300L,leftDeparture,emptyList(),15f,Calibration()).risks.isEmpty())
    }
    @Test fun frameGapDoesNotCountAsRiskConfirmation() {
        val e=RiskEngine();e.update(0,leftDeparture,emptyList(),15f,calibration)
        val result=e.update(1500,leftDeparture,emptyList(),15f,calibration)
        assertTrue(result.risks.isEmpty());assertEquals(Phase.CANDIDATE,result.phases[Risk.LEFT_DEPARTURE])
    }
    @Test fun alertCooldownSurvivesConfidenceReset() {
        val e=RiskEngine();var alerts=0
        for(i in 0..3)alerts+=e.update(i*300L,leftDeparture,emptyList(),15f,calibration).newAlerts.size
        e.update(1200,Lanes(null,null),emptyList(),15f,calibration)
        for(i in 5..8)alerts+=e.update(i*300L,leftDeparture,emptyList(),15f,calibration).newAlerts.size
        assertEquals(1,alerts)
    }
    private fun vehicle(size:Float,center:Float=.5f)=Vehicle(Box(center-size/2,.6f-size/2,center+size/2,.6f+size/2),.95f,2)
    @Test fun confirmedExpansionTriggersFcwAndShrinkingEndsIt() {
        val e=RiskEngine();var triggered=false
        for(i in 0..9) {
            val size=(.12*exp(i*.3/2.0)).toFloat()
            val r=e.update(i*300L,normal,listOf(vehicle(size)),12f,calibration)
            triggered=triggered || Risk.COLLISION in r.risks
        }
        assertTrue(triggered)
        val recovery=e.update(3000,normal,listOf(vehicle(.38f)),12f,calibration)
        assertFalse(Risk.COLLISION in recovery.risks)
    }
    @Test fun closeButStableVehicleDoesNotTriggerFcw() {
        val e=RiskEngine()
        for(i in 0..20)assertFalse(Risk.COLLISION in e.update(i*300L,normal,listOf(vehicle(.45f)),12f,calibration).risks)
    }
    @Test fun lostTargetImmediatelySuspendsFcw() {
        val e=RiskEngine()
        for(i in 0..9)e.update(i*300L,normal,listOf(vehicle((.12*exp(i*.3/2)).toFloat())),12f,calibration)
        val lost=e.update(3000,normal,emptyList(),12f,calibration)
        assertFalse(Risk.COLLISION in lost.risks);assertEquals(Phase.UNAVAILABLE,lost.phases[Risk.COLLISION])
    }
    @Test fun stationaryGpsCannotTriggerCollision() {
        val e=RiskEngine()
        for(i in 0..9)assertFalse(Risk.COLLISION in e.update(i*300L,normal,listOf(vehicle((.12*exp(i*.3/2)).toFloat())),0f,calibration).risks)
    }
    @Test fun objectSwitchDoesNotInheritPreviousApproach() {
        val e=RiskEngine()
        for(i in 0..5)e.update(i*300L,normal,listOf(vehicle((.12*exp(i*.3/2)).toFloat())),12f,calibration)
        val switched=e.update(1800,normal,listOf(Vehicle(Box(.3f,.65f,.7f,.95f),.95f,7)),12f,calibration)
        assertNull(switched.ttcSeconds);assertFalse(Risk.COLLISION in switched.risks)
    }
    @Test fun configuredDepartureSpeedIsRespected() {
        val e=RiskEngine(RiskTuning(departureMinSpeedMs=20f))
        for(i in 0..10)assertTrue(e.update(i*300L,leftDeparture,emptyList(),15f,calibration).risks.isEmpty())
    }
    @Test fun configuredConfirmationCannotBeBypassed() {
        val e=RiskEngine(RiskTuning(confirmationMs=1500))
        for(i in 0..4)assertTrue(e.update(i*300L,leftDeparture,emptyList(),15f,calibration).risks.isEmpty())
        assertTrue(Risk.LEFT_DEPARTURE in e.update(1500,leftDeparture,emptyList(),15f,calibration).risks)
    }
    @Test fun invalidPhysicalMeasuresDisableCalibration() {assertFalse(Calibration(true,.5f,3f,2.5f).valid)}
    @Test fun upperStripeCannotArmDepartureByExtrapolatingBelowObservedPixels(){
        val e=RiskEngine();val upper=Lanes(leftDeparture.left!!.copy(yEnd=.8f),leftDeparture.right!!.copy(yEnd=.8f))
        for(i in 0..10){val r=e.update(i*300L,upper,emptyList(),15f,calibration);assertTrue(r.risks.isEmpty());assertEquals(Phase.UNAVAILABLE,r.phases[Risk.LEFT_DEPARTURE])}
    }
}
