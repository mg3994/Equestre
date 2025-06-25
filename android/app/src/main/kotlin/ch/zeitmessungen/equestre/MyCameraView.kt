package ch.zeitmessungen.equestre

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.media3.effect.Media3Effect
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextureOverlay
import com.google.common.collect.ImmutableList
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

@UnstableApi
class MyCameraView(
    private val context: Context,
    private val messenger: BinaryMessenger,
    private var creationParams: Map<String?, Any?>?,
    private val activity: FlutterActivity
) : PlatformView {

    private val rootView: FrameLayout = FrameLayout(context)
    private val viewFinder = PreviewView(context)
    private var overlayEffect: OverlayEffect? = null
    private var media3Effect: Media3Effect? = null
    private var camera: Camera? = null
    private var currentZoomRatio: Float = 1.0f

    init {
        rootView.addView(viewFinder)
        setupCamera()
        setupChannel()
        setupVolumeButtonZoom()
        startOverlayUpdateListener()
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
            val videoCapture = VideoCapture.withOutput(recorder)

            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(videoCapture)

            media3Effect = Media3Effect(
                context,
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
                ContextCompat.getMainExecutor(context)
            ) {
                Toast.makeText(context, "Media3 error: ${it.message}", Toast.LENGTH_LONG).show()
            }

            applyOverlayFromParams()
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

        }, ContextCompat.getMainExecutor(context))
    }

    private fun applyOverlayFromParams() {
        val overlays = mutableListOf<TextOverlay>()
        val data = creationParams ?: return

        val entries = listOf(
            Triple("horseNumber", -0.95f, 0.65f),
            Triple("horseName", -0.95f, 0.75f),
            Triple("rider", -0.95f, 0.85f),
            Triple("penalty", 0.65f, -0.95f),
            Triple("timeTaken", 0.75f, -0.95f),
            Triple("rank", 0.85f, -0.95f),
            Triple("gapToBest", 0.95f, -0.95f),
            Triple("liveMsg", 0.0f, 0.95f) // centered bottom
        )

        entries.forEach { (key, x, y) ->
            val value = data[key]?.toString()
            if (!value.isNullOrBlank()) {
                overlays.add(createOverlay(value, x, y))
            }
        }

        overlayEffect = OverlayEffect(ImmutableList.copyOf(overlays.map { it as TextureOverlay }))
        media3Effect?.setEffects(listOf(overlayEffect!!))
    }

    private fun createOverlay(text: String, x: Float, y: Float): TextOverlay {
        return object : TextOverlay() {
            override fun getText(presentationTimeUs: Long): SpannableString = SpannableString(text)

            override fun getOverlaySettings(presentationTimeUs: Long): StaticOverlaySettings {
                return StaticOverlaySettings.Builder()
                    .setOverlayFrameAnchor(x, y)
                    .build()
            }
        }
    }

    private fun setupChannel() {
        MethodChannel(messenger, "camera_overlay_channel").setMethodCallHandler { call, result ->
            when (call.method) {
                "updateOverlay" -> {
                    val updates = call.arguments as? Map<String, String>
                    if (updates != null) {
                        val newParams = creationParams?.toMutableMap() ?: mutableMapOf()
                        updates.forEach { (key, value) -> newParams[key] = value }
                        creationParams = newParams
                        applyOverlayFromParams()
                        result.success(null)
                    } else {
                        result.error("INVALID", "Missing update data", null)
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun startOverlayUpdateListener() {
        val handler = Handler(Looper.getMainLooper())
        val updateRunnable = object : Runnable {
            override fun run() {
                val updatedData = mapOf(
                    "horseName" to "Storm Fury ${System.currentTimeMillis() / 1000}",
                    "rider" to "John Doe",
                    "penalty" to "0.5s",
                    "timeTaken" to "52.3s",
                    "rank" to "2nd",
                    "gapToBest" to "+1.3s",
                    "liveMsg" to "Match Live"
                )
                val newParams = creationParams?.toMutableMap() ?: mutableMapOf()
                updatedData.forEach { (k, v) -> newParams[k] = v }
                creationParams = newParams
                applyOverlayFromParams()
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(updateRunnable)
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

    override fun getView(): View = rootView
    override fun dispose() {}
}
