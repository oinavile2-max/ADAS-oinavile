package br.com.dashcamadas

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.*
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.*
import android.util.Size
import android.view.Surface
import br.com.dashcamadas.vision.*
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Owns Camera2 or native USB, encoder and both neural models independently of LIVE. */
class DashCamForegroundService:Service() {
    data class State(val connected:Boolean=false,val recording:Boolean=false,val cameraId:String="",val message:String="Câmera indisponível",val videoWidth:Int=0,val videoHeight:Int=0,val directPreview:Boolean=false,val livePreviewWidth:Int=0,val livePreviewHeight:Int=0,val previewRotation:Int=0,val directPreviewUnavailable:Boolean=false)
    interface Callback {
        fun onState(state:State)
        fun onPreview(bitmap:Bitmap,timestampMs:Long)
        fun onFrame(frame:VisionFrame)
        fun onAlert(message:String)
    }
    inner class LocalBinder:Binder() {val service:DashCamForegroundService get()=this@DashCamForegroundService}
    @Volatile var callback:Callback?=null
    @Volatile var state=State();private set
    @Volatile private var currentId=""
    @Volatile private var lastFrame=0L
    @Volatile private var closing=false
    @Volatile private var generation=0
    private val binder=LocalBinder()
    private val main=Handler(Looper.getMainLooper())
    private val worker=HandlerThread("DashCamCamera",Process.THREAD_PRIORITY_MORE_FAVORABLE).apply{start()}
    private val cameraHandler=Handler(worker.looper)
    private val analysis=Executors.newSingleThreadExecutor { task -> Thread({
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); task.run()
    },"DashCamAnalysis") }
    private val evidence=Executors.newSingleThreadExecutor()
    private val busy=AtomicBoolean(false)
    private val uiPending=AtomicBoolean(false)
    private data class PreviewPacket(val callback:Callback,val bitmap:Bitmap,val timestamp:Long,val epoch:Int)
    private val previews=LatestFrameMailbox<PreviewPacket>()
    private val cadence=CaptureCadence()
    private val previewMeter=PreviewFrameMeter()
    private var conversionPixels=IntArray(0)
    @Volatile private var previewFps=15
    @Volatile private var sessionStartedAt=0L
    @Volatile private var capturedFrames=0L
    @Volatile private var maxSourceGap=0L
    @Volatile private var conversionMs=0L
    @Volatile private var modelThreads=1
    private val cameraPreferences by lazy { getSharedPreferences("camera",MODE_PRIVATE) }
    private val performanceListener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs,key ->
        if(key=="preview_fps")previewFps=prefs.getInt("preview_fps",15).coerceIn(10,30)
    }
    fun reportPreviewDrawn(timestamp:Long) {
        if(callback!=null && timestamp>=sessionStartedAt && !closing)previewMeter.drawn(timestamp,SystemClock.elapsedRealtime())
    }
    fun previewMetrics():PreviewFrameMeter.Snapshot=previewMeter.snapshot(SystemClock.elapsedRealtime())
    private val engine=AdasEngine()
    @Volatile private var detector:VehicleDetector?=null
    @Volatile private var laneNetwork:NeuralLaneDetector?=null
    @Volatile private var laneInferences=0L
    @Volatile private var vehicleInferences=0L
    @Volatile private var inferenceMs=0L
    @Volatile private var nativeFrames=0L
    private var nativeCapture:NativeUsbCapture?=null
    private var nativeVideoSink:RgbaVideoSink?=null
    private var camera:CameraDevice?=null
    private var session:CameraCaptureSession?=null
    private var reader:ImageReader?=null
    private var recorder:MediaRecorder?=null
    private var recordSurface:Surface?=null
    private var recordWanted=false
    private var isRecording=false
    private var segment:File?=null
    private var protectSegment=false
    private var captureAttempt=0
    private var opening=false
    private var analysisRotation=0
    private var videoSize=Size(1280,720)
    private var imageSize=Size(640,360)
    private var liveSize=Size(640,360)
    private var liveTexture:SurfaceTexture?=null
    private var liveSurface:Surface?=null
    @Volatile private var directActive=false
    private var directUnavailable=false
    /** The Activity owns the texture; only this service owns its Surface wrapper. */
    fun attachPreviewTexture(texture:SurfaceTexture?) { cameraHandler.post {
        if(closing || liveTexture===texture)return@post
        liveTexture=texture
        if(camera!=null && (texture!=null || liveSurface!=null))configure()
    } }
    private fun updateLiveSurface() {
        liveSurface?.release();liveSurface=null;directActive=false
        if(directUnavailable)return
        val texture=liveTexture ?: return
        try {texture.setDefaultBufferSize(liveSize.width,liveSize.height);val candidate=Surface(texture);if(candidate.isValid)liveSurface=candidate else {candidate.release();directUnavailable=true}}
        catch(e:Exception){directUnavailable=true;android.util.Log.w("DashCamPreview","Prévia alternativa ativada",e)}
    }
    private fun retryWithoutDirect(device:CameraDevice,requestedRecording:Boolean,message:String,hadDirect:Boolean) {
        if(hadDirect && camera===device && !closing){
            directUnavailable=true;directActive=false;configure()
        } else captureFailure(device,requestedRecording,message)
    }
    @Volatile private var running=false
    private var recoveryAttempts=0
    private var recoverAllowed=true
    private var availabilityRegistered=false
    private var captureStartDeadline=0L
    private val availability=object:CameraManager.AvailabilityCallback(){
        override fun onCameraAvailable(id:String){
            if(id==currentId && running && !closing && camera==null && !opening && recoverAllowed && recoveryAttempts<3){
                recoveryAttempts++
                cameraHandler.postDelayed({if(running && !closing && camera==null && !opening)open(id,true)},1500)
            }
        }
    }
    @Volatile private var processedFrames=0L
    @Volatile private var recordingFrames=0L
    @Volatile private var completedSegments=0L
    @Volatile private var lastCompletedClip=""
    private fun diagnostics() {
        if(!BuildConfig.DEBUG || closing)return
        val preview=previewMetrics()
        val value=JSONObject().put("camera_id",currentId).put("connected",state.connected).put("recording",isRecording)
            .put("processed_frames",processedFrames).put("recording_capture_frames",recordingFrames).put("completed_segments",completedSegments)
            .put("last_frame_elapsed_ms",lastFrame).put("observed_elapsed_ms",SystemClock.elapsedRealtime())
            .put("last_completed_clip",lastCompletedClip).put("model_loaded",detector!=null && laneNetwork!=null).put("gps_speed_available",DrivingContext.speed()!=null)
            .put("lane_model_loaded",laneNetwork!=null).put("vehicle_model_loaded",detector!=null)
            .put("lane_neural_inferences",laneInferences).put("vehicle_neural_inferences",vehicleInferences).put("last_inference_ms",inferenceMs)
            .put("camera_received_frames",capturedFrames).put("max_source_gap_ms",maxSourceGap).put("last_conversion_ms",conversionMs)
            .put("preview_rendered_frames",preview.frames).put("preview_render_fps",preview.fps)
            .put("preview_max_draw_gap_ms",preview.maxGapMs).put("preview_last_draw_gap_ms",preview.lastGapMs)
            .put("preview_direct_gpu",directActive).put("preview_fps_limit",previewFps).put("neural_interval_ms",cadence.analysisIntervalMs()).put("neural_threads_per_model",modelThreads)
            .put("native_rgba_frames",nativeFrames).put("source_kind",if(currentId.startsWith("usb:"))"USB_NATIVE_RGBA" else "CAMERA2_YUV")
        try {File(filesDir,"capture-diagnostics.json").writeText(value.toString())}catch(e:Exception){android.util.Log.w("DashCamDiagnostics","Diagnóstico não salvo",e)}
    }
    private var audio:AlertAudio?=null
    private var wakeDeadline=0L
    private val wakeLock by lazy {getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"DashCamADAS:Capture").apply{setReferenceCounted(false)}}
    private fun holdCapturePower(){val now=SystemClock.elapsedRealtime();if(!wakeLock.isHeld || now>=wakeDeadline){wakeLock.acquire(600000);wakeDeadline=now+540000}}
    private val manager by lazy{getSystemService(CameraManager::class.java)}
    private val segmentTimer=Runnable{ if(recordWanted) rotateSegment(false) }
    private val watchdog=object:Runnable {
        override fun run() {
            if(running && opening.not() && !state.connected && captureStartDeadline>0 && SystemClock.elapsedRealtime()>=captureStartDeadline){
                captureStartDeadline=0;recordWanted=false;closeCapture();publish(State(cameraId=currentId,message="Câmera sem quadros utilizáveis; confira compatibilidade do driver"))
            }
            if(running && lastFrame>0 && SystemClock.elapsedRealtime()-lastFrame>1800) {
                lastFrame=0
                analysis.execute{engine.reset()}
                if(nativeCapture!=null){recordWanted=false;closeCapture();publish(State(cameraId=currentId,message="USB sem quadros; gravação finalizada e ADAS suspenso"))}
                else publish(state.copy(message="Sem quadros reais; ADAS suspenso"))
            }
            if(BuildConfig.DEBUG && running) {
                val command=File(filesDir,"capture-test-command")
                if(command.exists()) {
                    val value=try{command.readText().trim()}catch(_:Exception){""}
                    command.delete()
                    when(value){"START_RECORD"->startRecording();"STOP_RECORD"->stopRecording();"PROTECT"->protect()}
                }
            }
            if(running && (camera!=null || nativeCapture!=null))holdCapturePower()
            diagnostics()
            if(!closing) cameraHandler.postDelayed(this,1000)
        }
    }
    override fun onCreate() {
        super.onCreate()
        if(Build.VERSION.SDK_INT>=26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Câmera e gravação",NotificationManager.IMPORTANCE_LOW))
        audio=AlertAudio(this)
        previewFps=cameraPreferences.getInt("preview_fps",15).coerceIn(10,30)
        cameraPreferences.registerOnSharedPreferenceChangeListener(performanceListener)
        val memory=android.app.ActivityManager.MemoryInfo()
        getSystemService(android.app.ActivityManager::class.java).getMemoryInfo(memory)
        modelThreads=if(Runtime.getRuntime().availableProcessors()<=4 || memory.totalMem<=2500L*1024*1024)1 else 2
        analysis.execute {
            try {laneNetwork=NeuralLaneDetector(this,modelThreads)}
            catch(e:Exception){android.util.Log.e("DashCamModel","Rede de faixas indisponível",e)}
            catch(e:LinkageError){android.util.Log.e("DashCamModel","Runtime neural de faixas incompatível",e)}
            try {detector=VehicleDetector(this,modelThreads)}
            catch(e:Exception){android.util.Log.e("DashCamModel","FCW indisponível: modelo não carregou",e)}
            catch(e:LinkageError){android.util.Log.e("DashCamModel","FCW indisponível neste runtime nativo",e)}
        }
        try{manager.registerAvailabilityCallback(availability,cameraHandler);availabilityRegistered=true}
        catch(e:Exception){android.util.Log.w("DashCamCamera","Camera2 indisponível; USB permanece independente",e)}
        cameraHandler.post(watchdog)
    }
    override fun onBind(intent:Intent):IBinder=binder
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action==STOP) {cameraHandler.post{running=false;recordWanted=false;closeCapture();publish(State(message="Sessão encerrada"));stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()};return START_NOT_STICKY}
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){cameraHandler.post{running=false;recordWanted=false;closeCapture();publish(State(message="Permissão de câmera ausente"));stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()};return START_NOT_STICKY}
        try {
            val n=notification("Câmera iniciando")
            if(Build.VERSION.SDK_INT>=30) startForeground(72,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA) else startForeground(72,n)
            val id=intent?.getStringExtra("camera_id") ?: run{stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY}
            running=true
            cameraHandler.post{open(id)}
        } catch(e:Exception){publish(State(message="Serviço de câmera não autorizado: ${e.javaClass.simpleName}"));stopSelf()}
        return START_NOT_STICKY
    }
    private fun notification(message:String):Notification {
        val open=PendingIntent.getActivity(this,2,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,3,Intent(this,DashCamForegroundService::class.java).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder=if(Build.VERSION.SDK_INT>=26) Notification.Builder(this,CHANNEL) else Notification.Builder(this)
        return builder.setSmallIcon(android.R.drawable.ic_menu_camera).setContentTitle("DashCam ADAS PRO")
            .setContentText(message).setContentIntent(open).setOngoing(true)
            .addAction(Notification.Action.Builder(null,"Encerrar câmera",stop).build()).build()
    }
    private fun publish(value:State) {
        val snapshot=value.copy(videoWidth=if(value.connected)videoSize.width else 0,videoHeight=if(value.connected)videoSize.height else 0,directPreview=value.connected && directActive,livePreviewWidth=liveSize.width,livePreviewHeight=liveSize.height,previewRotation=analysisRotation,directPreviewUnavailable=directUnavailable)
        state=snapshot
        main.post {
            if(state==snapshot) callback?.onState(snapshot)
            if(running) getSystemService(NotificationManager::class.java).notify(72,notification(state.message))
        }
    }
    @SuppressLint("MissingPermission")
    private fun open(id:String,preserveRecording:Boolean=false) {
        if(closing || !running || opening) return
        if(id.startsWith("usb:")){openNative(id);return}
        if(camera!=null && currentId==id) {
            val before=analysisRotation;updateRotation(manager.getCameraCharacteristics(id))
            if(before!=analysisRotation) {getSharedPreferences("adas_calibration",MODE_PRIVATE).edit().putBoolean("enabled_$currentId",false).apply();analysis.execute{engine.reset()};if(isRecording)rotateSegment(false) else publish(state)}
            return
        }
        if(!preserveRecording){recoveryAttempts=0;recoverAllowed=true}
        val resume=preserveRecording && recordWanted
        closeCapture();recordWanted=resume;currentId=id;opening=true;directUnavailable=false
        val epoch=generation
        try {
            val chars=manager.getCameraCharacteristics(id)
            val map=chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: error("Mapa de captura ausente")
            val yuv=map.getOutputSizes(ImageFormat.YUV_420_888).orEmpty().filter{it.width<=640 && it.height<=480}
            val video=map.getOutputSizes(MediaRecorder::class.java).orEmpty().filter{it.width<=1920 && it.height<=1080}
            videoSize=video.filter{size->yuv.any{ kotlin.math.abs(it.width.toFloat()/it.height-size.width.toFloat()/size.height)<.025f }}
                .minByOrNull{kotlin.math.abs(it.width.toLong()*it.height-1280L*720L)} ?: Size(0,0)
            val preferredRatio=if(videoSize.width>0) videoSize.width.toFloat()/videoSize.height else 16f/9
            imageSize=yuv.filter{kotlin.math.abs(it.width.toFloat()/it.height-preferredRatio)<.025f}
                .minByOrNull{kotlin.math.abs(it.width.toLong()*it.height-640L*360L)} ?: yuv.minByOrNull{it.width*it.height} ?: error("Câmera sem saída YUV compatível")
            val ratio=imageSize.width.toFloat()/imageSize.height
            liveSize=map.getOutputSizes(SurfaceTexture::class.java).orEmpty()
                .filter{it.width<=1280 && it.height<=720 && kotlin.math.abs(it.width.toFloat()/it.height-ratio)<.025f}
                .maxByOrNull{it.width*it.height} ?: imageSize
            updateRotation(chars)
            reader=ImageReader.newInstance(imageSize.width,imageSize.height,ImageFormat.YUV_420_888,3).apply {
                setOnImageAvailableListener({ source -> receive(source,epoch) },cameraHandler)
            }
            manager.openCamera(id,object:CameraDevice.StateCallback() {
                override fun onOpened(device:CameraDevice) {
                    if(epoch!=generation || !running){device.close();return}
                    opening=false;recoveryAttempts=0;camera=device;configure()
                }
                override fun onDisconnected(device:CameraDevice){if(camera===device || epoch==generation){closeCapture();publish(State(cameraId=id,message="Câmera desconectada; gravação e ADAS suspensos"))}else device.close()}
                override fun onError(device:CameraDevice,error:Int){if(camera===device || epoch==generation){closeCapture();publish(State(cameraId=id,message="Erro de câmera $error; ADAS suspenso"))}else device.close()}
            },cameraHandler)
        } catch(e:Exception){closeCapture();publish(State(cameraId=id,message="Captura indisponível: ${e.message}"))}
    }
    private fun openNative(id:String){
        if(currentId==id && nativeCapture!=null){updateNativeRotation();return}
        closeCapture();recordWanted=false;currentId=id;opening=true
        captureStartDeadline=SystemClock.elapsedRealtime()+10000
        updateNativeRotation();videoSize=Size(640,360)
        val epoch=generation
        val capture=NativeUsbCapture(this,cameraHandler,{source->receive(source,epoch)},
            {if(epoch==generation){opening=false;holdCapturePower();publish(State(cameraId=id,message="USB inicializada; aguardando imagem real"))}},
            {message->if(epoch==generation){recordWanted=false;closeCapture();publish(State(cameraId=id,message=message+"; ADAS suspenso"))}})
        nativeCapture=capture
        try{capture.open(id.removePrefix("usb:"))}
        catch(e:Exception){recordWanted=false;closeCapture();publish(State(cameraId=id,message="USB indisponível: ${e.message}"))}
        catch(e:LinkageError){recordWanted=false;closeCapture();publish(State(cameraId=id,message="Biblioteca USB incompatível: ${e.message}"))}
    }
    private fun updateNativeRotation(){
        val before=analysisRotation
        analysisRotation=getSharedPreferences("camera_orientation",MODE_PRIVATE).getInt("rotation_$currentId",0).takeIf{it in listOf(0,90,180,270)} ?: 0
        if(before!=analysisRotation){getSharedPreferences("adas_calibration",MODE_PRIVATE).edit().putBoolean("enabled_$currentId",false).apply();analysis.execute{engine.reset()}}
    }
    private fun updateRotation(chars:CameraCharacteristics) {
        val prefs=getSharedPreferences("camera_orientation",MODE_PRIVATE)
        val override=prefs.getInt("rotation_$currentId",-1)
        val sensor=chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        val display=prefs.getInt("display_rotation_$currentId",90)
        val front=chars.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_FRONT
        analysisRotation=if(override in listOf(0,90,180,270)) override else if(front) (sensor+display)%360 else (sensor-display+360)%360
    }
    fun orientationChanged(){cameraHandler.post{
        if(currentId.isNotEmpty()) {
            getSharedPreferences("adas_calibration",MODE_PRIVATE).edit().putBoolean("enabled_$currentId",false).apply()
            if(currentId.startsWith("usb:"))updateNativeRotation() else updateRotation(manager.getCameraCharacteristics(currentId))
            if(isRecording)rotateSegment(false) else if(camera!=null)publish(state)
        }
        analysis.execute{engine.reset()}
    }}
    fun startRecording(){cameraHandler.post{if(!state.connected || (camera==null && nativeCapture==null)){publish(state.copy(message="Câmera indisponível para gravar"));return@post};recordWanted=true;configure()}}
    fun stopRecording(){cameraHandler.post{recordWanted=false;finishRecorder();configure()}}
    fun protect(){cameraHandler.post{if(!isRecording){publish(state.copy(message="Inicie uma gravação para proteger o clipe"));return@post};rotateSegment(true)}}
    private fun configure() {
        if(nativeCapture!=null){configureNative();return}
        val device=camera ?: return
        val source=reader?.surface ?: return
        val attempt=++captureAttempt
        val requestedRecording=recordWanted
        var hadDirect=false
        try {
            try{session?.stopRepeating()}catch(e:Exception){android.util.Log.w("DashCamPreview","Sessão anterior encerrada",e)}
            session?.close();session=null
            updateLiveSurface();hadDirect=liveSurface!=null
            if(recordWanted && recorder==null) prepareRecorder()
            val outputs=listOfNotNull(source,liveSurface,if(recordWanted) recordSurface else null)
            val recordingRequest=recordWanted && recordSurface!=null
            val req=device.createCaptureRequest(if(recordWanted) CameraDevice.TEMPLATE_RECORD else CameraDevice.TEMPLATE_PREVIEW).apply{outputs.forEach{addTarget(it)}}
            device.createCaptureSession(outputs,object:CameraCaptureSession.StateCallback() {
                override fun onConfigured(value:CameraCaptureSession) {
                    if(attempt!=captureAttempt || camera!==device){value.close();return}
                    session=value
                    try {
                        value.setRepeatingRequest(req.build(),object:CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(capture:CameraCaptureSession,request:CaptureRequest,result:TotalCaptureResult) {
                                if(attempt==captureAttempt && isRecording && recordingRequest)recordingFrames++
                            }
                        },cameraHandler)
                        if(recordWanted && !isRecording){recorder?.start();isRecording=true;cameraHandler.postDelayed(segmentTimer,segmentMinutes()*60000L)}
                        directActive=hadDirect
                        holdCapturePower()
                        publish(State(true,isRecording,currentId,if(isRecording) "● REC — gravação em serviço" else "LIVE — câmera em serviço"))
                    } catch(e:Exception){retryWithoutDirect(device,requestedRecording,"Captura falhou: ${e.message}",hadDirect)}
                }
                override fun onConfigureFailed(value:CameraCaptureSession) {
                    value.close();if(attempt!=captureAttempt)return
                    retryWithoutDirect(device,requestedRecording,"Combinação de saídas não suportada",hadDirect)
                }
            },cameraHandler)
        } catch(e:Exception){retryWithoutDirect(device,requestedRecording,"Gravação/captura indisponível: ${e.message}",hadDirect)}
    }
    private fun configureNative(){
        val capture=nativeCapture ?: return
        if(!capture.ready)return
        try{
            if(recordWanted && !isRecording){
                prepareRecorder();nativeVideoSink=RgbaVideoSink(recordSurface!!,videoSize.width,videoSize.height);recorder!!.start();isRecording=true
                cameraHandler.postDelayed(segmentTimer,segmentMinutes()*60000L)
            }
            holdCapturePower()
            publish(State(true,isRecording,currentId,if(isRecording)"● REC — USB em serviço" else "LIVE — USB em serviço"))
        }catch(e:Exception){recordWanted=false;finishRecorder();publish(state.copy(recording=false,message="Gravação USB falhou: ${e.message}"))}
    }
    private fun captureFailure(device:CameraDevice,requestedRecording:Boolean,message:String) {
        recordWanted=false;finishRecorder()
        publish(State(cameraId=currentId,message=message))
        if(requestedRecording && camera===device && !closing)configure()
        else {recoverAllowed=false;closeCapture()}
    }
    private fun segmentMinutes()=getSharedPreferences("camera",MODE_PRIVATE).getInt("segment",3).coerceIn(1,5)
    private fun prepareRecorder() {
        require(videoSize.width>0){"Câmera sem tamanho de gravação compatível com YUV"}
        val folder=File(getExternalFilesDir(null),"clips").apply{mkdirs()}
        val quota=getSharedPreferences("camera",MODE_PRIVATE).getInt("storage_mb",0)
        if(quota>0) ClipStorage.prune(folder,null,quota*1024L*1024L)
        require(folder.usableSpace>100*1024L*1024L){"Espaço insuficiente"}
        val file=File(folder,"clip_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.US).format(Date())}.mp4")
        val rec=if(Build.VERSION.SDK_INT>=31) MediaRecorder(this) else MediaRecorder()
        recorder=rec;segment=file
        rec.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        rec.setVideoSize(videoSize.width,videoSize.height);rec.setVideoFrameRate(30);rec.setVideoEncodingBitRate(4_000_000)
        rec.setOrientationHint(analysisRotation)
        rec.setOutputFile(file.absolutePath)
        rec.prepare();recordSurface=rec.surface
        rec.setOnErrorListener{_,_,_->cameraHandler.post{recordWanted=false;finishRecorder();configure()}}
    }
    private fun finishRecorder() {
        cameraHandler.removeCallbacks(segmentTimer)
        captureAttempt++
        nativeVideoSink?.close();nativeVideoSink=null
        try{session?.stopRepeating();session?.abortCaptures()}catch(_:Exception){}
        session?.close();session=null
        val file=segment
        var valid=false
        if(isRecording) try{recorder?.stop();valid=file?.exists()==true && file.length()>0}catch(_:RuntimeException){}
        isRecording=false
        try{recorder?.release()}catch(_:Exception){}
        recorder=null;recordSurface?.release();recordSurface=null;segment=null
        if(valid){completedSegments++;lastCompletedClip=file?.absolutePath ?: ""}
        if(!valid) file?.delete()
        else if(file!=null && protectSegment) {
            val protected=File(File(getExternalFilesDir(null),"protegidos").apply{mkdirs()},file.name)
            val recordedCamera=currentId
            val lock=File(file.parentFile,file.name+".lock")
            try {lock.createNewFile()}catch(e:Exception){android.util.Log.e("DashCamEvidence","Falha ao marcar proteção",e)}
            if(file.renameTo(protected)) {
                lock.delete();lastCompletedClip=protected.absolutePath
                evidence.execute{
                try {
                    val hash=ClipStorage.sha256(protected)
                    val hashFile=File(protected.parentFile,protected.name+".sha256")
                    val temporary=File(protected.parentFile,protected.name+".sha256.tmp")
                    temporary.writeText("$hash  ${protected.name}\n")
                    require(temporary.renameTo(hashFile)){"Falha ao salvar hash completo"}
                    File(protected.parentFile,protected.name+".json").writeText(JSONObject().put("sha256",hash).put("camera",recordedCamera).put("created_ms",System.currentTimeMillis()).put("pre_post_buffer",false).toString())
                } catch(e:Exception){android.util.Log.e("DashCamEvidence","Falha no hash/metadados",e)}
                }
            } else {
                EventStore(this).add("FALHA AO MOVER CLIPE PROTEGIDO",file.name,"GRAVAÇÃO / arquivo .lock preserva da exclusão circular")
            }
        }
        protectSegment=false
    }
    private fun rotateSegment(protect:Boolean){protectSegment=protect;finishRecorder();if(recordWanted) configure()}
    private fun receive(source:ImageReader,epoch:Int) {
        val image=try{source.acquireLatestImage()}catch(_:IllegalStateException){null} ?: return
        var analysisClaimed=false
        try {
            val now=SystemClock.elapsedRealtime()
            if(epoch!=generation || closing)return
            if(lastFrame>0)maxSourceGap=maxOf(maxSourceGap,now-lastFrame)
            lastFrame=now;capturedFrames++
            captureStartDeadline=0
            if(currentId.startsWith("usb:")){
                nativeFrames++
                if(isRecording)try{nativeVideoSink?.render(image,SystemClock.elapsedRealtimeNanos());recordingFrames++}
                catch(e:Exception){recordWanted=false;finishRecorder();publish(state.copy(recording=false,message="Encoder USB falhou: ${e.message}"))}
                if(!state.connected)publish(State(true,isRecording,currentId,"LIVE — USB com quadros RGBA"))
            }
            val previewCallback=callback
            val showPreview=previewCallback!=null && cadence.claimPreview(now,if(directActive)4 else previewFps)
            val analyze=(laneNetwork!=null || detector!=null) && cadence.analysisDue(now) && busy.compareAndSet(false,true)
            analysisClaimed=analyze
            if(!showPreview && !analyze)return
            if(analyze)cadence.analysisAccepted(now)
            val started=SystemClock.elapsedRealtime()
            val bitmap=convert(image)
            val rotation=analysisRotation
            val oriented=if(rotation==0) bitmap else Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,Matrix().apply{postRotate(rotation.toFloat())},true)
            if(oriented!==bitmap)bitmap.recycle()
            conversionMs=SystemClock.elapsedRealtime()-started
            if(showPreview && previewCallback!=null && previews.offer(PreviewPacket(previewCallback,oriented,now,epoch)))main.post {
                val next=previews.take() ?: return@post
                if(callback===next.callback && next.epoch==generation && !closing)next.callback.onPreview(next.bitmap,next.timestamp)
            }
            if(!analyze)return
            val cameraId=currentId
            analysis.execute {
                try {
                    if(epoch!=generation || closing)return@execute
                    if(SystemClock.elapsedRealtime()-now>500){engine.reset();return@execute}
                    if(oriented.width<oriented.height){engine.reset();publish(state.copy(message="Gire a câmera para a estrada horizontal; ADAS suspenso"));return@execute}
                    val masks=laneNetwork?.detect(oriented)
                    if(masks!=null)laneInferences++
                    val scene=detector?.detectScene(oriented) ?: SceneDetections(emptyList(),emptyList())
                    val vehicles=scene.vehicles
                    if(detector!=null)vehicleInferences++
                    val prefs=getSharedPreferences("adas_calibration",MODE_PRIVATE)
                    val calibration=Calibration(prefs.getBoolean("enabled_$cameraId",false),prefs.getFloat("ego_$cameraId",.5f),prefs.getFloat("vehicle_width_$cameraId",1.8f),prefs.getFloat("lane_width_$cameraId",3.5f))
                    val settings=getSharedPreferences("adas_tuning",MODE_PRIVATE)
                    engine.configure(RiskTuning(settings.getFloat("ldw_kmh",30f).coerceIn(10f,100f)/3.6f,settings.getFloat("fcw_ttc",2.5f).coerceIn(1f,5f),settings.getLong("confirm_ms",900).coerceIn(300,2000),settings.getLong("cooldown_ms",10000).coerceIn(5000,60000),settings.getInt("lane_sensitivity",50).coerceIn(0,100)))
                    val actualMask=masks ?: NeuralMasks(320,180,IntArray(320*180),BooleanArray(320*180),0,0)
                    val observed=engine.observeNeural(actualMask,if(calibration.valid)calibration.egoX else .5f)
                    inferenceMs=SystemClock.elapsedRealtime()-now
                    val outcome=engine.evaluateNeural(actualMask,observed,vehicles,now,if(inferenceMs<=500) DrivingContext.speed() else null,calibration,scene.pedestrians)
                    val laneResult=outcome.lanes
                    val evaluated=outcome.risk
                    val frame=VisionFrame(oriented,laneResult,vehicles,evaluated,now,masks!=null,detector!=null,inferenceMs,actualMask.lanePixels,actualMask.roadPixels,scene.pedestrians)
                    if(epoch!=generation || closing)return@execute
                    processedFrames++
                    for(kind in evaluated.newAlerts) {
                        val message=when(kind){Risk.PEDESTRIAN->"Atenção! Pedestre à frente!";Risk.COLLISION->"Atenção! Risco de colisão à frente!";Risk.LEFT_DEPARTURE->"Atenção! Saída de faixa à esquerda!";Risk.RIGHT_DEPARTURE->"Atenção! Saída de faixa à direita!"}
                        main.post{if(epoch==generation && !closing){audio?.announce(message,true);callback?.onAlert(message);EventStore(this).add(kind.name,"Risco estimado por visão; câmera $cameraId","VISION / EXPERIMENTAL",true)}}
                        cameraHandler.post{if(epoch==generation && isRecording)rotateSegment(true)}
                    }
                    val cb=callback
                    if(cb!=null && uiPending.compareAndSet(false,true))main.post{try{if(callback===cb && epoch==generation)cb.onFrame(frame)}finally{uiPending.set(false)}}
                } catch(e:Exception){engine.reset();publish(state.copy(message="IA indisponível: ${e.message}"));android.util.Log.e("DashCamVision","Análise suspensa",e)}
                finally{if(epoch==generation)cadence.analysisFinished(SystemClock.elapsedRealtime()-now);busy.set(false)}
            }
        } catch(e:Exception){if(analysisClaimed)busy.set(false);android.util.Log.e("DashCamFrame","Falha no quadro",e)}
        finally{image.close()}
    }
    private fun convert(image:Image):Bitmap {
        val crop=image.cropRect
        val size=crop.width()*crop.height()
        if(conversionPixels.size!=size)conversionPixels=IntArray(size)
        if(image.format==PixelFormat.RGBA_8888){
            val p=image.planes[0]
            val pixels=RgbaConverter.rgbInto(crop.width(),crop.height(),crop.left,crop.top,p.buffer,p.rowStride,p.pixelStride,conversionPixels)
            return Bitmap.createBitmap(pixels,crop.width(),crop.height(),Bitmap.Config.ARGB_8888)
        }
        val planes=image.planes.map{YuvConverter.Plane(it.buffer,it.rowStride,it.pixelStride)}
        val pixels=YuvConverter.rgbInto(crop.width(),crop.height(),crop.left,crop.top,planes[0],planes[1],planes[2],conversionPixels)
        return Bitmap.createBitmap(pixels,crop.width(),crop.height(),Bitmap.Config.ARGB_8888)
    }
    private fun closeCapture() {
        generation++;opening=false
        captureStartDeadline=0
        if(wakeLock.isHeld)wakeLock.release();wakeDeadline=0
        finishRecorder();directActive=false;liveSurface?.release();liveSurface=null;nativeCapture?.close();nativeCapture=null;camera?.close();camera=null
        reader?.setOnImageAvailableListener(null,null);reader?.close();reader=null
        lastFrame=0;processedFrames=0;recordingFrames=0;nativeFrames=0
        capturedFrames=0;maxSourceGap=0;cadence.reset();previewMeter.reset();sessionStartedAt=SystemClock.elapsedRealtime()
        analysis.execute{engine.reset()}
    }
    override fun onUnbind(intent:Intent?):Boolean{callback=null;attachPreviewTexture(null);return super.onUnbind(intent)}
    override fun onDestroy() {
        closing=true;running=false;callback=null
        cameraPreferences.unregisterOnSharedPreferenceChangeListener(performanceListener)
        if(availabilityRegistered)manager.unregisterAvailabilityCallback(availability)
        cameraHandler.post{recordWanted=false;closeCapture();cameraHandler.removeCallbacksAndMessages(null);worker.quitSafely();analysis.execute{detector?.close();laneNetwork?.close()};analysis.shutdown();evidence.shutdown()}
        audio?.release();audio=null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    companion object {private const val CHANNEL="dashcam_capture";private const val STOP="br.com.dashcamadas.STOP_CAPTURE"}
}
