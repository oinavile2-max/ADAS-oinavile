package br.com.dashcamadas

/** Live video never waits for inference; the analyzed snapshot is an explicit viewing mode. */
object AuxiliaryPanelPolicy {
    fun wantsPreview(analysisView:Boolean,lastAnalysis:Long?,now:Long):Boolean =
        !analysisView || lastAnalysis==null || now-lastAnalysis !in 0L..1200L
    fun mayAnnotate(analysisView:Boolean,analysisTime:Long,liveTime:Long):Boolean = analysisView || analysisTime==liveTime
}
