package ch.zeitmessungen.equestre

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
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
import com.google.common.collect.ImmutableList
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import androidx.media3.effect.TextureOverlay

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

    init {
        rootView.addView(viewFinder)
        setupCamera()
        setupChannel()
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
                cameraProvider.bindToLifecycle(activity as LifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup.build())
            } catch (e: Exception) {
                Log.e("MyCameraView", "Camera bind failed", e)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun applyOverlayFromParams() {
        val overlays = mutableListOf<TextOverlay>()
        creationParams?.forEach { (key, value) ->
            val text = value?.toString() ?: return@forEach
            val (x, y) = when (key) {
                "horseNumber" -> -0.95f to 0.65f
                "horseName" -> -0.95f to 0.75f
                "rider" -> -0.95f to 0.85f
                else -> return@forEach
            }
            overlays.add(createOverlay(text, x, y))
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
                    "rider" to "Rider: John Doe"
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

    override fun getView(): View = rootView
    override fun dispose() {}
}