package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class CameraLightingPolicyTest {
    @Test fun shortShadowsDoNotActivateNightAndHeadlightsDoNotCancelIt(){
        val policy=CameraLightingPolicy()
        assertFalse(policy.observe(30,0));assertFalse(policy.observe(30,1000))
        assertFalse(policy.observe(90,2000));assertFalse(policy.observe(30,3000))
        assertFalse(policy.observe(30,4000));assertFalse(policy.observe(30,5000));assertTrue(policy.observe(30,6000))
        assertTrue(policy.observe(120,7000));assertTrue(policy.observe(30,8000))
        for(t in 9000L..16000L step 1000)assertTrue(policy.observe(120,t))
        assertFalse(policy.observe(120,17000))
    }
    @Test fun missingAndBackwardsFramesCannotCompleteAConfirmation(){
        val policy=CameraLightingPolicy()
        policy.observe(20,0);policy.observe(20,1000)
        assertFalse(policy.observe(20,9000));assertFalse(policy.observe(20,10000))
        assertFalse(policy.observe(20,500));assertFalse(policy.observe(20,1500))
        policy.observe(20,2500);assertTrue(policy.observe(20,3500))
        policy.reset();assertFalse(policy.dark)
    }
    @Test fun manualProfilesOverrideOnlyTheDecision(){
        val policy=CameraLightingPolicy()
        assertTrue(policy.night(CameraLightMode.NIGHT));assertFalse(policy.night(CameraLightMode.DAY))
        for(t in 0L..3000L step 1000)policy.observe(10,t)
        assertTrue(policy.night(CameraLightMode.AUTO));assertFalse(policy.night(CameraLightMode.DAY))
        assertEquals(CameraLightMode.AUTO,CameraLightMode.read("invalid"))
    }
    @Test fun exposureIsBoundedByDeviceRangeAndReturnsToZeroByDay(){
        assertEquals(2,CameraLightingPolicy.exposureSteps(true,.7f,1f/3,-3..3))
        assertEquals(1,CameraLightingPolicy.exposureSteps(true,2f,.5f,-1..1))
        assertEquals(0,CameraLightingPolicy.exposureSteps(false,2f,.5f,-4..4))
        assertEquals(0,CameraLightingPolicy.exposureSteps(true,Float.NaN,.5f,-4..4))
        assertNull(CameraLightingPolicy.exposureSteps(true,1f,0f,-4..4))
        assertNull(CameraLightingPolicy.exposureSteps(true,1f,.5f,1..0))
    }
    @Test fun fpsAlwaysComesFromSupportedDeviceRanges(){
        val fixed=CameraLightingPolicy.Fps(30,30);val variable=CameraLightingPolicy.Fps(15,30)
        assertEquals(variable,CameraLightingPolicy.fps(true,listOf(fixed,variable)))
        assertEquals(fixed,CameraLightingPolicy.fps(false,listOf(fixed,variable)))
        assertNull(CameraLightingPolicy.fps(true,listOf(CameraLightingPolicy.Fps(30,60))))
        assertNull(CameraLightingPolicy.fps(true,listOf(CameraLightingPolicy.Fps(20,10))))
    }
    @Test fun yuvLightSamplingHonorsCropPositionAndPaddedStridesWithoutMutatingBuffer(){
        val b=ByteBuffer.allocate(100);b.position(3)
        for(y in 1..3)for(x in 2..5)b.put(3+y*20+x*2,126.toByte())
        assertEquals(128,CameraLightingPolicy.mean(b,20,2,2,1,4,3,false))
        assertEquals(3,b.position());assertEquals(100,b.limit())
    }
    @Test fun rgbaSamplingUsesRgbInsteadOfAlphaOrPadding(){
        val b=ByteBuffer.allocate(90);b.position(2)
        for(y in 1..2)for(x in 1..3){val i=2+y*24+x*5;b.put(i,100);b.put(i+1,50);b.put(i+2,0);b.put(i+3,255.toByte())}
        assertEquals(59,CameraLightingPolicy.mean(b,24,5,1,1,3,2,true))
        assertEquals(2,b.position())
        assertThrows(IllegalArgumentException::class.java){CameraLightingPolicy.mean(b,24,5,0,0,30,20,true)}
    }
}
