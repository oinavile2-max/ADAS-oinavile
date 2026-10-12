package br.com.dashcamadas

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.widget.*

class SettingsActivity: Activity() {
    private lateinit var audio: AlertAudio
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = AlertAudio(this)
        val root=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(28,20,28,16);setBackgroundColor(Color.rgb(12,22,34))}
        fun title(t:String)=TextView(this).apply {text=t;textSize=20f;setTextColor(Color.WHITE)}
        root.addView(title("ÁUDIO E CÂMERA — K2000N / K2001N / TABLET"))
        root.addView(title("Nunca solicitar foco de áudio nem reduzir música ou rádio"))
        val modes=arrayOf("mix","voice","beep","silent")
        val selector=Spinner(this)
        selector.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("Som + voz (MIX)","Somente voz","Somente bipe","Somente visual"))
        selector.setSelection(modes.indexOf(audio.mode()).coerceAtLeast(0))
        root.addView(selector)
        fun volume(label:String,initial:Int):SeekBar {
            root.addView(title(label))
            return SeekBar(this).apply {max=100;progress=initial;root.addView(this)}
        }
        val sound=volume("Efeitos: 65% padrão",(audio.soundGain()*100).toInt())
        val voice=volume("Voz: 80% padrão",(audio.voiceGain()*100).toInt())
        root.addView(title("Padrão do bip"))
        val tones=AlertTone.entries
        val toneSelector=Spinner(this).apply{adapter=ArrayAdapter(this@SettingsActivity,android.R.layout.simple_spinner_dropdown_item,tones.map{it.label});setSelection(tones.indexOf(audio.tone()))}
        root.addView(toneSelector)
        val frequencyLabel=title("")
        root.addView(frequencyLabel)
        val frequency=SeekBar(this).apply{max=1400;progress=audio.frequency()-400}
        root.addView(frequency)
        val durationLabel=title("")
        root.addView(durationLabel)
        val duration=SeekBar(this).apply{max=160;progress=audio.durationMs()-80}
        root.addView(duration)
        fun labels(){frequencyLabel.text="Frequência: ${frequency.progress+400} Hz";durationLabel.text="Duração de cada tom: ${duration.progress+80} ms"}
        val change=object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean){labels()};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}}
        frequency.setOnSeekBarChangeListener(change);duration.setOnSeekBarChangeListener(change);labels()
        root.addView(Button(this).apply{text="OUVIR BIP SELECIONADO";setOnClickListener{audio.previewTone(tones[toneSelector.selectedItemPosition],frequency.progress+400,duration.progress+80,sound.progress/100f)}})
        root.addView(Button(this).apply{text="TESTAR CÂMERA USB NATIVA — ARM 32 BITS";setOnClickListener{startActivity(android.content.Intent(this@SettingsActivity,NativeUsbActivity::class.java))}})
        root.addView(title("Duração dos clipes: 1, 3 ou 5 minutos"))
        val segments = intArrayOf(1,3,5)
        val segmentSelector = Spinner(this)
        segmentSelector.adapter = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("1 minuto","3 minutos","5 minutos"))
        val savedSegment = getSharedPreferences("camera",MODE_PRIVATE).getInt("segment",3)
        segmentSelector.setSelection(segments.indexOf(savedSegment).coerceAtLeast(0))
        root.addView(segmentSelector)
        root.addView(Button(this).apply {text="TESTAR AVISO (com música tocando)";setOnClickListener {audio.announce("Teste de áudio ADAS sem interromper a multimídia",true)}})
        root.addView(title("Recolher controles após inatividade (provisório, ajustável)"))
        val timeouts = intArrayOf(0, 5, 10, 15, 30)
        val timeoutSelector = Spinner(this)
        timeoutSelector.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Desativado", "5 segundos", "10 segundos", "15 segundos", "30 segundos"))
        timeoutSelector.setSelection(timeouts.indexOf(getSharedPreferences("camera", MODE_PRIVATE).getInt("controls_timeout", 0)).coerceAtLeast(0))
        root.addView(timeoutSelector)
        root.addView(title("Limite dos clipes comuns; protegidos não são apagados"))
        val storageLimits = intArrayOf(0, 512, 1024, 2048, 4096)
        val storageSelector = Spinner(this)
        storageSelector.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Sem exclusão automática", "512 MB", "1 GB", "2 GB", "4 GB"))
        storageSelector.setSelection(storageLimits.indexOf(getSharedPreferences("camera", MODE_PRIVATE).getInt("storage_mb", 0)).coerceAtLeast(0))
        root.addView(storageSelector)
        root.addView(title("ADAS EXPERIMENTAL — limiares provisórios, ajuste com o veículo parado"))
        val tuning=getSharedPreferences("adas_tuning",MODE_PRIVATE)
        fun numeric(label:String,value:String):EditText {
            root.addView(title(label))
            return EditText(this).apply {inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;setText(value);setTextColor(Color.WHITE);root.addView(this)}
        }
        val ldwSpeed=numeric("Velocidade mínima LDW (10–100 km/h)",tuning.getFloat("ldw_kmh",30f).toString())
        val fcwTtc=numeric("TTC experimental FCW (1–5 s)",tuning.getFloat("fcw_ttc",2.5f).toString())
        val confirmation=numeric("Confirmação temporal (300–2000 ms)",tuning.getLong("confirm_ms",900).toString())
        val cooldown=numeric("Intervalo mínimo entre avisos (5000–60000 ms)",tuning.getLong("cooldown_ms",10000).toString())
        val blink=numeric("Período do piscar (250–1000 ms)",tuning.getLong("blink_ms",500).toString())
        root.addView(Button(this).apply {text="SALVAR LIMITES ADAS";setOnClickListener {
            val speed=ldwSpeed.text.toString().replace(',','.').toFloatOrNull()
            val ttc=fcwTtc.text.toString().replace(',','.').toFloatOrNull()
            val confirm=confirmation.text.toString().toLongOrNull()
            val interval=cooldown.text.toString().toLongOrNull()
            val blinkPeriod=blink.text.toString().toLongOrNull()
            if(speed==null || speed !in 10f..100f || ttc==null || ttc !in 1f..5f || confirm==null || confirm !in 300..2000 || interval==null || interval !in 5000..60000 || blinkPeriod==null || blinkPeriod !in 250..1000) {
                Toast.makeText(this@SettingsActivity,"Limites inválidos; não salvos",Toast.LENGTH_SHORT).show()
            } else {
                tuning.edit().putFloat("ldw_kmh",speed).putFloat("fcw_ttc",ttc).putLong("confirm_ms",confirm).putLong("cooldown_ms",interval).putLong("blink_ms",blinkPeriod).apply()
                Toast.makeText(this@SettingsActivity,"Limites experimentais salvos",Toast.LENGTH_SHORT).show()
            }
        }})
        root.addView(Button(this).apply {text="SALVAR";setOnClickListener {audio.configure(modes[selector.selectedItemPosition],voice.progress,sound.progress,tones[toneSelector.selectedItemPosition],frequency.progress+400,duration.progress+80);getSharedPreferences("camera",MODE_PRIVATE).edit().putInt("segment",segments[segmentSelector.selectedItemPosition]).putInt("controls_timeout",timeouts[timeoutSelector.selectedItemPosition]).putInt("storage_mb",storageLimits[storageSelector.selectedItemPosition]).apply();Toast.makeText(this@SettingsActivity,"Preferências salvas",Toast.LENGTH_SHORT).show();finish()}})
        root.addView(title("Teste obrigatório: Spotify, Waze e rádio FM. O firmware da K2001N pode impedir a mistura com rádio."))
        setContentView(ScrollView(this).apply {addView(root)})
        SafeInsets.apply(this)
    }
    override fun onDestroy(){audio.release();super.onDestroy()}
}
