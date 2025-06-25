package ch.zeitmessungen.texture_camera

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
//
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
//
import android.content.Context
import androidx.camera.core.CameraEffect
import androidx.camera.media3.effect.Media3Effect

/** TextureCameraPlugin */
/**
 * This plugin require android Lolipop version (21) as a min version in your Android's gradle build
 */
class TextureCameraPlugin:  FlutterPlugin, MethodCallHandler, ActivityAware {

  val TAG: String = TextureCameraPlugin::class.java.getName()

  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  // 
  private lateinit var context: Context

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "texture_camera")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    if (call.method == "getPlatformVersion") {
      result.success("Android ${Build.VERSION.RELEASE}")
    } else if(call.method == "updateOverlay"){
       val args = call.arguments as? Map<String, String>
                if (args != null) {
                    CameraOverlayHandler.updateOverlayData(args)
                    result.success(null)
                } else {
                    result.error("INVALID_ARGS", "Expected map of horse data", null)
                }
    } else {
      result.notImplemented()
    }
  }

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
  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
  
    channel.setMethodCallHandler(null)
  }
}
