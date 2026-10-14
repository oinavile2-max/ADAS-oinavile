package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test

class AnalysisResolutionTest {
    @Test fun supportedNeuralSizeAvoidsConvertingAnUnnecessarilyLargeFrame() {
        val sizes=listOf(640 to 360,320 to 180,176 to 99)
        assertEquals(320 to 180,sizes.minByOrNull{AnalysisResolution.score(it.first,it.second)})
    }
    @Test fun lowCostThumbnailDoesNotWinWhenNeuralSizeIsUnavailable() {
        val sizes=listOf(160 to 90,640 to 360)
        assertEquals(640 to 360,sizes.minByOrNull{AnalysisResolution.score(it.first,it.second)})
    }
    @Test fun fourThreeFallbackSelectsUsefulResolutionAndTinyOnlyCameraStillWorks() {
        assertEquals(320 to 240,listOf(176 to 144,640 to 480,320 to 240).minByOrNull{AnalysisResolution.score(it.first,it.second)})
        assertEquals(176 to 144,listOf(176 to 144).minByOrNull{AnalysisResolution.score(it.first,it.second)})
    }
}
