package br.com.dashcamadas

import android.content.Context
import android.media.*
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** MIX preserves external volume and never requests audio focus. Actual FM mixing depends on firmware. */
class AlertAudio(context:Context):TextToSpeech.OnInitListener {
    private val prefs=context.getSharedPreferences("audio",Context.MODE_PRIVATE)
    @Volatile private var ready=false
    @Volatile private var released=false
    private val tts=TextToSpeech(context.applicationContext,this)
    private val executor=Executors.newSingleThreadExecutor()
    private val playing=AtomicBoolean(false)
    @Volatile private var activeTrack:AudioTrack?=null
    private var last=0L
    override fun onInit(status:Int){if(!released)ready=status==TextToSpeech.SUCCESS && tts.setLanguage(Locale("pt","BR"))>=TextToSpeech.LANG_AVAILABLE}
    fun mode()=prefs.getString("mode","mix") ?: "mix"
    fun voiceGain()=prefs.getInt("voice",80).coerceIn(0,100)/100f
    fun soundGain()=prefs.getInt("sound",65).coerceIn(0,100)/100f
    fun tone()=AlertTone.fromId(prefs.getString("tone",AlertTone.SOFT.name))
    fun frequency()=prefs.getInt("frequency",880).coerceIn(400,1800)
    fun durationMs()=prefs.getInt("duration_ms",160).coerceIn(80,240)
    fun configure(mode:String,voice:Int,sound:Int,tone:AlertTone=this.tone(),frequency:Int=this.frequency(),durationMs:Int=this.durationMs()){
        prefs.edit().putString("mode",mode).putInt("voice",voice.coerceIn(0,100)).putInt("sound",sound.coerceIn(0,100))
            .putString("tone",tone.name).putInt("frequency",frequency.coerceIn(400,1800)).putInt("duration_ms",durationMs.coerceIn(80,240)).apply()
    }
    @Synchronized fun announce(message:String,urgent:Boolean=false){
        if(released || mode()=="silent")return
        val now=SystemClock.elapsedRealtime()
        if(!urgent && last>0 && now-last<6000)return
        last=now
        if(mode()=="beep" || mode()=="mix")previewTone(tone(),frequency(),durationMs(),soundGain())
        if((mode()=="voice" || mode()=="mix") && ready){
            val params=android.os.Bundle().apply{putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,voiceGain())}
            tts.speak(message,TextToSpeech.QUEUE_FLUSH,params,"adas-"+now)
        }
    }
    fun previewTone(tone:AlertTone,frequency:Int,durationMs:Int,gain:Float){
        if(released || !playing.compareAndSet(false,true))return
        val pcm=AlertTone.pcm(tone,frequency,durationMs,gain)
        try{executor.execute{
            var track:AudioTrack?=null
            try{
                if(released)return@execute
                val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                val format=AudioFormat.Builder().setSampleRate(AlertTone.SAMPLE_RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                track=AudioTrack.Builder().setAudioAttributes(attributes).setAudioFormat(format).setBufferSizeInBytes(pcm.size*2).setTransferMode(AudioTrack.MODE_STATIC).build()
                activeTrack=track
                if(released)return@execute
                val written=track.write(pcm,0,pcm.size)
                if(written!=pcm.size)error("PCM write failed: "+written)
                track.play()
                Thread.sleep(pcm.size*1000L/AlertTone.SAMPLE_RATE+40)
            }catch(_:InterruptedException){Thread.currentThread().interrupt()}
            catch(e:Exception){android.util.Log.w("DashCamAudio","Efeito não reproduzido",e)}
            finally{
                activeTrack=null
                try{track?.stop()}catch(_:IllegalStateException){}
                track?.release();playing.set(false)
            }
        }}catch(_:java.util.concurrent.RejectedExecutionException){playing.set(false)}
    }
    fun release(){released=true;ready=false;executor.shutdownNow();tts.stop();tts.shutdown()}
}
