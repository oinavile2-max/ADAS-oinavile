package br.com.dashcamadas

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.usb.UsbManager
import android.os.Build

/** Diagnóstico passivo. Enumerar USB não significa que haja driver ou frames disponíveis. */
object DeviceCapabilities {
    data class CameraInfo(val id: String, val type: String, val streamSizes: Int)
    data class UsbInfo(val vendorId: Int, val productId: Int, val deviceClass: Int, val interfaces: String,
        val deviceSubclass:Int=0,val permission:Boolean=false)
    data class Report(
        val manufacturer: String, val model: String, val sdk: Int, val memoryMb: Long,
        val profile: String, val cameraPermission: Boolean, val usbHost: Boolean,
        val cameras: List<CameraInfo>, val usbDevices: List<UsbInfo>, val errors: List<String>
    ) {
        fun asText(): String = buildString {
            appendLine("DASHCAM ADAS PRO — DIAGNÓSTICO DE COMPATIBILIDADE")
            appendLine("Dispositivo: $manufacturer / $model | API $sdk | RAM estimada: $memoryMb MB")
            appendLine("Android: ${Build.VERSION.RELEASE} | ABIs: ${Build.SUPPORTED_ABIS.joinToString()} | Processo: ${if(android.os.Process.is64Bit()) "64" else "32"} bits")
            appendLine("Driver USB do APK enviado: ${if(BuildConfig.K2000_NATIVE) "incluído (ARM32, teste de prévia Android 7/8)" else "ausente neste APK genérico"}")
            appendLine("Perfil sugerido: $profile | Permissão câmera: $cameraPermission | USB host: $usbHost")
            appendLine("Câmeras Camera2 (${cameras.size}):")
            cameras.forEach { appendLine(" • ID ${it.id}: ${it.type} | resoluções de preview: ${it.streamSizes}") }
            appendLine("USB conectados (${usbDevices.size}):")
            usbDevices.forEach {
                appendLine(" • VID %04X PID %04X classe %d/%d interfaces %s | autorização USB: %s".format(it.vendorId, it.productId, it.deviceClass,it.deviceSubclass,it.interfaces,it.permission))
                if(NativeUsbPolicy.referenceDvr(it.vendorId,it.productId,it.deviceClass,it.deviceSubclass)){
                    appendLine("   DVR referenciado no APK original (UFD/armazenamento).")
                    appendLine(if(BuildConfig.K2000_NATIVE && sdk in 24..27) "   CÂMERA → USB NATIVO: selecione e autorize; imagem ainda exige teste." else "   Exige APK NWD ARM32 no Android 7/8.")
                }else if(!NativeUsbPolicy.cameraCandidate(it.deviceClass,it.interfaces.split(',').mapNotNull{item->item.substringBefore('/').toIntOrNull()},it.vendorId,it.productId,it.deviceSubclass) &&
                    (it.deviceClass==8 || it.interfaces.split(',').any{item->item.startsWith("8/")})){
                    appendLine("   Armazenamento USB sem referência DVR; não oferecido como câmera.")
                }
            }
            errors.forEach { appendLine("Aviso: $it") }
            appendLine("NOTA: USB enumerado NÃO confirma UVC, decodificação ou acesso a quadros.")
            appendLine("LDW/FCW: implementação experimental com calibração e GPS; exige validação no dispositivo. Pedestres: aviso experimental com pré-condições. Ciclistas, placas e semáforos: sem alertas automáticos integrados.")
        }
    }

    fun inspect(context: Context): Report {
        val errors = mutableListOf<String>()
        val activity = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        activity.getMemoryInfo(memory)
        val memoryMb = memory.totalMem / (1024L * 1024L)
        val profile = when {
            memoryMb < 1800 -> "ECONÔMICO"
            memoryMb < 3500 -> "EQUILIBRADO"
            else -> "AVANÇADO"
        }
        val permission = context.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameras = try {
            cameraManager.cameraIdList.mapNotNull { id ->
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val type = when {
                        level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNA"
                        facing == CameraCharacteristics.LENS_FACING_BACK -> "TRASEIRA"
                        facing == CameraCharacteristics.LENS_FACING_FRONT -> "FRONTAL"
                        else -> "OUTRA"
                    }
                    val sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        ?.getOutputSizes(android.graphics.SurfaceTexture::class.java)?.size ?: 0
                    CameraInfo(id, type, sizes)
                } catch (e: Exception) {
                    errors.add("Camera2 ID $id: ${describe(e)}")
                    null
                }
            }
        } catch (e: Exception) {
            errors.add("Camera2: ${describe(e)}. Lista não acessível; contagem zero não comprova ausência de câmera física.")
            emptyList()
        }
        val usbHost = context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        val usb = try {
            val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            manager.deviceList.values.map { device ->
                val interfaces = (0 until device.interfaceCount).joinToString(",") { index ->
                    val item = device.getInterface(index)
                    "${item.interfaceClass}/${item.interfaceSubclass}"
                }
                UsbInfo(device.vendorId, device.productId, device.deviceClass, interfaces,device.deviceSubclass,manager.hasPermission(device))
            }
        } catch (e: Exception) {
            errors.add("USB: ${e.javaClass.simpleName}")
            emptyList()
        }
        return Report(Build.MANUFACTURER ?: "?", Build.MODEL ?: "?", Build.VERSION.SDK_INT,
            memoryMb, profile, permission, usbHost, cameras, usb, errors)
    }
    private fun describe(error:Exception)=error.javaClass.simpleName+
        (error.message?.replace('\n',' ')?.take(240)?.let{": $it"} ?: "")
}
