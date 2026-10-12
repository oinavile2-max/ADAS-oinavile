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
        val detector=CurveLaneDetector()
        detector.detect(scene(),320,180)
        val result=detector.detect(scene(),320,180)
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
    @Test fun firstObservationCannotArmLaneAlarm(){val result=CurveLaneDetector().detect(scene(),320,180);assertTrue(result.left!!.confidence<.65f);assertTrue(result.right!!.confidence<.65f)}
    @Test fun bothBoundariesCanBendToTheRightWithMeasuredErrorBound(){
        val frame=IntArray(320*180){40}
        for(y in 96 until 176){val t=(y/180f-.5f)/.48f
            for(center in listOf(.24f*t*t-.39f*t+.44f,.24f*t*t+.12f*t+.55f)){
                val x=(center*320).toInt();for(dx in -2..2)frame[y*320+x+dx]=120
            }
        }
        val detector=CurveLaneDetector();detector.detect(frame,320,180);val result=detector.detect(frame,320,180)
        assertNotNull(result.left);assertNotNull(result.right)
        for(y in listOf(.6f,.75f,.9f)){val t=(y-.5f)/.48f;assertEquals(.24f*t*t-.39f*t+.44f,result.left!!.x(y),.018f);assertEquals(.24f*t*t+.12f*t+.55f,result.right!!.x(y),.018f)}
    }
    @Test fun widePaintAndDashedPaintStayOnObservedPixels(){
        val frame=scene()
        for(y in 95 until 176){val t=(y/180f-.5f)/.48f;val x=((.06f*t*t+.28f*t+.54f)*320).toInt()
            for(dx in -5..5)frame[y*320+x+dx]=if(y%18<12)100 else 40
        }
        val detector=CurveLaneDetector();detector.detect(frame,320,180);val result=detector.detect(frame,320,180)
        assertNotNull(result.right)
        for(y in listOf(.65f,.8f,.93f)){val t=(y-.5f)/.48f;assertEquals(.06f*t*t+.28f*t+.54f,result.right!!.x(y),.02f)}
    }
    @Test fun brightIsolatedPointsDoNotDeflectDimStripe(){
        val frame=scene();val random=java.util.Random(4)
        repeat(200){frame[95*320+random.nextInt(80*320)]=240}
        val detector=CurveLaneDetector();detector.detect(frame,320,180);val result=detector.detect(frame,320,180)
        assertNotNull(result.left);assertNotNull(result.right)
        assertEquals(.2f,result.left!!.x(.9f),.04f)
    }
    @Test fun confidenceRequiresReconfirmationAfterDisappearance(){
        val detector=CurveLaneDetector();repeat(3){detector.detect(scene(),320,180)}
        detector.detect(IntArray(320*180){40},320,180)
        assertTrue(detector.detect(scene(),320,180).left!!.confidence<.65f)
    }
    @Test fun supportedRangeDoesNotExtendAboveMeasuredStripe(){
        val frame=scene();for(y in 0 until 115)java.util.Arrays.fill(frame,y*320,(y+1)*320,40)
        val result=CurveLaneDetector().detect(frame,320,180)
        assertNotNull(result.left);assertTrue(result.left!!.yStart>=115f/180);assertTrue(result.left.yEnd<.98f)
    }
    @Test fun randomTextureCannotBecomeConfirmedHighConfidenceLane(){
        val random=java.util.Random(7);val frame=IntArray(320*180){random.nextInt(255)};val detector=CurveLaneDetector()
        repeat(3){val result=detector.detect(frame,320,180);assertTrue((result.left?.confidence ?: 0f)<.65f);assertTrue((result.right?.confidence ?: 0f)<.65f)}
    }
}
