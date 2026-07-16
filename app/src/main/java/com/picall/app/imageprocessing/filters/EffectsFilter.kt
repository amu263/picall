package com.picall.app.imageprocessing.filters

import android.graphics.Bitmap
import com.picall.app.imageprocessing.ImageFilter

/**
 * 效果滤镜 — 褪色 (Fade) + 留银冲洗 (Silver Retention / Bleach Bypass)
 *
 * 褪色: 降低对比度 + 提亮暗部 + 全局轻微偏白
 * 留银冲洗: 模拟胶片留银工艺 — 降低饱和度 + 提高对比度 + 金属质感
 */
class EffectsFilter(
    private val fade: Float = 0f,              // 0.0 .. 1.0
    private val silverRetention: Float = 0f    // 0.0 .. 1.0
) : ImageFilter {

    override fun apply(input: Bitmap, intensity: Float): Bitmap {
        val out = input.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)

        val fadeStrength = fade * intensity
        val srStrength = silverRetention * intensity

        for (i in pixels.indices) {
            val a = pixels[i] shr 24 and 0xFF
            var r = (pixels[i] shr 16 and 0xFF).toFloat()
            var g = (pixels[i] shr 8 and 0xFF).toFloat()
            var b = (pixels[i] and 0xFF).toFloat()

            // ── 褪色效果 ──
            // 1. 降低对比度 (向 128 靠拢)
            // 2. 提亮暗部
            // 3. 混入白色
            if (fadeStrength > 0.001f) {
                // 降低对比度
                r = r + (128f - r) * fadeStrength * 0.5f
                g = g + (128f - g) * fadeStrength * 0.5f
                b = b + (128f - b) * fadeStrength * 0.5f

                // 提亮暗部
                val luminance = 0.299f * r + 0.587f * g + 0.114f * b
                val shadowMask = (1f - (luminance / 255f)).coerceIn(0f, 1f)
                r += shadowMask * fadeStrength * 60f
                g += shadowMask * fadeStrength * 60f
                b += shadowMask * fadeStrength * 60f

                // 混入白色
                r = mix(r, 255f, fadeStrength * 0.2f)
                g = mix(g, 255f, fadeStrength * 0.2f)
                b = mix(b, 255f, fadeStrength * 0.2f)
            }

            // ── 留银冲洗效果 (Bleach Bypass) ──
            // 1. 去饱和度
            // 2. 大幅提高对比度
            // 3. 暗部偏蓝，亮部偏黄
            if (srStrength > 0.001f) {
                // 去饱和（但保留一些）
                val luminance = 0.299f * r + 0.587f * g + 0.114f * b
                r = mix(r, luminance, srStrength * 0.85f)
                g = mix(g, luminance, srStrength * 0.85f)
                b = mix(b, luminance, srStrength * 0.85f)

                // 强对比度
                r = (r - 128f) * (1f + srStrength * 1.5f) + 128f
                g = (g - 128f) * (1f + srStrength * 1.5f) + 128f
                b = (b - 128f) * (1f + srStrength * 1.5f) + 128f

                // 暗部色调偏移：暗部 → 蓝/绿; 亮部 → 黄
                val toneMask = (luminance / 255f - 0.5f) * 2f  // -1..1
                r += toneMask * srStrength * 30f      // 亮部加红
                b -= toneMask * srStrength * 20f      // 暗部加蓝
            }

            pixels[i] = clampPixel(r, g, b, a)
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)
}
