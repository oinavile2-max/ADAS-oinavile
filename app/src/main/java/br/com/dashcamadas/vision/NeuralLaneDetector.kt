package br.com.dashcamadas.vision

import android.content.Context
import android.graphics.Bitmap

/** Android adapter for the shared, genuinely executed neural session. */
class NeuralLaneDetector(context:Context,threads:Int=2):AutoCloseable {
    private val network=RoadNeuralSession(context.assets.open("road_lanes_yolop_320.onnx").use{it.readBytes()},threads)
    private var pixels=IntArray(0)
    fun detect(bitmap:Bitmap):NeuralMasks {
        if(pixels.size!=bitmap.width*bitmap.height)pixels=IntArray(bitmap.width*bitmap.height)
        bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        return network.detect(pixels,bitmap.width,bitmap.height)
    }
    override fun close(){network.close()}
}
