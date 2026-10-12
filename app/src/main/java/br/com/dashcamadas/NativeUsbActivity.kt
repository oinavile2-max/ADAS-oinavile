package br.com.dashcamadas

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.usb.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import git.my.camera.core.MyCamera
import java.util.UUID

/** Explicit native USB preview test. It does not advertise USB recording or USB ADAS. */
class NativeUsbActivity:Activity(),TextureView.SurfaceTextureListener {
    private val worker=HandlerThread("NativeUsb").apply{start()}
    private val io=Handler(worker.looper)
    private val main=Handler(Looper.getMainLooper())
    private lateinit var text:TextView
    private lateinit var preview:TextureView
    private lateinit var manager:UsbManager
    private var registered=false
    private var action=""
    @Volatile private var stopped=false
    private var pointer=0L
    private var native:MyCamera?=null
    private var connection:UsbDeviceConnection?=null
    private var surface:Surface?=null
    private var deviceName=""
    private var rotation=0
    private var mirror=false
    private var epoch=0
    private val callback=object:MyCamera.NativeCameraCallback {
        override fun event2String(a:Int,b:Int)="USB $a/$b"
        override fun onLog(level:Int,message:String?){android.util.Log.d("DashCamNative",message ?: "")}
        override fun onCallback(event:Int,value:Int,message:String?){
            status("Driver: evento $event / $value ${message.orEmpty().take(180)}")
        }
        override fun onPostedCallback(action:Int,notify:Int,a:Int,b:Int,message:String?){status("USB: ação $action / retorno $notify ${message.orEmpty().take(180)}")}
        override fun onSharedSrcOpened(file:MemoryFile?,header:Int,data:Int){}
        override fun onSharedSrcClosed(){}
        override fun onSharedSrcUpdated(){}
        override fun onSharedYuvOpened(file:MemoryFile?,header:Int,data:Int){}
        override fun onSharedYuvClosed(){}
        override fun onSharedYuvUpdated(){}
        override fun onVStreamStarted(a:Int,b:Int,c:Int,d:String?){}
        override fun onVStreamFrame(data:ByteArray?,size:Int){}
        override fun onVStreamStopped(){}
        override fun onAStreamStarted(a:Int,b:Int,c:Int,d:String?){}
        override fun onAStreamFrame(data:ByteArray?,size:Int){}
        override fun onAStreamStopped(){}
        override fun umsdOpen(path:String?)=""
        override fun umsdSize()=0
        override fun umsdChunkSize()=0
        override fun umsdSeek(position:Int){}
        override fun umsdRead(data:ByteArray?,a:Int,b:Int,c:Int)=-1
        override fun umsdWrite(data:ByteArray?,size:Int){}
    }
    private val receiver=object:BroadcastReceiver(){
        override fun onReceive(context:Context,intent:Intent){
            val device=intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
            if(intent.action==action){
                if(device.deviceName==deviceName && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,false) && manager.hasPermission(device))connect(device)
                else status("Permissão USB negada")
            }else if(intent.action==UsbManager.ACTION_USB_DEVICE_DETACHED && device.deviceName==deviceName){io.post{close()};status("USB desconectado")}
        }
    }
    private fun status(message:String){main.post{if(!stopped && ::text.isInitialized)text.text=message}}
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        manager=getSystemService(UsbManager::class.java)
        action=packageName+".USB_PERMISSION."+UUID.randomUUID()
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,12,16,12);setBackgroundColor(Color.rgb(5,16,29))}
        root.addView(TextView(this).apply{text="USB NATIVO — TESTE K2000N / ANDROID 7.1";textSize=18f;setTextColor(Color.WHITE)})
        root.addView(TextView(this).apply{text="Ao conectar, a sessão Camera2 será encerrada. Esta tela testa somente a prévia USB; gravação e ADAS USB ainda não integrados.";setTextColor(Color.LTGRAY)})
        val bar=LinearLayout(this)
        fun button(label:String,click:()->Unit){bar.addView(Button(this).apply{text=label;setOnClickListener{click()}},LinearLayout.LayoutParams(0,-2,1f))}
        button("CONECTAR USB"){choose()}
        button("GIRAR"){rotation=(rotation+90)%360;transform()}
        button("ESPELHAR"){mirror=!mirror;transform()}
        button("VOLTAR"){finish()}
        root.addView(bar)
        text=TextView(this).apply{text=if(BuildConfig.K2000_NATIVE)"Selecione sua câmera USB para autorizar o teste" else "Instale o APK K2000N ARM32 para usar a biblioteca do APK enviado";setTextColor(Color.CYAN)}
        root.addView(text)
        preview=TextureView(this).apply{surfaceTextureListener=this@NativeUsbActivity}
        root.addView(preview,LinearLayout.LayoutParams(-1,0,1f));setContentView(root);SafeInsets.apply(this)
    }
    override fun onStart(){super.onStart();stopped=false
        ContextCompat.registerReceiver(this,receiver,IntentFilter(action).apply{addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)},ContextCompat.RECEIVER_NOT_EXPORTED);registered=true
    }
    private fun choose(){
        if(!BuildConfig.K2000_NATIVE){status("Use o APK K2000N ARM32; o APK genérico não contém libmycamera.so");return}
        if(Build.VERSION.SDK_INT>27){status("Driver legado: esta prévia é habilitada somente no Android 7/8 (API 24–27)");return}
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.CAMERA),12);return}
        val devices=manager.deviceList.values.filter{NativeUsbPolicy.cameraCandidate(it.deviceClass,(0 until it.interfaceCount).map{i->it.getInterface(i).interfaceClass})}
        if(devices.isEmpty()){status("Nenhuma câmera USB candidata conectada");return}
        AlertDialog.Builder(this).setTitle("Selecionar câmera USB (teste do driver UFD)")
            .setItems(devices.map{"VID %04X / PID %04X".format(it.vendorId,it.productId)}.toTypedArray()){_,i->
                val device=devices[i];deviceName=device.deviceName
                if(manager.hasPermission(device))connect(device) else {
                    val intent=Intent(action).setPackage(packageName)
                    val flags=PendingIntent.FLAG_UPDATE_CURRENT or (if(Build.VERSION.SDK_INT>=31)PendingIntent.FLAG_MUTABLE else 0)
                    manager.requestPermission(device,PendingIntent.getBroadcast(this,0,intent,flags));status("Aguardando autorização USB do Android")
                }
            }.setNegativeButton("Cancelar",null).show()
    }
    private fun connect(device:UsbDevice){
        if(!manager.hasPermission(device) || stopped)return
        // Closing Camera2 finalizes its active recording; only this explicit test requests it.
        stopService(Intent(this,DashCamForegroundService::class.java))
        val requested=++epoch
        io.post{
            try{
                close()
                if(stopped || requested!=epoch)return@post
                val version=MyCamera.loadAndDescribe(this)
                val opened=manager.openDevice(device) ?: error("USB não aberto")
                connection=opened;native=MyCamera()
                pointer=MyCamera.nativeCreateUFD(callback)
                check(pointer!=0L){"Driver não criou sessão UFD"}
                native?.nativePost(pointer,18,0,0,"{\"isDebug\":false,\"isDebugData\":false,\"debugLevel\":0,\"debugTypes\":0,\"isTest\":false}")
                surface?.let{native?.nativeAddPreview(pointer,0,0,it)}
                native?.nativeConnect(pointer,device.deviceName,opened.fileDescriptor)
                awaitInitialization(requested,0)
                status("Driver carregado: $version. Aguardando quadros USB reais.")
            }catch(e:Exception){close();status("Teste USB falhou: ${e.message}")}
            catch(e:LinkageError){close();status("Biblioteca incompatível com este runtime: ${e.message}")}
        }
    }
    private fun awaitInitialization(requested:Int,attempt:Int){
        if(stopped || requested!=epoch || pointer==0L)return
        try{
            if(native?.nativeIsInitialized(pointer)==true)native?.nativePost(pointer,6,0,0,"")
            else if(attempt<20)io.postDelayed({awaitInitialization(requested,attempt+1)},250)
            else{close();status("Driver não inicializou a câmera em 5 segundos; confira VID/PID e compatibilidade")}
        }catch(e:Exception){close();status("Inicialização USB falhou: ${e.message}")}
    }
    private fun close(){
        val handle=pointer;pointer=0
        if(handle!=0L)try{native?.nativeDisconnect(handle);MyCamera.nativeDestroy(handle)}catch(e:Exception){android.util.Log.w("DashCamNative","Fechamento nativo",e)}
        connection?.close();connection=null;native=null
    }
    private fun transform(){
        val w=preview.width.toFloat();val h=preview.height.toFloat();if(w==0f || h==0f)return
        val sourceW=1280;val sourceH=720
        val mapping=CameraTransform(sourceW,sourceH,w.toInt(),h.toInt(),rotation,mirror,false)
        val input=floatArrayOf(0f,0f,w,0f,0f,h)
        val output=floatArrayOf(mapping.project(0f,0f).first,mapping.project(0f,0f).second,mapping.project(sourceW.toFloat(),0f).first,mapping.project(sourceW.toFloat(),0f).second,mapping.project(0f,sourceH.toFloat()).first,mapping.project(0f,sourceH.toFloat()).second)
        preview.setTransform(Matrix().apply{setPolyToPoly(input,0,output,0,3)})
    }
    override fun onSurfaceTextureAvailable(texture:SurfaceTexture,w:Int,h:Int){io.post{surface=Surface(texture);if(pointer!=0L)native?.nativeAddPreview(pointer,0,0,surface)};transform()}
    override fun onSurfaceTextureSizeChanged(texture:SurfaceTexture,w:Int,h:Int){transform()}
    override fun onSurfaceTextureUpdated(texture:SurfaceTexture){if(!stopped)text.text="Superfície USB atualizada: confira a imagem • sem gravação/ADAS nesta tela"}
    override fun onSurfaceTextureDestroyed(texture:SurfaceTexture):Boolean{io.post{close();surface?.release();surface=null;texture.release()};return false}
    override fun onStop(){stopped=true;epoch++;if(registered){unregisterReceiver(receiver);registered=false};io.post{close()};super.onStop()}
    override fun onDestroy(){main.removeCallbacksAndMessages(null);io.post{close();surface?.release();surface=null;worker.quitSafely()};super.onDestroy()}
}
