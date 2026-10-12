package br.com.dashcamadas.vision

import kotlin.math.*

/** Local stripe contrast plus robust quadratic fitting. Not a semantic segmentation model. */
class CurveLaneDetector {
    private var previous=Lanes(null,null)
    fun reset() { previous=Lanes(null,null) }
    fun detect(gray:IntArray,w:Int,h:Int):Lanes {
        require(gray.size==w*h)
        if(w<80 || h<60) return Lanes(null,null)
        fun side(left:Boolean):LaneCurve? {
            val prior=if(left) previous.left else previous.right
            val points=ArrayList<Triple<Double,Double,Double>>()
            for(y in (h*.53f).toInt() until (h*.98f).toInt() step max(1,h/80)) {
                val ny=y.toFloat()/h
                val expected=prior?.x(ny)
                val lo=if(expected!=null) max(.03f,expected-.12f) else if(left) .03f else .50f
                val hi=if(expected!=null) min(.97f,expected+.12f) else if(left) .50f else .97f
                var bx=-1;var strength=0f
                val radius=max(3,w/80)
                for(x in max(radius,(lo*w).toInt()) until min(w-radius,(hi*w).toInt())) {
                    val center=gray[y*w+x].toFloat()
                    val neighbours=(gray[y*w+x-radius]+gray[y*w+x+radius])/2f
                    val contrast=center-neighbours
                    val edgeLeft=center-gray[y*w+x-radius]
                    val edgeRight=center-gray[y*w+x+radius]
                    if(edgeLeft<5 || edgeRight<5) continue
                    val continuity=if(expected!=null) 1f-abs(x.toFloat()/w-expected)/.24f else 1f
                    val score=contrast*continuity
                    if(score>strength) {strength=score;bx=x}
                }
                if(bx>=0 && strength>=9f) points.add(Triple(((ny-.5f)/.48f).toDouble(),bx.toDouble()/w,min(strength.toDouble(),60.0)))
            }
            if(points.size<9) return null
            var inliers=points.toList()
            var fitted:DoubleArray?=null
            repeat(3) {
                fitted=fit(inliers)
                val p=fitted ?: return null
                inliers=points.filter { abs(it.second-(p[0]*it.first*it.first+p[1]*it.first+p[2]))<.025 }
                if(inliers.size<9) return null
            }
            val p=fitted ?: return null
            val span=inliers.maxOf{it.first}-inliers.minOf{it.first}
            if(span<.42 || inliers.size<points.size*.65) return null
            val residual=inliers.sumOf { abs(it.second-(p[0]*it.first*it.first+p[1]*it.first+p[2])) }/inliers.size
            val curve=LaneCurve(p[0].toFloat(),p[1].toFloat(),p[2].toFloat(),(min(1.0,inliers.size/20.0)*min(1.0,span/.7)*(1-residual/.025)).toFloat())
            val top=curve.x(.56f);val bottom=curve.x(.95f)
            if(top !in 0f..1f || bottom !in 0f..1f || abs(bottom-top)<.025f || abs(curve.a)>.65f) return null
            if(left && bottom>=top+.04f || !left && bottom<=top-.04f) return null
            return curve
        }
        var left=side(true);var right=side(false)
        if(left!=null && right!=null) {
            val width=right.x(.93f)-left.x(.93f)
            if(width !in .16f.. .92f || left.x(.6f)>=right.x(.6f)) {left=null;right=null}
        }
        previous=Lanes(left,right)
        return previous
    }
    private fun fit(points:List<Triple<Double,Double,Double>>):DoubleArray? {
        val m=Array(3){DoubleArray(4)}
        for((t,x,weight) in points) {
            val f=doubleArrayOf(t*t,t,1.0)
            for(i in 0..2) {for(j in 0..2) m[i][j]+=weight*f[i]*f[j];m[i][3]+=weight*f[i]*x}
        }
        for(col in 0..2) {
            val pivot=(col..2).maxByOrNull{abs(m[it][col])} ?: return null
            if(abs(m[pivot][col])<1e-9) return null
            val swap=m[col];m[col]=m[pivot];m[pivot]=swap
            val factor=m[col][col];for(j in col..3) m[col][j]/=factor
            for(row in 0..2) if(row!=col) { val f=m[row][col];for(j in col..3) m[row][j]-=f*m[col][j] }
        }
        return DoubleArray(3){m[it][3]}
    }
}
