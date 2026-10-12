package br.com.dashcamadas

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Commercial cards show actual local data; demonstrations retain explicit TESTE labels. */
class PremiumSectionActivity : Activity() {
    private lateinit var page: CommercialPage
    private lateinit var audio: AlertAudio
    private lateinit var events: EventStore
    private var activeBanner: PopupWindow? = null
    private val bannerHandler = android.os.Handler(android.os.Looper.getMainLooper())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = AlertAudio(this); events = EventStore(this)
        val section = intent.getStringExtra("section") ?: "ADAS"
        val title = when (section) { "MIDIA" -> "Seus vídeos"; "EVENTOS" -> "Eventos"; "RADARES" -> "Radares"; "CONFIG" -> "Dispositivo e câmera"; "PLACAS" -> "Placas · demonstração"; "SEMAFOROS" -> "Semáforos · demonstração"; else -> "Assistência de condução" }
        val subtitle = when (section) { "MIDIA" -> "Gravações e clipes protegidos, organizados em um só lugar."; "EVENTOS" -> "Consulte os registros locais da sua condução."; "RADARES" -> "Gerencie os pontos cadastrados no seu GPS."; "CONFIG" -> "Consulte as fontes disponíveis e informações do aparelho."; else -> "Informações e controles do assistente." }
        page = CommercialPage(this, title, subtitle)
        when (section) {
            "MIDIA" -> mediaPage(page.body)
            "EVENTOS" -> eventsPage(page.body)
            "RADARES" -> radarPage(page.body)
            "CONFIG" -> devicePage(page.body)
            else -> detectionPage(page.body, section)
        }
        page.show()
    }
    private fun live() { startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
    private fun detectionPage(parent: LinearLayout, section: String) {
        if (section == "ADAS") {
            val lanes = page.card("Assistente de faixa", "Faixas e área dirigível são analisadas por uma rede neural local. Alertas exigem GPS, calibração e resultados recentes.", "lane")
            lanes.addView(page.action("Abrir LIVE para calibrar") { live() })
            val objects = page.card("Veículos e pedestres", "Detecções locais alimentam caixas e avisos experimentais. O aviso de pedestre exige confirmação e suporte da área dirigível.", "car")
            objects.addView(page.action("Ajustar o assistente") { startActivity(Intent(this, SettingsActivity::class.java).putExtra("section", "FAIXAS")) })
            page.pair(parent, lanes, objects)
            page.note(parent, "Funcionamento e desempenho na central ainda precisam de validação física. Alertas de ciclistas continuam pendentes.")
            return
        }
        page.note(parent, "Demonstração: reconhecimento automático destas placas/sinais ainda não está implementado. Os avisos abaixo são registrados como TESTE.", true)
        val choices = if (section == "PLACAS") listOf("Limite de velocidade" to "Atenção. Velocidade acima do limite.", "Placa PARE" to "Atenção! Placa de parada obrigatória.", "Entrada proibida" to "Atenção! Entrada proibida.")
            else listOf("Semáforo fechado" to "Atenção. Semáforo fechado.", "Semáforo amarelo" to "Atenção. Semáforo amarelo.", "Semáforo aberto" to "Semáforo aberto.")
        choices.forEach { (label, voice) ->
            val card = page.card(label, "Aviso demonstrativo, sem detecção automática.", if (section == "SEMAFOROS") "traffic" else "warning")
            card.addView(page.action("Testar popup") { events.add(label, "Aviso demonstrativo: $voice"); audio.announce(voice, true); showTestBanner(label, voice) })
            page.addCard(parent, card)
        }
    }
    private fun radarPage(parent: LinearLayout) {
        val radars = RadarStore(this).all()
        val overview = page.card("${radars.size} pontos cadastrados", "A base é local, com pontos GPS adicionados manualmente.", "radar_wave")
        overview.addView(page.action("Gerenciar radares na LIVE") { live() }); page.addCard(parent, overview)
        if (radars.isEmpty()) page.note(parent, "Nenhum radar cadastrado. Na LIVE, use o botão RADARES para adicionar um ponto.")
        radars.forEach {
            val title = when (it.type) { "fixo" -> "Radar fixo"; "movel" -> "Radar móvel"; "semaforo" -> "Radar semafórico"; else -> "Radar cadastrado" }
            page.addCard(parent, page.card(title, "${it.limit} km/h · ${it.lat}, ${it.lon}", "radar"))
        }
    }
    private fun eventsPage(parent: LinearLayout) {
        val data = events.all()
        val summary = page.card("${data.length()} registros", "Histórico local. Avisos de teste mantêm sua identificação de origem.", "folder")
        if (data.length() > 0) summary.addView(page.action("Limpar histórico") {
            NeonDialogs.Builder(this).setTitle("Excluir eventos locais?").setMessage("Esta ação remove os registros do histórico.")
                .setNegativeButton("Cancelar", null).setPositiveButton("Excluir") { _, _ -> events.clear(); recreate() }.show()
        })
        page.addCard(parent, summary)
        if (data.length() == 0) page.note(parent, "Seu histórico está vazio. Os eventos registrados pelo aplicativo aparecerão aqui.")
        val date = SimpleDateFormat("dd/MM · HH:mm:ss", Locale("pt", "BR"))
        for (index in data.length() - 1 downTo 0) {
            val entry = data.optJSONObject(index) ?: continue
            val source = entry.optString("origem", "Origem indisponível")
            val time = entry.optLong("timestamp", 0)
            val card = page.card(entry.optString("tipo", "Evento"), "${if (time > 0) date.format(Date(time)) else "Horário indisponível"} · $source", "warning")
            card.addView(page.label(entry.optString("detalhes"), 14f, CommercialPage.MUTED))
            page.addCard(parent, card)
        }
    }
    private fun mediaPage(parent: LinearLayout) {
        page.note(parent, "Seus vídeos ficam no armazenamento do aplicativo, sem sincronização com nuvem.")
        listOf("clips" to "Gravações", "protegidos" to "Clipes protegidos").forEach { (folder, title) ->
            val directory = getExternalFilesDir(null)?.let { File(it, folder) }
            val files = directory?.listFiles()?.filter { it.isFile && it.extension == "mp4" && !File(it.parentFile, it.name + ".lock").exists() }?.sortedByDescending { it.lastModified() } ?: emptyList()
            val card = page.card(title, "${files.size} vídeo(s)${if (folder == "protegidos") " · preservados na limpeza automática" else ""}", "media")
            if (files.isEmpty()) card.addView(page.label("Nenhum vídeo finalizado nesta pasta.", 14f, CommercialPage.MUTED))
            files.forEach { file ->
                val date = SimpleDateFormat("dd/MM · HH:mm", Locale("pt", "BR")).format(Date(file.lastModified()))
                val size = String.format(Locale("pt", "BR"), "%.1f MB", file.length() / 1_000_000.0)
                card.addView(page.action("$date  ·  $size\n${file.name}") {
                    val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.clips", file)
                    try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                    catch (_: Exception) { NeonDialogs.Builder(this).setTitle("Abrir vídeo").setMessage("Instale um reprodutor de vídeo compatível para abrir este clipe.").setPositiveButton("OK", null).show() }
                }.apply { textSize = 13f; setPadding(page.dp(12), page.dp(12), page.dp(12), page.dp(12)) }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = page.dp(8) })
            }
            page.addCard(parent, card)
        }
    }
    private fun devicePage(parent: LinearLayout) {
        val camera = page.card("Fontes de câmera", "Selecione a fonte disponível na LIVE. A autorização USB específica depende do perfil e da versão Android.", "camera")
        camera.addView(page.action("Selecionar câmera na LIVE") { live() })
        if (BuildConfig.K2000_NATIVE && android.os.Build.VERSION.SDK_INT in 24..27) camera.addView(page.action("Autorizar câmera USB") { startActivity(Intent(this, NativeUsbActivity::class.java)) })
        page.addCard(parent, camera)
        val report = DeviceCapabilities.inspect(this)
        val device = page.card("${report.manufacturer} ${report.model}", "Android ${android.os.Build.VERSION.RELEASE} · API ${report.sdk}", "device")
        val details = page.label(report.asText(), 13f, CommercialPage.MUTED).apply { visibility = android.view.View.GONE }
        device.addView(page.action("Mostrar diagnóstico") { details.visibility = if (details.visibility == android.view.View.VISIBLE) android.view.View.GONE else android.view.View.VISIBLE })
        device.addView(details); page.addCard(parent, device)
    }
    private fun showTestBanner(label: String, voice: String) {
        activeBanner?.dismiss()
        val icon = when { label.contains("Semáforo", true) -> "traffic"; label.contains("Pedestre", true) -> "pedestrian"; label.contains("Ciclista", true) -> "cyclist"; label.contains("faixa", true) -> "lane"; label.contains("colisão", true) -> "car"; else -> "warning" }
        val banner = EventPopupView(this, page.visuals).apply { presentation = EventPresentation("TESTE • ${label.uppercase(Locale("pt", "BR"))}", voice, icon, true) }
        val width = (resources.displayMetrics.widthPixels * .64f).toInt()
        val popup = PopupWindow(banner, width, (width * 194f / 752f).toInt(), false)
        popup.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)); popup.isOutsideTouchable = false
        activeBanner = popup; popup.showAtLocation(page.root, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, page.dp(32))
        bannerHandler.postDelayed({ if (activeBanner === popup) { popup.dismiss(); activeBanner = null } }, 3500)
    }
    override fun onResume() { super.onResume(); CommercialUi.applyAppearance(this) }
    override fun onDestroy() { activeBanner?.dismiss(); bannerHandler.removeCallbacksAndMessages(null); if (::audio.isInitialized) audio.release(); super.onDestroy() }
}
