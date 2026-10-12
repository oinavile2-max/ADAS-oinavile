package br.com.dashcamadas.vision

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/** Quantized SSD MobileNet v2 COCO, Apache-2.0; CPU execution for ARM32/ARM64. */
class VehicleDetector(context:Context):AutoCloseable {
    private val interpreter:Interpreter
    private val input=ByteBuffer.allocateDirect(300*300*3).order(ByteOrder.nativeOrder())
    private val locations=Array(1){Array(20){FloatArray(4)}}
    private val classes=Array(1){FloatArray(20)}
    private val scores=Array(1){FloatArray(20)}
    private val count=FloatArray(1)
    private val pixels=IntArray(300*300)
    init {
        val bytes=context.assets.open("vehicles_ssd_mobilenet_v2.tflite").use{it.readBytes()}
        val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt() and 255)}
        require(hash=="42fb3d70ffb7bb37dd518f730f7be784b831c2078f30c497d0019cc2e987fa26") {"Model integrity failure"}
        val model=ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply{put(bytes);rewind()}
        interpreter=Interpreter(model,Interpreter.Options().setNumThreads(2))
        require(interpreter.getInputTensor(0).shape().contentEquals(intArrayOf(1,300,300,3)))
    }
    fun detect(bitmap:Bitmap):List<Vehicle> {
        val resized=Bitmap.createScaledBitmap(bitmap,300,300,true)
        resized.getPixels(pixels,0,300,0,0,300,300)
        input.rewind()
        for(pixel in pixels) {input.put((pixel shr 16 and 255).toByte());input.put((pixel shr 8 and 255).toByte());input.put((pixel and 255).toByte())}
        input.rewind()
        if(resized!==bitmap) resized.recycle()
        interpreter.runForMultipleInputsOutputs(arrayOf(input),mapOf(0 to locations,1 to classes,2 to scores,3 to count))
        return (0 until count[0].toInt().coerceIn(0,20)).mapNotNull { index ->
            val category=classes[0][index].toInt()
            if(category !in setOf(2,5,7) || scores[0][index]<.5f) return@mapNotNull null
            val b=locations[0][index]
            val box=Box(b[1].coerceIn(0f,1f),b[0].coerceIn(0f,1f),b[3].coerceIn(0f,1f),b[2].coerceIn(0f,1f))
            if(box.width<=0 || box.height<=0) null else Vehicle(box,scores[0][index],category)
        }
    }
    override fun close()=interpreter.close()
}
