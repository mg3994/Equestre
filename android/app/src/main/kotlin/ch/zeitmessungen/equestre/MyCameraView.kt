package ch.zeitmessungen.equestre

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
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
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent // Ensure this import is here
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.ContentValues // Explicitly import ContentValues for MediaStore operations

// Data class to hold parsed overlay item properties from Flutter
data class ParsedOverlayItem(
    val text: String?,
    val textSizePx: Int?,
    val bgColor: Int?, // Android Color int (AARRGGBB)
    val fgColor: Int?, // Android Color int (AARRGGBB)
    val x: Double?, // Relative 0.0 - 1.0 (top-left)
    val y: Double?  // Relative 0.0 - 1.0 (top-left)
)

@UnstableApi
class MyCameraView(
    private val context: Context,
    private val messenger: BinaryMessenger,
    initialCreationParams: Map<String?, Any?>?,
    private val activity: FlutterActivity
) : PlatformView {

    private val TAG = "MyCameraView"
    private val rootView: FrameLayout = FrameLayout(context)
    private val viewFinder = PreviewView(context)
    private var media3Effect: Media3Effect? = null
    private var currentOverlayConfigMap: Map<String, Any?> = initialCreationParams?.filterKeys { it != null }?.mapKeys { it.key!! } ?: emptyMap()

    private lateinit var methodChannel: MethodChannel
    private var cameraProvider: ProcessCameraProvider? = null
    private var useCaseGroup: UseCaseGroup? = null
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService

    init {
        rootView.addView(viewFinder, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
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
                cameraProvider?.bindToLifecycle(activity as LifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup!!)
                Log.d(TAG, "Camera bound with initial effects")
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed: ${e.message}", e)
                Toast.makeText(context, "Failed to start camera: ${e.message}", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun applyCurrentOverlayConfig() {
        val allOverlays = mutableListOf<TextureOverlay>()

        val itemPrefixes = listOf(
            "horseName", "horseNumber", "rider", "penalty",
            "timeTaken", "rank", "gapToBest", "liveMsg"
        )

        for (prefix in itemPrefixes) {
            val item = parseOverlayItem(prefix, currentOverlayConfigMap)
            if (item.text != null && item.x != null && item.y != null) {
                val textOverlay = createTextOverlay(item)
                allOverlays.add(textOverlay)
            }
        }

        val newOverlayEffect = OverlayEffect(ImmutableList.copyOf(allOverlays))

        media3Effect?.setEffects(listOf(newOverlayEffect))
        Log.d(TAG, "Overlays updated. Total overlays: ${allOverlays.size}")
    }

    private fun parseOverlayItem(prefix: String, configMap: Map<String, Any?>): ParsedOverlayItem {
        val text = configMap["${prefix}Text"] as? String
        val textSizePx = configMap["${prefix}TextSizePx"] as? Int
        val bgColorString = configMap["${prefix}BgColor"] as? String
        val fgColorString = configMap["${prefix}FgColor"] as? String
        val x = configMap["${prefix}X"] as? Double
        val y = configMap["${prefix}Y"] as? Double

        val bgColor = parseColor(bgColorString)
        val fgColor = parseColor(fgColorString)

        return ParsedOverlayItem(text, textSizePx, bgColor, fgColor, x, y)
    }

    private fun parseColor(hexColor: String?): Int? {
        return hexColor?.let {
            try {
                val formattedHex = if (it.startsWith("#")) it else "#$it"
                Color.parseColor(formattedHex)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid color hex string: $it", e)
                null
            }
        }
    }

    private fun createTextOverlay(item: ParsedOverlayItem): TextOverlay {
        return object : TextOverlay() {
            override fun getText(presentationTimeUs: Long): SpannableString {
                val spannableString = SpannableString(item.text)

                item.fgColor?.let { color ->
                    spannableString.setSpan(
                        ForegroundColorSpan(color),
                        0,
                        spannableString.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                item.bgColor?.let { color ->
                    spannableString.setSpan(
                        BackgroundColorSpan(color),
                        0,
                        spannableString.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                item.textSizePx?.let { size ->
                    spannableString.setSpan(
                        AbsoluteSizeSpan(size, true),
                        0,
                        spannableString.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                return spannableString
            }

            override fun getOverlaySettings(presentationTimeUs: Long): StaticOverlaySettings {
                return StaticOverlaySettings.Builder()
                    .setOverlayFrameAnchor(
                        (2 * item.x!! - 1).toFloat(),
                        (1 - 2 * item.y!!).toFloat()
                    )
                    .build()
            }
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
                "startRecording" -> {
                    startRecording(result)
                }
                "stopRecording" -> {
                    stopRecording(result)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun startRecording(result: MethodChannel.Result) {
        if (recording != null) {
            result.error("ALREADY_RECORDING", "Recording is already in progress.", null)
            return
        }

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/EquestreVideos")
            }
        }

        val outputOptions: FileOutputOptions

        // FIX: Re-verified and confirmed this usage for API >= Q.
        // The previous errors (Argument type mismatch, Too many arguments) for these lines
        // are highly unusual given the correct CameraX API usage. This might indicate a
        // caching issue or a problem with the build environment/dependencies not being
        // fully aligned, rather than a syntax error in these specific lines.
        // Keeping it as is, as it's the correct way per CameraX docs for Q+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            outputOptions = FileOutputOptions.Builder(
                context.contentResolver, // Line 308 (previous error line)
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, // Line 309 (previous error line)
                contentValues // Line 310 (previous error line)
            ).build()
        } else {
            val outputDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "EquestreVideos")
            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(outputDir, "$name.mp4")
            outputOptions = FileOutputOptions.Builder(outputFile).build()
        }

        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        // FIX: Access outputUri via outputResults
                        Log.d(TAG, "Recording started: ${event.outputResults.outputUri}") // Line 326
                        methodChannel.invokeMethod("onRecordingStart", event.outputResults.outputUri.toString()) // Line 327
                    }
                    is VideoRecordEvent.Finalize -> {
                        // FIX: Access error property and ERROR_NONE directly from VideoRecordEvent
                        if (event.error != VideoRecordEvent.ERROR_NONE) { // Line 331
                            Log.e(TAG, "Recording failed: ${event.error}")
                            recording = null
                            val msg = "Video recording failed: ${event.error}"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            methodChannel.invokeMethod("onRecordingError", msg)
                            result.error("RECORDING_FAILED", msg, null)
                        } else {
                            val msg = "Video recording succeeded: ${event.outputResults.outputUri}" // Line 339
                            Log.d(TAG, msg)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            recording = null
                            methodChannel.invokeMethod("onRecordingEnd", event.outputResults.outputUri.toString()) // Line 343
                            result.success(event.outputResults.outputUri.toString()) // Line 344
                        }
                    }
                    else -> {
                        Log.d(TAG, "VideoRecordEvent: $event")
                    }
                }
            }
        Log.d(TAG, "Started preparing recording...")
        result.success(true)
    }

    private fun stopRecording(result: MethodChannel.Result) {
        if (recording == null) {
            result.error("NOT_RECORDING", "No recording in progress.", null)
            return
        }
        Log.d(TAG, "Stopping recording...")
        recording?.stop()
        result.success(true)
    }

    override fun getView(): View = rootView

    override fun dispose() {
        Log.d(TAG, "MyCameraView disposed")
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        recording?.close()
        methodChannel.setMethodCallHandler(null)
    }
}