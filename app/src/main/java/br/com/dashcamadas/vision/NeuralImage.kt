package br.com.dashcamadas.vision

import java.nio.FloatBuffer
import kotlin.math.*

/** ImageNet RGB NCHW and aspect-preserving letterbox used by official YOLOP export. */
data class Letterbox(val width:Int,val height:Int,val size:Int=320) {
    init{require(width>0 && height>0 && size>0)}
    val scale=min(size.toDouble()/width,size.toDouble()/height)
    val resizedWidth=(width*scale).roundToInt().coerceIn(1,size)
    val resizedHeight=(height*scale).roundToInt().coerceIn(1,size)
    val left=(size-resizedWidth)/2
    val top=(size-resizedHeight)/2
    fun modelIndex(x:Int,y:Int,outWidth:Int,outHeight:Int):Int {
        require(x in 0 until outWidth && y in 0 until outHeight)
        val mx=(left+(x+.5)*resizedWidth/outWidth).toInt().coerceIn(left,left+resizedWidth-1)
        val my=(top+(y+.5)*resizedHeight/outHeight).toInt().coerceIn(top,top+resizedHeight-1)
        return my*size+mx
    }
}
object NeuralImage {
    private val mean=floatArrayOf(.485f,.456f,.406f)
    private val std=floatArrayOf(.229f,.224f,.225f)
    fun usefulVariation(pixels:IntArray):Boolean {
        require(pixels.isNotEmpty())
        var lo=255;var hi=0
        for(i in pixels.indices step max(1,pixels.size/1024)){
            val c=pixels[i];val l=((c shr 16 and 255)*30+(c shr 8 and 255)*59+(c and 255)*11)/100
            lo=min(lo,l);hi=max(hi,l)
        }
        return hi-lo>=12
    }
    fun encode(pixels:IntArray,layout:Letterbox,out:FloatBuffer) {
        require(pixels.size==layout.width*layout.height && out.capacity()==layout.size*layout.size*3)
        val plane=layout.size*layout.size
        for(channel in 0..2)for(i in 0 until plane)out.put(channel*plane+i,(114/255f-mean[channel])/std[channel])
        for(y in 0 until layout.resizedHeight)for(x in 0 until layout.resizedWidth) {
            val sx=((x+.5)*layout.width/layout.resizedWidth-.5).coerceIn(0.0,layout.width-1.0)
            val sy=((y+.5)*layout.height/layout.resizedHeight-.5).coerceIn(0.0,layout.height-1.0)
            val x0=sx.toInt();val y0=sy.toInt();val x1=min(x0+1,layout.width-1);val y1=min(y0+1,layout.height-1)
            val fx=(sx-x0).toFloat();val fy=(sy-y0).toFloat()
            for(channel in 0..2){
                val shift=(2-channel)*8
                fun value(xx:Int,yy:Int)=(pixels[yy*layout.width+xx] shr shift and 255).toFloat()
                val a=value(x0,y0)*(1-fx)+value(x1,y0)*fx
                val b=value(x0,y1)*(1-fx)+value(x1,y1)*fx
                out.put(channel*plane+(y+layout.top)*layout.size+x+layout.left,((a*(1-fy)+b*fy)/255f-mean[channel])/std[channel])
            }
        }
        out.rewind()
    }
}

data class NeuralMasks(val width:Int,val height:Int,val lanes:IntArray,val road:BooleanArray,val lanePixels:Int,val roadPixels:Int) {
    init{require(width>0 && height>0 && lanes.size==width*height && road.size==lanes.size)}
    fun roadUnder(box:Box):Boolean {
        var hits=0;var count=0
        val y=((box.bottom+.035f).coerceIn(0f,.999f)*height).toInt()
        for(i in -2..2){val x=((box.centerX+box.width*i/5).coerceIn(0f,.999f)*width).toInt();if(road[y*width+x])hits++;count++}
        return hits>count/2
    }
    fun besideLane(curve:LaneCurve,left:Boolean):Boolean {
        var hits=0;var count=0
        for(i in 0..4){
            val y=curve.yStart+(curve.yEnd-curve.yStart)*i/4
            val x=curve.x(y)+(if(left).035f else -.035f)
            if(x !in 0f.. .999f || y !in 0f.. .999f)continue
            if(road[(y*height).toInt()*width+(x*width).toInt()])hits++
            count++
        }
        return count>=3 && hits>=2
    }
    companion object {
        fun decode(layout:Letterbox,roadScores:FloatArray,laneScores:FloatArray,width:Int=320,height:Int=180):NeuralMasks {
            val plane=layout.size*layout.size
            require(roadScores.size==plane*2 && laneScores.size==plane*2)
            // Optimized sigmoid/resize yielded 1 + one float ULP on two real video frames.
            val tolerance=.000001f
            require(roadScores.all{it.isFinite() && it in -tolerance..1f+tolerance} && laneScores.all{it.isFinite() && it in -tolerance..1f+tolerance})
            val lanes=IntArray(width*height);val road=BooleanArray(lanes.size);var laneCount=0;var roadCount=0
            for(y in 0 until height)for(x in 0 until width){
                val index=y*width+x;val p=layout.modelIndex(x,y,width,height)
                if(laneScores[plane+p]>=.55f && laneScores[plane+p]-laneScores[p]>=.1f){lanes[index]=(laneScores[plane+p].coerceIn(0f,1f)*255).toInt();laneCount++}
                road[index]=roadScores[plane+p]>=.55f && roadScores[plane+p]-roadScores[p]>=.1f
                if(road[index])roadCount++
            }
            return NeuralMasks(width,height,lanes,road,laneCount,roadCount)
        }
    }
}
