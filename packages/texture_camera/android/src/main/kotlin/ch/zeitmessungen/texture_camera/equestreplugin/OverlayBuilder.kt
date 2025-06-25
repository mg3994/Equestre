import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import androidx.media3.common.OverlaySettings
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import com.google.common.collect.ImmutableList

object OverlayBuilder {

    fun buildOverlay(data: Map<String, String>): OverlayEffect {
        fun overlayText(
            text: String,
            bgColor: Int,
            x: Float,
            y: Float,
            size: Int
        ): TextOverlay = object : TextOverlay() {
            override fun getText(presentationTimeUs: Long): SpannableString {
                return SpannableString(text).apply {
                    setSpan(BackgroundColorSpan(bgColor), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(ForegroundColorSpan(Color.WHITE), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(AbsoluteSizeSpan(size, false), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
                return StaticOverlaySettings.Builder()
                    .setOverlayFrameAnchor(x, y)
                    .setAlphaScale(1f)
                    .build()
            }
        }

        val horseNumber = overlayText(data["horseNumber"] ?: "#--", Color.BLUE, -0.95f, 0.65f, 60)
        val horseName = overlayText(data["horseName"] ?: "--", Color.GRAY, -0.95f, 0.75f, 60)
        val rider = overlayText(data["rider"] ?: "Rider: --", Color.MAGENTA, -0.95f, 0.85f, 60)
        val gap = overlayText(data["gap"] ?: "Gap: --", Color.GREEN, 0.95f, 0.60f, 50)
        val penalties = overlayText(data["penalties"] ?: "Penalties: --", Color.RED, 0.95f, 0.70f, 50)
        val time = overlayText(data["time"] ?: "Time: --", Color.DKGRAY, 0.95f, 0.80f, 50)
        val rank = overlayText(data["rank"] ?: "Rank: --", Color.CYAN, 0.95f, 0.90f, 50)

        val overlays = listOf(
            horseNumber, horseName, rider, gap, penalties, time, rank
        ).map { it as TextureOverlay }

        return OverlayEffect(ImmutableList.copyOf(overlays))
    }
}
