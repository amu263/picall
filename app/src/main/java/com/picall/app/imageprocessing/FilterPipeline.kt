package com.picall.app.imageprocessing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/**
 * 滤镜管线 — 串联所有图像处理滤镜，依次应用到输入 Bitmap
 * 支持实时预览（低分辨率）和最终导出（全分辨率）两种模式
 */
class FilterPipeline {

    private val filters = mutableListOf<FilterStage>()

    /**
     * 添加一个滤镜阶段
     * @param filter 滤镜实现
     * @param intensity 该滤镜的强度 (0.0 .. 1.0)
     */
    fun addStage(filter: ImageFilter, intensity: Float = 1f): FilterPipeline {
        filters.add(FilterStage(filter, intensity.coerceIn(0f, 1f)))
        return this
    }

    /**
     * 清空所有滤镜
     */
    fun clear() {
        filters.clear()
    }

    /**
     * 获取滤镜数量
     */
    fun size(): Int = filters.size

    /**
     * 处理图片 — 依次应用所有滤镜
     * @param input 输入 Bitmap
     * @param globalIntensity 全局强度 (0.0 .. 1.0)
     * @return 处理后的 Bitmap
     */
    fun process(input: Bitmap, globalIntensity: Float = 1f): Bitmap {
        // 使用可变副本
        var current = input.copy(Bitmap.Config.ARGB_8888, true)

        val globalFactor = globalIntensity.coerceIn(0f, 1f)

        for (stage in filters) {
            val effectiveIntensity = stage.intensity * globalFactor
            if (effectiveIntensity > 0.001f) {
                val next = stage.filter.apply(current, effectiveIntensity)
                if (next !== current) {
                    current.recycle()  // 释放中间结果
                    current = next
                }
            }
        }

        return current
    }

    /**
     * 快速预览处理 — 使用低分辨率以提高性能
     */
    fun processPreview(input: Bitmap, globalIntensity: Float = 1f, maxDimension: Int = 1024): Bitmap {
        val scale = calculateScale(input.width, input.height, maxDimension)
        if (scale < 1f) {
            val scaled = Bitmap.createScaledBitmap(
                input,
                (input.width * scale).toInt(),
                (input.height * scale).toInt(),
                true
            )
            return processDirect(scaled, globalIntensity)
        }
        return process(input, globalIntensity)
    }

    /**
     * 处理已缩放的bitmap (不额外复制)
     */
    private fun processDirect(input: Bitmap, globalIntensity: Float): Bitmap {
        var current = input
        val globalFactor = globalIntensity.coerceIn(0f, 1f)

        for (stage in filters) {
            val effectiveIntensity = stage.intensity * globalFactor
            if (effectiveIntensity > 0.001f) {
                val next = stage.filter.apply(current, effectiveIntensity)
                if (next !== current) {
                    current.recycle()
                    current = next
                }
            }
        }

        return current
    }

    private fun calculateScale(width: Int, height: Int, maxDim: Int): Float {
        val maxSide = maxOf(width, height)
        return if (maxSide > maxDim) maxDim.toFloat() / maxSide else 1f
    }

    data class FilterStage(
        val filter: ImageFilter,
        val intensity: Float
    )
}

/**
 * 图像滤镜接口 — 所有滤镜的基础
 */
interface ImageFilter {
    /**
     * 应用滤镜
     * @param input 输入 Bitmap (ARGB_8888)
     * @param intensity 滤镜强度 (0.0 .. 1.0)
     * @return 处理后的 Bitmap
     */
    fun apply(input: Bitmap, intensity: Float): Bitmap
}
