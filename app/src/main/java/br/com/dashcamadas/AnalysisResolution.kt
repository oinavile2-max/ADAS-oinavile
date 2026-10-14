package br.com.dashcamadas

/** Prefer the neural input resolution, without choosing tiny frames merely because they are cheap. */
object AnalysisResolution {
    fun score(width:Int,height:Int):Long = kotlin.math.abs(width.toLong()*height-320L*180L)+
        if(width<320 || height<180)1_000_000L else 0L
}
