package br.com.dashcamadas

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.*
import android.os.*

/** Owns GPS/radar monitoring independently of LIVE. Camera and recording are not owned here. */
class RadarForegroundService : Service(), LocationListener {
    interface Callback {
        fun onLocation(location: Location?)
        fun onAlert(alert: RadarAlert)
    }
    inner class LocalBinder : Binder() { val service: RadarForegroundService get() = this@RadarForegroundService }
    private val binder = LocalBinder()
    var callback: Callback? = null
    private var lastLocation: Location? = null
    private var manager: LocationManager? = null
    private var audio: AlertAudio? = null
    private var monitor: RadarMonitor? = null
    private val handler=Handler(Looper.getMainLooper())
    private val expireFix = object : Runnable {
        override fun run() {
            if(lastLocation!=null && currentLocation()==null) { lastLocation=null;callback?.onLocation(null) }
            handler.postDelayed(this,1000L)
        }
    }
    override fun onBind(intent: Intent): IBinder = binder
    override fun onCreate() {
        super.onCreate()
        val notifications = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) notifications.createNotificationChannel(
            NotificationChannel(CHANNEL,"Monitoramento GPS",NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopMonitoring(); stopSelf(); return START_NOT_STICKY }
        val fine=checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
        val coarse=checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) { stopMonitoring(); stopSelf(); return START_NOT_STICKY }
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,1,Intent(this,RadarForegroundService::class.java).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder=if(Build.VERSION.SDK_INT>=26) Notification.Builder(this,CHANNEL) else Notification.Builder(this)
        val notification=builder.setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("DashCam ADAS PRO — GPS")
            .setContentText("Radares cadastrados — monitoramento GPS")
            .setContentIntent(open).setOngoing(true).addAction(Notification.Action.Builder(null,"Encerrar GPS",stop).build()).build()
        try {
            if(Build.VERSION.SDK_INT>=29) startForeground(71,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            else startForeground(71,notification)
            handler.removeCallbacks(expireFix)
            handler.post(expireFix)
            if(manager==null) {
                audio=AlertAudio(this)
                val events=EventStore(this)
                monitor=RadarMonitor(RadarStore(this)) { alert ->
                    audio?.announce(alert.spokenMessage)
                    events.add("RADAR GPS",alert.spokenMessage,"GPS / BANCO LOCAL")
                    callback?.onAlert(alert)
                }
                manager=getSystemService(LocationManager::class.java)
                val provider=if(fine) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
                manager?.requestLocationUpdates(provider,1000L,1f,this)
            }
        } catch(e:Exception) { android.util.Log.e("DashCamGPS","Falha no serviço GPS",e); stopMonitoring(); stopSelf() }
        return START_NOT_STICKY
    }
    fun currentLocation(): Location? = lastLocation?.takeIf { SystemClock.elapsedRealtimeNanos()-it.elapsedRealtimeNanos < 10_000_000_000L }
    override fun onLocationChanged(location: Location) {
        lastLocation=location
        DrivingContext.update(location)
        monitor?.onLocation(location)
        callback?.onLocation(location)
    }
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) { lastLocation=null; DrivingContext.update(null); callback?.onLocation(null) }
    @Deprecated("Legacy Android callback")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    private fun stopMonitoring() {
        handler.removeCallbacksAndMessages(null)
        manager?.removeUpdates(this);manager=null
        lastLocation=null
        DrivingContext.update(null)
        callback?.onLocation(null)
        audio?.release();audio=null
        stopForeground(STOP_FOREGROUND_REMOVE)
        monitor=null
    }
    override fun onUnbind(intent: Intent?): Boolean { callback=null;return super.onUnbind(intent) }
    override fun onDestroy() { stopMonitoring();callback=null;super.onDestroy() }
    companion object {
        private const val CHANNEL="dashcam_gps"
        private const val STOP="br.com.dashcamadas.STOP_GPS"
    }
}
