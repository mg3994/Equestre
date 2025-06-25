package ch.zeitmessungen.equestre

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.plugin.platform.PlatformViewFactory
import android.content.Context
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.common.BinaryMessenger // Import BinaryMessenger

class MainActivity: FlutterActivity() {

    private var myCameraView: MyCameraView? = null

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine) // Call super.configureFlutterEngine

        // Correct way to register the PlatformViewFactory
        flutterEngine.platformViewsController.registry.registerViewFactory(
            "camera_overlay_view",
            MyCameraViewFactory(this, flutterEngine.dartExecutor.binaryMessenger) { view ->
                myCameraView = view // Store the reference
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        myCameraView?.handlePermissionsResult(requestCode, permissions, grantResults)
        Log.d("MainActivity", "Permissions result handled by MyCameraView: $requestCode")
    }

    override fun onDestroy() {
        super.onDestroy()
        myCameraView = null // Clear the reference to prevent memory leaks
        Log.d("MainActivity", "onDestroy: MyCameraView reference cleared.")
    }
}

// This factory should ideally be in its own file (e.g., MyCameraViewFactory.kt),
// but for the "single file" concept, we'll put it here.
class MyCameraViewFactory(
    private val activity: FlutterActivity,
    private val messenger: BinaryMessenger, // Pass BinaryMessenger directly
    private val onViewCreated: (MyCameraView) -> Unit
) : PlatformViewFactory(null) {

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        @Suppress("UNCHECKED_CAST")
        val creationParams = args as? Map<String?, Any?>

        // Pass the messenger received by the factory directly to MyCameraView
        val view = MyCameraView(context, messenger, creationParams, activity)
        onViewCreated(view)
        return view
    }
}