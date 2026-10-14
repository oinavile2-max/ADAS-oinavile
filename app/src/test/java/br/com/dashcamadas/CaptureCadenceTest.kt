package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test

class CaptureCadenceTest {
    @Test fun previewKeepsFifteenFramesPerSecondWhileNeuralAnalysisIsSlow() {
        val cadence=CaptureCadence()
        cadence.analysisAccepted(0);cadence.analysisFinished(450)
        val draws=(0 until 30).map { it*1000L/30 }.filter { cadence.claimPreview(it,15) }
        assertEquals(15,draws.size)
        assertFalse(cadence.analysisDue(599));assertTrue(cadence.analysisDue(600))
        assertTrue(draws.last()>=900)
    }
    @Test fun overloadedAnalysisGetsRestWithoutSlowingPreview() {
        val cadence=CaptureCadence()
        cadence.analysisAccepted(0);cadence.analysisFinished(1400)
        assertEquals(2800L,cadence.analysisIntervalMs())
        assertFalse(cadence.analysisDue(2799));assertTrue(cadence.analysisDue(2800))
        assertEquals(30,(0 until 30).count { cadence.claimPreview(it*1000L/30,30) })
    }
    @Test fun rateChangeAndRestartDoNotWaitForOldDeadlines() {
        val cadence=CaptureCadence()
        assertTrue(cadence.claimPreview(0,4));assertFalse(cadence.claimPreview(100,4))
        assertTrue(cadence.claimPreview(101,15))
        cadence.analysisAccepted(101);cadence.reset()
        assertTrue(cadence.claimPreview(102,15));assertTrue(cadence.analysisDue(102))
    }
    @Test fun blockedUiGetsNewestFrameInOneScheduledDrain() {
        val mailbox=LatestFrameMailbox<Int>()
        assertTrue(mailbox.offer(0))
        for(i in 1..100)assertFalse(mailbox.offer(i))
        assertEquals(100,mailbox.take());assertNull(mailbox.take())
        assertTrue(mailbox.offer(101));assertEquals(101,mailbox.take())
    }
    @Test fun lateInferenceCannotRewindVideoOrAnnotateAnotherFrame() {
        val timeline=PreviewTimeline()
        assertTrue(timeline.accept(100));assertTrue(timeline.accept(133))
        assertFalse(timeline.matches(100));assertFalse(timeline.accept(100))
        assertEquals(133L,timeline.timestamp);assertTrue(timeline.matches(133))
        timeline.reset();assertTrue(timeline.accept(1))
    }
    @Test fun fpsCountsDrawsAndExpiresAfterVideoStops() {
        val meter=PreviewFrameMeter()
        for(i in 0..30){val t=i*1000L/30;meter.drawn(t,t);meter.drawn(t,t)}
        assertEquals(31L,meter.snapshot(1000).frames)
        assertEquals(30.0,meter.snapshot(1000).fps,.01)
        assertEquals(20.0,meter.snapshot(1500).fps,.01)
        assertEquals(0.0,meter.snapshot(3100).fps,.01)
        meter.reset();assertEquals(0L,meter.snapshot(3100).frames)
    }
}
