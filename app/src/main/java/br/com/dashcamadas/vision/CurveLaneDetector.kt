package br.com.dashcamadas.vision

import kotlin.math.*

/** Multi-width stripe observations and consensus fitting. No inferred missing boundary. */
class CurveLaneDetector {
    private data class Point(val t:Double,val x:Double,val strength:Double)
    private data class Row(val t:Double,val points:List<Point>)
    private var previous=Lanes(null,null)
    private var leftAge=0;private var rightAge=0
    fun reset(){previous=Lanes(null,null);leftAge=0;rightAge=0}
    fun detect(gray:IntArray,w:Int,h:Int,colors:IntArray?=null,semantic:Boolean=false,egoX:Float=.5f):Lanes {
        require(gray.size==w*h && (colors==null || colors.size==gray.size))
        require(egoX in .15f.. .85f)
        if(w<80 || h<60){reset();return Lanes(null,null)}
        val radii=intArrayOf(max(2,w/160),max(3,w/80),max(5,w/40),max(7,w/27)).distinct()
        val rows=ArrayList<Row>()
        for(y in (h*.53).toInt() until (h*.98).toInt() step max(1,h/80)) {
            val candidates=ArrayList<Point>()
            for(x in max((w*.025).toInt(),radii.last()) until min((w*.975).toInt(),w-radii.last())) {
                val v=(gray[y*w+x-1]+gray[y*w+x]*2+gray[y*w+x+1])/4.0
                var best=0.0
                var selectedRadius=radii.first()
                for(radius in radii) {
                    val a=v-gray[y*w+x-radius];val b=v-gray[y*w+x+radius]
                    if(a>=5 && b>=5 && min(a,b)>best){best=min(a,b);selectedRadius=radius}
                }
                if(best>=9) {
                    if(!semantic){
                    // A stripe persists along a local direction. Strong texture alone is insufficient.
                    var coherence=0.0
                    var direction=0
                    for(slope in -2..2){
                        var sum=0.0;var square=0.0;var contrast=0.0;var count=0
                        for(dy in -2..2){
                            val xx=x+slope*dy;val yy=y+dy
                            if(yy !in 0 until h || xx-selectedRadius<0 || xx+selectedRadius>=w)continue
                            val value=(gray[yy*w+xx-1]+2*gray[yy*w+xx]+gray[yy*w+xx+1])/4.0
                            sum+=value;square+=value*value
                            contrast+=value-(gray[yy*w+xx-selectedRadius]+gray[yy*w+xx+selectedRadius])/2.0;count++
                        }
                        if(count==5){val variance=max(0.0,square/5-(sum/5)*(sum/5));val value=contrast/5-sqrt(variance)*1.25;if(value>coherence){coherence=value;direction=slope}}
                    }
                    best=min(best,coherence)
                    if(best<9)continue
                    var texture=0.0;var textureSamples=0
                    for(dy in -2..2){
                        val xx=x+direction*dy;val yy=y+dy
                        val left=xx-selectedRadius-1;val right=xx+selectedRadius+1
                        if(yy in 0 until h && left>0 && right<w-1){texture+=abs(gray[yy*w+left]-gray[yy*w+left-1])+abs(gray[yy*w+right]-gray[yy*w+right+1]);textureSamples+=2}
                    }
                    if(textureSamples==0 || best<texture/textureSamples*1.05+8)continue
                    }
                    val color=colors?.get(y*w+x)
                    if(color!=null){val r=color shr 16 and 255;val g=color shr 8 and 255;val b=color and 255;if(r>b*1.3 && g>b*1.2 && abs(r-g)<100)best*=1.15}
                    candidates.add(Point((y.toDouble()/h-.5)/.48,x.toDouble()/w,min(best,70.0)))
                }
            }
            val peaks=ArrayList<Point>()
            for(p in candidates.sortedByDescending{it.strength}) {
                if(peaks.none{abs(it.x-p.x)<.018})peaks.add(p)
                if(peaks.size==8)break
            }
            rows.add(Row((y.toDouble()/h-.5)/.48,peaks))
        }
        fun side(isLeft:Boolean):LaneCurve? {
            val available=rows.map{row->Row(row.t,row.points.filter{if(isLeft)it.x<egoX+.01 else it.x>egoX-.01})}.filter{it.points.isNotEmpty()}
            if(available.size<9)return null
            val anchors=listOf(available.take(3),available.drop(available.size/2-1).take(3),available.takeLast(3))
                .map{group->group.flatMap{it.points.take(4)}}
            var best:DoubleArray?=null;var bestScore=0.0;var bestPoints=emptyList<Point>()
            fun evaluate(p:DoubleArray) {
                if(abs(p[0])>1.25)return
                fun at(t:Double)=p[0]*t*t+p[1]*t+p[2]
                val bottom=at(available.last().t);val top=at(available.first().t)
                if(bottom !in .005.. .995 || top !in -.025..1.025 || abs(bottom-top)<.03)return
                if(isLeft && bottom>=egoX+.01 || !isLeft && bottom<=egoX-.01)return
                val observed=ArrayList<Point>()
                for(row in rows) {
                    row.points.minByOrNull{abs(it.x-at(row.t))}?.takeIf{abs(it.x-at(row.t))<.018}?.let{observed.add(it)}
                }
                if(observed.size<9 || observed.last().t-observed.first().t<.42)return
                val score=observed.sumOf{1.0+min(it.strength,40.0)/80.0-abs(it.x-at(it.t))*12}+(if(isLeft)bottom else 1-bottom)*.1
                if(score>bestScore){bestScore=score;best=p;bestPoints=observed}
            }
            for(a in anchors[0])for(b in anchors[1])for(c in anchors[2]) {
                if(b.t-a.t<.1 || c.t-b.t<.1)continue
                val slope1=(b.x-a.x)/(b.t-a.t);val slope2=(c.x-b.x)/(c.t-b.t)
                val qa=(slope2-slope1)/(c.t-a.t);val qb=slope1-qa*(a.t+b.t)
                evaluate(doubleArrayOf(qa,qb,a.x-qa*a.t*a.t-qb*a.t))
            }
            if(best==null)return null
            val p=fit(bestPoints) ?: return null
            val residual=bestPoints.sumOf{abs(it.x-(p[0]*it.t*it.t+p[1]*it.t+p[2]))}/bestPoints.size
            val span=bestPoints.last().t-bestPoints.first().t
            val coverage=bestPoints.size.toDouble()/rows.count{it.t>=bestPoints.first().t && it.t<=bestPoints.last().t}
            if(coverage<(if(semantic).20 else .38))return null
            val confidence=(min(1.0,bestPoints.size/(if(semantic)16.0 else 20.0))*min(1.0,span/.7)*min(1.0,coverage/(if(semantic).45 else .7))*(1-residual/.018)).toFloat().coerceIn(0f,1f)
            return LaneCurve(p[0].toFloat(),p[1].toFloat(),p[2].toFloat(),confidence,
                (.5+bestPoints.first().t*.48).toFloat(),(.5+bestPoints.last().t*.48).toFloat())
        }
        var left=side(true);var right=side(false)
        if(left!=null && right!=null) {
            val start=max(left.yStart,right.yStart);val end=min(left.yEnd,right.yEnd)
            val widths=(0..8).map{i->val y=start+(end-start)*i/8;right.x(y)-left.x(y)}
            // Both boundaries may curve in the same direction.
            if(widths.any{it !in .025f.. .94f} || widths.last()<.16f || widths.last()<widths.first()*1.15f){left=null;right=null}
        }
        fun stabilize(lane:LaneCurve?,old:LaneCurve?,isLeft:Boolean):LaneCurve? {
            if(lane==null){if(isLeft)leftAge=0 else rightAge=0;return null}
            val agrees=old!=null && listOf(.62f,.78f,.93f).all{abs(lane.x(it)-old.x(it))<.035f}
            val age=if(agrees)(if(isLeft)leftAge else rightAge)+1 else 1
            if(isLeft)leftAge=age else rightAge=age
            val confirmed=lane.copy(confidence=if(age<2)min(lane.confidence,.55f) else lane.confidence)
            if(!agrees || old==null)return confirmed
            val smooth=confirmed.copy(a=old.a*.35f+lane.a*.65f,b=old.b*.35f+lane.b*.65f,c=old.c*.35f+lane.c*.65f)
            return if(listOf(.62f,.78f,.93f).all{abs(smooth.x(it)-lane.x(it))<.018f})smooth else confirmed
        }
        previous=Lanes(stabilize(left,previous.left,true),stabilize(right,previous.right,false))
        return previous
    }
    private fun fit(points:List<Point>):DoubleArray? {
        val m=Array(3){DoubleArray(4)}
        for(p in points) {
            val f=doubleArrayOf(p.t*p.t,p.t,1.0)
            for(i in 0..2){for(j in 0..2)m[i][j]+=p.strength*f[i]*f[j];m[i][3]+=p.strength*f[i]*p.x}
        }
        for(col in 0..2) {
            val pivot=(col..2).maxByOrNull{abs(m[it][col])} ?: return null
            if(abs(m[pivot][col])<1e-9)return null
            val swap=m[col];m[col]=m[pivot];m[pivot]=swap
            val d=m[col][col];for(j in col..3)m[col][j]/=d
            for(row in 0..2)if(row!=col){val f=m[row][col];for(j in col..3)m[row][j]-=f*m[col][j]}
        }
        return DoubleArray(3){m[it][3]}
    }
}
