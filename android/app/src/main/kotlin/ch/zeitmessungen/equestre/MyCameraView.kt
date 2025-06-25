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
import android.widget.Button
import android.widget.FrameLayout
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
    private val messenger: BinaryMessenger,
    private var creationParams: Map<String?, Any?>?,
    private val activity: FlutterActivity
) : PlatformView, DefaultLifecycleObserver {

    private val rootView: FrameLayout = FrameLayout(context)
    private val viewFinder = PreviewView(context)
    private val recordButton = Button(context)
    private var overlayEffect: OverlayEffect? = null
    private var media3Effect: Media3Effect? = null
    private var camera: Camera? = null
    private var currentZoomRatio: Float = 1.0f
    private var recording: Recording? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private val methodChannel = MethodChannel(messenger, "camera_overlay_channel")

    init {
        activity.lifecycle.addObserver(this)
        rootView.addView(viewFinder)
        setupCamera()
        setupChannel()
        setupVolumeButtonZoom()
        setupRecordButton()
    }

    override fun onPause(owner: LifecycleOwner) {
        stopRecording()
        releaseCameraResources()
    }

    override fun onResume(owner: LifecycleOwner) {
        releaseCameraResources()
        setupCamera()
    }

    private fun releaseCameraResources() {
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider.unbindAll()
        } catch (e: Exception) {
            Log.e("MyCameraView", "Failed to unbind camera", e)
        }
        media3Effect?.release()
        media3Effect = null
        overlayEffect = null
        videoCapture = null
        recording = null
        camera = null
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
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
            ) {
                Toast.makeText(context, "Media3 error: ${it.message}", Toast.LENGTH_LONG).show()
            }

            media3Effect?.let { useCaseGroup.addEffect(it) }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    activity as LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    useCaseGroup.build()
                )
                currentZoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1.0f
            } catch (e: Exception) {
                Log.e("MyCameraView", "Camera bind failed", e)
            }

            applyOverlayFromParams()

        }, ContextCompat.getMainExecutor(context))
    }

    private fun applyOverlayFromParams() {
        val overlays = mutableListOf<TextOverlay>()
        val data = creationParams ?: return

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

        overlayEffect = OverlayEffect(ImmutableList.copyOf(overlays.map { it as TextureOverlay }))
        media3Effect?.setEffects(listOf(overlayEffect!!))
    }

    private fun createOverlay(text: String, x: Float, y: Float, bgColor: Int, fgColor: Int, textSizePx: Int): TextOverlay {
        return object : TextOverlay() {
            override fun getText(presentationTimeUs: Long): SpannableString {
                val spannable = SpannableString(text)
                spannable.setSpan(BackgroundColorSpan(bgColor), 0, text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(ForegroundColorSpan(fgColor), 0, text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(AbsoluteSizeSpan(textSizePx, false), 0, text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                return spannable
            }

            override fun getOverlaySettings(presentationTimeUs: Long): StaticOverlaySettings {
                return StaticOverlaySettings.Builder()
                    .setOverlayFrameAnchor(x, y)
                    .setBackgroundFrameAnchor(x, y)
                    .build()
            }
        }
    }

    private fun setupChannel() {
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "updateOverlay" -> {
                    val updates = call.arguments as? Map<String, Any?>
                    if (updates != null) {
                        val newParams = creationParams?.toMutableMap() ?: mutableMapOf()
                        updates.forEach { (key, value) -> newParams[key] = value }
                        creationParams = newParams
                        applyOverlayFromParams()
                        result.success(true)
                    } else {
                        result.error("INVALID", "Missing update data", null)
                    }
                }
                "startRecording" -> {
                    val started = startRecording()
                    result.success(started)
                }
                "stopRecording" -> {
                    val stopped = stopRecording()
                    result.success(stopped)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun setupRecordButton() {
        recordButton.text = "Record"
        recordButton.setBackgroundColor(Color.RED)
        recordButton.setTextColor(Color.WHITE)
        val size = 200
        val params = FrameLayout.LayoutParams(size, size)
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.bottomMargin = 50
        recordButton.layoutParams = params

        recordButton.setOnClickListener {
            if (recording == null) {
                if (startRecording()) {
                    recordButton.text = "Stop"
                }
            } else {
                if (stopRecording()) {
                    recordButton.text = "Record"
                }
            }
        }

        rootView.addView(recordButton)
    }

    private fun setupVolumeButtonZoom() {
        rootView.isFocusableInTouchMode = true
        rootView.requestFocus()
        rootView.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    zoomCamera(true)
                    true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    zoomCamera(false)
                    true
                }
                else -> false
            }
        }
    }

    private fun zoomCamera(zoomIn: Boolean) {
        camera?.let {
            val zoomState = it.cameraInfo.zoomState.value ?: return
            val zoomStep = 0.1f
            val newZoom = if (zoomIn)
                (zoomState.zoomRatio + zoomStep).coerceAtMost(zoomState.maxZoomRatio)
            else
                (zoomState.zoomRatio - zoomStep).coerceAtLeast(zoomState.minZoomRatio)

            it.cameraControl.setZoomRatio(newZoom)
            currentZoomRatio = newZoom
        }
    }

    private fun startRecording(): Boolean {
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
                        recordButton.text = "Record"
                        recording = null
                    }
                }
            }

        return true
    }

    private fun stopRecording(): Boolean {
        if (recording == null) {
            Toast.makeText(context, "No active recording", Toast.LENGTH_SHORT).show()
            return false
        }
        recording?.stop()
        return true
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

    override fun getView(): View = rootView
    override fun dispose() {
        releaseCameraResources()
    }
}
