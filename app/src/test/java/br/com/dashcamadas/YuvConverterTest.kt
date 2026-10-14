package br.com.dashcamadas

import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class YuvConverterTest {
    @Test fun neutralChromaPreservesBlackAndWhite() {
        val y=YuvConverter.Plane(ByteBuffer.wrap(byteArrayOf(16,235.toByte())),2,1)
        val uv=YuvConverter.Plane(ByteBuffer.wrap(byteArrayOf(128.toByte())),1,1)
        assertArrayEquals(intArrayOf(0xff000000.toInt(),0xffffffff.toInt()),YuvConverter.rgb(2,1,0,0,y,uv,uv))
    }
    @Test fun bt601RedIsConvertedToRgb() {
        val y=YuvConverter.Plane(ByteBuffer.wrap(byteArrayOf(81)),1,1)
        val u=YuvConverter.Plane(ByteBuffer.wrap(byteArrayOf(90)),1,1)
        val v=YuvConverter.Plane(ByteBuffer.wrap(byteArrayOf(240.toByte())),1,1)
        assertEquals(0xffff0000.toInt(),YuvConverter.rgb(1,1,0,0,y,u,v)[0])
    }
    @Test fun planePositionCropAndPaddedStridesAreRespected() {
        val luma=ByteBuffer.wrap(byteArrayOf(99,16,16,16,235.toByte(),99,99)).apply{position(1)}
        val chroma=ByteBuffer.wrap(byteArrayOf(55,128.toByte(),77,128.toByte(),77)).apply{position(1)}
        val y=YuvConverter.Plane(luma,6,1)
        val uv=YuvConverter.Plane(chroma,4,2)
        assertEquals(0xffffffff.toInt(),YuvConverter.rgb(1,1,3,0,y,uv,uv)[0])
        assertEquals(1,luma.position());assertEquals(1,chroma.position())
    }
    @Test fun reusedOutputIsFullyReplacedWithoutChangingPreviousCopy() {
        val luma=ByteBuffer.wrap(byteArrayOf(16,235.toByte()))
        val y=YuvConverter.Plane(luma,2,1)
        val uv=YuvConverter.Plane(ByteBuffer.wrap(byteArrayOf(128.toByte())),1,1)
        val out=IntArray(2)
        assertSame(out,YuvConverter.rgbInto(2,1,0,0,y,uv,uv,out))
        val previous=out.copyOf()
        luma.put(0,235.toByte());luma.put(1,16)
        YuvConverter.rgbInto(2,1,0,0,y,uv,uv,out)
        assertArrayEquals(intArrayOf(0xffffffff.toInt(),0xff000000.toInt()),out)
        assertArrayEquals(intArrayOf(0xff000000.toInt(),0xffffffff.toInt()),previous)
    }
}
