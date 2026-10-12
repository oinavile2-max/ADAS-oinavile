package br.com.dashcamadas

import java.nio.ByteBuffer

/** Android YUV_420_888, with arbitrary row/pixel strides and cropped buffers. BT.601 limited range. */
object YuvConverter {
    data class Plane(val buffer:ByteBuffer,val rowStride:Int,val pixelStride:Int,val offset:Int=buffer.position())
    fun rgb(width:Int,height:Int,left:Int,top:Int,y:Plane,u:Plane,v:Plane):IntArray {
        require(width>0 && height>0 && left>=0 && top>=0)
        val out=IntArray(width*height)
        for(row in 0 until height) for(col in 0 until width) {
            val x=left+col;val yy=top+row
            val c=(y.buffer.get(y.offset+yy*y.rowStride+x*y.pixelStride).toInt() and 255)-16
            val d=(u.buffer.get(u.offset+(yy/2)*u.rowStride+(x/2)*u.pixelStride).toInt() and 255)-128
            val e=(v.buffer.get(v.offset+(yy/2)*v.rowStride+(x/2)*v.pixelStride).toInt() and 255)-128
            val red=((298*c+409*e+128) shr 8).coerceIn(0,255)
            val green=((298*c-100*d-208*e+128) shr 8).coerceIn(0,255)
            val blue=((298*c+516*d+128) shr 8).coerceIn(0,255)
            out[row*width+col]=(255 shl 24) or (red shl 16) or (green shl 8) or blue
        }
        return out
    }
}
