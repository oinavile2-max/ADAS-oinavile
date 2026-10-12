package br.com.dashcamadas

import kotlin.math.*

enum class AlertTone(val label:String,val notes:List<Double>) {
    SOFT("Bip suave",listOf(.8)), DOUBLE("Bip duplo",listOf(1.0,1.0)),
    TRIPLE("Bip triplo",listOf(1.0,1.0,1.0)), ASCENDING("Tons ascendentes",listOf(.75,1.0,1.25)),
    DESCENDING("Tons descendentes",listOf(1.25,1.0,.75)), URGENT("Alternado",listOf(.85,1.3,.85,1.3));
    companion object {
        const val SAMPLE_RATE=22050
        fun fromId(id:String?)=entries.firstOrNull{it.name==id} ?: SOFT
        fun pcm(tone:AlertTone,frequency:Int,durationMs:Int,gain:Float):ShortArray {
            require(frequency in 400..1800 && durationMs in 80..240 && gain.isFinite() && gain in 0f..1f)
            val noteSamples=SAMPLE_RATE*durationMs/1000;val gap=SAMPLE_RATE*60/1000
            val output=ShortArray(noteSamples*tone.notes.size+gap*(tone.notes.size-1))
            val ramp=SAMPLE_RATE*8/1000
            tone.notes.forEachIndexed{index,multiplier->
                val offset=index*(noteSamples+gap)
                for(i in 0 until noteSamples){val envelope=min(1.0,min(i.toDouble()/ramp,(noteSamples-1-i).toDouble()/ramp));output[offset+i]=(sin(2*PI*frequency*multiplier*i/SAMPLE_RATE)*envelope*gain*14000).toInt().toShort()}
            }
            return output
        }
    }
}
