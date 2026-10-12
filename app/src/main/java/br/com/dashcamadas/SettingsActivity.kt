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
        fun title(t:String)=TextView(this).apply {text=t;textSize=18f;CommercialUi.heading(this)}
        root.addView(title("FAIXAS, ÁUDIO E CÂMERA — NWD K2001N"))
        root.addView(title("Nunca solicitar foco de áudio nem reduzir música ou rádio"))
        val modes=arrayOf("mix","voice","beep","silent")
        val selector=Spinner(NeonDialogs.themed(this),Spinner.MODE_DROPDOWN)
        selector.adapter=ArrayAdapter(NeonDialogs.themed(this),android.R.layout.simple_spinner_dropdown_item,arrayOf("Som + voz (MIX)","Somente voz","Somente bipe","Somente visual"))
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
        val toneSelector=Spinner(NeonDialogs.themed(this),Spinner.MODE_DROPDOWN).apply{adapter=ArrayAdapter(NeonDialogs.themed(this@SettingsActivity),android.R.layout.simple_spinner_dropdown_item,tones.map{it.label});setSelection(tones.indexOf(audio.tone()))}
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
        root.addView(Button(this).apply{text="Ouvir bip selecionado";setOnClickListener{audio.previewTone(tones[toneSelector.selectedItemPosition],frequency.progress+400,duration.progress+80,sound.progress/100f)}})
        root.addView(Button(this).apply{text="Autorizar câmera USB";setOnClickListener{startActivity(android.content.Intent(this@SettingsActivity,NativeUsbActivity::class.java))}})
        root.addView(title("Duração dos clipes: 1, 3 ou 5 minutos"))
        val segments = intArrayOf(1,3,5)
        val segmentSelector = Spinner(NeonDialogs.themed(this),Spinner.MODE_DROPDOWN)
        segmentSelector.adapter = ArrayAdapter(NeonDialogs.themed(this),android.R.layout.simple_spinner_dropdown_item,arrayOf("1 minuto","3 minutos","5 minutos"))
        val savedSegment = getSharedPreferences("camera",MODE_PRIVATE).getInt("segment",3)
        segmentSelector.setSelection(segments.indexOf(savedSegment).coerceAtLeast(0))
        root.addView(segmentSelector)
        root.addView(Button(this).apply {text="Testar aviso com música";setOnClickListener {audio.announce("Teste de áudio ADAS sem interromper a multimídia",true)}})
        root.addView(title("Recolher controles após inatividade (provisório, ajustável)"))
        val timeouts = intArrayOf(0, 5, 10, 15, 30)
        val timeoutSelector = Spinner(NeonDialogs.themed(this),Spinner.MODE_DROPDOWN)
        timeoutSelector.adapter = ArrayAdapter(NeonDialogs.themed(this), android.R.layout.simple_spinner_dropdown_item, arrayOf("Desativado", "5 segundos", "10 segundos", "15 segundos", "30 segundos"))
        timeoutSelector.setSelection(timeouts.indexOf(getSharedPreferences("camera", MODE_PRIVATE).getInt("controls_timeout", 0)).coerceAtLeast(0))
        root.addView(timeoutSelector)
        root.addView(title("Limite dos clipes comuns; protegidos não são apagados"))
        val storageLimits = intArrayOf(0, 512, 1024, 2048, 4096)
        val storageSelector = Spinner(NeonDialogs.themed(this),Spinner.MODE_DROPDOWN)
        storageSelector.adapter = ArrayAdapter(NeonDialogs.themed(this), android.R.layout.simple_spinner_dropdown_item, arrayOf("Sem exclusão automática", "512 MB", "1 GB", "2 GB", "4 GB"))
        storageSelector.setSelection(storageLimits.indexOf(getSharedPreferences("camera", MODE_PRIVATE).getInt("storage_mb", 0)).coerceAtLeast(0))
        root.addView(storageSelector)
        val adasHeading=title("ASSISTENTE DE FAIXA — ajuste com o veículo parado");root.addView(adasHeading)
        val tuning=getSharedPreferences("adas_tuning",MODE_PRIVATE)
        val sensitivityLabel=title("");root.addView(sensitivityLabel)
        val sensitivity=SeekBar(this).apply { max=100;progress=tuning.getInt("lane_sensitivity",50).coerceIn(0,100) };root.addView(sensitivity)
        val thicknessLabel=title("");root.addView(thicknessLabel)
        val thickness=SeekBar(this).apply { max=22;progress=((tuning.getFloat("lane_width_dp",4f).coerceIn(1f,12f)-1f)*2).toInt() };root.addView(thickness)
        fun laneLabels(){sensitivityLabel.text="Sensibilidade LDW: ${sensitivity.progress}%";thicknessLabel.text="Espessura das faixas: ${1f+thickness.progress/2f} dp"}
        val laneChange=object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean){laneLabels()};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}}
        sensitivity.setOnSeekBarChangeListener(laneChange);thickness.setOnSeekBarChangeListener(laneChange);laneLabels()
        root.addView(title("Maior sensibilidade antecipa a proximidade da borda; mantém GPS, calibração e confiança obrigatórios. A espessura muda somente o desenho."))
        fun numeric(label:String,value:String):EditText {
            root.addView(title(label))
            return EditText(this).apply {inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;setText(value);setTextColor(Color.WHITE);root.addView(this)}
        }
        val ldwSpeed=numeric("Velocidade mínima LDW (10–100 km/h)",tuning.getFloat("ldw_kmh",30f).toString())
        val fcwTtc=numeric("TTC experimental FCW (1–5 s)",tuning.getFloat("fcw_ttc",2.5f).toString())
        val confirmation=numeric("Confirmação temporal (300–2000 ms)",tuning.getLong("confirm_ms",900).toString())
        val cooldown=numeric("Intervalo mínimo entre avisos (5000–60000 ms)",tuning.getLong("cooldown_ms",10000).toString())
        val blink=numeric("Período do piscar (250–1000 ms)",tuning.getLong("blink_ms",500).toString())
        root.addView(Button(this).apply {text="Salvar ajustes do assistente";setOnClickListener {
            if((DrivingContext.speed() ?: 0f)>.5f){NeonDialogs.notice(this@SettingsActivity,"Pare o veículo para ajustar o assistente");return@setOnClickListener}
            val speed=ldwSpeed.text.toString().replace(',','.').toFloatOrNull()
            val ttc=fcwTtc.text.toString().replace(',','.').toFloatOrNull()
            val confirm=confirmation.text.toString().toLongOrNull()
            val interval=cooldown.text.toString().toLongOrNull()
            val blinkPeriod=blink.text.toString().toLongOrNull()
            if(speed==null || speed !in 10f..100f || ttc==null || ttc !in 1f..5f || confirm==null || confirm !in 300..2000 || interval==null || interval !in 5000..60000 || blinkPeriod==null || blinkPeriod !in 250..1000) {
                NeonDialogs.notice(this@SettingsActivity,"Limites inválidos; não salvos")
            } else {
                tuning.edit().putFloat("ldw_kmh",speed).putFloat("fcw_ttc",ttc).putLong("confirm_ms",confirm).putLong("cooldown_ms",interval).putLong("blink_ms",blinkPeriod).putInt("lane_sensitivity",sensitivity.progress).putFloat("lane_width_dp",1f+thickness.progress/2f).apply()
                NeonDialogs.notice(this@SettingsActivity,"Limites experimentais salvos")
            }
        }})
        root.addView(Button(this).apply {text="Salvar áudio e câmera";setOnClickListener {audio.configure(modes[selector.selectedItemPosition],voice.progress,sound.progress,tones[toneSelector.selectedItemPosition],frequency.progress+400,duration.progress+80);getSharedPreferences("camera",MODE_PRIVATE).edit().putInt("segment",segments[segmentSelector.selectedItemPosition]).putInt("controls_timeout",timeouts[timeoutSelector.selectedItemPosition]).putInt("storage_mb",storageLimits[storageSelector.selectedItemPosition]).apply();NeonDialogs.notice(this@SettingsActivity,"Preferências salvas");finish()}})
        root.addView(title("Teste obrigatório: Spotify, Waze e rádio FM. O firmware da K2001N pode impedir a mistura com rádio."))
        val scroll=ScrollView(this).apply { addView(root) };setContentView(scroll)
        if(intent.getStringExtra("section")=="FAIXAS")scroll.post { scroll.smoothScrollTo(0,adasHeading.top) }
        NeonDialogs.styleContent(root)
        CommercialUi.buttonsIn(root)
        SafeInsets.apply(this)
    }
    override fun onDestroy(){audio.release();super.onDestroy()}
}
