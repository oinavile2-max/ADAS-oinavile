package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test

class AuxiliaryPanelPolicyTest {
    @Test fun livePanelKeepsUpdatingEvenWithRecentSlowInference() {
        val clock=PreviewTimeline();var lastAnalysis:Long?=null;var shown=0
        for(t in 0L..3000L step 67) {
            if(t>=949)lastAnalysis=if(t<1900)0L else 1000L
            if(AuxiliaryPanelPolicy.wantsPreview(false,lastAnalysis,t) && clock.accept(t))shown++
        }
        assertEquals(45,shown);assertTrue(clock.timestamp>=2900)
    }
    @Test fun analysisModeHoldsExactSnapshotAndReturnsToLiveWhenExpired() {
        assertFalse(AuxiliaryPanelPolicy.wantsPreview(true,1000,1949))
        assertTrue(AuxiliaryPanelPolicy.wantsPreview(true,1000,2201))
        assertTrue(AuxiliaryPanelPolicy.wantsPreview(true,null,1949))
        assertTrue(AuxiliaryPanelPolicy.wantsPreview(false,1000,1949))
    }
    @Test fun livePanelRejectsBoxesFromAnotherFrameButAnalysisPanelAcceptsItsOwnSnapshot() {
        assertFalse(AuxiliaryPanelPolicy.mayAnnotate(false,1000,1100))
        assertTrue(AuxiliaryPanelPolicy.mayAnnotate(false,1100,1100))
        assertTrue(AuxiliaryPanelPolicy.mayAnnotate(true,1000,1100))
    }
}
