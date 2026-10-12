package br.com.dashcamadas

/** One mapping shared by preview and analysis overlays. Input coordinates are raw frame pixels. */
data class CameraTransform(
    val frameWidth: Int, val frameHeight: Int, val viewWidth: Int, val viewHeight: Int,
    val rotation: Int, val mirror: Boolean, val fill: Boolean
) {
    init { require(frameWidth > 0 && frameHeight > 0 && viewWidth > 0 && viewHeight > 0); require(rotation in listOf(0,90,180,270)) }
    private val swapped = rotation == 90 || rotation == 270
    private val orientedWidth = if (swapped) frameHeight else frameWidth
    private val orientedHeight = if (swapped) frameWidth else frameHeight
    val scale = if (fill) maxOf(viewWidth.toFloat()/orientedWidth,viewHeight.toFloat()/orientedHeight)
                else minOf(viewWidth.toFloat()/orientedWidth,viewHeight.toFloat()/orientedHeight)
    fun unproject(x: Float, y: Float): Pair<Float,Float> {
        val rx = (x-viewWidth/2f)/scale * (if(mirror) -1 else 1)
        val ry = (y-viewHeight/2f)/scale
        val (dx,dy)=when(rotation) { 90 -> ry to -rx; 180 -> -rx to -ry; 270 -> -ry to rx; else -> rx to ry }
        return (dx+frameWidth/2f) to (dy+frameHeight/2f)
    }
    fun project(x: Float, y: Float): Pair<Float,Float> {
        val dx=x-frameWidth/2f; val dy=y-frameHeight/2f
        val (rx,ry)=when(rotation) { 90 -> -dy to dx; 180 -> -dx to -dy; 270 -> dy to -dx; else -> dx to dy }
        return (viewWidth/2f + (if(mirror) -rx else rx)*scale) to (viewHeight/2f+ry*scale)
    }
}
