// package ch.zeitmessungen.equestre
// 
// import android.Manifest
// import android.content.pm.PackageManager
// import android.os.Build
// import android.os.Bundle
// import androidx.core.app.ActivityCompat
// import androidx.core.content.ContextCompat
// import io.flutter.embedding.android.FlutterActivity
// import io.flutter.embedding.engine.FlutterEngine
// 
// class MainActivity : FlutterActivity() {
// 
//     private val PERMISSION_REQUEST_CODE = 1001
// 
//     override fun onCreate(savedInstanceState: Bundle?) {
//         super.onCreate(savedInstanceState)
//         checkAndRequestPermissions()
//     }
// 
//     override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
//         super.configureFlutterEngine(flutterEngine)
//         flutterEngine
//             .platformViewsController
//             .registry
//             .registerViewFactory("camera_overlay_view", MyCameraViewFactory(this, flutterEngine.dartExecutor.binaryMessenger))
//     }
// 
//     private fun checkAndRequestPermissions() {
//         val requiredPermissions = arrayOf(
//             Manifest.permission.CAMERA,
//             Manifest.permission.RECORD_AUDIO,
//             Manifest.permission.WRITE_EXTERNAL_STORAGE,
//             Manifest.permission.READ_EXTERNAL_STORAGE // Added for previous devices
//         )
//         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//             val permissionsToRequest = requiredPermissions.filter {
//                 ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
//             }
//             if (permissionsToRequest.isNotEmpty()) {
//                 ActivityCompat.requestPermissions(
//                     this,
//                     permissionsToRequest.toTypedArray(),
//                     PERMISSION_REQUEST_CODE
//                 )
//             }
//         }
//         // For devices below Android M, permissions are granted at install time.
//     }
// }


package ch.zeitmessungen.equestre

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.plugin.platform.PlatformViewFactory
import android.content.Context
import io.flutter.plugin.platform.PlatformView

class MainActivity: FlutterActivity() {

    // A mutable reference to your MyCameraView instance.
    private var myCameraView: MyCameraView? = null

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine) // Call super.configureFlutterEngine

        // Register your PlatformViewFactory.
        flutterEngine.platformViews.registerViewFactory(
            "camera_overlay_view",
            MyCameraViewFactory(this) { view ->
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
        // Pass the permission result to the current MyCameraView instance if it exists
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
    private val onViewCreated: (MyCameraView) -> Unit
) : PlatformViewFactory(null) {

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        @Suppress("UNCHECKED_CAST")
        val creationParams = args as? Map<String?, Any?>

        val view = MyCameraView(context, (activity as FlutterActivity).flutterEngine!!.dartExecutor.binaryMessenger, creationParams, activity)
        onViewCreated(view)
        return view
    }
}