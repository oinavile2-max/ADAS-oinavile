package br.com.dashcamadas.vision

import org.junit.Assert.*
import org.junit.Test

class SceneOutputDecoderTest {
    @Test fun personIsSeparatedFromVehiclesAndUnsupportedCategories(){
        val boxes=Array(4){floatArrayOf(.3f,.4f,.8f,.6f)}
        val decoded=SceneOutputDecoder.decode(boxes,floatArrayOf(0f,2f,1f,9f),FloatArray(4){.9f},4f)
        assertEquals(1,decoded.pedestrians.size);assertEquals(1,decoded.vehicles.size)
        assertEquals(2,decoded.vehicles.single().category)
    }
    @Test fun malformedScoresClassesOrCoordinatesCannotBecomeDetections(){
        val decoded=SceneOutputDecoder.decode(arrayOf(floatArrayOf(.3f,.4f,.8f,.6f),floatArrayOf(Float.NaN,.4f,.8f,.6f)),floatArrayOf(.2f,0f),floatArrayOf(.9f,.9f),2f)
        assertTrue(decoded.vehicles.isEmpty());assertTrue(decoded.pedestrians.isEmpty())
        assertTrue(SceneOutputDecoder.decode(emptyArray(),floatArrayOf(),floatArrayOf(),Float.NaN).pedestrians.isEmpty())
    }
}
