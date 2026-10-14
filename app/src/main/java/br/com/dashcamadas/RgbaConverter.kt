package br.com.dashcamadas

import java.nio.ByteBuffer

/** RGBA ImageReader output from the native renderer, honoring crop and padded strides. */
object RgbaConverter {
    fun rgb(width:Int,height:Int,left:Int,top:Int,buffer:ByteBuffer,rowStride:Int,pixelStride:Int):IntArray {
        return rgbInto(width,height,left,top,buffer,rowStride,pixelStride,IntArray(width*height))
    }
    fun rgbInto(width:Int,height:Int,left:Int,top:Int,buffer:ByteBuffer,rowStride:Int,pixelStride:Int,out:IntArray):IntArray {
        require(out.size>=width*height)
        require(width>0 && height>0 && left>=0 && top>=0 && pixelStride>=4 && rowStride>0)
        val offset=buffer.position()
        require(offset+(top+height-1)*rowStride+(left+width-1)*pixelStride+3<buffer.limit())
        for(i in 0 until width*height){val p=offset+(top+i/width)*rowStride+(left+i%width)*pixelStride
            val r=buffer.get(p).toInt() and 255;val g=buffer.get(p+1).toInt() and 255;val b=buffer.get(p+2).toInt() and 255
            out[i]=(255 shl 24) or (r shl 16) or (g shl 8) or b}
        return out
    }
}
