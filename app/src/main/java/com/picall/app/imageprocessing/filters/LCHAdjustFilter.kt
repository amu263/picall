package com.picall.app.imageprocessing.filters

import android.graphics.Bitmap
import com.picall.app.imageprocessing.ImageFilter

/**
 * LCH (Lightness, Chroma, Hue) 颜色空间调节滤镜
 *
 * 在 CIE L*C*h 颜色空间中进行调整，比 RGB/HSL 更符合人眼感知
 *
 * 转换路径: RGB → Linear RGB → XYZ (D65) → CIELAB → LCH → adjust → ... → RGB
 */
class LCHAdjustFilter(
    private val lightness: Float = 0f,    // -1.0 .. 1.0 : 明度偏移
    private val chroma: Float = 0f,       // -1.0 .. 1.0 : 彩度（饱和度）偏移
    private val hue: Float = 0f           // -1.0 .. 1.0 : 色相旋转 (-180° .. 180°)
) : ImageFilter {

    // D65 白点 XYZ 值
    companion object {
        private const val REF_X = 95.047f
        private const val REF_Y = 100.000f
        private const val REF_Z = 108.883f

        // sRGB → XYZ 矩阵 (D65)
        private val SRGB_TO_XYZ = floatArrayOf(
            0.4124564f, 0.3575761f, 0.1804375f,
            0.2126729f, 0.7151522f, 0.0721750f,
            0.0193339f, 0.1191920f, 0.9503041f
        )

        // XYZ → sRGB 矩阵
        private val XYZ_TO_SRGB = floatArrayOf(
            3.2404542f, -1.5371385f, -0.4985314f,
            -0.9692660f, 1.8760108f, 0.0415560f,
            0.0556434f, -0.2040259f, 1.0572252f
        )
    }

    override fun apply(input: Bitmap, intensity: Float): Bitmap {
        val lShift = lightness * intensity
        val cShift = chroma * intensity
        val hShift = hue * intensity

        val out = input.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val a = pixels[i] shr 24 and 0xFF
            val r = (pixels[i] shr 16 and 0xFF) / 255f
            val g = (pixels[i] shr 8 and 0xFF) / 255f
            val b = (pixels[i] and 0xFF) / 255f

            // RGB → Linear RGB (gamma 反矫正)
            val lr = srgbToLinear(r)
            val lg = srgbToLinear(g)
            val lb = srgbToLinear(b)

            // Linear RGB → XYZ
            val x = SRGB_TO_XYZ[0] * lr + SRGB_TO_XYZ[1] * lg + SRGB_TO_XYZ[2] * lb
            val y = SRGB_TO_XYZ[3] * lr + SRGB_TO_XYZ[4] * lg + SRGB_TO_XYZ[5] * lb
            val z = SRGB_TO_XYZ[6] * lr + SRGB_TO_XYZ[7] * lg + SRGB_TO_XYZ[8] * lb

            // XYZ → CIELAB
            val fx = labF(x / REF_X)
            val fy = labF(y / REF_Y)
            val fz = labF(z / REF_Z)
            val lVal = 116f * fy - 16f
            val aVal = 500f * (fx - fy)
            val bVal = 200f * (fy - fz)

            // CIELAB → LCH
            var cVal = Math.sqrt((aVal * aVal + bVal * bVal).toDouble()).toFloat()
            var hVal = Math.toDegrees(Math.atan2(bVal.toDouble(), aVal.toDouble())).toFloat()
            if (hVal < 0) hVal += 360f

            // 应用调整
            // L* 明度: 直接调整
            val lNew = (lVal + lShift * 50f).coerceIn(0f, 100f)

            // C* 彩度: 缩放
            val cNew = (cVal * (1f + cShift)).coerceIn(0f, 200f)

            // H* 色相: 旋转
            val hNew = (hVal + hShift * 180f) % 360f
            val hRad = Math.toRadians(hNew.toDouble())

            // LCH → CIELAB
            val aNew = cNew * Math.cos(hRad).toFloat()
            val bNew = cNew * Math.sin(hRad).toFloat()

            // CIELAB → XYZ
            val fyNew = (lNew + 16f) / 116f
            val fxNew = aNew / 500f + fyNew
            val fzNew = fyNew - bNew / 200f

            val xNew = labFInv(fxNew) * REF_X
            val yNew = labFInv(fyNew) * REF_Y
            val zNew = labFInv(fzNew) * REF_Z

            // XYZ → Linear RGB
            val lrNew = XYZ_TO_SRGB[0] * xNew + XYZ_TO_SRGB[1] * yNew + XYZ_TO_SRGB[2] * zNew
            val lgNew = XYZ_TO_SRGB[3] * xNew + XYZ_TO_SRGB[4] * yNew + XYZ_TO_SRGB[5] * zNew
            val lbNew = XYZ_TO_SRGB[6] * xNew + XYZ_TO_SRGB[7] * yNew + XYZ_TO_SRGB[8] * zNew

            // Linear RGB → sRGB (gamma 矫正)
            val rNew = linearToSrgb(lrNew)
            val gNew = linearToSrgb(lgNew)
            val bNew = linearToSrgb(lbNew)

            pixels[i] = clampPixel(rNew * 255f, gNew * 255f, bNew * 255f, a)
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun srgbToLinear(c: Float): Float {
        return if (c <= 0.04045f) c / 12.92f
        else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    }

    private fun linearToSrgb(c: Float): Float {
        return if (c <= 0.0031308f) c * 12.92f
        else (1.055f * Math.pow(c.toDouble(), 1.0 / 2.4).toFloat() - 0.055f)
    }

    private fun labF(t: Float): Float {
        return if (t > 0.008856f) Math.pow(t.toDouble(), 1.0 / 3.0).toFloat()
        else (7.787f * t + 16f / 116f)
    }

    private fun labFInv(t: Float): Float {
        return if (t > 0.206897f) t * t * t
        else (t - 16f / 116f) / 7.787f
    }
}
