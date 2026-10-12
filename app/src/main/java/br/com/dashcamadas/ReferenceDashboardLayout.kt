package br.com.dashcamadas

import android.content.Context
import android.view.View
import android.widget.FrameLayout

/** All positions follow the supplied 1679×937 landscape reference, uniformly fitted to the display. */
class ReferenceDashboardLayout(context: Context, private val referenceWidth: Float = 1679f, private val referenceHeight: Float = 937f) : FrameLayout(context) {
    private data class Placement(val view: View, val x: Float, val y: Float, val width: Float, val height: Float)
    private val placements = mutableListOf<Placement>()
    var fullscreenView: View? = null
        set(value) { field=value;requestLayout() }
    fun place(view: View, x: Float, y: Float, width: Float, height: Float) {
        placements += Placement(view,x,y,width,height)
        addView(view,LayoutParams(1,1))
    }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int){
        val w=MeasureSpec.getSize(widthMeasureSpec);val h=MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(w,h)
        val scale=minOf((w-paddingLeft-paddingRight)/referenceWidth,(h-paddingTop-paddingBottom)/referenceHeight)
        for(p in placements){
            val full=p.view===fullscreenView
            p.view.measure(MeasureSpec.makeMeasureSpec(if(full)w-paddingLeft-paddingRight else (p.width*scale).toInt().coerceAtLeast(1),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(if(full)h-paddingTop-paddingBottom else (p.height*scale).toInt().coerceAtLeast(1),MeasureSpec.EXACTLY))
        }
    }
    override fun onLayout(changed:Boolean,left:Int,top:Int,right:Int,bottom:Int){
        val usableWidth=width-paddingLeft-paddingRight;val usableHeight=height-paddingTop-paddingBottom
        val scale=minOf(usableWidth/referenceWidth,usableHeight/referenceHeight)
        val ox=paddingLeft+(usableWidth-referenceWidth*scale)/2f;val oy=paddingTop+(usableHeight-referenceHeight*scale)/2f
        for(p in placements){
            val full=p.view===fullscreenView
            val x=if(full)paddingLeft else (ox+p.x*scale).toInt();val y=if(full)paddingTop else (oy+p.y*scale).toInt()
            p.view.layout(x,y,x+p.view.measuredWidth,y+p.view.measuredHeight)
        }
    }
}
