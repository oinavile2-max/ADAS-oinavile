package br.com.dashcamadas

import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class RgbaConverterTest {
    @Test fun rgbaOrderAndAlphaProduceCorrectArgb(){val p=byteArrayOf(255.toByte(),0,0,0,0,255.toByte(),0,255.toByte());assertArrayEquals(intArrayOf(0xffff0000.toInt(),0xff00ff00.toInt()),RgbaConverter.rgb(2,1,0,0,ByteBuffer.wrap(p),8,4))}
    @Test fun cropPaddingPixelStrideAndBufferPositionAreHonored(){
        val b=ByteBuffer.allocate(2+3*32);b.position(2)
        val p=2+32+8;b.put(p,20);b.put(p+1,40);b.put(p+2,60)
        assertEquals(0xff14283c.toInt(),RgbaConverter.rgb(1,1,1,1,b,32,8)[0])
    }
    @Test(expected=IllegalArgumentException::class) fun truncatedBufferIsRejected(){RgbaConverter.rgb(2,2,0,0,ByteBuffer.allocate(5),8,4)}
}
