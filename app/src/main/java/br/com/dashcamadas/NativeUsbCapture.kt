package br.com.dashcamadas

import android.content.*
import android.graphics.PixelFormat
import android.hardware.usb.*
import android.media.ImageReader
import android.os.*
import android.view.Surface
import androidx.core.content.ContextCompat
import git.my.camera.core.MyCamera

/** Service-owned EGL-to-RGBA capture; no output surface belongs to an Activity. */
class NativeUsbCapture(private val context:Context,private val handler:Handler,
    private val onFrame:(ImageReader)->Unit,private val onReady:()->Unit,private val onFailure:(String)->Unit):AutoCloseable {
    private var handle=0L
    private var native:MyCamera?=null
    private var connection:UsbDeviceConnection?=null
    @Volatile private var storage:DvrUsbStorage?=null
    private var reader:ImageReader?=null
    private var startup:NativePreviewStartup?=null
    @Volatile private var stage="não iniciado"
    @Volatile private var lastNativeEvent="sem callback"
    fun diagnosticSummary()="etapa=$stage; SDK: $lastNativeEvent"
    private fun nativeDetail(text:String){
        if(closed)return
        lastNativeEvent=text.replace(Regex("[\\p{Cntrl}]")," ").take(240)
        android.util.Log.d("DashCamUSB",lastNativeEvent)
    }
    private var name=""
    private var registered=false
    private var mountRegistered=false
    private val mountReceiver=object:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){
        val root=i.data?.path ?: return
        if(name.startsWith(root.trimEnd('/')+"/"))handler.post{if(!closed){close();onFailure("Volume da câmera DVR removido")}}
    }}
    @Volatile var ready=false;private set
    @Volatile private var closed=false
    private val transportFailure=java.util.concurrent.atomic.AtomicBoolean()
    private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){
        val d=i.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        if(d?.deviceName==name)handler.post{if(!closed){close();onFailure("Câmera USB desconectada")}}
    }}
    private val callback=object:MyCamera.NativeCameraCallback {
        override fun event2String(a:Int,b:Int)="USB $a/$b"
        override fun onLog(level:Int,message:String?){nativeDetail("Log $level: ${message.orEmpty()}")}
        override fun onCallback(event:Int,value:Int,message:String?){nativeDetail("Evento 0x${event.toUInt().toString(16)} / $value: ${message.orEmpty()}")}
        override fun onPostedCallback(action:Int,notify:Int,a:Int,b:Int,message:String?){nativeDetail("Ação $action / retorno $notify / $a,$b: ${message.orEmpty()}")}
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
        override fun umsdOpen(path:String?)=storage?.protocolPath()
        override fun umsdSize()=storage?.channel?.size ?: 0
        override fun umsdChunkSize()=storage?.channel?.chunkSize ?: 0
        override fun umsdSeek(position:Int){
            try{storage?.channel?.seek(position) ?: error("Canal DVR ausente")}
            catch(e:Exception){failTransport("Posição do canal DVR inválida: ${e.message}")}
        }
        override fun umsdRead(data:ByteArray?,a:Int,b:Int,c:Int):Int {
            return try{
                val result=storage?.channel?.read(data,a,b,c) ?: -1
                if(result<0){failTransport("Falha de leitura do canal DVR");0}else result
            }catch(e:Exception){failTransport("Leitura DVR: ${e.message}");0}
        }
        override fun umsdWrite(data:ByteArray?,size:Int){
            try{storage?.channel?.write(data,size) ?: error("Canal DVR ausente")}
            catch(e:Exception){failTransport("Comando DVR rejeitado: ${e.message}")}
        }
    }
    private fun failTransport(message:String){
        if(!closed && transportFailure.compareAndSet(false,true))handler.post{if(!closed)onFailure(message)}
    }
    fun open(deviceName:String){
        stage="abrindo fonte"
        if(DvrSources.mounted(deviceName)){openMounted(deviceName);return}
        check(BuildConfig.K2000_NATIVE && Build.VERSION.SDK_INT in 24..27){"Driver legado exige APK ARM32 e Android 7/8"}
        check(handle==0L && !closed)
        val manager=context.getSystemService(UsbManager::class.java)
        val device=manager.deviceList.values.firstOrNull{it.deviceName==deviceName} ?: error("USB ausente")
        check(manager.hasPermission(device)){"Selecione a câmera USB e autorize o Android"}
        val interfaces=(0 until device.interfaceCount).map{device.getInterface(it).interfaceClass}
        check(NativeUsbPolicy.cameraCandidate(device.deviceClass,interfaces,device.vendorId,device.productId,device.deviceSubclass))
        val isDvr=NativeUsbPolicy.referenceDvr(device.vendorId,device.productId,device.deviceClass,device.deviceSubclass)
        MyCamera.loadAndDescribe(context)
        name=deviceName
        val connectName:String
        val connectTransport:Int
        if(isDvr){
            storage=DvrUsbStorage()
            connectName=storage!!.open(context,device);connectTransport=0
        }else{
            connection=manager.openDevice(device) ?: error("USB não pôde ser aberto")
            connectName=deviceName;connectTransport=connection!!.fileDescriptor
        }
        ContextCompat.registerReceiver(context,receiver,IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED),null,handler,ContextCompat.RECEIVER_NOT_EXPORTED);registered=true
        startPreview(connectName,connectTransport,!isDvr && (device.deviceClass==14 || 14 in interfaces))
    }
    private fun openMounted(source:String){
        check(BuildConfig.K2000_NATIVE && Build.VERSION.SDK_INT in 24..27 && handle==0L && !closed)
        val file=MountedDvrDiscovery.validate(context,source)
        MyCamera.loadAndDescribe(context)
        name=file.path
        ContextCompat.registerReceiver(context,mountReceiver,IntentFilter().apply{
            addAction(Intent.ACTION_MEDIA_UNMOUNTED);addAction(Intent.ACTION_MEDIA_EJECT);addAction(Intent.ACTION_MEDIA_REMOVED);addDataScheme("file")
        },null,handler,ContextCompat.RECEIVER_NOT_EXPORTED);mountRegistered=true
        val json=org.json.JSONObject().put("name",file.path).put("dvr",DvrProtocol.type(file.name)).toString()
        // Original FILE route passes -1; the native SDK performs fileRead/fileWrite itself.
        startPreview(json,-1,false)
    }
    private fun startPreview(connectName:String,connectTransport:Int,uvc:Boolean){
        stage="criando sessão nativa"
        native=MyCamera()
        handle=if(uvc)MyCamera.nativeCreateUVC(callback) else MyCamera.nativeCreateUFD(callback)
        check(handle!=0L){"Driver não criou sessão USB"}
        reader=ImageReader.newInstance(640,360,PixelFormat.RGBA_8888,3).apply{
            setOnImageAvailableListener({source->
                if(!closed)try{if(ready)onFrame(source) else source.acquireLatestImage()?.close()}
                catch(e:Exception){onFailure("Quadro USB inválido: ${e.message}")}
            },handler)
        }
        check(native!!.nativePost(handle,18,0,0,"{\"isDebug\":false,\"isDebugData\":false,\"debugLevel\":0,\"debugTypes\":0,\"isTest\":false}")>=0)
        val sdk=native!!;val sessionHandle=handle;val preview=reader!!.surface
        startup=NativePreviewStartup(object:NativePreviewStartup.Driver {
            override fun connect(){stage="conectando SDK (transporte $connectTransport)";sdk.nativeConnect(sessionHandle,connectName,connectTransport)}
            override fun initialized()=sdk.nativeIsInitialized(sessionHandle)
            override fun connected()=sdk.nativeIsConnected(sessionHandle)
            // The native preview pointer is null before connect; early registration is silently lost.
            override fun attachPreview(){stage="registrando superfície após conexão";sdk.nativeAddPreview(sessionHandle,0,preview.hashCode(),preview)}
            override fun startPreview():Int{stage="iniciando prévia";return sdk.nativePost(sessionHandle,6,0,0,"")}
        }).also{it.begin(SystemClock.elapsedRealtime())}
        awaitInitialization()
    }
    private fun awaitInitialization(){
        if(closed || handle==0L)return
        try{
            if(startup?.poll(SystemClock.elapsedRealtime())==true){
                stage="prévia iniciada; aguardando quadros"
                ready=true;onReady()
            }else handler.postDelayed({awaitInitialization()},250)
        }catch(e:Exception){val detail=diagnosticSummary();close();onFailure("Inicialização USB: ${e.message}; $detail")}
    }
    override fun close(){
        if(closed)return
        closed=true;ready=false
        startup?.close();startup=null
        storage?.channel?.cancel()
        if(registered){context.unregisterReceiver(receiver);registered=false}
        if(mountRegistered){context.unregisterReceiver(mountReceiver);mountRegistered=false}
        val old=handle;handle=0L
        if(old!=0L){
            try{native?.nativeDisconnect(old)}catch(e:Exception){android.util.Log.w("DashCamUSB","Desconexão",e)}
            try{MyCamera.nativeDestroy(old)}catch(e:Exception){android.util.Log.w("DashCamUSB","Destruição",e)}
        }
        reader?.setOnImageAvailableListener(null,null);reader?.close();reader=null
        connection?.close();connection=null;native=null
        storage?.close();storage=null
    }
}
