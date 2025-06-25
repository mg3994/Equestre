package ch.zeitmessungen.equestre

import android.content.Context
import android.graphics.Color
import android.os.Environment
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.media3.effect.Media3Effect
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.*
import com.google.common.collect.ImmutableList
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.io.File

@UnstableApi
class MyCameraView(
    private val context: Context,
    messenger: BinaryMessenger,
    private var creationParams: Map<String?, Any?>?,
    private val activity: FlutterActivity
) : PlatformView, DefaultLifecycleObserver {

    private val rootView = FrameLayout(context)
    private val previewView = PreviewView(context)

    private val cameraManager = CameraManager(context, activity, previewView)
    private val overlayManager = OverlayManager()
    private val videoRecorder = VideoRecorder(context)

    private val methodChannel = MethodChannel(messenger, "camera_overlay_channel")

    private val recordButton = ImageButton(context).apply {
       setBackgroundResource(R.drawable.circular_button)
        setImageResource(android.R.drawable.ic_media_play)
        val size = (64 * context.resources.displayMetrics.density).toInt() // 64dp size
        layoutParams = FrameLayout.LayoutParams(size, size, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = (32 * context.resources.displayMetrics.density).toInt()
        }
        scaleType = ImageButton.ScaleType.CENTER_INSIDE
        isClickable = true
        isFocusable = true
    }

    private var isRecording = false

    init {
        activity.lifecycle.addObserver(this)
        rootView.addView(previewView)
        rootView.addView(recordButton)

        setupCamera()
        setupChannel()
        setupVolumeButtonZoom()
        setupRecordButton()
    }

    private fun setupRecordButton() {
        recordButton.setOnClickListener {
            if (isRecording) {
                val stopped = videoRecorder.stopRecording()
                if (stopped) {
                    Toast.makeText(context, "Recording stopped", Toast.LENGTH_SHORT).show()
                    recordButton.setImageResource(android.R.drawable.ic_media_play)
                    isRecording = false
                }
            } else {
                val started = videoRecorder.startRecording(methodChannel)
                if (started) {
                    Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
                    recordButton.setImageResource(android.R.drawable.ic_media_pause)
                    isRecording = true
                }
            }
        }
    }

    private fun setupCamera() {
        cameraManager.initialize { mediaEffect ->
            videoRecorder.setMedia3Effect(mediaEffect)
            videoRecorder.bindVideoCapture(cameraManager.getVideoCapture())
            creationParams?.let { overlayManager.updateOverlayFromParams(it, mediaEffect) }
        }
    }

    private fun setupChannel() {
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "updateOverlay" -> {
                    val updates = call.arguments as? Map<String, Any?>
                    if (updates != null) {
                        creationParams = (creationParams ?: emptyMap()).toMutableMap().apply {
                            putAll(updates)
                        }
                        cameraManager.media3Effect?.let {
                            overlayManager.updateOverlayFromParams(creationParams, it)
                        }
                        result.success(true)
                    } else {
                        result.error("INVALID", "Missing update data", null)
                    }
                }
                "startRecording" -> {
                    val started = videoRecorder.startRecording(methodChannel)
                    if (started) {
                        recordButton.setImageResource(android.R.drawable.ic_media_pause)
                        isRecording = true
                    }
                    result.success(started)
                }
                "stopRecording" -> {
                    val stopped = videoRecorder.stopRecording()
                    if (stopped) {
                        recordButton.setImageResource(android.R.drawable.ic_media_play)
                        isRecording = false
                    }
                    result.success(stopped)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun setupVolumeButtonZoom() {
        rootView.isFocusableInTouchMode = true
        rootView.requestFocus()
        rootView.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    cameraManager.zoomCamera(true)
                    true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    cameraManager.zoomCamera(false)
                    true
                }
                else -> false
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        videoRecorder.stopRecording()
        cameraManager.releaseResources()
    }

    override fun onResume(owner: LifecycleOwner) {
        cameraManager.releaseResources()
        setupCamera()
    }

    override fun dispose() {
        videoRecorder.stopRecording()
        cameraManager.releaseResources()
    }

    override fun getView(): View = rootView
}

// CameraManager: manages camera setup and zoom
class CameraManager(
    private val context: Context,
    private val activity: FlutterActivity,
    private val previewView: PreviewView
) {
    var media3Effect: Media3Effect? = null
        private set
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null

    fun initialize(onEffectReady: (Media3Effect) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(videoCapture!!)

            media3Effect = Media3Effect(
                context,
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
                ContextCompat.getMainExecutor(context)
            ) { error ->
                Toast.makeText(context, "Media3 error: ${error.message}", Toast.LENGTH_LONG).show()
            }
            media3Effect?.let { useCaseGroup.addEffect(it) }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    activity as LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    useCaseGroup.build()
                )
            } catch (e: Exception) {
                Log.e("CameraManager", "Camera bind failed", e)
            }

            media3Effect?.let(onEffectReady)
        }, ContextCompat.getMainExecutor(context))
    }

    fun zoomCamera(zoomIn: Boolean) {
        camera?.let {
            val zoomState = it.cameraInfo.zoomState.value ?: return
            val zoomStep = 0.1f
            val newZoom = if (zoomIn)
                (zoomState.zoomRatio + zoomStep).coerceAtMost(zoomState.maxZoomRatio)
            else
                (zoomState.zoomRatio - zoomStep).coerceAtLeast(zoomState.minZoomRatio)
            it.cameraControl.setZoomRatio(newZoom)
        }
    }

    fun getVideoCapture(): VideoCapture<Recorder>? = videoCapture

    fun releaseResources() {
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider.unbindAll()
        } catch (e: Exception) {
            Log.e("CameraManager", "Failed to unbind camera", e)
        }
        media3Effect?.release()
        media3Effect = null
        camera = null
        videoCapture = null
    }
}

// VideoRecorder: manages start/stop video recording
class VideoRecorder(private val context: Context) {

    private var media3Effect: Media3Effect? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    fun setMedia3Effect(effect: Media3Effect) {
        this.media3Effect = effect
    }

    fun bindVideoCapture(videoCapture: VideoCapture<Recorder>?) {
        this.videoCapture = videoCapture
    }

    fun startRecording(methodChannel: MethodChannel): Boolean {
        if (videoCapture == null || recording != null) {
            Toast.makeText(context, "Recording already in progress", Toast.LENGTH_SHORT).show()
            return false
        }

        val fileName = "equestre_${System.currentTimeMillis()}.mp4"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val outputDir = File(downloadsDir, "Equestre")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, fileName)

        val outputOptions = FileOutputOptions.Builder(file).build()

        recording = videoCapture!!.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        val msg = if (event.hasError()) {
                            "Recording error: ${event.error}"
                        } else {
                            methodChannel.invokeMethod("onRecordingStopped", mapOf(
                                "reason" to "finalized",
                                "filePath" to file.absolutePath,
                                "success" to true
                            ))
                            "Video saved: ${file.absolutePath}"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        recording = null
                    }
                }
            }

        return true
    }

    fun stopRecording(): Boolean {
        if (recording == null) {
            Toast.makeText(context, "No active recording", Toast.LENGTH_SHORT).show()
            return false
        }
        recording?.stop()
        return true
    }
}

// OverlayManager: creates and updates overlay texts dynamically
class OverlayManager {

    fun updateOverlayFromParams(
        creationParams: Map<String?, Any?>?,
        effect: Media3Effect
    ) {
        val overlays = mutableListOf<TextOverlay>()
        val data = creationParams ?: return

        // Find keys that end with X (coordinate keys), then use base key name to get all props
        val keys = data.keys.mapNotNull { it }.filter { it.endsWith("X") }.map { it.removeSuffix("X") }

        keys.forEach { key ->
            val text = data[key]?.toString() ?: return@forEach
            val textSizePx = (data["${key}TextSizePx"] as? Int) ?: 22
            val bgColor = parseColor(data["${key}BgColor"] as? String) ?: 0x88000000.toInt()
            val fgColor = parseColor(data["${key}FgColor"] as? String) ?: 0xFFFFFFFF.toInt()
            val x = (data["${key}X"] as? Double)?.toFloat() ?: 0f
            val y = (data["${key}Y"] as? Double)?.toFloat() ?: 0f
            overlays.add(createOverlay(text, x, y, bgColor, fgColor, textSizePx))
        }

        val overlayEffect = OverlayEffect(ImmutableList.copyOf(overlays.map { it as TextureOverlay }))
        effect.setEffects(listOf(overlayEffect))
    }

    private fun createOverlay(
        text: String,
        x: Float,
        y: Float,
        bgColor: Int,
        fgColor: Int,
        textSizePx: Int
    ): TextOverlay {
        return object : TextOverlay() {
            override fun getText(presentationTimeUs: Long): SpannableString {
                return SpannableString(text).apply {
                    setSpan(BackgroundColorSpan(bgColor), 0, text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(ForegroundColorSpan(fgColor), 0, text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(AbsoluteSizeSpan(textSizePx, false), 0, text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            override fun getOverlaySettings(presentationTimeUs: Long): StaticOverlaySettings {
                return StaticOverlaySettings.Builder()
                    .setOverlayFrameAnchor(x, y)
                    .setBackgroundFrameAnchor(x, y)
                    .build()
            }
        }
    }

    private fun parseColor(colorStr: String?): Int? {
        return try {
            if (colorStr != null && colorStr.startsWith("#")) {
                Color.parseColor(colorStr)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
