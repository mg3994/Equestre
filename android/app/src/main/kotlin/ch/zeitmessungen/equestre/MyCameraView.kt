package ch.zeitmessungen.equestre

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import android.widget.ImageView // Added ImageView import for ScaleType
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.media3.effect.Media3Effect
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

//region Constants and Data Classes

object CameraConstants {
    const val CHANNEL_NAME = "camera_overlay_channel"

    // Method Channel Names
    const val METHOD_UPDATE_OVERLAY = "updateOverlay"
    const val METHOD_START_RECORDING = "startRecording"
    const val METHOD_STOP_RECORDING = "stopRecording"
    const val METHOD_ON_RECORDING_STOPPED = "onRecordingStopped"

    // Overlay Item Prefixes (matching Dart's OverlayConfig.toMap())
    const val PREFIX_HORSE_NAME = "horseName"
    const val PREFIX_HORSE_NUMBER = "horseNumber"
    const val PREFIX_RIDER = "rider"
    const val PREFIX_PENALTY = "penalty"
    const val PREFIX_TIME_TAKEN = "timeTaken"
    const val PREFIX_RANK = "rank"
    const val PREFIX_GAP_TO_BEST = "gapToBest"
    const val PREFIX_LIVE_MSG = "liveMsg"

    // Overlay Property Suffixes
    const val SUFFIX_TEXT_SIZE_PX = "TextSizePx"
    const val SUFFIX_BG_COLOR = "BgColor"
    const val SUFFIX_FG_COLOR = "FgColor"
    const val SUFFIX_X = "X"
    const val SUFFIX_Y = "Y"

    // Permissions
    const val PERMISSIONS_REQUEST_CODE = 101
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE // For older APIs; Media Store is preferred for Q+
    )
}

data class OverlayProperties(
    val text: String,
    val textSizePx: Int,
    val bgColor: Int,
    val fgColor: Int,
    val x: Float,
    val y: Float
)

//endregion

//region Permission Handling

class PermissionHandler(private val activity: FlutterActivity) {

    fun allPermissionsGranted(): Boolean {
        return CameraConstants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions() {
        ActivityCompat.requestPermissions(
            activity,
            CameraConstants.REQUIRED_PERMISSIONS,
            CameraConstants.PERMISSIONS_REQUEST_CODE
        )
    }

    // This method would be called from FlutterActivity's onRequestPermissionsResult
    fun handlePermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (requestCode == CameraConstants.PERMISSIONS_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }
}

//endregion

//region Overlay Management

interface ITextOverlayFactory {
    fun createTextOverlay(props: OverlayProperties): TextOverlay
}

class TextOverlayFactory : ITextOverlayFactory {
    override fun createTextOverlay(props: OverlayProperties): TextOverlay {
        return object : TextOverlay() {
            override fun getText(presentationTimeUs: Long): SpannableString {
                return SpannableString(props.text).apply {
                    setSpan(BackgroundColorSpan(props.bgColor), 0, props.text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(ForegroundColorSpan(props.fgColor), 0, props.text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(AbsoluteSizeSpan(props.textSizePx, false), 0, props.text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            override fun getOverlaySettings(presentationTimeUs: Long): StaticOverlaySettings {
                return StaticOverlaySettings.Builder()
                    .setOverlayFrameAnchor(props.x, props.y)
                    .setBackgroundFrameAnchor(props.x, props.y)
                    .build()
            }
        }
    }
}

class OverlayConfigParser(private val textOverlayFactory: ITextOverlayFactory) {

    private val overlayItemPrefixes = listOf(
        CameraConstants.PREFIX_HORSE_NAME,
        CameraConstants.PREFIX_HORSE_NUMBER,
        CameraConstants.PREFIX_RIDER,
        CameraConstants.PREFIX_PENALTY,
        CameraConstants.PREFIX_TIME_TAKEN,
        CameraConstants.PREFIX_RANK,
        CameraConstants.PREFIX_GAP_TO_BEST,
        CameraConstants.PREFIX_LIVE_MSG
    )

    fun parseOverlayData(data: Map<String?, Any?>): List<TextOverlay> {
        val overlays = mutableListOf<TextOverlay>()

        overlayItemPrefixes.forEach { prefix ->
            val text = data[prefix]?.toString()
            if (text != null) {
                val textSizePx = (data["${prefix}${CameraConstants.SUFFIX_TEXT_SIZE_PX}"] as? Int) ?: 22
                val bgColor = parseColor(data["${prefix}${CameraConstants.SUFFIX_BG_COLOR}"] as? String) ?: 0x88000000.toInt()
                val fgColor = parseColor(data["${prefix}${CameraConstants.SUFFIX_FG_COLOR}"] as? String) ?: 0xFFFFFFFF.toInt()
                val x = (data["${prefix}${CameraConstants.SUFFIX_X}"] as? Double)?.toFloat() ?: 0f
                val y = (data["${prefix}${CameraConstants.SUFFIX_Y}"] as? Double)?.toFloat() ?: 0f

                val props = OverlayProperties(text, textSizePx, bgColor, fgColor, x, y)
                overlays.add(textOverlayFactory.createTextOverlay(props))
            }
        }
        return overlays
    }

    private fun parseColor(colorStr: String?): Int? {
        return try {
            if (colorStr != null && colorStr.startsWith("#")) {
                Color.parseColor(colorStr)
            } else null
        } catch (e: Exception) {
            Log.e("OverlayConfigParser", "Failed to parse color string: '$colorStr'", e)
            null
        }
    }
}

class OverlayManager(
    private val overlayConfigParser: OverlayConfigParser
) {
    fun updateOverlay(creationParams: Map<String?, Any?>?, media3Effect: Media3Effect) {
        val data = creationParams ?: return
        val overlays = overlayConfigParser.parseOverlayData(data)
        val overlayEffect = OverlayEffect(ImmutableList.copyOf(overlays.map { it as TextureOverlay }))
        media3Effect.setEffects(listOf(overlayEffect))
    }
}

//endregion

//region Camera and Recording

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner, // Use LifecycleOwner directly
    private val previewView: PreviewView,
    private val backgroundExecutor: ExecutorService // Inject background executor
) {
    var media3Effect: Media3Effect? = null
        private set
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null

    fun initialize(onEffectReady: (Media3Effect) -> Unit, onCameraError: (String) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = try {
                cameraProviderFuture.get()
            } catch (e: Exception) {
                Log.e("CameraManager", "Error getting camera provider", e)
                onCameraError("Failed to get camera provider: ${e.message}")
                return@addListener
            }

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(videoCapture!!) // Safe because videoCapture is just initialized

            media3Effect = Media3Effect(
                context,
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
                ContextCompat.getMainExecutor(context)
            ) { error ->
                // The `error` here is an instance of `Effect.Listener.Error`, which has a `message` property.
                onCameraError("Media3 error: ${error.message}")
            }
            media3Effect?.let { useCaseGroup.addEffect(it) }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    useCaseGroup.build()
                )
                media3Effect?.let(onEffectReady)
            } catch (e: Exception) {
                Log.e("CameraManager", "Camera bind failed", e)
                onCameraError("Failed to bind camera: ${e.message}")
            }
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
            // Removed: media3Effect?.release()
            // CameraX lifecycle management should handle the release of Media3Effect
            // when use cases are unbound. Directly calling release() here caused an unresolved reference.
        } catch (e: Exception) {
            Log.e("CameraManager", "Failed to unbind camera: ${e.message}", e)
        } finally {
            // It's still good practice to nullify references to aid garbage collection
            media3Effect = null
            camera = null
            videoCapture = null
        }
    }
}

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
        if (videoCapture == null) {
            Toast.makeText(context, "Camera not ready for recording", Toast.LENGTH_SHORT).show()
            return false
        }
        if (recording != null) {
            Toast.makeText(context, "Recording already in progress", Toast.LENGTH_SHORT).show()
            return false
        }

        val fileName = "equestre_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val outputDir = File(downloadsDir, "Equestre")
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                Toast.makeText(context, "Failed to create recording directory", Toast.LENGTH_LONG).show()
                Log.e("VideoRecorder", "Failed to create recording directory: ${outputDir.absolutePath}")
                return false
            }
        }
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
                        val filePath = if (event.outputResults.outputUri != null)
                            event.outputResults.outputUri!!.path
                        else
                            file.absolutePath

                        if (event.hasError()) {
                            val errorMessage = getErrorMessageForCode(event.error) // Fixed here
                            methodChannel.invokeMethod(CameraConstants.METHOD_ON_RECORDING_STOPPED, mapOf(
                                "reason" to "error",
                                "success" to false,
                                "filePath" to filePath,
                                "errorMessage" to errorMessage
                            ))
                            Toast.makeText(context, "Recording error: ${errorMessage}", Toast.LENGTH_LONG).show()
                            Log.e("VideoRecorder", "Recording error code: ${event.error}, msg: $errorMessage")
                        } else {
                            methodChannel.invokeMethod(CameraConstants.METHOD_ON_RECORDING_STOPPED, mapOf(
                                "reason" to "finalized",
                                "filePath" to filePath,
                                "success" to true
                            ))
                            Toast.makeText(context, "Video saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                            Log.i("VideoRecorder", "Video saved: ${file.absolutePath}")
                        }
                        recording = null // Clear recording state regardless of success
                    }
                    // Add other events if needed, e.g., Pause, Resume
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
        recording = null // Immediately nullify to prevent re-stopping
        return true
    }

    /**
     * Maps VideoRecordEvent error codes to human-readable messages.
     * @param errorCode The error code from VideoRecordEvent.Finalize.error
     * @return A descriptive error message.
     */
    private fun getErrorMessageForCode(errorCode: Int): String {
        return when (errorCode) {
            VideoRecordEvent.Finalize.ERROR_NONE -> "No error"
            VideoRecordEvent.Finalize.ERROR_UNKNOWN -> "Unknown recording error"
            VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE -> "Recording source inactive (e.g., camera disconnected)"
            VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED -> "File size limit reached"
            VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED -> "Recording duration limit reached"
            VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA -> "No valid data received during recording"
            VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED -> "Video encoding failed"
            // VideoRecordEvent.Finalize.ERROR_IO -> "Input/Output error during recording"
            else -> "Unrecognized error code: $errorCode"
        }
    }
}

//endregion

//region Main Platform View

@UnstableApi
class MyCameraView(
    private val context: Context,
    messenger: BinaryMessenger,
    private var creationParams: Map<String?, Any?>?,
    private val activity: FlutterActivity // FlutterActivity as LifecycleOwner
) : PlatformView, DefaultLifecycleObserver {

    private val rootView = FrameLayout(context)
    private val previewView = PreviewView(context)

    // Dependencies
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val permissionHandler = PermissionHandler(activity)
    private val cameraManager = CameraManager(context, activity, previewView, backgroundExecutor)
    private val overlayManager = OverlayManager(OverlayConfigParser(TextOverlayFactory()))
    private val videoRecorder = VideoRecorder(context)

    private val methodChannel = MethodChannel(messenger, CameraConstants.CHANNEL_NAME)

    private val recordButton = ImageButton(context).apply {
        setBackgroundResource(R.drawable.circular_button)
        setImageResource(android.R.drawable.ic_media_play)
        val size = (64 * context.resources.displayMetrics.density).toInt() // 64dp size
        layoutParams = FrameLayout.LayoutParams(size, size, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = (32 * context.resources.displayMetrics.density).toInt()
        }
        scaleType = ImageView.ScaleType.CENTER_INSIDE // Fixed here
    }

    private var isRecording = false

    init {
        activity.lifecycle.addObserver(this)
        rootView.addView(previewView)
        rootView.addView(recordButton)

        setupRecordButton()
        setupChannel()
        setupVolumeButtonZoom()

        // Permissions check before camera setup
        if (permissionHandler.allPermissionsGranted()) {
            setupCamera()
        } else {
            permissionHandler.requestPermissions()
            // The camera setup will be re-attempted in onResume after permissions are handled
            Toast.makeText(context, "Permissions not granted. Please grant them in settings.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecordButton() {
        recordButton.setOnClickListener {
            toggleRecording()
        }
    }

    private fun toggleRecording() {
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

    private fun setupCamera() {
        cameraManager.initialize(
            onEffectReady = { mediaEffect ->
                videoRecorder.setMedia3Effect(mediaEffect)
                videoRecorder.bindVideoCapture(cameraManager.getVideoCapture())
                creationParams?.let { overlayManager.updateOverlay(it, mediaEffect) }
            },
            onCameraError = { errorMessage ->
                Toast.makeText(context, "Camera initialization error: $errorMessage", Toast.LENGTH_LONG).show()
                Log.e("MyCameraView", "Camera initialization error: $errorMessage")
                // Potentially send error back to Flutter
            }
        )
    }

    private fun setupChannel() {
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                CameraConstants.METHOD_UPDATE_OVERLAY -> {
                    val updates = call.arguments as? Map<String, Any?>
                    if (updates != null) {
                        // Merge updates into creationParams
                        creationParams = (creationParams ?: emptyMap()).toMutableMap().apply {
                            putAll(updates)
                        }
                        cameraManager.media3Effect?.let {
                            overlayManager.updateOverlay(creationParams, it)
                        }
                        result.success(true)
                    } else {
                        result.error("INVALID_ARGUMENTS", "Missing update data for overlay", null)
                    }
                }
                CameraConstants.METHOD_START_RECORDING -> {
                    val started = videoRecorder.startRecording(methodChannel)
                    if (started) {
                        recordButton.setImageResource(android.R.drawable.ic_media_pause)
                        isRecording = true
                    }
                    result.success(started)
                }
                CameraConstants.METHOD_STOP_RECORDING -> {
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
        super.onPause(owner)
        videoRecorder.stopRecording() // Ensure recording stops on pause
        cameraManager.releaseResources()
        Log.d("MyCameraView", "onPause: Camera resources released.")
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        // Re-initialize camera only if permissions are granted.
        // This handles returning from settings after granting permissions.
        if (permissionHandler.allPermissionsGranted()) {
            setupCamera()
        } else {
            Toast.makeText(context, "Permissions are required for camera functionality.", Toast.LENGTH_LONG).show()
        }
        Log.d("MyCameraView", "onResume: Camera setup attempted.")
    }

    override fun dispose() {
        activity.lifecycle.removeObserver(this) // Remove observer to prevent leaks
        videoRecorder.stopRecording()
        cameraManager.releaseResources()
        backgroundExecutor.shutdownNow() // Shut down the executor
        methodChannel.setMethodCallHandler(null) // Unregister method channel
        Log.d("MyCameraView", "dispose: All resources released.")
    }

    override fun getView(): View = rootView
}