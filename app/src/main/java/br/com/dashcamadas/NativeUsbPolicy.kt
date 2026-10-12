package br.com.dashcamadas

/** Only video or vendor camera interfaces are offered. No automatic USB attachment. */
object NativeUsbPolicy {
    fun cameraCandidate(deviceClass:Int,interfaces:List<Int>)=deviceClass==14 || deviceClass==255 || interfaces.any{it==14 || it==255}
}
