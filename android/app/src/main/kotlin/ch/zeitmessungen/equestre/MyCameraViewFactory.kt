package ch.zeitmessungen.equestre

import android.content.Context
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import io.flutter.embedding.android.FlutterActivity // Import FlutterActivity

class MyCameraViewFactory(private val activity: FlutterActivity, private val messenger: BinaryMessenger) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView { // FIX 16: Return type is PlatformView
        val creationParams = args as? Map<String?, Any?> // Initial overlay data
        return MyCameraView(context, messenger, creationParams, activity) // MyCameraView now implements PlatformView
    }
}