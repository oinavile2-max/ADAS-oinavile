package br.com.dashcamadas

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/** MIX: não solicita AudioFocus, não controla volume de apps externos. */
class AlertAudio(private val context: Context) : TextToSpeech.OnInitListener {
    private val prefs = context.getSharedPreferences("audio",Context.MODE_PRIVATE)
    private var ready = false
    private val tts = TextToSpeech(context,this)
    private var last = 0L
    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS && tts.setLanguage(Locale("pt", "BR")) >= TextToSpeech.LANG_AVAILABLE
    }
    fun mode(): String = prefs.getString("mode", "mix") ?: "mix"
    fun voiceGain(): Float = prefs.getInt("voice",80).coerceIn(0,100)/100f
    fun soundGain(): Float = prefs.getInt("sound",65).coerceIn(0,100)/100f
    fun configure(mode: String, voice: Int, sound: Int) {
        prefs.edit().putString("mode",mode).putInt("voice",voice).putInt("sound",sound).apply()
    }
    fun announce(message: String, urgent: Boolean = false) {
        if (mode() == "silent") return
        val now = System.currentTimeMillis()
        if (!urgent && now-last < 6000L) return
        last = now
        if (mode() == "beep" || mode() == "mix") Thread { beep() }.start()
        if ((mode() == "voice" || mode() == "mix") && ready) {
            val params = android.os.Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,voiceGain()) }
            // Sem requestAudioFocus; mixer do firmware pode ainda alterar o áudio do rádio.
            tts.speak(message,TextToSpeech.QUEUE_FLUSH,params,"adas-${now}")
        }
    }
    private fun beep() {
        val hz = 880.0; val rate=22050; val n=rate/6
        val pcm = ShortArray(n) { i ->
            val envelope = kotlin.math.min(1.0, kotlin.math.min(i.toDouble()/500.0,(n-i).toDouble()/500.0))
            (sin(2*PI*hz*i/rate)*envelope*soundGain()*12000).toInt().toShort()
        }
        val a = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val format = AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
        val track = AudioTrack.Builder().setAudioAttributes(a).setAudioFormat(format)
            .setBufferSizeInBytes(pcm.size*2).setTransferMode(AudioTrack.MODE_STATIC).build()
        try { track.write(pcm,0,pcm.size);track.play();Thread.sleep(220) } finally {track.stop();track.release()}
    }
    fun release(){tts.stop();tts.shutdown()}
}
