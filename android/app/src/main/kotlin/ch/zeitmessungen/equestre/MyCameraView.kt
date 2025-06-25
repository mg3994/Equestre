package ch.zeitmessungen.equestre

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.SpannableString
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.media3.effect.Media3Effect
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.*
import com.google.common.collect.ImmutableList
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.text.SimpleDateFormat
import java.util.*

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

    private var recording: Recording? = null
    private var videoCapture: VideoCapture<Recorder>? = null

    init {
        rootView.addView(viewFinder)
        addRecordButton()
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

    private fun addRecordButton() {
        val recordButton = Button(context).apply {
            text = "●"
            textSize = 24f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.RED)
            val size = 150
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 60
            }
        }

        recordButton.setOnClickListener {
            if (recording == null) {
                startRecording()
                recordButton.text = "■"
            } else {
                stopRecording()
                recordButton.text = "●"
            }
        }

        rootView.addView(recordButton)
    }

    private fun startRecording() {
        if (videoCapture == null) return

        val name = "equestre_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Equestre")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        val pendingRecording = videoCapture!!.output
            .prepareRecording(context, outputOptions)
            .apply {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }

        recording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            if (recordEvent is VideoRecordEvent.Finalize) {
                Toast.makeText(context, "Saved: ${recordEvent.outputResults.outputUri}", Toast.LENGTH_LONG).show()
                recording = null
            }
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
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
