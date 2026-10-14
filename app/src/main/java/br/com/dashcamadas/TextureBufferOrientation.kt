package br.com.dashcamadas

import kotlin.math.abs

/** BufferQueue transformations are already drawn by TextureView, unlike ImageReader pixels. */
data class TextureBufferOrientation(val flipH:Boolean=false,val flipV:Boolean=false,val quarterTurn:Boolean=false) {
    fun project(x:Float,y:Float):Pair<Float,Float> {
        val u=if(flipH)1f-x else x
        val v=if(flipV)1f-y else y
        return if(quarterTurn)1f-v to u else u to v
    }
    fun displayTriangle(width:Int,height:Int):FloatArray {
        val corners=listOf(project(0f,0f),project(1f,0f),project(0f,1f))
        return FloatArray(6){i->if(i%2==0)corners[i/2].first*width else corners[i/2].second*height}
    }
    companion object {
        /** SurfaceTexture: column-major matrix = GL vertical flip × crop × H × V × R90. */
        fun fromSurfaceMatrix(m:FloatArray):TextureBufferOrientation? {
            if(m.size!=16 || m.any{!it.isFinite()})return null
            val sx=maxOf(abs(m[0]),abs(m[4]));val sy=maxOf(abs(m[1]),abs(m[5]))
            if(sx<.00001f || sy<.00001f)return null
            val a=m[0]/sx;val b=m[4]/sx;val c=-m[1]/sy;val d=-m[5]/sy
            for(flags in 0..7) {
                val h=if(flags and 1!=0)-1f else 1f
                val v=if(flags and 2!=0)-1f else 1f
                val r=flags and 4!=0
                val expected=if(r)floatArrayOf(0f,-h,v,0f) else floatArrayOf(h,0f,0f,v)
                if(abs(a-expected[0])<.001f && abs(b-expected[1])<.001f && abs(c-expected[2])<.001f && abs(d-expected[3])<.001f)
                    return TextureBufferOrientation(h<0,v<0,r)
            }
            return null
        }
    }
}
