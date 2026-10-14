package br.com.dashcamadas

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import me.jahnen.libaums.core.UsbMassStorageDevice
import me.jahnen.libaums.core.fs.UsbFile
import me.jahnen.libaums.core.usb.UsbCommunicationFactory
import me.jahnen.libaums.core.usb.UsbCommunicationCreator
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbEndpoint
import org.json.JSONObject

/** Only the explicitly authorized reference DVR and an existing protocol endpoint are opened. */
class DvrUsbStorage:AutoCloseable {
    private var storage:UsbMassStorageDevice?=null
    private var endpoint:UsbFile?=null
    private var deviceName=""
    private var communication:DvrUsbCommunication?=null
    @Volatile var channel:DvrByteChannel?=null;private set
    fun open(context:Context,device:UsbDevice):String {
        check(NativeUsbPolicy.referenceDvr(device.vendorId,device.productId,device.deviceClass,device.deviceSubclass))
        check(context.getSystemService(UsbManager::class.java).hasPermission(device))
        check(deviceName.isEmpty())
        deviceName=device.deviceName
        check(active.putIfAbsent(deviceName,this)==null){"DVR já está em uso"}
        UsbCommunicationFactory.underlyingUsbCommunication=UsbCommunicationFactory.UnderlyingUsbCommunication.OTHER
        val disk=UsbMassStorageDevice.getMassStorageDevices(context).firstOrNull{it.usbDevice.deviceName==device.deviceName}
            ?: error("DVR sem interface USB de armazenamento compatível")
        storage=disk
        disk.init()
        for(partition in disk.partitions){
            val fs=partition.fileSystem
            val root=fs.rootDirectory
            val directories=listOf(root)+listOf("Android/data/com.ankai.cardvr","Android/data/${context.packageName}")
                .mapNotNull{root.search(it)?.takeIf{item->item.isDirectory}}
            for(directory in directories){
                val file=directory.listFiles().firstOrNull{!it.isDirectory && DvrProtocol.type(it.name)!=null} ?: continue
                check(file.length in 1..Int.MAX_VALUE.toLong()){"Endpoint DVR tem tamanho incompatível"}
                endpoint=file
                channel=DvrByteChannel(object:DvrByteChannel.Endpoint{
                    override val length get()=file.length
                    override fun read(position:Long,buffer:java.nio.ByteBuffer)=file.read(position,buffer)
                    override fun write(position:Long,buffer:java.nio.ByteBuffer)=file.write(position,buffer)
                },fs.chunkSize)
                // The UFD SDK expects endpoint JSON and transport=0, not a raw USB file descriptor.
                return JSONObject().put("name",file.absolutePath).put("dvr",DvrProtocol.type(file.name)).toString()
            }
        }
        error("DVR reconhecido, mas endpoint akfile/elene/kkfile/eefile/jrfile não encontrado. Verifique cartão e modo DVR.")
    }
    fun protocolPath():String?=endpoint?.absolutePath
    override fun close(){
        channel?.cancel();channel=null;endpoint=null
        // This is a camera command endpoint. Do not create/delete/resize files or flush FAT metadata.
        active.remove(deviceName,this)
        val old=storage;storage=null
        try{old?.close()}finally{
            // libaums marks initialized only after partition discovery. Release USB even if init failed.
            communication?.close();communication=null
        }
    }
    companion object {
        private val active=java.util.concurrent.ConcurrentHashMap<String,DvrUsbStorage>()
        init {
            UsbCommunicationFactory.registerCommunication(object:UsbCommunicationCreator{
                override fun create(usbManager:UsbManager,usbDevice:UsbDevice,usbInterface:UsbInterface,
                    outEndpoint:UsbEndpoint,inEndpoint:UsbEndpoint):DvrUsbCommunication? {
                    val owner=active[usbDevice.deviceName] ?: return null
                    check(usbManager.hasPermission(usbDevice))
                    return DvrUsbCommunication(usbManager,usbDevice,usbInterface,outEndpoint,inEndpoint).also{owner.communication=it}
                }
            })
        }
    }
}
