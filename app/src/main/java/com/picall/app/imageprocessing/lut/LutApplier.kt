package com.picall.app.imageprocessing.lut

import android.graphics.Bitmap

/**
 * LUT 应用器 — 对图片应用 3D LUT (Look-Up Table)
 *
 * 使用三线性插值从 3D LUT 中采样目标颜色
 * LUT 是一个 size³ 的 RGB 查找表
 */
object LutApplier {

    /**
     * 应用 LUT 到图片
     * @param input 输入图片
     * @param lutBase64 Base64 编码的 LUT 数据
     * @param lutSize LUT 立方体大小 (e.g., 33)
     * @param intensity LUT 强度 (0.0 .. 1.0)
     */
    fun applyLut(input: Bitmap, lutBase64: String, lutSize: Int, intensity: Float): Bitmap {
        if (intensity < 0.001f) return input

        val lutData = LutParser.decodeFromBase64(lutBase64)
        val values = lutData.values
        val size = lutData.size

        // 验证数据完整性
        val expectedSize = size * size * size * 3
        if (values.size < expectedSize) return input

        val out = input.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)

        val sizeF = size.toFloat()
        val maxIdx = size - 1

        for (i in pixels.indices) {
            val a = pixels[i] shr 24 and 0xFF
            val r = (pixels[i] shr 16 and 0xFF) / 255f
            val g = (pixels[i] shr 8 and 0xFF) / 255f
            val b = (pixels[i] and 0xFF) / 255f

            // 在 LUT 空间中的归一化坐标
            val rf = r * maxIdx
            val gf = g * maxIdx
            val bf = b * maxIdx

            // 三线性插值采样
            val sampledRgb = trilinearSample(values, size, rf, gf, bf)

            // 混合原始颜色和 LUT 颜色
            val rNew = r + (sampledRgb[0] - r) * intensity
            val gNew = g + (sampledRgb[1] - g) * intensity
            val bNew = b + (sampledRgb[2] - b) * intensity

            pixels[i] = (a shl 24) or
                    (rNew.toInt().coerceIn(0, 255) shl 16) or
                    (gNew.toInt().coerceIn(0, 255) shl 8) or
                    bNew.toInt().coerceIn(0, 255)
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * 三线性插值采样 3D LUT
     * @param rf, gf, bf: 在 LUT 索引空间中的浮点坐标 (0 .. size-1)
     * @return floatArrayOf(R, G, B) 归一化颜色 (0..1)
     */
    private fun trilinearSample(values: FloatArray, size: Int, rf: Float, gf: Float, bf: Float): FloatArray {
        val sizeMinus1 = size - 1

        // 包围盒的整数索引
        val r0 = rf.toInt().coerceIn(0, sizeMinus1)
        val g0 = gf.toInt().coerceIn(0, sizeMinus1)
        val b0 = bf.toInt().coerceIn(0, sizeMinus1)

        val r1 = (r0 + 1).coerceAtMost(sizeMinus1)
        val g1 = (g0 + 1).coerceAtMost(sizeMinus1)
        val b1 = (b0 + 1).coerceAtMost(sizeMinus1)

        // 插值权重
        val dr = rf - r0
        val dg = gf - g0
        val db = bf - b0

        // 8 个角的颜色值
        val c000 = getLutValue(values, size, r0, g0, b0)
        val c100 = getLutValue(values, size, r1, g0, b0)
        val c010 = getLutValue(values, size, r0, g1, b0)
        val c110 = getLutValue(values, size, r1, g1, b0)
        val c001 = getLutValue(values, size, r0, g0, b1)
        val c101 = getLutValue(values, size, r1, g0, b1)
        val c011 = getLutValue(values, size, r0, g1, b1)
        val c111 = getLutValue(values, size, r1, g1, b1)

        // 三线性插值
        return floatArrayOf(
            trilinear(c000[0], c100[0], c010[0], c110[0], c001[0], c101[0], c011[0], c111[0], dr, dg, db),
            trilinear(c000[1], c100[1], c010[1], c110[1], c001[1], c101[1], c011[1], c111[1], dr, dg, db),
            trilinear(c000[2], c100[2], c010[2], c110[2], c001[2], c101[2], c011[2], c111[2], dr, dg, db)
        )
    }

    private fun getLutValue(values: FloatArray, size: Int, r: Int, g: Int, b: Int): FloatArray {
        // LUT 布局: 通常是 R 变化最快，然后是 G，然后是 B (Blue 最外层)
        // 索引 = (b * size * size + g * size + r) * 3
        val idx = (b * size * size + g * size + r) * 3
        return if (idx + 2 < values.size) {
            floatArrayOf(values[idx], values[idx + 1], values[idx + 2])
        } else {
            floatArrayOf(0f, 0f, 0f)
        }
    }

    private fun trilinear(
        c000: Float, c100: Float, c010: Float, c110: Float,
        c001: Float, c101: Float, c011: Float, c111: Float,
        dx: Float, dy: Float, dz: Float
    ): Float {
        val c00 = c000 * (1 - dx) + c100 * dx
        val c01 = c001 * (1 - dx) + c101 * dx
        val c10 = c010 * (1 - dx) + c110 * dx
        val c11 = c011 * (1 - dx) + c111 * dx

        val c0 = c00 * (1 - dy) + c10 * dy
        val c1 = c01 * (1 - dy) + c11 * dy

        return c0 * (1 - dz) + c1 * dz
    }
}
