package com.picall.app.imageprocessing.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.picall.app.imageprocessing.ImageFilter

/**
 * 亮度滤镜 — 曝光 / 对比度 / 高光 / 阴影
 *
 * - 曝光 (exposure): 整体亮度的缩放，在近似线性空间进行
 * - 对比度 (contrast): 围绕中灰(128)拉伸或压缩
 * - 高光 (highlights): 只调整亮部区域
 * - 阴影 (shadows): 只调整暗部区域
 */
class BrightnessFilter(
    private val exposure: Float = 0f,      // -1.0 .. 1.0
    private val contrast: Float = 0f,      // -1.0 .. 1.0
    private val highlights: Float = 0f,    // -1.0 .. 1.0
    private val shadows: Float = 0f        // -1.0 .. 1.0
) : ImageFilter {

    override fun apply(input: Bitmap, intensity: Float): Bitmap {
        val out = input.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)

        val exp = exposure * intensity
        val cont = contrast * intensity
        val hl = highlights * intensity
        val shd = shadows * intensity

        for (i in pixels.indices) {
            var r = (pixels[i] shr 16 and 0xFF).toFloat()
            var g = (pixels[i] shr 8 and 0xFF).toFloat()
            var b = (pixels[i] and 0xFF).toFloat()
            val a = pixels[i] shr 24 and 0xFF

            // 1. 曝光 (乘法缩放，模拟线性空间)
            val exposureFactor = 2.0f.pow(exp * 2f) // exp=1 -> 4x, exp=-1 -> 0.25x
            r *= exposureFactor
            g *= exposureFactor
            b *= exposureFactor

            // 2. 对比度 (围绕 128 拉伸)
            // contrast=-1 -> 全灰, contrast=0 -> 不变, contrast=1 -> 最大对比
            val contrastFactor = (1f + cont) / (1f - cont + 0.0001f)
            r = (r - 128f) * contrastFactor + 128f
            g = (g - 128f) * contrastFactor + 128f
            b = (b - 128f) * contrastFactor + 128f

            // 3. 高光调整 (亮度高于 128 的区域)
            if (hl != 0f) {
                val luminance = 0.299f * r + 0.587f * g + 0.114f * b
                val highlightMask = smoothstep(100f, 200f, luminance)
                val factor = 1f + hl * highlightMask * 0.5f
                if (hl > 0) {
                    r *= factor; g *= factor; b *= factor
                } else {
                    r *= (1f + hl * highlightMask)
                    g *= (1f + hl * highlightMask)
                    b *= (1f + hl * highlightMask)
                }
            }

            // 4. 阴影调整 (亮度低于 128 的区域)
            if (shd != 0f) {
                val luminance = 0.299f * r + 0.587f * g + 0.114f * b
                val shadowMask = 1f - smoothstep(50f, 150f, luminance)
                val factor = 1f + shd * shadowMask * 0.5f
                r *= factor; g *= factor; b *= factor
            }

            pixels[i] = clampPixel(r, g, b, a)
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    // 平滑阶梯函数
    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}

/** ARGB 像素钳位 */
fun clampPixel(r: Float, g: Float, b: Float, a: Int): Int {
    val ri = r.toInt().coerceIn(0, 255)
    val gi = g.toInt().coerceIn(0, 255)
    val bi = b.toInt().coerceIn(0, 255)
    return (a shl 24) or (ri shl 16) or (gi shl 8) or bi
}

/** 指数扩展 */
fun Float.pow(exp: Float): Float = Math.pow(this.toDouble(), exp.toDouble()).toFloat()
