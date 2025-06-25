package ch.zeitmessungen.equestre

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
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

@UnstableApi
class MyCameraView(
    private val context: Context,
    private val messenger: BinaryMessenger,
    private var initialCreationParams: Map<String?, Any?>?, // Renamed for clarity
    private val activity: FlutterActivity
) : PlatformView {

    private val TAG = "MyCameraView"
    private val rootView: FrameLayout = FrameLayout(context)
    private val viewFinder = PreviewView(context)
    private var media3Effect: Media3Effect? = null
    private var currentOverlayConfigMap: Map<String, Any?> = initialCreationParams ?: emptyMap()
    private lateinit var methodChannel: MethodChannel

    // You can keep a reference to your CameraProvider and UseCaseGroup if you need to unbind/rebind
    private var cameraProvider: ProcessCameraProvider? = null
    private var useCaseGroup: UseCaseGroup? = null

    init {
        rootView.addView(viewFinder)
        // Initialize the channel first so it's ready to receive updates even before camera starts
        setupChannel()
        setupCamera()
        // Removed startOverlayUpdateListener for now, as Flutter will drive updates
        // If you need native-side updates for testing, re-add carefully.
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            // Recorder is typically for video capture, keeping it as in your original code
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            val videoCapture = VideoCapture.withOutput(recorder)

            useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(videoCapture)
                .build()

            media3Effect = Media3Effect(
                context,
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
                ContextCompat.getMainExecutor(context)
            ) {
                Toast.makeText(context, "Media3 error: ${it.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Media3 error: ${it.message}", it)
            }

            // Apply initial overlays from current configuration
            applyCurrentOverlayConfig()
            media3Effect?.let { useCaseGroup?.addEffect(it) } // Add effect to the use case group

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(activity as LifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup!!)
                Log.d(TAG, "Camera bound with initial effects")
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
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
                // Media3 TextOverlay handles relative coordinates (-1 to 1) directly
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
                // Ensure the hex string starts with # for Color.parseColor
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
                        AbsoluteSizeSpan(size, true), // true means DIP/PX, here assuming PX as per name
                        0,
                        spannableString.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // Add more styles (bold, italic, typeface) if your OverlayItem includes them
                // Example: for bold (requires CustomTypefaceSpan if not using default bold)
                // spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                return spannableString
            }

            override fun getOverlaySettings(presentationTimeUs: Long): StaticOverlaySettings {
                return StaticOverlaySettings.Builder()
                    // Media3 uses normalized coordinates where (-1, -1) is bottom-left, (1, 1) is top-right.
                    // Your Flutter `x, y` are 0.0 to 1.0 (top-left is 0,0, bottom-right is 1,1).
                    // We need to convert:
                    // newX = 2 * oldX - 1
                    // newY = 1 - 2 * oldY (y-axis is inverted)
                    .setOverlayFrameAnchor(
                        (2 * item.x!! - 1).toFloat(),
                        (1 - 2 * item.y!!).toFloat()
                    )
                    // You might also want to set the text size based on a factor of the frame
                    // .setTextSize(item.textSizePx?.toFloat() ?: 0.0f) // This might be for different TextOverlay usage
                    // For TextOverlay, the textSizePx is usually handled by AbsoluteSizeSpan in getText().
                    // Setting a pixel size here might override or combine in unexpected ways.
                    // We rely on AbsoluteSizeSpan.
                    .build()
            }
        }
    }


    private fun setupChannel() {
        methodChannel = MethodChannel(messenger, "camera_overlay_channel")
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "updateOverlay" -> {
                    // Receive the full OverlayConfig map from Flutter
                    val updates = call.arguments as? Map<String, Any?>
                    if (updates != null) {
                        currentOverlayConfigMap = updates // Update the entire config map
                        applyCurrentOverlayConfig() // Re-apply all overlays based on the new config
                        result.success(null)
                    } else {
                        result.error("INVALID_ARGUMENT", "Missing or invalid update data", null)
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

    override fun getView(): View = rootView

    override fun dispose() {
        // Unbind use cases to release camera resources
        cameraProvider?.unbindAll()
        media3Effect?.release() // Release Media3Effect resources
        methodChannel.setMethodCallHandler(null)
        Log.d(TAG, "MyCameraView disposed")
    }
}

// Data class to hold parsed overlay item properties (same as before)
data class ParsedOverlayItem(
    val text: String?,
    val textSizePx: Int?,
    val bgColor: Int?, // Android Color int
    val fgColor: Int?, // Android Color int
    val x: Double?, // Relative 0.0 - 1.0
    val y: Double?  // Relative 0.0 - 1.0
)