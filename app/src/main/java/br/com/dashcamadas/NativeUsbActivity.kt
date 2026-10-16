package br.com.dashcamadas

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.hardware.usb.*
import android.os.*
import android.widget.*
import androidx.core.content.ContextCompat
import java.util.UUID
import java.util.concurrent.Executors

/** Asynchronous discovery and explicit authorization. Real capture remains in the foreground service. */
class NativeUsbActivity:Activity(){
    private lateinit var manager:UsbManager
    private lateinit var status:TextView
    private lateinit var report:TextView
    private val action="br.com.dashcamadas.USB_PERMISSION."+UUID.randomUUID()
    private val main=Handler(Looper.getMainLooper())
    private val io=Executors.newSingleThreadExecutor()
    private var registered=false
    private val permissionRequest=UsbPermissionRequest()
    private var scanGeneration=0
    private var mode=DvrSearchMode.USB_FIRST
    private var choices=emptyList<String>()
    private var scanning=false
    private var reportText="Busca ainda não executada"
    private val timeout=Runnable{if(permissionRequest.expire(SystemClock.elapsedRealtime()))status.text="Autorização USB expirou após 20 s. Selecione novamente; a busca continua disponível."}
    private val refreshTask=Runnable{refresh()}
    private val receiver=object:BroadcastReceiver(){
        override fun onReceive(context:Context,intent:Intent){
            if(intent.action==action){
                val device=intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
                when(permissionRequest.complete(device.deviceName,intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,false),manager.hasPermission(device),SystemClock.elapsedRealtime())){
                    UsbPermissionRequest.Result.GRANTED->{main.removeCallbacks(timeout);activate("usb:"+device.deviceName,device)}
                    UsbPermissionRequest.Result.DENIED->{main.removeCallbacks(timeout);status.text="Permissão USB não concedida. Selecione novamente."}
                    UsbPermissionRequest.Result.EXPIRED->{main.removeCallbacks(timeout);status.text="Autorização USB expirou. Selecione novamente."}
                    UsbPermissionRequest.Result.STALE->return
                }
            }else{
                if(intent.action==UsbManager.ACTION_USB_DEVICE_DETACHED && intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.deviceName==permissionRequest.device){permissionRequest.cancel();main.removeCallbacks(timeout)}
                main.removeCallbacks(refreshTask);main.postDelayed(refreshTask,450)
            }
        }
    }
    override fun onCreate(state:Bundle?){
        super.onCreate(state);manager=getSystemService(UsbManager::class.java)
        val prefs=getSharedPreferences("camera",MODE_PRIVATE)
        mode=DvrSearchMode.read(prefs.getString("dvr_search_mode",null))
        val page=CommercialPage(this,"Buscar câmera DVR","USB e volumes montados são caminhos independentes da Camera2.")
        val card=page.card("Descoberta da K2001N","Escolha a ordem da busca. Só a seleção explícita abre o driver e pode encerrar o clipe atual.","camera")
        val modes=Spinner(NeonDialogs.themed(this),Spinner.MODE_DROPDOWN).apply{
            adapter=ArrayAdapter(NeonDialogs.themed(this@NativeUsbActivity),android.R.layout.simple_spinner_dropdown_item,DvrSearchMode.entries.map{it.label})
            setSelection(mode.ordinal);minimumHeight=page.dp(52)
        }
        modes.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent:AdapterView<*>?,view:android.view.View?,position:Int,id:Long){
                val selected=DvrSearchMode.entries[position]
                if(selected!=mode){mode=selected;prefs.edit().putString("dvr_search_mode",mode.name).apply();refresh()}
            }
            override fun onNothingSelected(parent:AdapterView<*>?){}
        }
        card.addView(modes)
        status=page.label("Buscando fontes disponíveis…",15f,CommercialPage.MUTED);card.addView(status)
        card.addView(page.action("Atualizar busca"){refresh()})
        card.addView(page.action("Selecionar e autorizar câmera"){choose()})
        card.addView(page.action("Permitir busca em volumes Android 7/8"){
            if(!nativeSupported()){status.text="O driver exige perfil NWD ARM32 e Android 7/8";return@action}
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE),13)
        })
        page.addCard(page.body,card)
        val info=page.card("Diagnóstico completo","Todos os USBs são listados, inclusive os que não são candidatos a câmera.","device")
        report=page.label("",12f,CommercialPage.MUTED).apply{setTextIsSelectable(true)};info.addView(report)
        info.addView(page.action("Copiar relatório"){
            getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Diagnóstico DVR",reportText))
            status.text="Relatório copiado"
        })
        info.addView(page.action("Compartilhar relatório"){
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,reportText),"Compartilhar diagnóstico"))
        })
        page.addCard(page.body,info)
        page.note(page.body,"Um USB enumerado ou um marcador encontrado não comprova imagem. Armazenamento comum não é aberto como câmera. Não usa root nem permissões de aplicativo de sistema.")
        page.show()
        ContextCompat.registerReceiver(this,receiver,IntentFilter(action).apply{addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)},ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(this,receiver,IntentFilter().apply{addAction(Intent.ACTION_MEDIA_MOUNTED);addAction(Intent.ACTION_MEDIA_UNMOUNTED);addAction(Intent.ACTION_MEDIA_REMOVED);addDataScheme("file")},ContextCompat.RECEIVER_NOT_EXPORTED)
        registered=true;refresh()
    }
    override fun onResume(){
        super.onResume();CommercialUi.applyAppearance(this)
        if(permissionRequest.expire(SystemClock.elapsedRealtime())){main.removeCallbacks(timeout);status.text="Autorização USB expirou. Selecione novamente."}
        val waiting=manager.deviceList.values.firstOrNull{it.deviceName==permissionRequest.device}
        if(waiting!=null && manager.hasPermission(waiting) && permissionRequest.complete(waiting.deviceName,true,true,SystemClock.elapsedRealtime())==UsbPermissionRequest.Result.GRANTED){
            main.removeCallbacks(timeout);activate("usb:"+waiting.deviceName,waiting)
        }
    }
    private fun nativeSupported()=BuildConfig.K2000_NATIVE && Build.VERSION.SDK_INT in 24..27
    private fun refresh(){
        val token=++scanGeneration;val selectedMode=mode
        scanning=true;choices=emptyList()
        status.text=if(permissionRequest.device==null)"Atualizando descoberta…" else "Aguardando autorização USB do Android"
        io.execute{
            val text=StringBuilder("DASHCAM ADAS PRO — BUSCA DVR\nVersão: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nModo: ${selectedMode.label}\nPerfil nativo: ${nativeSupported()} | API ${Build.VERSION.SDK_INT}\n")
            val usb=mutableListOf<String>();var files=emptyList<String>()
            try{
                val devices=manager.deviceList.values.toList()
                text.append("TODOS os dispositivos USB (${devices.size}):\n")
                devices.forEach{d->
                    val classes=(0 until d.interfaceCount).map{d.getInterface(it).interfaceClass}
                    val candidate=NativeUsbPolicy.cameraCandidate(d.deviceClass,classes,d.vendorId,d.productId,d.deviceSubclass)
                    text.append("VID %04X PID %04X classe %d/%d perm=%s candidato=%s\n".format(d.vendorId,d.productId,d.deviceClass,d.deviceSubclass,manager.hasPermission(d),candidate))
                    for(i in 0 until d.interfaceCount){val face=d.getInterface(i)
                        text.append("  interface ${face.id}: classe ${face.interfaceClass}, sub ${face.interfaceSubclass}, protocolo ${face.interfaceProtocol}, endpoints ${face.endpointCount}\n")
                    }
                    if(candidate)usb+="usb:"+d.deviceName
                }
                if(nativeSupported()){
                    files=MountedDvrDiscovery.scan(this)
                    text.append("Busca em volumes autorizada: ${MountedDvrDiscovery.allowed(this)}\n")
                    text.append("Volumes removíveis acessíveis: ${MountedDvrDiscovery.roots(this).joinToString{it.path}}\n")
                    text.append("Marcadores encontrados (${files.size}):\n${files.joinToString("\n")}\n")
                }
            }catch(e:Exception){text.append("Falha de busca: ${e.javaClass.simpleName}: ${e.message}\n")}
            val last=getSharedPreferences("native_dvr_last",MODE_PRIVATE)
            if(last.contains("diagnostic"))text.append("ÚLTIMA CAPTURA NATIVA (${java.util.Date(last.getLong("time",0))}):\nFonte: ${last.getString("source","")}\n${last.getString("diagnostic","")}\n")
            try{
                val file=java.io.File(filesDir,"capture-diagnostics.json")
                if(file.isFile && file.length()<=32768){
                    val capture=org.json.JSONObject(file.readText())
                    text.append("CAPTURA — fonte: ${capture.optString("camera_id")}; conectada: ${capture.optBoolean("connected")}; quadros: ${capture.optLong("camera_received_frames")}\n")
                    text.append("Estado: ${capture.optString("state_message")}\nSDK: ${capture.optString("native_sdk_diagnostic")}\n")
                }
            }catch(e:Exception){text.append("Diagnóstico de captura indisponível: ${e.javaClass.simpleName}\n")}
            text.append("Marcadores: akfile, elene, kkfile, eefile, jrfile. Dispositivo/marcador não confirma decodificação ou vídeo.\n")
            main.post{
                if(isDestroyed || isFinishing || token!=scanGeneration)return@post
                scanning=false;choices=selectedMode.order(usb,files);reportText=text.toString();report.text=reportText
                if(permissionRequest.device==null)status.text="${choices.size} fonte(s) candidata(s). Selecione para conectar; Atualizar repete a busca."
            }
        }
    }
    private fun choose(){
        if(!nativeSupported()){status.text="Use o perfil NWD ARM32 no Android 7/8 para este driver";return}
        if(scanning){status.text="Aguarde a busca terminar para selecionar a fonte";return}
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.CAMERA),12);return}
        if(choices.isEmpty()){status.text="Nenhuma fonte candidata neste modo. Confira o diagnóstico, a permissão de volumes e Atualizar busca.";return}
        val snapshot=choices.toList()
        NeonDialogs.Builder(this).setTitle("Selecionar fonte DVR")
            .setItems(snapshot.map{label(it)}.toTypedArray()){_,i->
                val source=snapshot[i]
                if(DvrSources.mounted(source))activate(source)
                else manager.deviceList.values.firstOrNull{it.deviceName==source.removePrefix("usb:")}?.let{authorize(it)} ?: refresh()
            }.setNegativeButton("Cancelar",null).show()
    }
    private fun label(source:String):String{
        if(DvrSources.mounted(source))return "VOLUME DVR • "+source.removePrefix(DvrSources.FILE)
        val d=manager.deviceList.values.firstOrNull{it.deviceName==source.removePrefix("usb:")}
        return if(d==null)"USB desconectado" else "USB NATIVO • %04X:%04X".format(d.vendorId,d.productId)
    }
    private fun authorize(device:UsbDevice){
        if(manager.hasPermission(device)){activate("usb:"+device.deviceName,device);return}
        permissionRequest.begin(device.deviceName,SystemClock.elapsedRealtime());main.removeCallbacks(timeout)
        val flags=PendingIntent.FLAG_UPDATE_CURRENT or (if(Build.VERSION.SDK_INT>=31)PendingIntent.FLAG_MUTABLE else 0)
        try{
            manager.requestPermission(device,PendingIntent.getBroadcast(this,0,Intent(action).setPackage(packageName),flags))
            status.text="Aguardando autorização USB do Android (até 20 s)";main.postDelayed(timeout,20000)
        }catch(e:Exception){permissionRequest.cancel();status.text="Pedido USB falhou: ${e.message}"}
    }
    private fun activate(source:String,device:UsbDevice?=null){
        val edit=getSharedPreferences("camera",MODE_PRIVATE).edit().putString("preferred",source)
        if(device!=null)edit.putInt("preferred_usb_vid",device.vendorId).putInt("preferred_usb_pid",device.productId)
        else edit.remove("preferred_usb_vid").remove("preferred_usb_pid")
        edit.apply()
        startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP));finish()
    }
    override fun onRequestPermissionsResult(code:Int,permissions:Array<out String>,results:IntArray){
        super.onRequestPermissionsResult(code,permissions,results)
        if(code==12 && results.isNotEmpty() && results[0]==PackageManager.PERMISSION_GRANTED)choose() else refresh()
    }
    override fun onDestroy(){
        if(registered)unregisterReceiver(receiver)
        main.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy()
    }
}
