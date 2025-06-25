package ch.zeitmessungen.equestre

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface // Added Typeface import for potential future styling
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
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
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
import io.flutter.plugin.platform.PlatformView // IMPORTERT: PlatformView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.ContentValues // Explicitly import ContentValues

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
    initialCreationParams: Map<String?, Any?>?, // Keep as Map<String?, Any?> for incoming
    private val activity: FlutterActivity
) : PlatformView { // FIX 1 & 13 & 14: Implement PlatformView

    private val TAG = "MyCameraView"
    private val rootView: FrameLayout = FrameLayout(context)
    private val viewFinder = PreviewView(context)
    private var media3Effect: Media3Effect? = null
    // FIX 2 & 3: Use Map<String, Any?> for internal config by explicitly converting nullable keys to non-null
    // This assumes Flutter always sends non-null String keys.
    private var currentOverlayConfigMap: Map<String, Any?> = initialCreationParams?.filterKeys { it != null }?.mapKeys { it.key!! } ?: emptyMap()

    private lateinit var methodChannel: MethodChannel
    private var cameraProvider: ProcessCameraProvider? = null
    private var useCaseGroup: UseCaseGroup? = null // This will be the built UseCaseGroup, not its builder
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService

    init {
        rootView.addView(viewFinder, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        cameraExecutor = Executors.newSingleThreadExecutor()
        setupChannel() // Setup channel to receive Flutter commands
        setupCamera() // Start camera and apply initial overlays
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST)) // Or Quality.HD, Quality.SD etc.
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            media3Effect = Media3Effect(
                context,
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE, // Apply effect to both preview and video capture
                ContextCompat.getMainExecutor(context)
            ) {
                // Error listener for Media3Effect
                Log.e(TAG, "Media3Effect error: ${it.message}", it)
                Toast.makeText(context, "Media3 error: ${it.message}", Toast.LENGTH_LONG).show()
            }

            val useCaseGroupBuilder = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(videoCapture)

            // FIX 4 & 5: Add effect to the UseCaseGroup.Builder BEFORE building the UseCaseGroup
            media3Effect?.let { effect ->
                useCaseGroupBuilder.addEffect(effect)
            }

            useCaseGroup = useCaseGroupBuilder.build() // Build the useCaseGroup after adding effect

            // Apply initial overlays from current configuration
            applyCurrentOverlayConfig() // This sets the effects within media3Effect

            try {
                cameraProvider?.unbindAll()
                // Cast `activity` to `LifecycleOwner`. `FlutterActivity` extends `AppCompatActivity` which is a LifecycleOwner.
                cameraProvider?.bindToLifecycle(activity as LifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup!!)
                Log.d(TAG, "Camera bound with initial effects")
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed: ${e.message}", e)
                Toast.makeText(context, "Failed to start camera: ${e.message}", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(context))
    }

    // This function rebuilds and applies all overlays to the Media3Effect
    private fun applyCurrentOverlayConfig() {
        val allOverlays = mutableListOf<TextureOverlay>()

        // Define the order of your overlay items based on your OverlayConfig in Flutter
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

        // Create a new OverlayEffect with the updated list of overlays
        val newOverlayEffect = OverlayEffect(ImmutableList.copyOf(allOverlays))

        // Apply the new OverlayEffect to the Media3Effect
        // This is how you update the effect applied to the camera stream
        media3Effect?.setEffects(listOf(newOverlayEffect))
        Log.d(TAG, "Overlays updated. Total overlays: ${allOverlays.size}")
    }

    // Parses a single OverlayItem's properties from the flattened map
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

    // Helper to parse hex color string to Android Color int
    private fun parseColor(hexColor: String?): Int? {
        return hexColor?.let {
            try {
                // Ensure the hex string starts with # for Color.parseColor
                val formattedHex = if (it.startsWith("#")) it else "#$it"
                Color.parseColor(formattedHex)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid color hex string: $it", e)
                null
            }
        }
    }

    // Creates a Media3 TextOverlay from a ParsedOverlayItem
    private fun createTextOverlay(item: ParsedOverlayItem): TextOverlay {
        return object : TextOverlay() {
            override fun getText(presentationTimeUs: Long): SpannableString {
                val spannableString = SpannableString(item.text)

                // Apply foreground (text) color
                item.fgColor?.let { color ->
                    spannableString.setSpan(
                        ForegroundColorSpan(color),
                        0,
                        spannableString.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // Apply background color for the text
                item.bgColor?.let { color ->
                    spannableString.setSpan(
                        BackgroundColorSpan(color),
                        0,
                        spannableString.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // Apply text size (converted from px to SP if necessary, but Media3 often handles absolute px)
                item.textSizePx?.let { size ->
                    // The 'true' argument means units are pixels. If you want scaled pixels (sp), you'd convert.
                    // For Media3Effect, absolute pixel values often work best.
                    spannableString.setSpan(
                        AbsoluteSizeSpan(size, true),
                        0,
                        spannableString.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // Optional: Add other styles like bold, italic if your OverlayItem supports them
                // Example: spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                return spannableString
            }

            override fun getOverlaySettings(presentationTimeUs: Long): StaticOverlaySettings {
                return StaticOverlaySettings.Builder()
                    // Convert Flutter's 0.0-1.0 (top-left) coordinates to Media3's -1.0-1.0 (center is 0,0, top-right is 1,1, bottom-left is -1,-1)
                    // newX = 2 * oldX - 1
                    // newY = 1 - 2 * oldY (Y-axis is inverted for Media3 vs UI coordinates)
                    .setOverlayFrameAnchor(
                        (2 * item.x!! - 1).toFloat(),
                        (1 - 2 * item.y!!).toFloat()
                    )
                    .build()
            }
        }
    }

    // Setup MethodChannel to receive commands from Flutter
    private fun setupChannel() {
        methodChannel = MethodChannel(messenger, "camera_overlay_channel")
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "updateOverlay" -> {
                    val updates = call.arguments as? Map<String?, Any?>
                    if (updates != null) {
                        // FIX 2 & 3: Filter null keys and explicitly convert to non-nullable strings for storage
                        currentOverlayConfigMap = updates.filterKeys { it != null }.mapKeys { it.key!! }
                        applyCurrentOverlayConfig() // Re-apply all overlays based on the new config
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

    // Function to start video recording
    private fun startRecording(result: MethodChannel.Result) {
        if (recording != null) {
            result.error("ALREADY_RECORDING", "Recording is already in progress.", null)
            return
        }

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply { // FIX 6: Use android.content.ContentValues
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/EquestreVideos")
            }
        }

        val outputOptions: FileOutputOptions // Declare type explicitly

        // FIX 6, 7, 8: Corrected FileOutputOptions.Builder usage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            outputOptions = FileOutputOptions.Builder(
                context.contentResolver,
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()
        } else {
            val outputDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "EquestreVideos")
            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(outputDir, "$name.mp4")
            outputOptions = FileOutputOptions.Builder(outputFile).build()
        }

        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled() // Enable audio recording
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        // FIX 9 & 10: Access properties directly from the event object
                        Log.d(TAG, "Recording started: ${event.outputUri}")
                        methodChannel.invokeMethod("onRecordingStart", event.outputUri.toString())
                    }
                    is VideoRecordEvent.Finalize -> {
                        // FIX 11 & 12: Check error property directly
                        if (event.error != VideoRecordEvent.ERROR_NONE) { // Use VideoRecordEvent.ERROR_NONE for clarity
                            Log.e(TAG, "Recording failed: ${event.error}")
                            recording = null // Clear recording reference
                            val msg = "Video recording failed: ${event.error}"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            methodChannel.invokeMethod("onRecordingError", msg)
                            result.error("RECORDING_FAILED", msg, null) // Notify Flutter about failure
                        } else {
                            val msg = "Video recording succeeded: ${event.outputUri}" // FIX 9 & 10
                            Log.d(TAG, msg)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            recording = null // Clear recording reference
                            methodChannel.invokeMethod("onRecordingEnd", event.outputUri.toString()) // FIX 9 & 10
                            result.success(event.outputUri.toString()) // Notify Flutter about success
                        }
                    }
                    // Handle other events like Pause, Resume, Status
                    else -> {
                        Log.d(TAG, "VideoRecordEvent: $event")
                    }
                }
            }
        Log.d(TAG, "Started preparing recording...")
        // Signal immediate success to the startRecording call as preparation has begun
        // The Finalize event will carry the actual outcome.
        result.success(true)
    }

    // Function to stop video recording
    private fun stopRecording(result: MethodChannel.Result) {
        if (recording == null) {
            result.error("NOT_RECORDING", "No recording in progress.", null)
            return
        }
        Log.d(TAG, "Stopping recording...")
        recording?.stop() // This will trigger the VideoRecordEvent.Finalize event
        // Do NOT set recording = null here, let Finalize event handle it to ensure correct state.
        result.success(true)
    }

    // FIX 13: Implement getView from PlatformView
    override fun getView(): View = rootView

    // FIX 14: Implement dispose from PlatformView
    override fun dispose() {
        Log.d(TAG, "MyCameraView disposed")
        // Unbind use cases and release resources
        cameraProvider?.unbindAll()
        // FIX 15: Removed media3Effect?.release(). Media3Effect's lifecycle is managed by CameraX when added to UseCaseGroup.
        cameraExecutor.shutdown() // Shut down the executor service
        recording?.close() // Ensure any ongoing recording is closed
        methodChannel.setMethodCallHandler(null) // Unset method channel handler
    }
}