package br.com.dashcamadas

import java.io.IOException
import java.nio.ByteBuffer

/** Seek/read/write contract of MyCamera's UMsd callbacks, independent of Android USB. */
class DvrByteChannel(private val file:Endpoint,val chunkSize:Int,
    private val pause:(Long)->Unit={Thread.sleep(it)}) {
    interface Endpoint {
        val length:Long
        fun read(position:Long,buffer:ByteBuffer)
        fun write(position:Long,buffer:ByteBuffer)
    }
    @Volatile private var closed=false
    private var position=0L
    val size:Int get()=file.length.coerceIn(0,Int.MAX_VALUE.toLong()).toInt()
    fun cancel(){closed=true}
    @Synchronized fun seek(offset:Int){check(!closed);require(offset>=0 && offset.toLong()<=file.length);position=offset.toLong()}
    @Synchronized fun read(data:ByteArray?,size:Int,chunk:Int,delay:Int):Int {
        if(closed || data==null || size<0 || size>data.size || chunk<=0 || delay !in 0..1000)return -1
        var copied=0
        try {
            while(copied<size && !closed){
                val count=minOf(chunk,size-copied)
                if(position>file.length-count)return if(copied==0)-1 else copied
                val buffer=ByteBuffer.wrap(data,copied,count)
                file.read(position,buffer)
                val actual=buffer.position()-copied
                if(actual<=0)return copied
                position+=actual;copied+=actual
                if(actual<count)return copied
                if(copied<size && delay>0)pause(delay.toLong())
            }
        }catch(e:InterruptedException){Thread.currentThread().interrupt();return if(copied==0)-1 else copied}
        catch(e:IOException){return if(copied==0)-1 else copied}
        return copied
    }
    @Synchronized fun write(data:ByteArray?,size:Int){
        check(!closed);require(data!=null && size>=0 && size<=data.size && position<=file.length-size)
        // Reference callback does not advance the cursor on a command write.
        file.write(position,ByteBuffer.wrap(data,0,size))
    }
}
