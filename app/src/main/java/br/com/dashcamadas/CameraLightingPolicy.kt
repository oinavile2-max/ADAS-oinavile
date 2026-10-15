package br.com.dashcamadas

import java.nio.ByteBuffer
import kotlin.math.roundToInt

enum class CameraLightMode(val key:String,val label:String){
    AUTO("auto","Automático"), DAY("day","Dia"), NIGHT("night","Noite");
    companion object {fun read(key:String?)=entries.firstOrNull{it.key==key} ?: AUTO}
}

/** Image observations only: no clock-based assumption, synthetic enhancement or neural pixel edits. */
class CameraLightingPolicy {
    private var last=-1L
    private var candidateSince=-1L
    var dark=false;private set
    fun reset(){last=-1;candidateSince=-1;dark=false}
    fun observe(mean:Int,now:Long):Boolean {
        require(mean in 0..255 && now>=0)
        if(last>=0 && (now<=last || now-last>3000))candidateSince=-1
        last=now
        val transition=if(dark)mean>=80 else mean<=45
        if(!transition)candidateSince=-1
        else {
            if(candidateSince<0)candidateSince=now
            if(now-candidateSince>=if(dark)8000 else 3000){dark=!dark;candidateSince=-1}
        }
        return dark
    }
    fun night(mode:CameraLightMode)=mode==CameraLightMode.NIGHT || (mode==CameraLightMode.AUTO && dark)
    data class Fps(val min:Int,val max:Int)
    companion object {
        fun exposureSteps(night:Boolean,ev:Float,step:Float,range:IntRange):Int? {
            if(range.isEmpty() || !step.isFinite() || step<=0)return null
            val requested=if(night && ev.isFinite())ev.coerceIn(0f,2f) else 0f
            return (requested/step).coerceIn(range.first.toFloat(),range.last.toFloat()).roundToInt().coerceIn(range)
        }
        fun fps(night:Boolean,ranges:List<Fps>):Fps?=ranges.filter{it.min>0 && it.min<=it.max && it.max<=30}
            .minByOrNull{if(night)kotlin.math.abs(it.min-15)*3+kotlin.math.abs(it.max-30) else kotlin.math.abs(it.min-30)+kotlin.math.abs(it.max-30)}

        /** Samples at most 256 pixels. Honors crop, buffer position and arbitrary row/pixel strides. */
        fun mean(buffer:ByteBuffer,rowStride:Int,pixelStride:Int,left:Int,top:Int,width:Int,height:Int,rgba:Boolean):Int {
            require(width>0 && height>0 && left>=0 && top>=0 && rowStride>0 && pixelStride>=if(rgba)4 else 1)
            val start=buffer.position();var sum=0L;var count=0
            val xs=minOf(16,width);val ys=minOf(16,height)
            for(y in 0 until ys)for(x in 0 until xs){
                val col=left+x*(width-1)/maxOf(1,xs-1)
                val row=top+y*(height-1)/maxOf(1,ys-1)
                val index=start.toLong()+row.toLong()*rowStride+col.toLong()*pixelStride
                require(index>=start && index+(if(rgba)2 else 0)<buffer.limit())
                val i=index.toInt();val a=buffer.get(i).toInt() and 255
                sum+=if(rgba)(a*30+(buffer.get(i+1).toInt() and 255)*59+(buffer.get(i+2).toInt() and 255)*11)/100
                    else ((a-16)*255/219).coerceIn(0,255)
                count++
            }
            return (sum/count).toInt()
        }
    }
}
