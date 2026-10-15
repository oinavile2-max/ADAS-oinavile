package br.com.dashcamadas

/** A deadline and actual UsbManager permission are both required, including after Activity resume. */
class UsbPermissionRequest {
    enum class Result{GRANTED,DENIED,EXPIRED,STALE}
    var device:String?=null;private set
    private var deadline=0L
    fun begin(name:String,now:Long){require(name.isNotBlank() && now in 0..Long.MAX_VALUE-20000);device=name;deadline=now+20000}
    fun cancel(){device=null;deadline=0}
    fun expire(now:Long):Boolean {if(device!=null && now>=deadline){cancel();return true};return false}
    fun complete(name:String,granted:Boolean,actualPermission:Boolean,now:Long):Result {
        if(device!=name)return Result.STALE
        if(expire(now))return Result.EXPIRED
        cancel();return if(granted && actualPermission)Result.GRANTED else Result.DENIED
    }
}
