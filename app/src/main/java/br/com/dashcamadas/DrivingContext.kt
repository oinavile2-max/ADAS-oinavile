package br.com.dashcamadas

import android.location.Location
import android.os.SystemClock

/** GPS observations survive Activity detachment, but stale/approximate fixes cannot drive critical alerts. */
object DrivingContext {
    @Volatile private var location:Location?=null
    fun update(value:Location?) {location=value?.let{Location(it)}}
    fun speed():Float? {
        val value=location ?: return null
        val age=SystemClock.elapsedRealtimeNanos()-value.elapsedRealtimeNanos
        if(age<0 || value.isFromMockProvider || !value.hasSpeed() || value.accuracy>30f || age>8_000_000_000L) return null
        return value.speed
    }
}
