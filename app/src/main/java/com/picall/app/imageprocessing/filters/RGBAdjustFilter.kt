package com.picall.app.imageprocessing.filters

import android.graphics.Bitmap
import com.picall.app.imageprocessing.ImageFilter

/**
 * RGB 原色色相和饱和度调节
 * 对图片中的红/绿/蓝区域分别调整色相偏移和饱和度
 *
 * 通过计算像素到目标颜色的"距离"来确定受影响程度
 * 然后在该颜色的色相方向上旋转 Hue，并调整饱和度
 */
class RGBAdjustFilter(
    private val redHue: Float = 0f,            // -1.0 .. 1.0
    private val redSaturation: Float = 0f,      // -1.0 .. 1.0
    private val greenHue: Float = 0f,
    private val greenSaturation: Float = 0f,
    private val blueHue: Float = 0f,
    private val blueSaturation: Float = 0f
) : ImageFilter {

    override fun apply(input: Bitmap, intensity: Float): Bitmap {
        val out = input.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val a = pixels[i] shr 24 and 0xFF
            var r = (pixels[i] shr 16 and 0xFF) / 255f
            var g = (pixels[i] shr 8 and 0xFF) / 255f
            var b = (pixels[i] and 0xFF) / 255f

            // 计算颜色对 RGB 三原色的归属度
            val maskR = rgbMask(r, g, b, 0) // red channel affinity
            val maskG = rgbMask(r, g, b, 1) // green channel affinity
            val maskB = rgbMask(r, g, b, 2) // blue channel affinity

            // 对每种原色区域分别处理
            if (maskR > 0.01f) {
                val hsl = rgbToHsl(r, g, b)
                val adjusted = adjustHsl(
                    hsl,
                    hueShift = redHue * intensity * maskR,
                    satShift = redSaturation * intensity * maskR
                )
                val rgb = hslToRgb(adjusted[0], adjusted[1], adjusted[2])
                r = mix(r, rgb[0], maskR)
                g = mix(g, rgb[1], maskR)
                b = mix(b, rgb[2], maskR)
            }

            if (maskG > 0.01f) {
                val hsl = rgbToHsl(r, g, b)
                val adjusted = adjustHsl(
                    hsl,
                    hueShift = greenHue * intensity * maskG,
                    satShift = greenSaturation * intensity * maskG
                )
                val rgb = hslToRgb(adjusted[0], adjusted[1], adjusted[2])
                r = mix(r, rgb[0], maskG)
                g = mix(g, rgb[1], maskG)
                b = mix(b, rgb[2], maskG)
            }

            if (maskB > 0.01f) {
                val hsl = rgbToHsl(r, g, b)
                val adjusted = adjustHsl(
                    hsl,
                    hueShift = blueHue * intensity * maskB,
                    satShift = blueSaturation * intensity * maskB
                )
                val rgb = hslToRgb(adjusted[0], adjusted[1], adjusted[2])
                r = mix(r, rgb[0], maskB)
                g = mix(g, rgb[1], maskB)
                b = mix(b, rgb[2], maskB)
            }

            pixels[i] = clampPixel(r * 255f, g * 255f, b * 255f, a)
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * 计算像素对指定原色的亲和度
     * targetChannel: 0=Red, 1=Green, 2=Blue
     */
    private fun rgbMask(r: Float, g: Float, b: Float, targetChannel: Int): Float {
        val values = floatArrayOf(r, g, b)
        val primary = values[targetChannel]
        val others = values.filterIndexed { idx, _ -> idx != targetChannel }

        // 该通道值显著高于其他通道时，亲和度高
        val diff = primary - others.max()
        val dominance = (diff / (primary + 0.001f)).coerceIn(0f, 1f)

        // 还要看饱和度：低饱和度的灰色不应被影响
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val saturation = if (maxC > 0.001f) (maxC - minC) / maxC else 0f

        return dominance * saturation
    }

    private fun rgbToHsl(r: Float, g: Float, b: Float): FloatArray {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f

        if (max - min < 0.0001f) return floatArrayOf(0f, 0f, l)

        val d = max - min
        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)

        val h = when (max) {
            r -> ((g - b) / d + (if (g < b) 6f else 0f)) / 6f
            g -> ((b - r) / d + 2f) / 6f
            else -> ((r - g) / d + 4f) / 6f
        }

        return floatArrayOf(h, s, l)
    }

    private fun adjustHsl(hsl: FloatArray, hueShift: Float, satShift: Float): FloatArray {
        val h = (hsl[0] + hueShift * 0.5f) % 1f
        val s = (hsl[1] + satShift).coerceIn(0f, 1f)
        return floatArrayOf(if (h < 0) h + 1f else h, s, hsl[2])
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): FloatArray {
        if (s < 0.001f) return floatArrayOf(l, l, l)

        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q

        return floatArrayOf(
            hueToRgb(p, q, h + 1f / 3f),
            hueToRgb(p, q, h),
            hueToRgb(p, q, h - 1f / 3f)
        )
    }

    private fun hueToRgb(p: Float, q: Float, t: Float): Float {
        var tt = t
        if (tt < 0) tt += 1f
        if (tt > 1) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }

    private fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)
}
