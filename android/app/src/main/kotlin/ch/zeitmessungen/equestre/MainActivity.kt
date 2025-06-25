package ch.zeitmessungen.equestre

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val PERMISSIONS_REQUEST_CODE = 1001
    private lateinit var permissionResult: MethodChannel.Result

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory("camera_overlay_view", MyCameraViewFactory(this, flutterEngine.dartExecutor.binaryMessenger))

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "ch.zeitmessungen.equestre/permissions").apply {
            setMethodCallHandler { call, result ->
                if (call.method == "requestCameraAndAudioPermissions") {
                    permissionResult = result
                    checkAndRequestCameraAndAudioPermissions()
                } else {
                    result.notImplemented()
                }
            }
        }
    }

    private fun checkAndRequestCameraAndAudioPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionsToRequest = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.CAMERA)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
            } else {
                permissionResult.success(true)
            }
        } else {
            permissionResult.success(true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
                permissionResult.success(true)
            } else {
                Toast.makeText(this, "Permissions denied!", Toast.LENGTH_LONG).show()
                permissionResult.success(false)
            }
        }
    }
}
