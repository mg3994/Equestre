import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import androidx.camera.core.CameraEffect
import androidx.camera.media3.effect.Media3Effect
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/**
 * This plugin allows Flutter to send horse data to the Android side,
 * which will then dynamically update the video overlay in real-time.
 */
@UnstableApi
class EquestrePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel
    private lateinit var context: Context

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "equestre_plugin")
        channel.setMethodCallHandler(this)
    }

    /**
     * Called when Flutter invokes a method on this plugin.
     * Here we handle `updateOverlay` to pass data into the camera overlay.
     */
    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "updateOverlay" -> {
                val args = call.arguments as? Map<String, String>
                if (args != null) {
                    CameraOverlayHandler.updateOverlayData(args)
                    result.success(null)
                } else {
                    result.error("INVALID_ARGS", "Expected map of horse data", null)
                }
            }
            else -> result.notImplemented()
        }
    }

    /**
     * When this plugin is attached to an Activity, bind the Media3Effect to handle overlays.
     */
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        val media3Effect = Media3Effect(
            context,
            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
            ContextCompat.getMainExecutor(context)
        ) { err ->
            Log.e("EquestrePlugin", "Media3 error: ${err.message}")
        }
        CameraOverlayHandler.bindMedia3Effect(media3Effect)
        CameraOverlayHandler.attachToLifecycle(binding.activity)
    }

    override fun onDetachedFromActivity() {
        CameraOverlayHandler.cleanup()
    }

    override fun onDetachedFromActivityForConfigChanges() {}
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}

