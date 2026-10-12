package br.com.dashcamadas.vision

import org.junit.Assert.*
import org.junit.Test

class CurveLaneDetectorTest {
    private fun scene(left:Boolean=true,right:Boolean=true):IntArray {
        val w=320;val h=180;val frame=IntArray(w*h){40}
        for(y in 95 until 176) {
            val t=(y.toFloat()/h-.5f)/.48f
            if(left) {val x=((-.06f*t*t-.28f*t+.46f)*w).toInt();for(dx in -1..1)frame[y*w+x+dx]=85}
            if(right) {val x=((.06f*t*t+.28f*t+.54f)*w).toInt();for(dx in -1..1)frame[y*w+x+dx]=85}
        }
        return frame
    }
    @Test fun dimCurvedStripesAreDetectedWithoutAbsoluteBrightnessThreshold() {
        val result=CurveLaneDetector().detect(scene(),320,180)
        assertNotNull(result.left);assertNotNull(result.right)
        assertTrue(result.left!!.a<-.02f);assertTrue(result.right!!.a>.02f)
        assertTrue(result.left.confidence>.65f);assertTrue(result.right.confidence>.65f)
    }
    @Test fun missingSideIsNeverSynthesized() {
        val result=CurveLaneDetector().detect(scene(right=false),320,180)
        assertNotNull(result.left);assertNull(result.right)
    }
    @Test fun blankFrameDoesNotInventLanes() {val d=CurveLaneDetector();assertEquals(Lanes(null,null),d.detect(IntArray(320*180){40},320,180))}
    @Test fun observationsAreClearedOnMissingFrame() {val d=CurveLaneDetector();d.detect(scene(),320,180);assertEquals(Lanes(null,null),d.detect(IntArray(320*180){40},320,180))}
    @Test fun verticalWindshieldStreakIsRejected() {
        val data=IntArray(320*180){40};for(y in 95 until 178)for(x in 190..192)data[y*320+x]=240
        assertEquals(Lanes(null,null),CurveLaneDetector().detect(data,320,180))
    }
}
