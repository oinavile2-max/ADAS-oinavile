package br.com.dashcamadas

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
class MainActivity : ComponentActivity() {
    private lateinit var texture: CameraPreviewView
    private lateinit var laneOverlay: RiskOverlayView
    private var laneEnabled = true
    private lateinit var status: TextView
    private lateinit var hud: LiveHudView
    private lateinit var dashboardScene: ReferenceDashboardLayout
    private lateinit var headerSurface: DashboardHeaderView
    private lateinit var qualityTag: TextView
    private lateinit var auxiliaryPreview: CameraPreviewView
    private lateinit var auxiliaryOverlay: RiskOverlayView
    private lateinit var auxiliaryPlaceholder: TextView
    private lateinit var record: Button
    private lateinit var manager: CameraManager
    private val handler=Handler(Looper.getMainLooper())
    private var recording=false
    private var selectedId:String?=null
    private var captureService:DashCamForegroundService?=null
    private var captureBound=false
    private var latestVision:VisionFrame?=null
    private val previewTimeline=PreviewTimeline()
    private var captureState=DashCamForegroundService.State()
    private val captureCallback=object:DashCamForegroundService.Callback {
        override fun onState(state:DashCamForegroundService.State) {
            captureState=state;recording=state.recording
            texture.requestDirectPreview(state.connected && state.cameraId.isNotEmpty() && !state.cameraId.startsWith("usb:") && !state.directPreviewUnavailable)
            texture.showDirectPreview(state.directPreview,state.livePreviewWidth,state.livePreviewHeight,state.previewRotation)
            if(state.directPreview)laneOverlay.result=null
            record.isSelected=recording
            record.background.invalidateSelf()
            record.contentDescription=if(recording) "Parar gravação" else "Iniciar gravação"
            record.isEnabled=state.connected
            updateHud();showStatus(state.message)
            if(!state.connected || state.message.startsWith("Sem quadros")) {
                previewTimeline.reset()
                texture.frame=null;laneOverlay.result=null;latestVision=null
                auxiliaryPreview.frame=null;auxiliaryPreview.focusBox=null;auxiliaryOverlay.result=null
                auxiliaryPlaceholder.visibility=View.VISIBLE
                cameraPlaceholder.visibility=View.VISIBLE
            }
            if(state.message.contains("ADAS suspenso") || state.message.startsWith("IA indisponível")){laneOverlay.result=null;auxiliaryOverlay.result=null;auxiliaryPreview.focusBox=null;latestVision=null}
        }
        override fun onPreview(bitmap:android.graphics.Bitmap,timestampMs:Long) {
            if(!texture.directActive && !previewTimeline.accept(timestampMs))return
            val analyzed=latestVision
            if(!texture.directActive){texture.timestampMs=timestampMs;texture.frame=bitmap}
            laneOverlay.result=null
            if(analyzed==null || SystemClock.elapsedRealtime()-analyzed.timestampMs>1200) {
                auxiliaryPreview.timestampMs=timestampMs;auxiliaryPreview.frame=bitmap;auxiliaryPreview.focusBox=null;auxiliaryOverlay.result=null
            }
            auxiliaryPlaceholder.visibility=View.GONE
            cameraPlaceholder.visibility=View.GONE
            updateCameraTransform()
        }
        override fun onFrame(frame:VisionFrame) {
            if(SystemClock.elapsedRealtime()-frame.timestampMs>1200 || (latestVision?.timestampMs ?: -1)>frame.timestampMs)return
            latestVision=frame
            if(::alertPopup.isInitialized && alertPopup.presentation.critical && frame.risk.risks.isEmpty())alertPopup.visibility=View.GONE
            // The main video never waits for inference or rewinds to an older analyzed bitmap.
            // Exact-frame overlays belong on the auxiliary analyzed view; main overlays require equality.
            if(!texture.directActive && previewTimeline.matches(frame.timestampMs))laneOverlay.result=frame
            auxiliaryPreview.timestampMs=frame.timestampMs;auxiliaryPreview.frame=frame.bitmap;auxiliaryOverlay.result=frame
            auxiliaryPreview.focusBox=when{
                br.com.dashcamadas.vision.Risk.PEDESTRIAN in frame.risk.risks->frame.risk.pedestrianTarget?.box
                br.com.dashcamadas.vision.Risk.COLLISION in frame.risk.risks->frame.risk.target?.box
                else->null
            }
            auxiliaryPlaceholder.visibility=View.GONE
            cameraPlaceholder.visibility=View.GONE
            updateCameraTransform()
            showStatus(if(!frame.laneNetworkActive)"Rede neural de faixas indisponível" else if(frame.inferenceMs>500)"IA ativa, ${frame.inferenceMs} ms — alarmes suspensos por atraso" else if(!frame.vehicleNetworkActive)"IA de faixas ativa · veículos e pessoas indisponíveis" else "IA ativa · ${frame.inferenceMs} ms · faixas, veículos e pessoas")
            updateHud()
        }
        override fun onAlert(message:String){showPremiumAlert(EventPresentation.adas(message))}
    }
    private val captureConnection=object:android.content.ServiceConnection {
        override fun onServiceConnected(name:android.content.ComponentName,binder:IBinder) {
            if(!captureBound)return
            captureService=(binder as DashCamForegroundService.LocalBinder).service
            captureService?.callback=captureCallback
            captureService?.state?.let{captureCallback.onState(it)}
            captureService?.attachPreviewTexture(texture.directTexture())
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
        }
        override fun onAlert(alert: RadarAlert) { showStatus(alert.spokenMessage);showPremiumAlert(EventPresentation.radar(alert)) }
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
            onBackPressedDispatcher.addCallback(this, fullscreenBack)
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

    private lateinit var cameraPlaceholder: TextView
    private lateinit var headerBar: ViewGroup
    private lateinit var sidePanel: ViewGroup
    private lateinit var footerBar: ViewGroup
    private lateinit var statusBar: TextView
    private var cameraFullscreen = false
    private val fullscreenBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() { toggleFullscreen() }
    }
    private var cameraFill = false
    private var cameraMirror = false
    private var controlsHidden = false
    private var touchRevealedControls = false
    private val hudTick = object : Runnable {
        override fun run() { updateHud();handler.postDelayed(this,1000L) }
    }
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
        if (!::hud.isInitialized) return
        val speedMs=DrivingContext.speed()
        val video=if(!captureState.connected)"VÍDEO --" else when {
            captureState.videoHeight>=2160 -> "4K"
            captureState.videoHeight>=1080 -> "FULL HD"
            captureState.videoHeight>=720 -> "HD"
            captureState.videoHeight>0 -> "${captureState.videoHeight}P"
            else -> "VÍDEO --"
        }
        hud.update(speedMs?.times(3.6f),speedMs!=null,video,recording,latestVision)
        qualityTag.text=video
        headerSurface.usbConnected=captureState.connected && captureState.cameraId.startsWith("usb:")
        headerSurface.wifiConnected=try {
            val connectivity=getSystemService(android.net.ConnectivityManager::class.java)
            connectivity?.let { manager->manager.getNetworkCapabilities(manager.activeNetwork)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)==true } ?: false
        } catch (_: Exception) { false }
        headerSurface.invalidate()
    }
    private var cameraFacing = CameraCharacteristics.LENS_FACING_BACK
    private var cameraRotationOverride = -1 // -1 = automática; 0/90/180/270 = manual
    private lateinit var alertPopup: EventPopupView
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
        if(::auxiliaryPreview.isInitialized){auxiliaryPreview.fill=true;auxiliaryPreview.mirror=cameraMirror;auxiliaryPreview.invalidate();auxiliaryOverlay.invalidate()}
    }

    private fun toggleFullscreen() {
        cameraFullscreen = !cameraFullscreen
        fullscreenBack.isEnabled = cameraFullscreen
        headerBar.visibility = if (cameraFullscreen) View.GONE else View.VISIBLE
        sidePanel.visibility = if (cameraFullscreen) View.GONE else View.VISIBLE
        footerBar.visibility = if (cameraFullscreen) View.GONE else View.VISIBLE
        statusBar.visibility = if (cameraFullscreen) View.GONE else View.VISIBLE
        // Tela cheia prioriza o preenchimento, sempre sem distorção.
        // FULL preserves the camera FIT/FILL preference.
        hud.visibility=if(cameraFullscreen)View.GONE else View.VISIBLE
        dashboardScene.fullscreenView=if(cameraFullscreen)texture.parent as View else null
        revealControls()
        texture.post { updateCameraTransform() }
    }
    private var bannerTask: Runnable? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun uiText(value: String, size: Float, color: Int = Color.WHITE): TextView =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

    private fun buildUi() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        val visuals=PremiumVisuals(this)
        val root=ReferenceDashboardLayout(this).apply { setBackgroundColor(Color.rgb(2,8,17)) }
        dashboardScene=root
        val header=ReferenceDashboardLayout(this,1679f,120f)
        headerBar=header
        headerSurface=DashboardHeaderView(this,visuals)
        header.place(headerSurface,0f,0f,1679f,120f)
        record=Button(this).apply {
            text="";isEnabled=false;setPadding(0,0,0,0)
            background=PremiumIconDrawable(visuals,"lens",true){recording}
            contentDescription="Iniciar gravação"
            setOnClickListener { if(recording)stopRecording() else startRecording() }
            setOnLongClickListener { protectCurrentClip();true }
        }
        header.place(record,38f,27f,57f,57f)
        root.place(header,0f,0f,1679f,120f)

        fun cameraPanel(referenceWidth:Float):ReferenceDashboardLayout {
            val view=ReferenceDashboardLayout(this,referenceWidth,623f)
            view.clipToOutline=true
            view.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_ ->
                view.background=GradientDrawable().apply { setColor(Color.rgb(5,16,29));cornerRadius=24f*view.width/referenceWidth }
            }
            return view
        }
        fun border(view:ReferenceDashboardLayout,referenceWidth:Float) {
            val edge=View(this)
            view.place(edge,0f,0f,referenceWidth,623f)
            edge.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_ ->
                edge.setTag(R.id.neon_border_width, (2.8f*edge.width/referenceWidth).toInt().coerceAtLeast(1))
                edge.background=GradientDrawable().apply { setColor(Color.TRANSPARENT);cornerRadius=24f*edge.width/referenceWidth;setStroke((2.8f*edge.width/referenceWidth).toInt().coerceAtLeast(1),NeonTheme.read(this@MainActivity).accent) }
            }
        }
        val cameraFrame=cameraPanel(1195f)
        texture=CameraPreviewView(this,true).apply {
            onFrameDrawn={timestamp -> captureService?.reportPreviewDrawn(timestamp)}
            onSurfaceChanged={surface -> captureService?.attachPreviewTexture(surface)}
        }
        cameraFrame.place(texture,0f,0f,1195f,623f)
        laneOverlay=RiskOverlayView(this).apply { preview=texture;showCaption=false;setOnTouchListener { _,_->false } }
        cameraFrame.place(laneOverlay,0f,0f,1195f,623f)
        texture.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_ -> updateCameraTransform() }
        val detector=android.view.GestureDetector(this,object:android.view.GestureDetector.SimpleOnGestureListener(){
            override fun onDown(e:MotionEvent)=true
            override fun onSingleTapConfirmed(e:MotionEvent):Boolean {
                if(touchRevealedControls){touchRevealedControls=false;return true}
                cameraFill=!cameraFill
                selectedId?.let { orientationPrefs().edit().putBoolean("fill_$it",cameraFill).apply() }
                updateCameraTransform();showStatus(if(cameraFill)"Câmera: preencher" else "Câmera: ajustar");return true
            }
            override fun onDoubleTap(e:MotionEvent):Boolean { toggleFullscreen();return true }
            override fun onLongPress(e:MotionEvent) { showCameraControls() }
        })
        texture.setOnTouchListener { _,event->detector.onTouchEvent(event) }
        cameraPlaceholder=uiText("CÂMERA INDISPONÍVEL\nSelecione uma câmera compatível",15f,Color.rgb(177,204,224)).apply { gravity=Gravity.CENTER;setBackgroundColor(Color.rgb(5,16,29)) }
        cameraFrame.place(cameraPlaceholder,0f,0f,1195f,623f)
        cameraPlaceholder.setOnClickListener { chooseCamera() }
        qualityTag=uiText("VÍDEO --",20f,Color.rgb(255,208,106)).apply {
            gravity=Gravity.CENTER;typeface=Typeface.create("sans-serif-condensed",Typeface.BOLD)
            background=GradientDrawable().apply { setColor(Color.argb(235,6,16,26));cornerRadius=dp(8).toFloat() }
            setOnClickListener { showCameraControls() }
        }
        qualityTag.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_ ->qualityTag.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,40f*qualityTag.width/177f) }
        cameraFrame.place(qualityTag,24f,20f,177f,63f)
        alertPopup=EventPopupView(this,visuals).apply { visibility=View.GONE }
        cameraFrame.place(alertPopup,408f,68f,752f,194f)
        border(cameraFrame,1195f)
        root.place(cameraFrame,29f,120f,1195f,623f)

        val side=cameraPanel(402f)
        sidePanel=side
        // This is an ADAS view of the actual primary frame, not a fabricated second camera.
        auxiliaryPreview=CameraPreviewView(this).apply { fill=true }
        side.place(auxiliaryPreview,0f,0f,402f,623f)
        auxiliaryOverlay=RiskOverlayView(this).apply { preview=auxiliaryPreview;showCaption=false }
        side.place(auxiliaryOverlay,0f,0f,402f,623f)
        auxiliaryPlaceholder=uiText("ADAS\nCÂMERA INDISPONÍVEL",12f,Color.rgb(149,177,198)).apply { gravity=Gravity.CENTER;setBackgroundColor(Color.rgb(5,16,29)) }
        side.place(auxiliaryPlaceholder,0f,0f,402f,623f)
        val cameraButton=Button(this).apply {
            text="";setPadding(0,0,0,0);background=PremiumIconDrawable(visuals,"camera",true)
            contentDescription="Controles e seleção de câmera";setOnClickListener { showCameraControls() }
        }
        side.place(cameraButton,313f,14f,72f,72f)
        border(side,402f)
        root.place(side,1247f,120f,402f,623f)

        hud=LiveHudView(this,visuals)
        root.place(hud,583f,8f,514f,146f)
        hud.setOnClickListener { calibrateAdas() }
        val pages=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        repeat(3){index->pages.addView(View(this).apply { background=GradientDrawable().apply { setColor(if(index==0)Color.rgb(0,219,255) else Color.rgb(53,88,120));cornerRadius=dp(3).toFloat() } },LinearLayout.LayoutParams(0,-1,1f).apply { if(index<2)rightMargin=dp(8) })}
        root.place(pages,785f,767f,104f,7f)
        status=uiText("Câmera: verificando disponibilidade",10f,Color.rgb(149,180,204))
        statusBar=status
        root.place(status,29f,778f,1620f,22f)

        val controls=ReferenceDashboardLayout(this,1679f,89f)
        fun add(label:String,icon:String,x:Float,w:Float,active:Boolean=false,action:()->Unit){
            val tile=PremiumNavigationView(this,visuals,label,icon,active).apply { setOnClickListener { action() } }
            controls.place(tile,x,0f,w,89f)
        }
        add("LIVE","play",29f,258f,true){chooseCamera()}
        add("ADAS","car",312f,238f){calibrateAdas()}
        add("RADARES","radar_wave",575f,252f){listRadars()}
        add("MÍDIA","media",851f,252f){startActivity(Intent(this,PremiumSectionActivity::class.java).putExtra("section","MIDIA"))}
        add("EVENTOS","folder",1128f,252f){startActivity(Intent(this,PremiumSectionActivity::class.java).putExtra("section","EVENTOS"))}
        add("CONFIG","gear",1405f,244f){startActivity(Intent(this,SettingsActivity::class.java))}
        footerBar=controls
        root.place(controls,0f,805f,1679f,89f)
        setContentView(root);SafeInsets.apply(this,immersive=true);revealControls()
    }

    private fun showCameraControls(){
        val options=arrayOf("Selecionar câmera","Girar imagem","Espelhar imagem","${if(laneEnabled)"Ocultar" else "Mostrar"} faixas","${if(recording)"Parar" else "Iniciar"} gravação","Proteger clipe","${if(cameraFullscreen)"Sair da" else "Abrir"} tela cheia","Sensibilidade e espessura das faixas")
        NeonDialogs.Builder(this).setTitle("Câmera").setItems(options){_,which->when(which){
            0->chooseCamera()
            1->cycleCameraRotation()
            2->{cameraMirror=!cameraMirror;selectedId?.let { orientationPrefs().edit().putBoolean("mirror_$it",cameraMirror).apply() };updateCameraTransform()}
            3->{laneEnabled=!laneEnabled;laneOverlay.showLanes=laneEnabled;auxiliaryOverlay.showLanes=laneEnabled;laneOverlay.invalidate();auxiliaryOverlay.invalidate()}
            4->if(recording)stopRecording() else startRecording()
            5->protectCurrentClip()
            6->toggleFullscreen()
            7->startActivity(Intent(this,SettingsActivity::class.java).putExtra("section","FAIXAS"))
        }}.setNegativeButton("Fechar",null).show()
    }

    /** Popup 2 keeps the reference shape; its content and icon come from the actual event. */
    private fun showPremiumAlert(presentation:EventPresentation){
        runOnUiThread {
            if(alertPopup.visibility==View.VISIBLE && alertPopup.presentation.critical && !presentation.critical)return@runOnUiThread
            bannerTask?.let { handler.removeCallbacks(it) }
            alertPopup.animate().cancel();alertPopup.presentation=presentation
            alertPopup.alpha=1f;alertPopup.visibility=View.VISIBLE
            bannerTask=Runnable { alertPopup.visibility=View.GONE }
            handler.postDelayed(bannerTask!!,4500L)
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

    override fun onRequestPermissionsResult(code: Int, permissions: Array<String>, grants: IntArray) {
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
            NeonDialogs.Builder(this).setTitle("Selecionar câmera disponível").setItems(labels.toTypedArray()){_,which->
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
        NeonDialogs.Builder(this)
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
        val dialog=NeonDialogs.Builder(this).setTitle("Calibrar LDW / FCW").setView(box)
            .setNegativeButton("Cancelar",null).setNeutralButton("Desativar alarmes"){_,_->prefs.edit().putBoolean("enabled_$id",false).apply()}
            .setPositiveButton("Confirmar medidas",null).create()
        dialog.setOnShowListener{NeonDialogs.decorate(dialog);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
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
        if(::hud.isInitialized)hud.stopSensors()
        handler.removeCallbacks(hudTick)
        if(radarService?.callback===radarCallback)radarService?.callback = null
        radarService = null
        if (radarBound) { unbindService(radarConnection); radarBound=false }
        if(captureService?.callback===captureCallback)captureService?.callback=null
        captureService?.attachPreviewTexture(null)
        captureService=null
        if(captureBound){unbindService(captureConnection);captureBound=false}
        latestVision=null
        if(::texture.isInitialized)texture.frame=null
        if(::laneOverlay.isInitialized)laneOverlay.result=null
        previewTimeline.reset()
        if(::auxiliaryPreview.isInitialized)auxiliaryPreview.frame=null
        if(::auxiliaryOverlay.isInitialized)auxiliaryOverlay.result=null
        super.onStop()
    }

    private fun addRadarDialog() {
        val loc = lastGps
        if (loc == null || loc.accuracy > 50f) { showStatus("Aguarde sinal GPS com precisão melhor que 50 m");return }
        val limits = arrayOf("30", "40", "50", "60", "70", "80", "90", "100", "110", "120")
        val types = arrayOf("fixo", "semaforo", "movel")
        val wrapper = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22,8,22,8) }
        val limit = Spinner(NeonDialogs.themed(this),Spinner.MODE_DROPDOWN).apply { adapter = ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,limits);setSelection(3) }
        val type = Spinner(NeonDialogs.themed(this),Spinner.MODE_DROPDOWN).apply { adapter = ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,types) }
        wrapper.addView(TextView(this).apply {text="Limite (km/h)"});wrapper.addView(limit)
        wrapper.addView(TextView(this).apply {text="Tipo"});wrapper.addView(type)
        NeonDialogs.Builder(this).setTitle("Adicionar radar neste GPS").setView(wrapper)
            .setNegativeButton("Cancelar",null).setPositiveButton("Salvar") { _, _ ->
                radarStore.add(loc,limits[limit.selectedItemPosition].toInt(),types[type.selectedItemPosition])
                showStatus("Radar salvo localmente • ${radarStore.all().size} ponto(s)")
            }.show()
    }

    private fun listRadars() {
        val items=radarStore.all()
        if (items.isEmpty()) {showStatus("Não há radares cadastrados");return}
        NeonDialogs.Builder(this).setTitle("Radares — toque para excluir")
            .setItems(items.map { "${it.type} ${it.limit} km/h • %.5f, %.5f".format(Locale.US,it.lat,it.lon) }.toTypedArray()) { _, index ->
                NeonDialogs.Builder(this).setTitle("Excluir radar?").setNegativeButton("Cancelar",null)
                    .setPositiveButton("Excluir") { _, _ -> radarStore.remove(items[index].id); showStatus("Radar excluído") }.show()
            }.setNegativeButton("Fechar",null).show()
    }

    private fun showStatus(message: String) { runOnUiThread { status.text = message } }
    override fun onResume() {
        super.onResume()
        CommercialUi.applyAppearance(this)
        if(!::hud.isInitialized)return
        revealControls()
        if(::hud.isInitialized){hud.startSensors();handler.removeCallbacks(hudTick);handler.post(hudTick);startGps()}
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)scanCameras()
    }
    override fun onDestroy() {handler.removeCallbacksAndMessages(null);if(::audioAlerts.isInitialized)audioAlerts.release();super.onDestroy()}
}
