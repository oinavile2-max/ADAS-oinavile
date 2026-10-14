package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test

class TextureBufferOrientationTest {
    // BufferQueue flag fixtures: the SurfaceTexture GL vertical flip is included.
    private val fixtures=listOf(
        floatArrayOf(1f,0f,0f,-1f),floatArrayOf(-1f,0f,0f,-1f),
        floatArrayOf(1f,0f,0f,1f),floatArrayOf(-1f,0f,0f,1f),
        floatArrayOf(0f,-1f,-1f,0f),floatArrayOf(0f,1f,-1f,0f),
        floatArrayOf(0f,-1f,1f,0f),floatArrayOf(0f,1f,1f,0f))
    private fun surface(index:Int,sx:Float=1f,sy:Float=1f)=FloatArray(16).apply {
        val q=fixtures[index];this[0]=q[0]*sx;this[4]=q[1]*sx;this[1]=q[2]*sy;this[5]=q[3]*sy
        this[10]=1f;this[15]=1f;this[12]=.05f;this[13]=.9f
    }
    @Test fun ordinarySurfaceVerticalFlipDoesNotFlipTheScreen() {
        val orientation=TextureBufferOrientation.fromSurfaceMatrix(surface(0))!!
        assertEquals(TextureBufferOrientation(),orientation)
        assertEquals(0f to 0f,orientation.project(0f,0f))
    }
    @Test fun allEightProducerTransformsAreDecodedIncludingCropScale() {
        for(i in 0..7) for(scale in listOf(1f to 1f,.8f to .6f)) {
            val orientation=TextureBufferOrientation.fromSurfaceMatrix(surface(i,scale.first,scale.second))!!
            assertEquals(i and 1!=0,orientation.flipH)
            assertEquals(i and 2!=0,orientation.flipV)
            assertEquals(i and 4!=0,orientation.quarterTurn)
        }
    }
    @Test fun producerNinetyAndRequestedNinetyNeedNoSecondRotation() {
        val orientation=TextureBufferOrientation.fromSurfaceMatrix(surface(4))!!
        val displayed=orientation.displayTriangle(480,640)
        val desired=CameraTransform(640,480,480,640,90,false,false)
        val points=listOf(0f to 0f,640f to 0f,0f to 480f)
        for(i in points.indices){val target=desired.project(points[i].first,points[i].second)
            assertEquals(target.first,displayed[i*2],.001f);assertEquals(target.second,displayed[i*2+1],.001f)}
    }
    @Test fun frontMirrorOccursBeforeProducerQuarterTurn() {
        val orientation=TextureBufferOrientation.fromSurfaceMatrix(surface(5))!!
        assertEquals(1f to 1f,orientation.project(0f,0f))
        assertEquals(1f to 0f,orientation.project(1f,0f))
        assertEquals(0f to 1f,orientation.project(0f,1f))
    }
    @Test fun compensationAlignsAllCornersForEveryProducerAndUserRotation() {
        for(producer in 0..7) for(angle in listOf(0,90,180,270)) for(mirror in listOf(false,true)) for(fill in listOf(false,true)) {
            val source=TextureBufferOrientation.fromSurfaceMatrix(surface(producer))!!
            val triangle=source.displayTriangle(1024,600)
            val desired=CameraTransform(1280,720,1024,600,angle,mirror,fill)
            val target=listOf(desired.project(0f,0f),desired.project(1280f,0f),desired.project(0f,720f))
            // Independent affine solve from three point correspondences, like the Android Matrix adapter.
            val x1=triangle[2]-triangle[0];val y1=triangle[3]-triangle[1]
            val x2=triangle[4]-triangle[0];val y2=triangle[5]-triangle[1]
            val det=x1*y2-x2*y1
            for(p in listOf(.2f to .4f,1f to 1f,1f to 0f,0f to 1f)) {
                val visible=source.project(p.first,p.second)
                val dx=visible.first*1024-triangle[0];val dy=visible.second*600-triangle[1]
                val a=(dx*y2-dy*x2)/det;val b=(x1*dy-y1*dx)/det
                val x=target[0].first+a*(target[1].first-target[0].first)+b*(target[2].first-target[0].first)
                val y=target[0].second+a*(target[1].second-target[0].second)+b*(target[2].second-target[0].second)
                val expected=desired.project(p.first*1280,p.second*720)
                assertEquals(expected.first,x,.002f);assertEquals(expected.second,y,.002f)
            }
        }
    }
    @Test fun invalidOrNonOrthogonalProducerMatrixIsRejected() {
        assertNull(TextureBufferOrientation.fromSurfaceMatrix(FloatArray(16)))
        assertNull(TextureBufferOrientation.fromSurfaceMatrix(FloatArray(15)))
        assertNull(TextureBufferOrientation.fromSurfaceMatrix(surface(0).apply{this[0]=Float.NaN}))
        assertNull(TextureBufferOrientation.fromSurfaceMatrix(surface(0).apply{this[4]=.4f}))
    }
}
