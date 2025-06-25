import android.app.Activity
import androidx.camera.media3.effect.Media3Effect
import androidx.media3.effect.OverlayEffect
import java.util.concurrent.atomic.AtomicReference

object CameraOverlayHandler {

    private var media3Effect: Media3Effect? = null
    private var latestData: Map<String, String> = emptyMap()

    fun bindMedia3Effect(effect: Media3Effect) {
        media3Effect = effect
    }

    fun updateOverlayData(data: Map<String, String>) {
        latestData = data
        refreshOverlay()
    }

    fun attachToLifecycle(activity: Activity) {
        refreshOverlay()
    }

    private fun refreshOverlay() {
        val overlay: OverlayEffect = OverlayBuilder.buildOverlay(latestData)
        media3Effect?.setEffects(listOf(overlay))
    }

    fun cleanup() {
        media3Effect = null
    }
}
