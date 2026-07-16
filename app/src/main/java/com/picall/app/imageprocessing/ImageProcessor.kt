package com.picall.app.imageprocessing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.picall.app.imageprocessing.filters.*
import com.picall.app.imageprocessing.lut.LutApplier
import com.picall.app.imageprocessing.watermark.WatermarkRenderer
import com.picall.app.data.model.ColorFormula
import com.picall.app.data.model.LutPreset
import com.picall.app.data.model.WatermarkPreset

/**
 * 图像处理器 — 主入口，根据配置构建滤镜管线并处理图片
 */
class ImageProcessor {

    private val pipeline = FilterPipeline()

    /**
     * 应用色彩配方 - 全分辨率
     */
    fun applyColorFormula(input: Bitmap, formula: ColorFormula): Bitmap {
        pipeline.clear()
        buildStages(formula)
        return pipeline.process(input, formula.globalIntensity)
    }

    /**
     * 快速预览 — 使用低分辨率
     */
    fun applyColorFormulaPreview(
        input: Bitmap,
        formula: ColorFormula,
        maxDimension: Int = 1024
    ): Bitmap {
        pipeline.clear()
        buildStages(formula)
        return pipeline.processPreview(input, formula.globalIntensity, maxDimension)
    }

    private fun buildStages(formula: ColorFormula) {
        if (hasAdjustment(formula.exposure) || hasAdjustment(formula.contrast) ||
            hasAdjustment(formula.highlights) || hasAdjustment(formula.shadows)
        ) {
            pipeline.addStage(
                BrightnessFilter(
                    exposure = formula.exposure,
                    contrast = formula.contrast,
                    highlights = formula.highlights,
                    shadows = formula.shadows
                ),
                formula.brightnessIntensity
            )
        }

        val hasCurves = formula.curvePointsW.any { it.x != it.y } ||
                formula.curvePointsR.any { it.x != it.y } ||
                formula.curvePointsG.any { it.x != it.y } ||
                formula.curvePointsB.any { it.x != it.y }
        if (hasCurves) {
            pipeline.addStage(
                CurvesFilter(
                    wPoints = formula.curvePointsW,
                    rPoints = formula.curvePointsR,
                    gPoints = formula.curvePointsG,
                    bPoints = formula.curvePointsB
                ),
                formula.curvesIntensity
            )
        }

        if (hasAdjustment(formula.saturation) || hasAdjustment(formula.colorTemperature) ||
            hasAdjustment(formula.tint)
        ) {
            pipeline.addStage(
                ColorAdjustFilter(
                    saturation = formula.saturation,
                    colorTemperature = formula.colorTemperature,
                    tint = formula.tint
                ),
                formula.colorIntensity
            )
        }

        if (hasAdjustment(formula.redHue) || hasAdjustment(formula.redSaturation) ||
            hasAdjustment(formula.greenHue) || hasAdjustment(formula.greenSaturation) ||
            hasAdjustment(formula.blueHue) || hasAdjustment(formula.blueSaturation)
        ) {
            pipeline.addStage(
                RGBAdjustFilter(
                    redHue = formula.redHue,
                    redSaturation = formula.redSaturation,
                    greenHue = formula.greenHue,
                    greenSaturation = formula.greenSaturation,
                    blueHue = formula.blueHue,
                    blueSaturation = formula.blueSaturation
                ),
                formula.rgbIntensity
            )
        }

        if (hasAdjustment(formula.lchLightness) || hasAdjustment(formula.lchChroma) ||
            hasAdjustment(formula.lchHue)
        ) {
            pipeline.addStage(
                LCHAdjustFilter(
                    lightness = formula.lchLightness,
                    chroma = formula.lchChroma,
                    hue = formula.lchHue
                ),
                formula.lchIntensity
            )
        }

        if (formula.fade > 0.001f || formula.silverRetention > 0.001f) {
            pipeline.addStage(
                EffectsFilter(
                    fade = formula.fade,
                    silverRetention = formula.silverRetention
                ),
                formula.effectsIntensity
            )
        }
    }

    /**
     * 应用 LUT
     */
    fun applyLut(input: Bitmap, lut: LutPreset): Bitmap {
        return if (lut.lutData.isNotEmpty()) {
            LutApplier.applyLut(input, lut.lutData, lut.lutSize, lut.intensity)
        } else {
            input
        }
    }

    /**
     * 应用水印相框
     */
    fun applyWatermark(input: Bitmap, watermark: WatermarkPreset): Bitmap {
        return WatermarkRenderer.render(input, watermark)
    }

    companion object {
        fun hasAdjustment(value: Float): Boolean = kotlin.math.abs(value) > 0.001f
    }
}
