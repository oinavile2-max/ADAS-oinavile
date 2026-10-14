package br.com.dashcamadas

import android.hardware.usb.*
import me.jahnen.libaums.core.usb.UsbCommunication
import java.io.IOException
import java.nio.ByteBuffer

/** USB Host transport for libaums; no additional legacy JNI library or filesystem permissions. */
class DvrUsbCommunication(manager:UsbManager,device:UsbDevice,
    override val usbInterface:UsbInterface,override val outEndpoint:UsbEndpoint,
    override val inEndpoint:UsbEndpoint):UsbCommunication {
    private val connection=manager.openDevice(device) ?: throw IOException("DVR não pôde ser aberto")
    @Volatile private var closed=false
    init {
        if(!connection.claimInterface(usbInterface,true)){
            connection.close();closed=true;throw IOException("Interface DVR ocupada; feche o outro aplicativo de câmera")
        }
    }
    override fun bulkOutTransfer(src:ByteBuffer)=transfer(outEndpoint,src)
    override fun bulkInTransfer(dest:ByteBuffer)=transfer(inEndpoint,dest)
    private fun transfer(endpoint:UsbEndpoint,buffer:ByteBuffer):Int {
        if(closed)throw IOException("DVR desconectado")
        require(buffer.hasArray())
        val requested=buffer.remaining()
        if(requested==0)return 0
        val count=connection.bulkTransfer(endpoint,buffer.array(),buffer.arrayOffset()+buffer.position(),requested,UsbCommunication.TRANSFER_TIMEOUT)
        if(count<=0 || count>requested)throw IOException("Transferência DVR falhou no endpoint ${endpoint.address}")
        buffer.position(buffer.position()+count)
        return count
    }
    override fun controlTransfer(requestType:Int,request:Int,value:Int,index:Int,buffer:ByteArray,length:Int):Int {
        if(closed)throw IOException("DVR desconectado")
        return connection.controlTransfer(requestType,request,value,index,buffer,length,UsbCommunication.TRANSFER_TIMEOUT)
    }
    override fun resetDevice(){
        // USB Mass Storage Bulk-Only reset recovery, without resetting the Android USB bus.
        if(controlTransfer(0x21,0xff,0,usbInterface.id,ByteArray(0),0)<0)throw IOException("DVR rejeitou recuperação USB")
        clearFeatureHalt(inEndpoint);clearFeatureHalt(outEndpoint)
    }
    override fun clearFeatureHalt(endpoint:UsbEndpoint){
        if(controlTransfer(0x02,1,0,endpoint.address,ByteArray(0),0)<0)throw IOException("DVR rejeitou liberar endpoint ${endpoint.address}")
    }
    @Synchronized override fun close(){
        if(closed)return
        closed=true
        try{connection.releaseInterface(usbInterface)}finally{connection.close()}
    }
}
