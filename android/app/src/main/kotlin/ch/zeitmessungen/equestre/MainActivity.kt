package ch.zeitmessungen.equestre

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val PERMISSIONS_REQUEST_CODE = 1001
    private lateinit var permissionResult: MethodChannel.Result // To send permission result back to Flutter

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Register the platform view factory
        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory("camera_overlay_view", MyCameraViewFactory(this, flutterEngine.dartExecutor.binaryMessenger))

        // Set up a MethodChannel to handle permission requests from Flutter
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "ch.zeitmessungen.equestre/permissions").apply {
            setMethodCallHandler { call, result ->
                if (call.method == "requestCameraAndAudioPermissions") {
                    permissionResult = result // Store result to respond later
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

            // Essential permissions for video recording
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.CAMERA)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }

            // Optional: Permissions for saving videos to specific directories
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) { // Android 12L (API 32) and below
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            // For Android 13 (API 33) and above, writing is handled by READ_MEDIA_VIDEO/IMAGES for specific media types,
            // or implicitly by MediaStore API without explicit WRITE_EXTERNAL_STORAGE for app-specific directories.
            // If you need access to all public media, you might consider READ_MEDIA_VIDEO.
            // For a simple app saving to MediaStore, WRITE_EXTERNAL_STORAGE isn't strictly needed on API 33+.

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
            } else {
                // All permissions already granted
                Toast.makeText(this, "All required permissions already granted.", Toast.LENGTH_SHORT).show()
                permissionResult.success(true) // Notify Flutter
            }
        } else {
            // Permissions are granted at install time on older Android versions (API < 23)
            Toast.makeText(this, "Permissions not required at runtime (Android < 6.0).", Toast.LENGTH_SHORT).show()
            permissionResult.success(true) // Notify Flutter
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }

            if (allGranted) {
                Toast.makeText(this, "All required permissions granted!", Toast.LENGTH_SHORT).show()
                permissionResult.success(true) // Notify Flutter that permissions are granted
            } else {
                Toast.makeText(this, "Some required permissions were denied. Cannot use camera features.", Toast.LENGTH_LONG).show()
                permissionResult.success(false) // Notify Flutter that permissions were denied
            }
        }
    }
}