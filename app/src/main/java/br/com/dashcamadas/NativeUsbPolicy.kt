package br.com.dashcamadas

/** Exact UFD filters from the supplied APK, plus video/vendor interfaces. Never attach automatically. */
object NativeUsbPolicy {
    fun referenceDvr(vendorId:Int,productId:Int,deviceClass:Int,deviceSubclass:Int):Boolean =
        deviceSubclass==0 && when(vendorId to productId){
            0x3322 to 0x6655, 0x2298 to 0x6698, 0x0098 to 0x1198,
            0x0198 to 0x1298, 0x1b3f to 0x8301 -> deviceClass==0
            0x0eff to 0x0001 -> deviceClass==8
            else -> false
        }

    fun cameraCandidate(deviceClass:Int,interfaces:List<Int>,vendorId:Int=-1,productId:Int=-1,deviceSubclass:Int=-1)=
        referenceDvr(vendorId,productId,deviceClass,deviceSubclass) ||
            deviceClass==14 || deviceClass==255 || interfaces.any{it==14 || it==255}
}
