package com.picall.app.imageprocessing.filters

import android.graphics.Bitmap
import com.picall.app.imageprocessing.ImageFilter

/**
 * 颜色调节滤镜 — 饱和度 / 色温 / 色调 / 色彩效果
 */
class ColorAdjustFilter(
    private val saturation: Float = 0f,          // -1.0 .. 1.0
    private val colorTemperature: Float = 0f,     // -1.0(cool/blue) .. 1.0(warm/yellow)
    private val tint: Float = 0f                 // -1.0(green) .. 1.0(magenta)
) : ImageFilter {

    override fun apply(input: Bitmap, intensity: Float): Bitmap {
        val out = input.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)

        val satVal = saturation * intensity
        val tempVal = colorTemperature * intensity
        val tintVal = tint * intensity

        for (i in pixels.indices) {
            val a = pixels[i] shr 24 and 0xFF
            var r = (pixels[i] shr 16 and 0xFF).toFloat()
            var g = (pixels[i] shr 8 and 0xFF).toFloat()
            var b = (pixels[i] and 0xFF).toFloat()

            // 1. 饱和度 (使用 Rec.709 亮度系数)
            if (satVal != 0f) {
                val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b
                val satFactor = 1f + satVal
                r = lum + (r - lum) * satFactor
                g = lum + (g - lum) * satFactor
                b = lum + (b - lum) * satFactor
            }

            // 2. 色温 (通过调整 R-B 平衡)
            // 暖色 = +红色 / -蓝色; 冷色 = -红色 / +蓝色
            if (tempVal != 0f) {
                val warmFactor = 1f + tempVal * 0.5f
                val coolFactor = 1f - tempVal * 0.5f
                r *= warmFactor
                b *= coolFactor
            }

            // 3. 色调 (green-magenta 轴)
            // 绿色 = +绿色 / -红色-蓝色; 洋红 = +红色+蓝色 / -绿色
            if (tintVal != 0f) {
                val magentaFactor = 1f + tintVal * 0.3f
                val greenFactor = 1f - tintVal * 0.3f
                r *= magentaFactor
                b *= magentaFactor
                g *= greenFactor
            }

            pixels[i] = clampPixel(r, g, b, a)
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }
}
