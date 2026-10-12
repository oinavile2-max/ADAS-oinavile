package br.com.dashcamadas

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.view.Gravity
import android.hardware.camera2.*
import android.location.LocationManager
import android.os.*
import android.util.Log
import android.view.*
import android.widget.*
import java.util.*

/**
 * LIVE renders real frames from the independent foreground camera service.
 * Camera2/native USB, recording and both neural models belong to the foreground service.
 */
class MainActivity : Activity() {
    private lateinit var texture: CameraPreviewView
    private lateinit var laneOverlay: RiskOverlayView
    private var laneEnabled = true
    private lateinit var status: TextView
    private lateinit var speed: TextView
    private lateinit var record: Button
    private lateinit var manager: CameraManager
    private val handler=Handler(Looper.getMainLooper())
    private var recording=false
    private var selectedId:String?=null
    private var captureService:DashCamForegroundService?=null
    private var captureBound=false
    private var latestVision:VisionFrame?=null
    private var captureState=DashCamForegroundService.State()
    private val captureCallback=object:DashCamForegroundService.Callback {
        override fun onState(state:DashCamForegroundService.State) {
            captureState=state;recording=state.recording
            record.text=if(recording) "STOP" else "REC"
            record.isEnabled=state.connected
            updateHud();showStatus(state.message)
            if(!state.connected || state.message.startsWith("Sem quadros")) {
                texture.frame=null;laneOverlay.result=null;latestVision=null
                cameraPlaceholder.visibility=View.VISIBLE
            }
            if(state.message.contains("ADAS suspenso") || state.message.startsWith("IA indisponível")){laneOverlay.result=null;latestVision=null}
        }
        override fun onPreview(bitmap:android.graphics.Bitmap,timestampMs:Long) {
            val analyzed=latestVision
            if(analyzed!=null && SystemClock.elapsedRealtime()-analyzed.timestampMs<1200)return
            texture.timestampMs=timestampMs;texture.frame=bitmap
            laneOverlay.result=null
            cameraPlaceholder.visibility=View.GONE
            updateCameraTransform()
        }
        override fun onFrame(frame:VisionFrame) {
            if(SystemClock.elapsedRealtime()-frame.timestampMs>1200 || (latestVision?.timestampMs ?: -1)>frame.timestampMs)return
            latestVision=frame
            // Display the analyzed bitmap with its own detections; no old boxes on a new image.
            texture.timestampMs=frame.timestampMs;texture.frame=frame.bitmap;laneOverlay.result=frame
            cameraPlaceholder.visibility=View.GONE
            showStatus(if(!frame.laneNetworkActive)"Rede neural de faixas indisponível" else if(frame.inferenceMs>500)"IA ativa, ${frame.inferenceMs} ms — alarmes suspensos por atraso" else if(!frame.vehicleNetworkActive)"IA de faixas ativa · veículos indisponíveis" else "IA ativa · ${frame.inferenceMs} ms · faixas e veículos")
        }
        override fun onAlert(message:String){showPremiumAlert("ADAS",message,2)}
    }
    private val captureConnection=object:android.content.ServiceConnection {
        override fun onServiceConnected(name:android.content.ComponentName,binder:IBinder) {
            if(!captureBound)return
            captureService=(binder as DashCamForegroundService.LocalBinder).service
            captureService?.callback=captureCallback
            captureService?.state?.let{captureCallback.onState(it)}
        }
        override fun onServiceDisconnected(name:android.content.ComponentName) {
            captureService=null;captureCallback.onState(DashCamForegroundService.State(message="Serviço de câmera indisponível"))
        }
    }
    private lateinit var events: EventStore
    private lateinit var radarStore: RadarStore
    private lateinit var audioAlerts: AlertAudio
    private var lastGps: android.location.Location? = null
    private var radarService: RadarForegroundService? = null
    private var radarBound = false
    private val radarCallback = object : RadarForegroundService.Callback {
        override fun onLocation(location: android.location.Location?) {
            lastGps = location
            updateHud()
            speed.text = if (location?.hasSpeed() == true) "%.0f km/h".format(Locale.US,location.speed*3.6f) else "-- km/h"
        }
        override fun onAlert(message: String) { showStatus(message);showPremiumAlert("RADAR GPS",message,1) }
    }
    private val radarConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName, binder: IBinder) {
            if (!radarBound) return
            radarService = (binder as RadarForegroundService.LocalBinder).service
            radarService?.callback = radarCallback
            radarCallback.onLocation(radarService?.currentLocation())
        }
        override fun onServiceDisconnected(name: android.content.ComponentName) { radarService=null;radarCallback.onLocation(null) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // A interface deve abrir mesmo quando serviços, TTS ou câmeras falharem.
        try {
            buildUi()
        } catch (t: Throwable) {
            Log.e("DashCamStartup", "Falha construindo LIVE", t)
            showStartupFailure("Falha na interface LIVE", t)
            return
        }
        try {
            manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        } catch (t: Throwable) {
            Log.e("DashCamStartup", "CameraManager indisponível", t)
            showStatus("Serviço de câmera indisponível; interface ativa")
        }
        try {
            events = EventStore(this)
            radarStore = RadarStore(this)
        } catch (t: Throwable) {
            Log.e("DashCamStartup", "Falha ao iniciar dados locais", t)
            showStatus("Dados locais indisponíveis; LIVE em modo limitado")
        }
        try {
            audioAlerts = AlertAudio(this)
        } catch (t: Throwable) {
            Log.e("DashCamStartup", "TTS indisponível; áudio desativado", t)
            showStatus("Voz indisponível; LIVE permanece ativa")
        }
        try {
            requestPermissionsIfNeeded()
        } catch (t: Throwable) {
            Log.e("DashCamStartup", "Falha nas permissões", t)
            showStatus("Permissões indisponíveis; abra configurações do Android")
        }
    }

    private fun showStartupFailure(message: String, error: Throwable) {
        val text = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            text = "$message\n${error.javaClass.simpleName}: ${error.message ?: "sem detalhes"}\nVerifique logcat: DashCamStartup"
            setBackgroundColor(Color.rgb(5, 16, 29))
        }
        setContentView(text)
        SafeInsets.apply(this)
    }

    private lateinit var alertBanner: TextView
    private lateinit var cameraPlaceholder: TextView
    private lateinit var headerBar: LinearLayout
    private lateinit var sidePanel: LinearLayout
    private lateinit var footerBar: LinearLayout
    private lateinit var statusBar: TextView
    private var cameraFullscreen = false
    private var cameraFill = false
    private var cameraMirror = false
    private var controlsHidden = false
    private var touchRevealedControls = false
    private lateinit var gpsRec: TextView
    private val hideControls = Runnable {
        if (!cameraFullscreen && ::footerBar.isInitialized) {
            controlsHidden = true
            footerBar.visibility = View.GONE
            sidePanel.visibility = View.GONE
        }
    }
    private fun revealControls() {
        controlsHidden = false
        if (::footerBar.isInitialized && !cameraFullscreen) {
            footerBar.visibility = View.VISIBLE; sidePanel.visibility = View.VISIBLE
        }
        handler.removeCallbacks(hideControls)
        val timeout = getSharedPreferences("camera", MODE_PRIVATE).getInt("controls_timeout", 0)
        if (timeout > 0) handler.postDelayed(hideControls, timeout * 1000L)
    }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) { touchRevealedControls = controlsHidden; revealControls() }
        return super.dispatchTouchEvent(event)
    }
    private fun updateHud() {
        if (::gpsRec.isInitialized) gpsRec.text = "GPS " + (if (lastGps != null) "OK" else "--") + " · " + (if (recording) "● REC" else "REC OFF")
    }
    private var cameraFacing = CameraCharacteristics.LENS_FACING_BACK
    private var cameraRotationOverride = -1 // -1 = automática; 0/90/180/270 = manual
    private lateinit var fullButton: TextView
    private lateinit var alertIcon: TextView
    private lateinit var alertMessage: TextView
    private lateinit var alertContainer: LinearLayout
    private fun orientationPrefs() = getSharedPreferences("camera_orientation", MODE_PRIVATE)
    private fun loadOrientation(id: String) {
        cameraRotationOverride = orientationPrefs().getInt("rotation_$id", -1)
        cameraFill = orientationPrefs().getBoolean("fill_$id", false)
        cameraMirror = orientationPrefs().getBoolean("mirror_$id", cameraFacing == CameraCharacteristics.LENS_FACING_FRONT)
    }
    private fun cycleCameraRotation() {
        val id = selectedId ?: return
        cameraRotationOverride = when(cameraRotationOverride) { -1 -> 0; 0 -> 90; 90 -> 180; 180 -> 270; else -> -1 }
        orientationPrefs().edit().putInt("rotation_$id", cameraRotationOverride).apply()
        updateCameraTransform()
        captureService?.orientationChanged()
        showStatus("Orientação: " + if (cameraRotationOverride == -1) "automática" else "${cameraRotationOverride}°")
    }
    /** Orientação do sensor + FIT/FILL sem distorção; manual por câmera quando firmware informa errado. */
    private fun updateCameraTransform() {
        if(!::texture.isInitialized)return
        texture.fill=cameraFill;texture.mirror=cameraMirror;texture.invalidate()
        if(::laneOverlay.isInitialized)laneOverlay.invalidate()
    }

    private fun toggleFullscreen() {
        cameraFullscreen = !cameraFullscreen
        headerBar.visibility = if (cameraFullscreen) View.GONE else View.VISIBLE
        sidePanel.visibility = if (cameraFullscreen) View.GONE else View.VISIBLE
        footerBar.visibility = if (cameraFullscreen) View.GONE else View.VISIBLE
        statusBar.visibility = if (cameraFullscreen) View.GONE else View.VISIBLE
        // Tela cheia prioriza o preenchimento, sempre sem distorção.
        // FULL preserves the camera FIT/FILL preference.
        if (::fullButton.isInitialized) fullButton.text = if (cameraFullscreen) "SAIR ⛶" else "⛶ FULL"
        revealControls()
        texture.post { updateCameraTransform() }
    }
    override fun onBackPressed() { if (cameraFullscreen) toggleFullscreen() else super.onBackPressed() }
    private var bannerTask: Runnable? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun panel(fill: Int, stroke: Int, radius: Int = 14): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(fill, Color.rgb(6, 15, 28))).apply {
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
        }

    private fun uiText(value: String, size: Float, color: Int = Color.WHITE): TextView =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

    private fun navButton(title: String, action: () -> Unit): TextView =
        uiText(title, 12f).apply {
            gravity = Gravity.CENTER
            background = panel(Color.rgb(23, 38, 58), Color.rgb(65, 102, 137), 10)
            setOnClickListener { action() }
        }

    private fun premiumTile(label: String, active: Boolean, action: () -> Unit): TextView =
        uiText(label, 12f, if (active) Color.WHITE else Color.rgb(221, 233, 247)).apply {
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            val top = if (active) Color.rgb(15, 110, 188) else Color.rgb(48, 62, 79)
            val bottom = if (active) Color.rgb(5, 37, 85) else Color.rgb(9, 20, 36)
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(top, bottom)).apply {
                cornerRadius = dp(12).toFloat()
                setStroke(dp(if (active) 2 else 1),
                    if (active) Color.rgb(58, 208, 255) else Color.rgb(86, 107, 133))
            }
            setOnClickListener { action() }
        }

    private fun buildUi() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        val cyan = Color.rgb(46, 195, 255)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(2, 10, 23))
            setPadding(dp(12), dp(7), dp(12), dp(9))
        }
        // HUD: clear hierarchy, short labels and truthful sensor status.
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(18, 34, 53), Color.rgb(3, 12, 27))).apply {
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.rgb(38, 95, 141))
            }
            setPadding(dp(12), 0, dp(8), 0)
        }
        headerBar = header
        header.addView(uiText("◉  DashCam ADAS PRO", 19f, Color.WHITE).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, -1, 1f))
        speed = uiText("-- km/h", 21f, cyan).apply {
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        header.addView(speed, LinearLayout.LayoutParams(dp(112), -1))
        gpsRec = uiText("GPS -- · REC OFF", 11f, Color.rgb(170, 212, 236))
        header.addView(gpsRec,
            LinearLayout.LayoutParams(dp(94), -1))
        header.addView(premiumTile("⚙", false) {
            startActivity(Intent(this, PremiumMenuActivity::class.java))
        }, LinearLayout.LayoutParams(dp(48), dp(42)))
        root.addView(header, LinearLayout.LayoutParams(-1, dp(57)))

        val liveRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val cameraFrame = FrameLayout(this).apply {
            background = panel(Color.rgb(6, 18, 35), cyan)
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }
        // TextureView rejeita qualquer background drawable (inclusive setBackgroundColor).
        // A cor e a borda pertencem ao FrameLayout pai, nunca à superfície de vídeo.
        texture = CameraPreviewView(this)
        cameraFrame.addView(texture, FrameLayout.LayoutParams(-1, -1))
        laneOverlay = RiskOverlayView(this).apply { preview=texture;setOnTouchListener { _, _ -> false } }
        cameraFrame.addView(laneOverlay, FrameLayout.LayoutParams(-1, -1))
        texture.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateCameraTransform() }
        val detector = android.view.GestureDetector(this,
            object : android.view.GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: android.view.MotionEvent): Boolean = true
                override fun onSingleTapConfirmed(e: android.view.MotionEvent): Boolean {
                    if (touchRevealedControls) { touchRevealedControls = false; return true }
                    cameraFill = !cameraFill
                    selectedId?.let { orientationPrefs().edit().putBoolean("fill_$it", cameraFill).apply() }
                    updateCameraTransform()
                    showStatus(if (cameraFill) "Câmera: preencher" else "Câmera: ajustar")
                    return true
                }
                override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                    toggleFullscreen()
                    return true
                }
            })
        texture.setOnTouchListener { _, event -> detector.onTouchEvent(event) }

        cameraPlaceholder = uiText("◉\nCÂMERA INDISPONÍVEL\nConecte uma câmera compatível", 15f,
            Color.rgb(177, 204, 224)).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(5, 16, 29))
        }
        cameraFrame.addView(cameraPlaceholder, FrameLayout.LayoutParams(-1, -1))

        // Transparent premium alert, inside the camera rather than a blocking Android dialog.
        alertContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(7), dp(12), dp(7))
            visibility = View.GONE
            elevation = dp(8).toFloat()
        }
        alertIcon = uiText("⚠", 29f, Color.WHITE).apply { gravity = Gravity.CENTER }
        alertMessage = uiText("", 16f, Color.WHITE).apply {
            gravity = Gravity.CENTER_VERTICAL
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        alertContainer.addView(alertIcon, LinearLayout.LayoutParams(dp(56), -1))
        alertContainer.addView(alertMessage, LinearLayout.LayoutParams(0, -1, 1f))
        cameraFrame.addView(alertContainer,
            FrameLayout.LayoutParams(-1, dp(84), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                setMargins(dp(105), dp(52), dp(105), 0)
            })
        val cameraTag = uiText("  ● LIVE", 11f, cyan).apply {
            background = panel(Color.rgb(10, 26, 44), Color.rgb(39, 110, 153), 7)
        }
        cameraFrame.addView(cameraTag, FrameLayout.LayoutParams(dp(80), dp(29),
            Gravity.TOP or Gravity.LEFT).apply {
            setMargins(dp(12), dp(12), 0, 0)
        })
        fullButton = premiumTile("⛶ FULL", false) { toggleFullscreen() }
        cameraFrame.addView(fullButton, FrameLayout.LayoutParams(dp(94), dp(36),
            Gravity.TOP or Gravity.RIGHT).apply { setMargins(0, dp(8), dp(10), 0) })
        val laneButton = premiumTile("FAIXAS BETA", false) {
            laneEnabled = !laneEnabled
            laneOverlay.showLanes=laneEnabled
            laneOverlay.invalidate()
            showStatus(if (laneEnabled) "Faixas experimentais ativadas" else "Faixas desativadas")
        }
        cameraFrame.addView(laneButton, FrameLayout.LayoutParams(dp(150), dp(36),
            Gravity.BOTTOM or Gravity.LEFT).apply { setMargins(dp(10), 0, 0, dp(10)) })
        val rotationButton = premiumTile("⟳ GIRAR", false) { cycleCameraRotation() }
        cameraFrame.addView(rotationButton, FrameLayout.LayoutParams(dp(94), dp(36),
            Gravity.BOTTOM or Gravity.RIGHT).apply { setMargins(0, 0, dp(10), dp(10)) })
        val mirrorButton = premiumTile("ESPELHAR", false) {
            cameraMirror = !cameraMirror
            selectedId?.let { orientationPrefs().edit().putBoolean("mirror_$it", cameraMirror).apply() }
            updateCameraTransform()
        }
        cameraFrame.addView(mirrorButton, FrameLayout.LayoutParams(dp(110), dp(36), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(8) })
        liveRow.addView(cameraFrame, LinearLayout.LayoutParams(0, -1, 3.25f))

        // No fake secondary camera: clearly show unsupported feed until actual ADAS exists.
        val side = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panel(Color.rgb(10, 31, 54), cyan)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        side.addView(uiText("◈  ADAS", 13f, cyan).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-1, dp(38)))
        side.addView(uiText("VÍDEO AUXILIAR\nINDISPONÍVEL", 12f,
            Color.rgb(162, 195, 216)).apply {
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        side.addView(premiumTile("CÂMERA", false) { chooseCamera() },
            LinearLayout.LayoutParams(-1, dp(43)))
        liveRow.addView(side, LinearLayout.LayoutParams(0, -1, 1.0f).apply {
            leftMargin = dp(9)
        })
        sidePanel = side
        root.addView(liveRow, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = dp(9)
        })

        status = uiText("Câmera: verificando disponibilidade", 10f, Color.rgb(149, 180, 204))
        statusBar = status
        root.addView(status, LinearLayout.LayoutParams(-1, dp(25)))

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        fun add(label: String, weight: Float = 1f, active: Boolean = false, action: () -> Unit) {
            controls.addView(premiumTile(label, active, action),
                LinearLayout.LayoutParams(0, dp(57), weight).apply { rightMargin = dp(6) })
        }
        record = Button(this).apply {
            text = "REC"; textSize = 12f; isEnabled = false
            setTextColor(Color.WHITE)
            background = panel(Color.rgb(100, 26, 39), Color.rgb(255, 79, 99), 11)
            setOnClickListener { if (recording) stopRecording() else startRecording() }
        }
        add("LIVE", active = true) { chooseCamera() }
        add("ADAS") { calibrateAdas() }
        add("RADARES") { listRadars() }
        add("MÍDIA") { showStatus("Clipes disponíveis no armazenamento do aplicativo") }
        add("EVENTOS") { protectCurrentClip() }
        add("CONFIG") { startActivity(Intent(this, PremiumMenuActivity::class.java)) }
        controls.addView(record, LinearLayout.LayoutParams(0, dp(57), 0.72f))
        root.addView(controls)
        footerBar = controls
        setContentView(root)
        SafeInsets.apply(this)
        revealControls()
    }

    /** Banner integrado à LIVE; não usa AlertDialog nem apresenta simulações como detecção real. */
    private fun showPremiumAlert(title: String, detail: String, severity: Int) {
        runOnUiThread {
            bannerTask?.let { handler.removeCallbacks(it) }
            val edge = if (severity == 2) Color.rgb(255, 79, 90)
                else if (severity == 1) Color.rgb(255, 184, 64)
                else Color.rgb(66, 195, 255)
            val fill = if (severity == 2) Color.rgb(85, 23, 32)
                else if (severity == 1) Color.rgb(87, 51, 17)
                else Color.rgb(14, 49, 75)
            alertContainer.background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(224, Color.red(fill), Color.green(fill), Color.blue(fill)),
                    Color.argb(235, 12, 19, 30))
            ).apply {
                cornerRadius = dp(16).toFloat()
                setStroke(dp(2), edge)
            }
            val label = (title + " " + detail).uppercase(Locale.ROOT)
            alertIcon.text = when {
                "PARE" in label -> "🛑"
                "PEDESTRE" in label -> "🚶"
                "CICLIST" in label || "BICICLET" in label -> "🚴"
                "RADAR" in label || "VELOCIDADE" in label -> "◉"
                "COLIS" in label -> "⚠"
                "SEMÁFORO" in label || "SEMAFORO" in label -> "🚦"
                else -> "⚠"
            }
            alertMessage.text = "$title\n$detail"
            alertContainer.visibility = View.VISIBLE
            bannerTask = Runnable { alertContainer.visibility = View.GONE }
            handler.postDelayed(bannerTask!!, 4500L)
        }
    }

    private fun requestPermissionsIfNeeded() {
        val required = buildList {
            add(Manifest.permission.CAMERA)
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (required.isNotEmpty()) requestPermissions(required.toTypedArray(), 41)
        else { scanCameras(); startGps() }
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, grants: IntArray) {
        super.onRequestPermissionsResult(code, permissions, grants)
        if (code == 41) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) scanCameras()
            else showStatus("Permissão de câmera necessária")
            startGps()
        }
    }

    private fun chooseCamera() {
        try {
            val ids = availableCamera2Ids()
            val usbIds=availableNativeUsb()
            if(ids.isEmpty() && usbIds.isEmpty()){
                showCompatibilityReport()
                return
            }
            val cameraLabels=ids.map { id ->
                val c=manager.getCameraCharacteristics(id)
                val facing=c.get(CameraCharacteristics.LENS_FACING)
                val level=c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                "ID $id • " + when {level==CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "USB/EXTERNA";facing==CameraCharacteristics.LENS_FACING_BACK->"TRASEIRA TABLET";facing==CameraCharacteristics.LENS_FACING_FRONT->"FRONTAL TABLET";else->"OUTRA"}
            }
            val all=ids.toList()+usbIds
            val labels=cameraLabels+usbIds.map{"USB NATIVO • "+it.removePrefix("usb:")}
            AlertDialog.Builder(this).setTitle("Selecionar câmera disponível").setItems(labels.toTypedArray()){_,which->
                val id=all[which]
                if(id.startsWith("usb:"))startActivity(Intent(this,NativeUsbActivity::class.java).putExtra("device_name",id.removePrefix("usb:")))
                else {selectedId=id;openCamera(id);getSharedPreferences("camera",MODE_PRIVATE).edit().putString("preferred",id).apply()}
            }.setNeutralButton("Diagnóstico") { _, _ -> showCompatibilityReport() }
                .setNegativeButton("Cancelar",null).show()
        } catch(e:Exception){showStatus("Erro ao listar câmeras: ${e.message}")}
    }
    private fun availableCamera2Ids():Array<String>{
        if(!::manager.isInitialized)return emptyArray()
        return try{manager.cameraIdList}catch(e:Exception){Log.w("DashCamCamera","Camera2 indisponível",e);emptyArray()}
    }
    private fun availableNativeUsb():List<String>{
        if(!BuildConfig.K2000_NATIVE || Build.VERSION.SDK_INT !in 24..27)return emptyList()
        val usb=getSystemService(android.hardware.usb.UsbManager::class.java)
        return usb.deviceList.values.filter{NativeUsbPolicy.cameraCandidate(it.deviceClass,(0 until it.interfaceCount).map{i->it.getInterface(i).interfaceClass})}.map{"usb:"+it.deviceName}
    }

    private fun showCompatibilityReport() {
        val report = DeviceCapabilities.inspect(this)
        AlertDialog.Builder(this)
            .setTitle("Compatibilidade do dispositivo")
            .setMessage(report.asText())
            .setPositiveButton("Fechar", null)
            .setNeutralButton("Compartilhar") { _, _ ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, report.asText())
                }
                startActivity(Intent.createChooser(intent, "Enviar diagnóstico"))
            }
            .show()
    }

    private fun scanCameras() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            showStatus("Autorize a câmera antes de buscar dispositivos"); return
        }
        try {
            val ids = availableCamera2Ids()
            val preferred = getSharedPreferences("camera",MODE_PRIVATE).getString("preferred",null)
            if(preferred!=null && preferred in availableNativeUsb()){
                val usb=getSystemService(android.hardware.usb.UsbManager::class.java)
                val device=usb.deviceList.values.first{it.deviceName==preferred.removePrefix("usb:")}
                if(!usb.hasPermission(device)){showStatus("Selecione a câmera USB e autorize o Android");return}
                if(selectedId==preferred && captureState.connected && captureBound)return
                openCamera(preferred);return
            }
            if (ids.isEmpty()) {
                selectedId = null
                if (::record.isInitialized) record.isEnabled = false
                showStatus(if(availableNativeUsb().isNotEmpty())"Selecione USB pelo botão CÂMERA para autorizar e iniciar a IA" else "Nenhuma câmera disponível")
                cameraPlaceholder.visibility = View.VISIBLE
                return
            }
            val external = ids.firstOrNull { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL
            }
            val rear = ids.firstOrNull { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
            val newId = preferred?.takeIf { ids.contains(it) } ?: external ?: rear ?: ids.first()
            if (selectedId == newId && captureState.connected && captureBound) return
            selectedId = newId
            showStatus("Camera2 ID $newId detectada; tentando conectar...")
            openCamera(newId)
        } catch (e: Exception) { showStatus("Erro de descoberta: ${e.message}") }
    }

    private fun openCamera(id:String) {
        selectedId=id
        if(id.startsWith("usb:"))cameraFacing=CameraCharacteristics.LENS_FACING_BACK
        else {val chars=manager.getCameraCharacteristics(id);cameraFacing=chars.get(CameraCharacteristics.LENS_FACING) ?: CameraCharacteristics.LENS_FACING_BACK}
        loadOrientation(id)
        val display=when(windowManager.defaultDisplay.rotation){Surface.ROTATION_90->90;Surface.ROTATION_180->180;Surface.ROTATION_270->270;else->0}
        orientationPrefs().edit().putInt("display_rotation_$id",display).apply()
        val intent=Intent(this,DashCamForegroundService::class.java).putExtra("camera_id",id)
        try {
            if(Build.VERSION.SDK_INT>=26)startForegroundService(intent) else startService(intent)
            if(!captureBound)captureBound=bindService(intent,captureConnection,Context.BIND_AUTO_CREATE)
        }catch(e:Exception){showStatus("Serviço de câmera não iniciou: ${e.javaClass.simpleName}")}
    }
    private fun startRecording(){captureService?.startRecording() ?: showStatus("Aguarde conexão com o serviço de câmera")}
    private fun stopRecording(){captureService?.stopRecording()}
    private fun protectCurrentClip(){captureService?.protect() ?: showStatus("Serviço de gravação indisponível")}
    private fun calibrateAdas() {
        val id=selectedId ?: return
        if((DrivingContext.speed() ?: 0f)>.5f){showStatus("Pare o veículo antes de calibrar");return}
        val prefs=getSharedPreferences("adas_calibration",MODE_PRIVATE)
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(8),dp(16),dp(8))}
        box.addView(TextView(this).apply{text="CALIBRAÇÃO EXPERIMENTAL — veículo parado e câmera alinhada. Informe medidas reais. Alarmes exigem GPS válido e detecções confiáveis; não substituem atenção ao conduzir."})
        fun field(label:String,value:Float):EditText {
            box.addView(TextView(this).apply{text=label})
            return EditText(this).apply{inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;setText(value.toString());box.addView(this)}
        }
        val l=latestVision?.lanes?.left;val r=latestVision?.lanes?.right
        val observed=if(l!=null && r!=null && minOf(l.confidence,r.confidence)>=.65f && l.yStart<=.93f && r.yStart<=.93f && l.yEnd>=.93f && r.yEnd>=.93f)(l.x(.93f)+r.x(.93f))/2 else prefs.getFloat("ego_$id",.5f)
        val center=field("Centro no quadro completo (0,15 a 0,85; confira montagem)",if(cameraMirror)1f-observed else observed)
        val vehicle=field("Largura real do veículo (metros)",prefs.getFloat("vehicle_width_$id",1.8f))
        val lane=field("Largura de referência da faixa (metros)",prefs.getFloat("lane_width_$id",3.5f))
        val dialog=AlertDialog.Builder(this).setTitle("Calibrar LDW / FCW").setView(box)
            .setNegativeButton("Cancelar",null).setNeutralButton("Desativar alarmes"){_,_->prefs.edit().putBoolean("enabled_$id",false).apply()}
            .setPositiveButton("Confirmar medidas",null).create()
        dialog.setOnShowListener{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
            val displayCenter=center.text.toString().replace(',','.').toFloatOrNull() ?: -1f
            val calibration=br.com.dashcamadas.vision.Calibration(true,if(cameraMirror)1f-displayCenter else displayCenter,vehicle.text.toString().replace(',','.').toFloatOrNull() ?: -1f,lane.text.toString().replace(',','.').toFloatOrNull() ?: -1f)
            if(!calibration.valid){showStatus("Medidas inválidas; calibração não salva");return@setOnClickListener}
            prefs.edit().putBoolean("enabled_$id",true).putFloat("ego_$id",calibration.egoX).putFloat("vehicle_width_$id",calibration.vehicleWidthM).putFloat("lane_width_$id",calibration.laneWidthM).apply()
            showStatus("Calibração salva para câmera $id; alertas experimentais sujeitos à confiança")
            dialog.dismiss()
        }}
        dialog.show()
    }

    private fun startGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val intent = Intent(this, RadarForegroundService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
            if (!radarBound) radarBound = bindService(intent, radarConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) { radarCallback.onLocation(null); showStatus("GPS indisponível: ${e.javaClass.simpleName}") }
    }
    override fun onStop() {
        if(radarService?.callback===radarCallback)radarService?.callback = null
        radarService = null
        if (radarBound) { unbindService(radarConnection); radarBound=false }
        if(captureService?.callback===captureCallback)captureService?.callback=null
        captureService=null
        if(captureBound){unbindService(captureConnection);captureBound=false}
        latestVision=null;texture.frame=null;laneOverlay.result=null
        super.onStop()
    }

    private fun addRadarDialog() {
        val loc = lastGps
        if (loc == null || loc.accuracy > 50f) { showStatus("Aguarde sinal GPS com precisão melhor que 50 m");return }
        val limits = arrayOf("30", "40", "50", "60", "70", "80", "90", "100", "110", "120")
        val types = arrayOf("fixo", "semaforo", "movel")
        val wrapper = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22,8,22,8) }
        val limit = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,limits);setSelection(3) }
        val type = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,types) }
        wrapper.addView(TextView(this).apply {text="Limite (km/h)"});wrapper.addView(limit)
        wrapper.addView(TextView(this).apply {text="Tipo"});wrapper.addView(type)
        AlertDialog.Builder(this).setTitle("Adicionar radar neste GPS").setView(wrapper)
            .setNegativeButton("Cancelar",null).setPositiveButton("Salvar") { _, _ ->
                radarStore.add(loc,limits[limit.selectedItemPosition].toInt(),types[type.selectedItemPosition])
                showStatus("Radar salvo localmente • ${radarStore.all().size} ponto(s)")
            }.show()
    }

    private fun listRadars() {
        val items=radarStore.all()
        if (items.isEmpty()) {showStatus("Não há radares cadastrados");return}
        AlertDialog.Builder(this).setTitle("Radares — toque para excluir")
            .setItems(items.map { "${it.type} ${it.limit} km/h • %.5f, %.5f".format(Locale.US,it.lat,it.lon) }.toTypedArray()) { _, index ->
                AlertDialog.Builder(this).setTitle("Excluir radar?").setNegativeButton("Cancelar",null)
                    .setPositiveButton("Excluir") { _, _ -> radarStore.remove(items[index].id); showStatus("Radar excluído") }.show()
            }.setNegativeButton("Fechar",null).show()
    }

    private fun showStatus(message: String) { runOnUiThread { status.text = message } }
    override fun onResume() {
        super.onResume()
        revealControls()
        if(::speed.isInitialized)startGps()
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)scanCameras()
    }
    override fun onDestroy() {handler.removeCallbacksAndMessages(null);if(::audioAlerts.isInitialized)audioAlerts.release();super.onDestroy()}
}
