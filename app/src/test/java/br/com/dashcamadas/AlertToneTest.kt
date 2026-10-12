package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test

class AlertToneTest {
    @Test fun sixPatternsHaveDifferentWaveforms(){val samples=AlertTone.entries.map{AlertTone.pcm(it,880,160,.7f).contentHashCode()};assertEquals(6,samples.toSet().size)}
    @Test fun silenceAndGainAreRespected(){for(tone in AlertTone.entries){assertTrue(AlertTone.pcm(tone,880,160,0f).all{it==0.toShort()});assertTrue(AlertTone.pcm(tone,880,160,.5f).all{kotlin.math.abs(it.toInt())<=7000})}}
    @Test fun allTonesFitShortPcmWithoutClipping(){for(tone in AlertTone.entries){val samples=AlertTone.pcm(tone,1800,240,1f);assertTrue(samples.size<=AlertTone.SAMPLE_RATE*1.2);assertEquals(0,samples.first().toInt());assertEquals(0,samples.last().toInt());assertTrue(samples.all{kotlin.math.abs(it.toInt())<=14000})}}
    @Test fun gapsBetweenNotesAreActuallySilent(){val pcm=AlertTone.pcm(AlertTone.DOUBLE,880,160,1f);val end=AlertTone.SAMPLE_RATE*160/1000;val gap=AlertTone.SAMPLE_RATE*60/1000;assertTrue(pcm.slice(end until end+gap).all{it==0.toShort()})}
    @Test fun toneFrequencyIsMeasuredFromZeroCrossings(){val pcm=AlertTone.pcm(AlertTone.DOUBLE,1000,200,1f);val samples=pcm.take(AlertTone.SAMPLE_RATE*200/1000);val crossings=samples.zipWithNext().count{(a,b)->a<=0 && b>0};assertEquals(1000.0,crossings/.2,15.0)}
    @Test fun unknownPreferenceFallsBackToSoftTone(){assertEquals(AlertTone.SOFT,AlertTone.fromId("unrecognized"))}
    @Test(expected=IllegalArgumentException::class) fun invalidFrequencyIsRejected(){AlertTone.pcm(AlertTone.SOFT,0,160,1f)}
}
