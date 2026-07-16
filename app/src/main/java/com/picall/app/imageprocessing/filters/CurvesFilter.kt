package com.picall.app.imageprocessing.filters

import android.graphics.Bitmap
import com.picall.app.data.model.CurvePoint
import com.picall.app.imageprocessing.ImageFilter

/**
 * WRGB 曲线滤镜 — 使用 Catmull-Rom 样条插值生成平滑曲线查找表
 *
 * W (White): 亮度曲线，应用于所有通道
 * R / G / B: 各通道独立曲线
 */
class CurvesFilter(
    private val wPoints: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    private val rPoints: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    private val gPoints: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    private val bPoints: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
) : ImageFilter {

    override fun apply(input: Bitmap, intensity: Float): Bitmap {
        val wLut = buildLut(wPoints)
        val rLut = buildLut(rPoints)
        val gLut = buildLut(gPoints)
        val bLut = buildLut(bPoints)

        val out = input.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val a = pixels[i] shr 24 and 0xFF
            val r = pixels[i] shr 16 and 0xFF
            val g = pixels[i] shr 8 and 0xFF
            val b = pixels[i] and 0xFF

            // 对每个通道查表
            var rNew = rLut[r]
            var gNew = gLut[g]
            var bNew = bLut[b]

            // W 亮度曲线：在 RGB 基础上统一调节
            val luminance = (0.299f * rNew + 0.587f * gNew + 0.114f * bNew).toInt().coerceIn(0, 255)
            val wFactor = wLut[luminance] / (luminance + 1).toFloat()  // 比例因子

            rNew = (rNew * wFactor).coerceIn(0f, 255f)
            gNew = (gNew * wFactor).coerceIn(0f, 255f)
            bNew = (bNew * wFactor).coerceIn(0f, 255f)

            // 强度混合
            rNew = r + (rNew - r) * intensity
            gNew = g + (gNew - g) * intensity
            bNew = b + (bNew - b) * intensity

            pixels[i] = clampPixel(rNew, gNew, bNew, a)
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * 从控制点数组生成 256 级查找表
     * 使用 Catmull-Rom 样条插值
     */
    private fun buildLut(points: List<CurvePoint>): FloatArray {
        val sorted = points.sortedBy { it.x }
        if (sorted.isEmpty()) return FloatArray(256) { it.toFloat() }
        if (sorted.size == 1) {
            val v = sorted[0].y * 255f
            return FloatArray(256) { v }
        }

        val lut = FloatArray(256)
        var segIdx = 0

        for (i in 0 until 256) {
            val t = i / 255f

            // 找到 t 所在的段
            while (segIdx < sorted.size - 2 && sorted[segIdx + 1].x < t) {
                segIdx++
            }

            val p0 = sorted.getOrElse(segIdx - 1) { sorted[0] }
            val p1 = sorted[segIdx]
            val p2 = sorted.getOrElse(segIdx + 1) { sorted.last() }
            val p3 = sorted.getOrElse(segIdx + 2) { sorted.last() }

            // 段内局部参数
            val localT = if (p2.x != p1.x) (t - p1.x) / (p2.x - p1.x) else 0f
            val value = catmullRom(p0.y, p1.y, p2.y, p3.y, localT.coerceIn(0f, 1f))

            lut[i] = (value.coerceIn(0f, 1f) * 255f)
        }

        return lut
    }

    /**
     * Catmull-Rom 样条插值
     */
    private fun catmullRom(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5f * (
            (2f * p1) +
            (-p0 + p2) * t +
            (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
            (-p0 + 3f * p1 - 3f * p2 + p3) * t3
        )
    }
}
