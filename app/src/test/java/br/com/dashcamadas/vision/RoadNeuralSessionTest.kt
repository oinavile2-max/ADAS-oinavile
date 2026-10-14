package br.com.dashcamadas.vision

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Executes the real pretrained network on CPU, not a mock interpreter. */
class RoadNeuralSessionTest {
    @Test fun pretrainedWeightsExecuteThroughSameSessionUsedByApp(){
        val model=File("src/main/assets/road_lanes_yolop_320.onnx").readBytes()
        for(threads in listOf(1,2)) RoadNeuralSession(model,threads).use{network->
            val pixels=IntArray(320*180){i->val y=i/320;val x=i%320;0xff000000.toInt() or ((x*255/320) shl 16) or ((y*255/180) shl 8) or 80}
            val result=network.detect(pixels,320,180)
            assertEquals(320,result.width);assertEquals(180,result.height)
            assertEquals(result.lanes.count{it>0},result.lanePixels);assertEquals(result.road.count{it},result.roadPixels)
            assertTrue(result.lanes.all{it in 0..255})
        }
    }
    @Test(expected=IllegalArgumentException::class) fun changedModelCannotBeExecuted(){RoadNeuralSession(byteArrayOf(0,1,2))}
}
