package br.com.dashcamadas

/** The supplied SDK creates its preview during connect, not during nativeCreateUFD. */
class NativePreviewStartup(private val driver:Driver,private val timeoutMs:Long=5000){
    interface Driver {
        fun connect()
        fun initialized():Boolean
        fun connected():Boolean
        fun attachPreview()
        fun startPreview():Int
    }
    enum class Phase { NEW,WAITING,READY,FAILED,CLOSED }
    var phase=Phase.NEW;private set
    private var startedAt=0L
    fun begin(now:Long){
        check(phase==Phase.NEW)
        startedAt=now;phase=Phase.WAITING
        try{driver.connect()}catch(e:Exception){phase=Phase.FAILED;throw e}
    }
    fun poll(now:Long):Boolean {
        if(phase==Phase.READY)return true
        if(phase!=Phase.WAITING)return false
        try{
            val initialized=driver.initialized()
            val connected=driver.connected()
            if(now<startedAt || now-startedAt>=timeoutMs){
                error("SDK sem prévia em ${timeoutMs/1000} s: conectado=$connected, inicializado=$initialized")
            }
            if(!initialized || !connected)return false
            driver.attachPreview()
            check(driver.startPreview()>=0){"Driver rejeitou iniciar prévia"}
            phase=Phase.READY
            return true
        }catch(e:Exception){phase=Phase.FAILED;throw e}
    }
    fun close(){phase=Phase.CLOSED}
}
