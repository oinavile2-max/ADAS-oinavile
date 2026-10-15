package br.com.dashcamadas

/** One automatic start per explicitly opened source; a manual command invalidates queued starts. */
class AutoRecordingGate {
    private var consumed=false
    private var command=0L
    fun opened(){consumed=false;command++}
    fun manual(){consumed=true;command++}
    fun claim(enabled:Boolean,hasRealFrame:Boolean,alreadyRecording:Boolean):Long? {
        if(!enabled || !hasRealFrame || alreadyRecording || consumed)return null
        consumed=true;return command
    }
    fun valid(token:Long)=token==command
}
