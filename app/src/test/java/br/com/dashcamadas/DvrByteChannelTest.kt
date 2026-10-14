package br.com.dashcamadas

import java.io.IOException
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class DvrByteChannelTest {
    private class MemoryEndpoint: DvrByteChannel.Endpoint {
        val bytes=ByteArray(32){it.toByte()}
        val reads=mutableListOf<Pair<Long,Int>>()
        var failRead=false
        override val length get()=bytes.size.toLong()
        override fun read(position:Long,buffer:ByteBuffer){
            if(failRead)throw IOException("disconnected")
            reads+=position to buffer.remaining()
            buffer.put(bytes,position.toInt(),buffer.remaining())
        }
        override fun write(position:Long,buffer:ByteBuffer){buffer.get(bytes,position.toInt(),buffer.remaining())}
    }
    @Test fun seekThenReadInChunksPreservesCursorAndRemainder(){
        val file=MemoryEndpoint();val pauses=mutableListOf<Long>()
        val channel=DvrByteChannel(file,4){pauses+=it};val data=ByteArray(10)
        channel.seek(3)
        assertEquals(10,channel.read(data,10,4,5))
        assertArrayEquals(ByteArray(10){(it+3).toByte()},data)
        assertEquals(listOf(3L to 4,7L to 4,11L to 2),file.reads)
        assertEquals(listOf(5L,5L),pauses)
        val next=ByteArray(2);assertEquals(2,channel.read(next,2,4,0));assertArrayEquals(byteArrayOf(13,14),next)
    }
    @Test fun commandWriteDoesNotAdvanceAndCannotExtendEndpoint(){
        val file=MemoryEndpoint();val channel=DvrByteChannel(file,4)
        channel.seek(2);channel.write(byteArrayOf(91,92),2)
        val data=ByteArray(2);assertEquals(2,channel.read(data,2,4,0));assertArrayEquals(byteArrayOf(91,92),data)
        channel.seek(31)
        assertThrows(IllegalArgumentException::class.java){channel.write(byteArrayOf(1,2),2)}
        assertEquals(31,file.bytes[31].toInt())
    }
    @Test fun invalidNativeReadArgumentsDoNotTouchUsb(){
        val file=MemoryEndpoint();val channel=DvrByteChannel(file,4)
        assertEquals(-1,channel.read(null,1,1,0));assertEquals(-1,channel.read(ByteArray(2),3,1,0))
        assertEquals(-1,channel.read(ByteArray(2),2,0,0));assertEquals(-1,channel.read(ByteArray(2),2,1,-1))
        assertEquals(-1,channel.read(ByteArray(2),2,1,1001));assertEquals(-1,channel.read(ByteArray(2),-1,1,0))
        assertTrue(file.reads.isEmpty())
    }
    @Test fun disconnectAndEndOfEndpointDoNotSpinOrInventBytes(){
        val file=MemoryEndpoint();val channel=DvrByteChannel(file,4)
        channel.seek(30);assertEquals(-1,channel.read(ByteArray(4),4,4,0));assertTrue(file.reads.isEmpty())
        channel.seek(0);file.failRead=true
        assertEquals(-1,channel.read(ByteArray(4),4,4,0))
    }
    @Test fun cancellationStopsAnInFlightChunkedRead(){
        val file=MemoryEndpoint();lateinit var channel:DvrByteChannel
        channel=DvrByteChannel(file,4){channel.cancel()}
        assertEquals(4,channel.read(ByteArray(12),12,4,1));assertEquals(1,file.reads.size)
        assertEquals(-1,channel.read(ByteArray(1),1,1,0))
        assertThrows(IllegalStateException::class.java){channel.write(byteArrayOf(1),1)}
    }
    @Test fun aliasesSelectTheirOriginalProtocolAndOrdinaryFilesAreRejected(){
        assertEquals(0,DvrProtocol.type("AKFILE"));assertEquals(0,DvrProtocol.type("elene"))
        assertEquals(1,DvrProtocol.type("kkfile"));assertEquals(1,DvrProtocol.type("EEFILE"))
        assertEquals(2,DvrProtocol.type("jrfile"))
        listOf("video.mp4","akfile.mp4","../akfile","notes.txt").forEach{assertNull(DvrProtocol.type(it))}
    }
}
