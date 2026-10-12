package br.com.dashcamadas.vision

import ai.onnxruntime.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/** Genuine pretrained YOLOP road/lane heads, CPU execution including ARM32. */
class RoadNeuralSession(model:ByteArray):AutoCloseable {
    private val environment=OrtEnvironment.getEnvironment()
    private val input=ByteBuffer.allocateDirect(3*320*320*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val session:OrtSession
    init{
        val hash=MessageDigest.getInstance("SHA-256").digest(model).joinToString(""){"%02x".format(it.toInt() and 255)}
        require(hash=="f93ced8254ccbb532ff9f9593e7b7d0022f28210831b615f5039066c9f5222fe"){"Lane model integrity failure"}
        environment.setTelemetry(false)
        OrtSession.SessionOptions().use{options->
            options.setIntraOpNumThreads(2);options.setInterOpNumThreads(1)
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            session=environment.createSession(model,options)
        }
        require(session.inputNames==setOf("images") && session.outputNames==setOf("drive_area_seg","lane_line_seg"))
        val inputInfo=session.inputInfo.getValue("images").info as TensorInfo
        require(inputInfo.shape.contentEquals(longArrayOf(1,3,320,320)) && inputInfo.type==OnnxJavaType.FLOAT)
    }
    fun detect(pixels:IntArray,width:Int,height:Int):NeuralMasks {
        val layout=Letterbox(width,height)
        NeuralImage.encode(pixels,layout,input)
        OnnxTensor.createTensor(environment,input,longArrayOf(1,3,320,320)).use{tensor->
            session.run(mapOf("images" to tensor)).use{result->
                fun values(name:String):FloatArray {
                    val value=result.get(name).orElseThrow{IllegalStateException("Missing output: $name")} as OnnxTensor
                    require(value.info.shape.contentEquals(longArrayOf(1,2,320,320)))
                    require(value.info.type==OnnxJavaType.FLOAT)
                    val buffer=value.floatBuffer ?: error("Invalid float output")
                    return FloatArray(2*320*320).apply{buffer.get(this)}
                }
                val masks=NeuralMasks.decode(layout,values("drive_area_seg"),values("lane_line_seg"))
                return if(NeuralImage.usefulVariation(pixels))masks else NeuralMasks(320,180,IntArray(320*180),BooleanArray(320*180),0,0)
            }
        }
    }
    override fun close(){session.close()}
}
