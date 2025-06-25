package ch.zeitmessungen.equestre

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.media3.effect.Media3Effect
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import com.google.common.collect.ImmutableList
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@UnstableApi
class MyCameraView(
    private val context: Context,
    private val messenger: BinaryMessenger,
    initialCreationParams: Map<String?, Any?>?,
    private val activity: FlutterActivity
) : PlatformView {

    private val TAG = "MyCameraView"
    private val rootView = FrameLayout(context)
    private val viewFinder = PreviewView(context)
    private var media3Effect: Media3Effect? = null
    private var currentOverlayConfigMap: Map<String, Any?> =
        initialCreationParams?.filterKeys { it != null }?.mapKeys { it.key!! } ?: emptyMap()

    private lateinit var methodChannel: MethodChannel
    private var cameraProvider: ProcessCameraProvider? = null
    private var useCaseGroup: UseCaseGroup? = null
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService

    init {
        rootView.addView(
            viewFinder, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        cameraExecutor = Executors.newSingleThreadExecutor()
        setupChannel()
        setupCamera()
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            media3Effect = Media3Effect(
                context,
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
                ContextCompat.getMainExecutor(context)
            ) {
                Log.e(TAG, "Media3Effect error: ${it.message}", it)
                Toast.makeText(context, "Media3 error: ${it.message}", Toast.LENGTH_LONG).show()
            }

            val useCaseGroupBuilder = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(videoCapture)

            media3Effect?.let { effect ->
                useCaseGroupBuilder.addEffect(effect)
            }

            useCaseGroup = useCaseGroupBuilder.build()
            applyCurrentOverlayConfig()

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    activity as LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    useCaseGroup!!
                )
                Log.d(TAG, "Camera bound with initial effects")
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed: ${e.message}", e)
                Toast.makeText(context, "Failed to start camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun applyCurrentOverlayConfig() {
        val overlays = mutableListOf<TextureOverlay>()
        val keys =
            listOf("horseName", "horseNumber", "rider", "penalty", "timeTaken", "rank", "gapToBest", "liveMsg")
        keys.mapNotNull { parseOverlayItem(it, currentOverlayConfigMap) }
            .forEach { overlays.add(it) }

        val effect = OverlayEffect(ImmutableList.copyOf(overlays))
        media3Effect?.setEffects(listOf(effect))
        Log.d(TAG, "Applied ${overlays.size} overlays")
    }

    private fun parseOverlayItem(key: String, config: Map<String, Any?>): TextOverlay? {
        val text = config["${key}Text"] as? String ?: return null
        val size = config["${key}TextSizePx"] as? Int
        val bg = parseColor(config["${key}BgColor"] as? String)
        val fg = parseColor(config["${key}FgColor"] as? String)
        val x = config["${key}X"] as? Double ?: return null
        val y = config["${key}Y"] as? Double ?: return null

        return object : TextOverlay() {
            override fun getText(presentationTimeUs: Long): SpannableString {
                return SpannableString(text).apply {
                    fg?.let { setSpan(ForegroundColorSpan(it), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
                    bg?.let { setSpan(BackgroundColorSpan(it), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
                    size?.let { setSpan(AbsoluteSizeSpan(it, true), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
                }
            }

            override fun getOverlaySettings(presentationTimeUs: Long): StaticOverlaySettings {
                return StaticOverlaySettings.Builder()
                    .setOverlayFrameAnchor((2 * x - 1).toFloat(), (1 - 2 * y).toFloat())
                    .build()
            }
        }
    }

    private fun parseColor(hex: String?): Int? {
        return try {
            hex?.let { Color.parseColor(if (it.startsWith("#")) it else "#$it") }
        } catch (e: Exception) {
            Log.e(TAG, "Invalid color: $hex", e)
            null
        }
    }

    private fun setupChannel() {
        methodChannel = MethodChannel(messenger, "camera_overlay_channel")
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "updateOverlay" -> {
                    val updates = call.arguments as? Map<String?, Any?>
                    if (updates != null) {
                        currentOverlayConfigMap = updates.filterKeys { it != null }.mapKeys { it.key!! }
                        applyCurrentOverlayConfig()
                        result.success(true)
                    } else {
                        result.error("INVALID_ARGUMENT", "Overlay data is null or invalid", null)
                    }
                }
                "startRecording" -> startRecording(result)
                "stopRecording" -> stopRecording(result)
                else -> result.notImplemented()
            }
        }
    }

    private fun startRecording(result: MethodChannel.Result) {
        if (recording != null) {
            result.error("ALREADY_RECORDING", "Recording already in progress.", null)
            return
        }

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/EquestreVideos")
            }
            val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
                .setContentValues(contentValues)
                .build()

            val pendingRecording: PendingRecording = videoCapture.output
                .prepareRecording(context, mediaStoreOutputOptions)
                .withAudioEnabled()

            recording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event: VideoRecordEvent ->
                handleRecordingEvent(event, name, result)
            }

        } else {
            val outputDir =
                File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "EquestreVideos").apply { if (!exists()) mkdirs() }
            val file = File(outputDir, "$name.mp4")

            val fileOutputOptions = FileOutputOptions.Builder(file).build()

            val pendingRecording: PendingRecording = videoCapture.output
                .prepareRecording(context, fileOutputOptions)
                .withAudioEnabled()

            recording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event: VideoRecordEvent ->
                handleRecordingEvent(event, file.absolutePath, result)
            }
        }
        result.success(true)
    }

    private fun handleRecordingEvent(event: VideoRecordEvent, pathOrName: String, result: MethodChannel.Result) {
        when (event) {
            is VideoRecordEvent.Start -> {
                Log.d(TAG, "Recording started")
                methodChannel.invokeMethod("onRecordingStart", null)
            }
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    val err = "Recording error: ${event.error}"
                    Log.e(TAG, err)
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    methodChannel.invokeMethod("onRecordingError", err)
                    result.error("RECORDING_FAILED", err, null)
                } else {
                    Log.d(TAG, "Recording saved: $pathOrName")
                    Toast.makeText(context, "Saved: $pathOrName", Toast.LENGTH_SHORT).show()
                    methodChannel.invokeMethod("onRecordingEnd", pathOrName)
                    result.success(pathOrName)
                }
                recording = null
            }
            else -> Log.d(TAG, "Record event: $event")
        }
    }

    private fun stopRecording(result: MethodChannel.Result) {
        recording?.stop() ?: run {
            result.error("NOT_RECORDING", "No recording in progress.", null)
            return
        }
        result.success(true)
    }

    override fun getView(): View = rootView

    override fun dispose() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        recording?.close()
        methodChannel.setMethodCallHandler(null)
    }
}
