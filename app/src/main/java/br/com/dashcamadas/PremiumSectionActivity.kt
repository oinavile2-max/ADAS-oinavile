package br.com.dashcamadas

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File

/** Páginas nativas de diagnóstico, testes, eventos e configurações.
 * Nenhuma IA fictícia é apresentada como detecção verdadeira. */
class PremiumSectionActivity:Activity() {
 private lateinit var root:LinearLayout
 private lateinit var audio:AlertAudio
 private lateinit var events:EventStore
 private val blue=Color.rgb(61,195,255)
 private val white=Color.WHITE
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  audio=AlertAudio(this);events=EventStore(this)
  val section=intent.getStringExtra("section") ?: "ADAS"
  root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(28,16,28,12);setBackgroundColor(Color.rgb(7,13,24)) }
  val bar=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  bar.addView(button("‹ MENU") {finish()})
  bar.addView(text("  DASHCAM ADAS PRO  /  $section",23f,blue),LinearLayout.LayoutParams(0,-2,1f))
  root.addView(bar)
  val scroll=ScrollView(this);val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(4,10,4,10)}
  scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root);SafeInsets.apply(this)
  when(section) {
   "ADAS","PLACAS","SEMAFOROS" -> detectionPage(body,section)
   "RADARES" -> radarPage(body)
   "EVENTOS" -> eventsPage(body)
   "MIDIA" -> mediaPage(body)
   "CONFIG" -> configPage(body)
   else -> detectionPage(body,section)
  }
 }
 private fun text(s:String,size:Float=17f,color:Int=white):TextView=TextView(this).apply{text=s;textSize=size;setTextColor(color);setPadding(10,10,10,10)}
 private fun button(s:String,click:()->Unit):Button=Button(this).apply{text=s;setTextColor(white);background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(42,75,104),Color.rgb(16,29,47))).apply{cornerRadius=18f;setStroke(2,blue)};setOnClickListener{click()}}
 private fun item(parent:LinearLayout,s:String,click:()->Unit){val density=resources.displayMetrics.density;parent.addView(button(s,click).apply{minHeight=(48*density).toInt();setPadding((12*density).toInt(),(10*density).toInt(),(12*density).toInt(),(10*density).toInt())},LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=(8*density).toInt()})}
 private fun note(parent:LinearLayout,s:String){parent.addView(text(s,16f,Color.rgb(169,192,213)))}
 private fun detectionPage(p:LinearLayout,section:String){
  if(section=="ADAS") {
   note(p,"Redes neurais locais detectam faixas/área dirigível e veículos. Camera2 e USB autorizado no APK ARM32 alimentam a mesma LIVE e gravação em serviço. Alertas LDW/FCW são experimentais e exigem calibração, GPS válido e resultados recentes.")
   item(p,"ABRIR LIVE — CALIBRAR NO BOTÃO ADAS") {startActivity(Intent(this,MainActivity::class.java))}
   note(p,"Reconhecimento com alertas de pedestres e ciclistas ainda não integrado.")
   return
  }
  note(p,"Reconhecimento automático por IA: NÃO IMPLEMENTADO. Os botões abaixo simulam o aviso e registram como TESTE, nunca como infração real.")
  val options=when(section){
   "PLACAS" -> listOf("Limite de velocidade" to "Atenção. Velocidade acima do limite.","Placa PARE" to "Atenção! Placa de parada obrigatória.","Entrada proibida" to "Atenção! Entrada proibida.")
   "SEMAFOROS" -> listOf("Semáforo fechado" to "Atenção. Semáforo fechado.","Semáforo amarelo" to "Atenção. Semáforo amarelo.","Semáforo aberto" to "Semáforo aberto.")
   else -> listOf("Risco de colisão" to "Atenção! Risco de colisão à frente!","Pedestre" to "Atenção! Pedestre à frente!","Ciclista" to "Atenção! Ciclista à frente!","Saída de faixa" to "Atenção, saída de faixa.","Veículo iniciou movimento" to "O veículo à frente começou a andar.")
  }
  options.forEach{ (label,voice)->item(p,"TESTAR POPUP • $label"){
   events.add(label,"Aviso demonstrativo: $voice")
   audio.announce(voice,true)
   showTestBanner(label, voice)
  }}
 }
 private var activeBanner: PopupWindow? = null
 private val bannerHandler = android.os.Handler(android.os.Looper.getMainLooper())
 private fun showTestBanner(label: String, voice: String) {
  activeBanner?.dismiss()
  val urgent = label.contains("colisão",true) || label.contains("Pedestre",true) || label.contains("fechado",true)
  val color = if (urgent) Color.rgb(255,82,91) else Color.rgb(255,187,66)
  val background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
   intArrayOf(Color.rgb(70,48,29),Color.rgb(17,25,40))).apply {
    cornerRadius=22f;setStroke(3,color)
  }
  val banner = TextView(this).apply {
   text="TESTE • ${label.uppercase()}\n$voice"
   textSize=18f;setTextColor(Color.WHITE);gravity=Gravity.CENTER
   setPadding(24,14,24,14);this.background=background
  }
  val popup=PopupWindow(banner,(resources.displayMetrics.widthPixels*0.64f).toInt(),
   (resources.displayMetrics.heightPixels*0.23f).toInt(),false)
  popup.isOutsideTouchable=false
  activeBanner=popup
  popup.showAtLocation(root,Gravity.TOP or Gravity.CENTER_HORIZONTAL,0,32)
  bannerHandler.postDelayed({if(activeBanner===popup){popup.dismiss();activeBanner=null}},3500)
 }
 private fun radarPage(p:LinearLayout){
  note(p,"Os radares são pontos GPS cadastrados manualmente. Não há detecção de radiofrequência ou base pré-carregada.")
  item(p,"ABRIR LIVE / GERENCIAR RADARES") {startActivity(Intent(this,MainActivity::class.java))}
  val radars=RadarStore(this).all();note(p,"Radares locais: ${radars.size}")
  radars.forEach {note(p,"${it.type.uppercase()} • ${it.limit} km/h • ${it.lat}, ${it.lon}")}
 }
 private fun eventsPage(p:LinearLayout){
  note(p,"Histórico local de eventos. Avisos de teste são explicitamente marcados como MANUAL / TESTE.")
  val rows=events.lines();if(rows.isEmpty())note(p,"Nenhum evento registrado.") else rows.forEach{note(p,it)}
  item(p,"LIMPAR HISTÓRICO") {AlertDialog.Builder(this).setTitle("Excluir eventos locais?").setNegativeButton("Cancelar",null).setPositiveButton("Excluir"){_,_->events.clear();recreate()}.show()}
 }
 private fun mediaPage(p:LinearLayout){
  note(p,"Gravações armazenadas no espaço privado do aplicativo. Não há sincronização com nuvem.")
  listOf("clips","protegidos").forEach{folder->
   val files=File(getExternalFilesDir(null),folder).listFiles()?.filter{it.extension=="mp4"} ?: emptyList()
   note(p,"$folder: ${files.size} vídeo(s)")
   files.sortedByDescending{it.lastModified()}.forEach{file->item(p,file.name){
    val uri=androidx.core.content.FileProvider.getUriForFile(this, "$packageName.clips", file)
    try{startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))}
    catch(e:Exception){AlertDialog.Builder(this).setMessage("Sem reprodutor disponível: ${e.message}").setPositiveButton("OK",null).show()}
   }}
  }
 }
 private fun configPage(p:LinearLayout){
  note(p,"K2000N / K2001N Android 7.1 e tablets. Resoluções e câmeras dependem do hardware.")
  val cm=getSystemService(Context.CAMERA_SERVICE) as CameraManager
  try {cm.cameraIdList.forEach{id->
   val chars=cm.getCameraCharacteristics(id)
   val facing=when(chars.get(CameraCharacteristics.LENS_FACING)){
    CameraCharacteristics.LENS_FACING_BACK->"TRASEIRA";CameraCharacteristics.LENS_FACING_FRONT->"FRONTAL";else->"EXTERNA/OUTRA"}
   note(p,"Camera2: ID $id • $facing")
  }}catch(e:Exception){note(p,"Erro Camera2: ${e.message}")}
  val usb=getSystemService(Context.USB_SERVICE) as UsbManager
  if(usb.deviceList.isEmpty())note(p,"Nenhum USB detectado pelo Android USB Host.")
  usb.deviceList.values.forEach{note(p,"USB VID=${it.vendorId} PID=${it.productId} • ${it.deviceName}")}
  item(p,"SELECIONAR CÂMERA / GRAVAR") {startActivity(Intent(this,MainActivity::class.java))}
  item(p,"CONFIGURAR ÁUDIO E PADRÕES DE BIP") {startActivity(Intent(this,SettingsActivity::class.java))}
  if(BuildConfig.K2000_NATIVE)item(p,"AUTORIZAR USB — ABRIR LIVE COM IA") {startActivity(Intent(this,NativeUsbActivity::class.java))}
  note(p,if(BuildConfig.K2000_NATIVE)"USB integrado ao serviço de captura, rede neural e REC. Compatibilidade EGL/encoder precisa de teste físico nesta central." else "Para o driver USB enviado, use o APK K2000N ARM32 na central Android 7/8.")
 }
 override fun onDestroy(){activeBanner?.dismiss();bannerHandler.removeCallbacksAndMessages(null);audio.release();super.onDestroy()}
}
