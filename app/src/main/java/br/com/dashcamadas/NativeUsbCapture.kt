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
    private var name=""
    private var registered=false
    @Volatile var ready=false;private set
    @Volatile private var closed=false
    private val transportFailure=java.util.concurrent.atomic.AtomicBoolean()
    private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){
        val d=i.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        if(d?.deviceName==name)handler.post{if(!closed){close();onFailure("Câmera USB desconectada")}}
    }}
    private val callback=object:MyCamera.NativeCameraCallback {
        override fun event2String(a:Int,b:Int)="USB $a/$b"
        override fun onLog(level:Int,message:String?){android.util.Log.d("DashCamUSB","Log nativo nível $level")}
        override fun onCallback(event:Int,value:Int,message:String?){android.util.Log.d("DashCamUSB","Evento $event / $value")}
        override fun onPostedCallback(action:Int,notify:Int,a:Int,b:Int,message:String?){android.util.Log.d("DashCamUSB","Ação $action / retorno $notify")}
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
        native=MyCamera()
        handle=if(!isDvr && (device.deviceClass==14 || 14 in interfaces))MyCamera.nativeCreateUVC(callback) else MyCamera.nativeCreateUFD(callback)
        check(handle!=0L){"Driver não criou sessão USB"}
        reader=ImageReader.newInstance(640,360,PixelFormat.RGBA_8888,3).apply{
            setOnImageAvailableListener({source->
                if(!closed)try{if(ready)onFrame(source) else source.acquireLatestImage()?.close()}
                catch(e:Exception){onFailure("Quadro USB inválido: ${e.message}")}
            },handler)
        }
        check(native!!.nativePost(handle,18,0,0,"{\"isDebug\":false,\"isDebugData\":false,\"debugLevel\":0,\"debugTypes\":0,\"isTest\":false}")>=0)
        val preview=reader!!.surface
        // Original SDK view type=0, identity=surface hash.
        native!!.nativeAddPreview(handle,0,preview.hashCode(),preview)
        ContextCompat.registerReceiver(context,receiver,IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED),null,handler,ContextCompat.RECEIVER_NOT_EXPORTED);registered=true
        native!!.nativeConnect(handle,connectName,connectTransport)
        awaitInitialization(0)
    }
    private fun awaitInitialization(attempt:Int){
        if(closed || handle==0L)return
        try{
            if(native!!.nativeIsInitialized(handle)){
                check(native!!.nativePost(handle,6,0,0,"")>=0){"Driver rejeitou iniciar prévia"}
                ready=true;onReady()
            }else if(attempt<20)handler.postDelayed({awaitInitialization(attempt+1)},250)
            else{close();onFailure("USB não inicializou em 5 segundos")}
        }catch(e:Exception){close();onFailure("Inicialização USB: ${e.message}")}
    }
    override fun close(){
        if(closed)return
        closed=true;ready=false
        storage?.channel?.cancel()
        if(registered){context.unregisterReceiver(receiver);registered=false}
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
