package br.com.dashcamadas

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import java.util.Locale

/** Four native tabs with persistent controls and the approved commercial cards. */
class SettingsActivity : Activity() {
    private lateinit var audio: AlertAudio
    private lateinit var page: CommercialPage
    private lateinit var status: TextView
    private var selected = "FAIXAS"
    private val dirtyTabs = mutableSetOf<String>()
    private val panels = linkedMapOf<String, LinearLayout>()
    private val tabs = linkedMapOf<String, Button>()
    private val saves = mutableMapOf<String, () -> Unit>()
    private val draftInts = mutableMapOf<String, () -> Int>()
    private val draftTexts = mutableMapOf<String, () -> String>()
    private val offsets = mutableMapOf<String, Int>()
    private var advancedOpen = false
    private var restored: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restored = savedInstanceState
        dirtyTabs.addAll(savedInstanceState?.getStringArrayList("dirty") ?: emptyList())
        audio = AlertAudio(this)
        page = CommercialPage(this, "Configurações", "Ajuste a experiência de condução com clareza e precisão.")
        status = page.label("Ajuste com o veículo parado", 12f, CommercialPage.MUTED)
        page.footer.addView(status, LinearLayout.LayoutParams(0, -2, 1f))
        page.footer.addView(page.action("Salvar ajustes", true) { saves.getValue(selected).invoke() })
        listOf(Triple("FAIXAS", "Assistente de faixa", "lane"), Triple("AUDIO", "Áudio e alertas", "mic"),
            Triple("CAMERA", "Câmera e gravação", "camera"), Triple("APARENCIA", "Aparência", "sun")).forEach { (id, label, icon) ->
            panels[id] = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
            page.body.addView(panels.getValue(id))
            tabs[id] = page.tab(label, icon) { select(id) }
        }
        lanes(panels.getValue("FAIXAS"))
        sound(panels.getValue("AUDIO"))
        camera(panels.getValue("CAMERA"))
        appearance(panels.getValue("APARENCIA"))
        select(savedInstanceState?.getString("selected") ?: intent.getStringExtra("section") ?: "FAIXAS")
        page.show()
    }
    private fun select(id: String) {
        offsets[selected] = page.scroll.scrollY
        selected = id.takeIf { it in panels } ?: "FAIXAS"
        panels.forEach { (key, panel) -> panel.visibility = if (key == selected) View.VISIBLE else View.GONE }
        page.selected(tabs.getValue(selected))
        page.scroll.post { page.scroll.scrollTo(0, offsets[selected] ?: 0) }
        status.text = if (selected in dirtyTabs) "Alterações ainda não salvas" else "Ajuste com o veículo parado"
    }
    private fun changed() { dirtyTabs += selected; status.text = "Alterações ainda não salvas" }
    private fun saved() { dirtyTabs -= selected; status.text = "Ajustes salvos"; NeonDialogs.notice(this, "Preferências salvas") }
    private fun slider(parent: LinearLayout, id: String, max: Int, initial: Int, value: (Int) -> String, after: (Int) -> Unit = {}): SeekBar {
        val output = page.label("", 27f).apply { setPadding(0, page.dp(10), 0, page.dp(6)) }
        parent.addView(output)
        val bar = SeekBar(this).apply {
            this.max = max; progress = (restored?.getInt(id, initial) ?: initial).coerceIn(0, max)
            contentDescription = when (id) {
                "sensitivity" -> "Sensibilidade do assistente de faixa"
                "thickness" -> "Espessura das linhas"
                "effects" -> "Volume dos efeitos"
                "voice" -> "Volume da voz"
                "frequency" -> "Frequência do bip"
                "duration" -> "Duração de cada tom"
                "neon_tone" -> "Ajuste de tom do neon"
                "neon_buttons" -> "Transparência do fundo dos botões"
                "neon_popups" -> "Transparência do fundo dos popups"
                else -> id
            }
            progressTintList = ColorStateList.valueOf(PremiumVisuals.CYAN)
            thumbTintList = ColorStateList.valueOf(PremiumVisuals.CYAN)
        }
        output.text = value(bar.progress); after(bar.progress)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) { output.text = value(p); after(p); if (user) changed() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        parent.addView(bar, LinearLayout.LayoutParams(-1, page.dp(48)))
        draftInts[id] = { bar.progress }
        return bar
    }
    private fun picker(parent: LinearLayout, id: String, labels: List<String>, initial: Int): Spinner {
        val view = Spinner(NeonDialogs.themed(this), Spinner.MODE_DROPDOWN)
        view.adapter = ArrayAdapter(NeonDialogs.themed(this), android.R.layout.simple_spinner_dropdown_item, labels)
        view.setSelection((restored?.getInt(id, initial) ?: initial).coerceIn(0, labels.lastIndex))
        var previous = view.selectedItemPosition
        view.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, item: View?, position: Int, id: Long) {
                if (position != previous) { previous = position; changed() }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        view.minimumHeight = page.dp(52)
        view.setPopupBackgroundDrawable(NeonSurface(this))
        parent.addView(view, LinearLayout.LayoutParams(-1, -2))
        draftInts[id] = { view.selectedItemPosition }
        return view
    }
    private fun numeric(parent: LinearLayout, id: String, label: String, initial: String): EditText {
        parent.addView(page.label(label, 14f, CommercialPage.MUTED).apply { setPadding(0, page.dp(12), 0, page.dp(4)) })
        val view = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(restored?.getString(id) ?: initial); setTextColor(PremiumVisuals.WHITE)
            backgroundTintList = ColorStateList.valueOf(PremiumVisuals.CYAN)
            minimumHeight = page.dp(48); contentDescription = label; isSingleLine = true
        }
        view.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        parent.addView(view); draftTexts[id] = { view.text.toString() }
        return view
    }
    private fun lanes(parent: LinearLayout) {
        val prefs = getSharedPreferences("adas_tuning", MODE_PRIVATE)
        val sensitivityCard = page.card("Sensibilidade", "Ajuste quando o aviso de proximidade da borda aparece.", "lane")
        val sensitivity = slider(sensitivityCard, "sensitivity", 100, prefs.getInt("lane_sensitivity", 50), { "$it%" })
        sensitivityCard.addView(page.label("Mais tolerante  ·  Mais sensível", 12f, CommercialPage.MUTED))
        val thicknessCard = page.card("Espessura das linhas", "Escolha como as faixas aparecem na sua tela.", "gear")
        val sample = LaneStrokeSampleView(this)
        val thickness = slider(thicknessCard, "thickness", 22, ((prefs.getFloat("lane_width_dp", 4f).coerceIn(1f, 12f) - 1f) * 2).toInt(),
            { String.format(Locale("pt", "BR"), "%.1f dp", 1f + it / 2f) }, { sample.strokeDp = 1f + it / 2f })
        thicknessCard.addView(sample, LinearLayout.LayoutParams(-1, page.dp(76)))
        thicknessCard.addView(page.label("EXEMPLO VISUAL DO TRAÇO", 10f, CommercialPage.MUTED))
        page.pair(parent, sensitivityCard, thicknessCard)
        val calibration = page.card("Calibração da câmera", "Alinhe o assistente à posição real da câmera, com o veículo parado.", "camera")
        calibration.addView(page.action("Abrir LIVE para calibrar") { startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)) })
        page.addCard(parent, calibration)
        page.note(parent, "Os avisos exigem GPS, calibração, confiança e imagem recente. A espessura muda somente o desenho.", true)
        val advanced = page.card("Limites avançados", "Ajustes técnicos do assistente experimental.", "gear")
        val fields = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val speed = numeric(fields, "speed", "Velocidade mínima LDW · 10–100 km/h", prefs.getFloat("ldw_kmh", 30f).toString())
        val ttc = numeric(fields, "ttc", "TTC experimental FCW · 1–5 s", prefs.getFloat("fcw_ttc", 2.5f).toString())
        val confirmation = numeric(fields, "confirmation", "Confirmação temporal · 300–2000 ms", prefs.getLong("confirm_ms", 900).toString())
        val cooldown = numeric(fields, "cooldown", "Intervalo entre avisos · 5000–60000 ms", prefs.getLong("cooldown_ms", 10000).toString())
        val blink = numeric(fields, "blink", "Período do piscar · 250–1000 ms", prefs.getLong("blink_ms", 500).toString())
        advancedOpen = restored?.getBoolean("advanced", false) ?: false
        fields.visibility = if (advancedOpen) View.VISIBLE else View.GONE
        val expand = page.action(if (advancedOpen) "Ocultar limites" else "Mostrar limites") {}
        expand.setOnClickListener {
            advancedOpen = !advancedOpen; fields.visibility = if (advancedOpen) View.VISIBLE else View.GONE
            expand.text = if (advancedOpen) "Ocultar limites" else "Mostrar limites"
        }
        advanced.addView(expand); advanced.addView(fields); page.addCard(parent, advanced)
        saves["FAIXAS"] = save@{
            if ((DrivingContext.speed() ?: 0f) > .5f) { NeonDialogs.notice(this, "Pare o veículo para ajustar o assistente"); return@save }
            val parsed = AdasSettingsValues.parse(speed.text.toString(), ttc.text.toString(), confirmation.text.toString(), cooldown.text.toString(), blink.text.toString())
            if (parsed == null) {
                fields.visibility = View.VISIBLE; advancedOpen = true; expand.text = "Ocultar limites"
                NeonDialogs.notice(this, "Confira os limites avançados; ajustes não salvos")
            } else {
                prefs.edit().putFloat("ldw_kmh", parsed.speed).putFloat("fcw_ttc", parsed.ttc).putLong("confirm_ms", parsed.confirmation)
                    .putLong("cooldown_ms", parsed.cooldown).putLong("blink_ms", parsed.blink)
                    .putInt("lane_sensitivity", sensitivity.progress).putFloat("lane_width_dp", 1f + thickness.progress / 2f).apply()
                saved()
            }
        }
    }
    private fun sound(parent: LinearLayout) {
        val modes = listOf("mix", "voice", "beep", "silent")
        val modeCard = page.card("Como receber avisos", "Voz, bip ou somente aviso visual.", "mic")
        val mode = picker(modeCard, "mode", listOf("Som + voz (MIX)", "Somente voz", "Somente bip", "Somente visual"), modes.indexOf(audio.mode()).coerceAtLeast(0))
        val effects = slider(modeCard, "effects", 100, (audio.soundGain() * 100).toInt(), { "Efeitos · $it%" })
        val voice = slider(modeCard, "voice", 100, (audio.voiceGain() * 100).toInt(), { "Voz · $it%" })
        val toneCard = page.card("Som do bip", "Personalize o tom para reconhecê-lo facilmente.", "sun")
        val tones = AlertTone.entries
        val tone = picker(toneCard, "tone", tones.map { it.label }, tones.indexOf(audio.tone()))
        val frequency = slider(toneCard, "frequency", 1400, audio.frequency() - 400, { "${it + 400} Hz" })
        val duration = slider(toneCard, "duration", 160, audio.durationMs() - 80, { "${it + 80} ms por tom" })
        toneCard.addView(page.action("Ouvir bip selecionado") { audio.previewTone(tones[tone.selectedItemPosition], frequency.progress + 400, duration.progress + 80, effects.progress / 100f) })
        page.pair(parent, modeCard, toneCard)
        val mixing = page.card("Música e rádio", "Os avisos não solicitam foco de áudio. A mistura com rádio FM depende da central.", "media")
        mixing.addView(page.action("Testar aviso com música") { audio.announce("Teste de áudio ADAS sem interromper a multimídia", true) })
        page.addCard(parent, mixing)
        saves["AUDIO"] = { audio.configure(modes[mode.selectedItemPosition], voice.progress, effects.progress, tones[tone.selectedItemPosition], frequency.progress + 400, duration.progress + 80); saved() }
    }
    private fun camera(parent: LinearLayout) {
        val prefs = getSharedPreferences("camera", MODE_PRIVATE)
        val segments = listOf(1, 3, 5)
        val segmentCard = page.card("Segmentos de gravação", "Vídeos menores facilitam localizar cada trecho.", "camera")
        val segment = picker(segmentCard, "segment", listOf("1 minuto", "3 minutos", "5 minutos"), segments.indexOf(prefs.getInt("segment", 3)))
        val limits = listOf(0, 512, 1024, 2048, 4096)
        val storageCard = page.card("Armazenamento", "Clipes protegidos ficam fora da limpeza automática.", "folder")
        val storage = picker(storageCard, "storage", listOf("Sem exclusão automática", "512 MB", "1 GB", "2 GB", "4 GB"), limits.indexOf(prefs.getInt("storage_mb", 0)))
        page.pair(parent, segmentCard, storageCard)
        val orientation = page.card("Orientação da câmera", "Rotação, espelhamento e seleção de fonte ficam no menu da câmera da LIVE.", "camera")
        orientation.addView(page.action("Abrir controles na LIVE") { startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)) })
        if (BuildConfig.K2000_NATIVE && Build.VERSION.SDK_INT in 24..27) orientation.addView(page.action("Autorizar câmera USB") { startActivity(Intent(this, NativeUsbActivity::class.java)) })
        page.addCard(parent, orientation)
        saves["CAMERA"] = { prefs.edit().putInt("segment", segments[segment.selectedItemPosition]).putInt("storage_mb", limits[storage.selectedItemPosition]).apply(); saved() }
    }
    private fun appearance(parent: LinearLayout) {
        val prefs = getSharedPreferences("ui_appearance", MODE_PRIVATE)
        val identity = page.card("Identidade visual", "Tema neon, superfícies em vidro e controles de leitura.", "sun")
        fun toggle(id: String, title: String, initial: Boolean, host: LinearLayout = identity): Switch {
            val view = Switch(this).apply { text = title; setTextColor(PremiumVisuals.WHITE); textSize = 15f; minimumHeight = page.dp(52)
                thumbTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(PremiumVisuals.CYAN, CommercialPage.MUTED))
                trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(Color.rgb(7, 78, 111), Color.rgb(49, 66, 88)))
                isChecked = (restored?.getInt(id, if (initial) 1 else 0) ?: if (initial) 1 else 0) == 1
                setOnCheckedChangeListener { _, _ -> changed() } }
            host.addView(view); draftInts[id] = { if (view.isChecked) 1 else 0 }; return view
        }
        val theme = NeonTheme.read(this)
        val paletteCard = page.card("Tema neon", "Escolha a cor e ajuste o tom antes de salvar.", "sun")
        val palette = picker(paletteCard, "neon_selection", NeonColor.entries.map { it.title }, theme.color.ordinal)
        palette.contentDescription = "Cor do tema neon"
        var previewChanged: (() -> Unit)? = null
        val tone = slider(paletteCard, "neon_tone", 60, theme.tone + 30, { "Tom · ${it - 30}°" }, { previewChanged?.invoke() })
        val opacityCard = page.card("Transparência", "Ajusta só o fundo; textos, ícones e bordas permanecem legíveis.", "gear")
        val buttons = slider(opacityCard, "neon_buttons", 80, theme.buttonTransparency, { "Botões · $it%" }, { previewChanged?.invoke() })
        val popups = slider(opacityCard, "neon_popups", 80, theme.popupTransparency, { "Popups · $it%" }, { previewChanged?.invoke() })
        page.pair(parent, paletteCard, opacityCard)
        val sampleCard = page.card("Prévia do tema", "O quadriculado permite conferir a transparência do fundo.", "gear")
        val follows = toggle("neon_follow", "Popups comuns acompanham o tema", theme.popupFollowsTheme, sampleCard)
        val preview = NeonThemePreviewView(this)
        fun selectedTheme() = NeonThemeSettings(NeonColor.entries[palette.selectedItemPosition], tone.progress - 30, buttons.progress, popups.progress, follows.isChecked)
        previewChanged = { preview.settings = selectedTheme() }
        sampleCard.addView(preview, LinearLayout.LayoutParams(-1, -2))
        val selectionListener = palette.onItemSelectedListener
        palette.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectionListener?.onItemSelected(parent, view, position, id); previewChanged?.invoke()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        follows.setOnCheckedChangeListener { _, _ -> changed(); previewChanged?.invoke() }
        sampleCard.addView(page.action("Restaurar cores e transparência") {
            palette.setSelection(0); tone.progress = 30; buttons.progress = 0; popups.progress = 0; follows.isChecked = false
            changed(); previewChanged?.invoke()
        })
        page.addCard(parent, sampleCard)
        previewChanged.invoke()
        page.note(parent, "Banners de evento permanecem vermelhos. Avisos críticos mantêm pelo menos 75% de opacidade no fundo.")
        val night = toggle("night", "Modo noturno", prefs.getBoolean("night", false))
        identity.addView(page.label("Reduz o brilho somente enquanto o aplicativo está aberto.", 12f, CommercialPage.MUTED))
        val animations = toggle("animations", "Animações suaves", prefs.getBoolean("animations", true))
        identity.addView(page.label("Desative para reduzir o movimento decorativo do HUD. Os avisos continuam ativos.", 12f, CommercialPage.MUTED))
        page.addCard(parent, identity)
        val controls = page.card("Controles da LIVE", "Recolha os controles após um período sem interação.", "gear")
        val times = listOf(0, 5, 10, 15, 30)
        val timeout = picker(controls, "timeout", listOf("Sempre visíveis", "5 segundos", "10 segundos", "15 segundos", "30 segundos"), times.indexOf(getSharedPreferences("camera", MODE_PRIVATE).getInt("controls_timeout", 0)))
        page.addCard(parent, controls)
        val device = page.card("Sobre o dispositivo", "Consulte a versão Android e as fontes de câmera disponíveis.", "device")
        device.addView(page.action("Informações do aparelho") { startActivity(Intent(this, PremiumSectionActivity::class.java).putExtra("section", "CONFIG")) })
        page.addCard(parent, device)
        page.note(parent, "A borda vermelha e o ícone do evento preservam a identidade dos popups aprovados.", true)
        saves["APARENCIA"] = {
            prefs.edit().putBoolean("night", night.isChecked).putBoolean("animations", animations.isChecked).apply()
            getSharedPreferences("camera", MODE_PRIVATE).edit().putInt("controls_timeout", times[timeout.selectedItemPosition]).apply()
            NeonTheme.save(this, selectedTheme())
            CommercialUi.applyAppearance(this); saved()
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("dirty", ArrayList(dirtyTabs))
        outState.putString("selected", selected); outState.putBoolean("advanced", advancedOpen)
        draftInts.forEach { (id, value) -> outState.putInt(id, value()) }
        draftTexts.forEach { (id, value) -> outState.putString(id, value()) }
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() { if (::audio.isInitialized) audio.release(); super.onDestroy() }
}

/** Illustrative line sample, separate from the real neural camera overlay. */
private class LaneStrokeSampleView(context: android.content.Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var strokeDp = 4f
        set(value) { field = value; invalidate() }
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.rgb(8, 22, 36))
        paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(20, 216, 255); paint.strokeWidth = strokeDp * resources.displayMetrics.density
        canvas.drawLine(width * .4f, 0f, width * .22f, height.toFloat(), paint)
        canvas.drawLine(width * .6f, 0f, width * .78f, height.toFloat(), paint)
    }
}
