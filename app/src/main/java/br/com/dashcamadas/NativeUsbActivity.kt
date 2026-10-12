package br.com.dashcamadas

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.hardware.usb.*
import android.os.*
import android.graphics.Color
import android.widget.*
import androidx.core.content.ContextCompat
import java.util.UUID

/** USB authorization only. Capture, neural processing and recording are owned by the service. */
class NativeUsbActivity:Activity(){
    private lateinit var manager:UsbManager
    private lateinit var status:TextView
    private val action="br.com.dashcamadas.USB_PERMISSION."+UUID.randomUUID()
    private var registered=false
    private var requested=""
    private val receiver=object:BroadcastReceiver(){
        override fun onReceive(context:Context,intent:Intent){
            val device=intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
            if(intent.action==action && device.deviceName==requested && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,false) && manager.hasPermission(device))activate(device)
            else status.text="Permissão USB negada; selecione novamente para tentar"
        }
    }
    override fun onCreate(state:Bundle?){
        super.onCreate(state);manager=getSystemService(UsbManager::class.java)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,16,24,16);setBackgroundColor(Color.rgb(5,16,29))}
        root.addView(TextView(this).apply{text="CÂMERA USB — K2000N ARM32";textSize=22f;setTextColor(Color.CYAN)})
        root.addView(TextView(this).apply{text="Autorize a câmera e abra a LIVE para rede neural de faixas/veículos e REC. A mudança de fonte finaliza eventual clipe Camera2. Compatibilidade USB precisa de teste nesta central.";textSize=17f;setTextColor(Color.WHITE)})
        status=TextView(this).apply{text="Selecione sua câmera USB";setTextColor(Color.CYAN)}
        root.addView(status)
        root.addView(Button(this).apply{text="SELECIONAR E AUTORIZAR USB";setOnClickListener{choose()}})
        root.addView(Button(this).apply{text="VOLTAR";setOnClickListener{finish()}})
        setContentView(root);SafeInsets.apply(this)
    }
    override fun onStart(){super.onStart();ContextCompat.registerReceiver(this,receiver,IntentFilter(action),ContextCompat.RECEIVER_NOT_EXPORTED);registered=true}
    private fun choose(){
        if(!BuildConfig.K2000_NATIVE || Build.VERSION.SDK_INT !in 24..27){status.text="Use o APK ARM32 no Android 7/8 para o driver enviado";return}
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.CAMERA),12);return}
        val devices=manager.deviceList.values.filter{NativeUsbPolicy.cameraCandidate(it.deviceClass,(0 until it.interfaceCount).map{i->it.getInterface(i).interfaceClass})}
        if(devices.isEmpty()){status.text="Nenhuma câmera USB candidata conectada";return}
        val suggested=intent.getStringExtra("device_name")
        val device=devices.firstOrNull{it.deviceName==suggested}
        if(device!=null){authorize(device);return}
        AlertDialog.Builder(this).setTitle("Selecionar câmera USB")
            .setItems(devices.map{"VID %04X / PID %04X".format(it.vendorId,it.productId)}.toTypedArray()){_,i->authorize(devices[i])}
            .setNegativeButton("Cancelar",null).show()
    }
    private fun authorize(device:UsbDevice){
        requested=device.deviceName
        if(manager.hasPermission(device)){activate(device);return}
        val flags=PendingIntent.FLAG_UPDATE_CURRENT or (if(Build.VERSION.SDK_INT>=31)PendingIntent.FLAG_MUTABLE else 0)
        manager.requestPermission(device,PendingIntent.getBroadcast(this,0,Intent(action).setPackage(packageName),flags))
        status.text="Aguardando autorização USB do Android"
    }
    private fun activate(device:UsbDevice){
        getSharedPreferences("camera",MODE_PRIVATE).edit().putString("preferred","usb:"+device.deviceName).apply()
        startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
    override fun onStop(){if(registered){unregisterReceiver(receiver);registered=false};super.onStop()}
}
