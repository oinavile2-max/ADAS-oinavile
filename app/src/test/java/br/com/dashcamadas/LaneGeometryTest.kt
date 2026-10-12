package br.com.dashcamadas
import org.junit.Assert.*
import org.junit.Test

class LaneGeometryTest {
    @Test fun blankFrameDoesNotInventLanes() {
        assertNull(LaneGeometry.detect(IntArray(320*180),320,180))
        assertNull(LaneGeometry.detect(IntArray(320*180) {255},320,180))
    }
    @Test fun smallFrameIsUnavailable() {assertNull(LaneGeometry.detect(IntArray(20*20),20,20))}
    @Test fun actualContrastProducesGeometricallyOrderedCandidates() {
        val w=320;val h=180;val pixels=IntArray(w*h) {20}
        for(y in 100 until h) {
            val left=(140-(y-100)*.9).toInt();val right=(180+(y-100)*.9).toInt()
            for(x in left-1..left+1) pixels[y*w+x]=240
            for(x in right-1..right+1) pixels[y*w+x]=240
        }
        val result=LaneGeometry.detect(pixels,w,h)
        assertNotNull(result)
        assertTrue(result!!.left!![2]<result.right!![2])
    }
    @Test fun singleObservedBoundaryIsNotDiscardedOrDuplicated() {
        val w=320;val h=180;val pixels=IntArray(w*h) {20}
        for(y in 100 until h) {
            val left=(140-(y-100)*.9).toInt()
            for(x in left-1..left+1) pixels[y*w+x]=240
        }
        val result=LaneGeometry.detect(pixels,w,h)
        assertNotNull(result?.left);assertNull(result?.right)
    }
    @Test fun verticalContrastStreakIsNotPresentedAsLane() {
        val w=320;val h=180;val pixels=IntArray(w*h) {20}
        for(y in 100 until h) for(x in 191..193) pixels[y*w+x]=240
        assertNull(LaneGeometry.detect(pixels,w,h))
    }
    @Test(expected=IllegalArgumentException::class) fun incompleteInputIsRejected() {LaneGeometry.detect(IntArray(10),320,180)}
}
