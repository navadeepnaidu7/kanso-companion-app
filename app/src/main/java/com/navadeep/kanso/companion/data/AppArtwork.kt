package com.navadeep.kanso.companion.data

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap

/** Decode once off the main thread, before publishing the library. No work on the scroll path. */
class AppArtwork {
    private val cache = LruCache<String, ImageBitmap>(100)
    fun decode(id: String, encoded: String): ImageBitmap? {
        if (encoded.isEmpty()) return null
        val key = "$id:${encoded.hashCode()}"
        cache.get(key)?.let { return it }
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()?.also { cache.put(key, it) }
        }.getOrNull()
    }

    fun accent(image: ImageBitmap?): Int {
        if (image == null) return 0xFF78838F.toInt()
        val bitmap = image.asAndroidBitmap()
        val hsv = FloatArray(3)
        var best = 0f
        var color = 0xFF78838F.toInt()
        for (y in 0 until bitmap.height step 8) for (x in 0 until bitmap.width step 8) {
            val pixel = bitmap.getPixel(x, y)
            if (android.graphics.Color.alpha(pixel) < 200) continue
            android.graphics.Color.colorToHSV(pixel, hsv)
            val score = hsv[1] * hsv[2]
            if (score > best) {
                best = score
                color = android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], hsv[1].coerceAtMost(.65f), .85f))
            }
        }
        return color
    }
}
