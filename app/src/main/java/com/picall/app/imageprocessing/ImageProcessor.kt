package com.picall.app.imageprocessing

import android.graphics.Bitmap
import android.net.Uri
import com.picall.app.data.model.ColorFormula
import com.picall.app.data.model.LutPreset
import com.picall.app.data.model.WatermarkPreset
import com.picall.app.imageprocessing.filters.*
import com.picall.app.imageprocessing.lut.LutApplier
import com.picall.app.imageprocessing.watermark.WatermarkRenderer

class ImageProcessor {

    fun processPreview(
        original: Bitmap,
        formula: ColorFormula,
        lut: LutPreset,
        watermark: WatermarkPreset,
        sourceUri: Uri? = null,
        maxDim: Int = 640
    ): Bitmap {
        val scale = maxDim.toFloat() / maxOf(original.width, original.height)
        val input = if (scale < 1f) {
            Bitmap.createScaledBitmap(original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(), true)
        } else {
            original.copy(Bitmap.Config.ARGB_8888, true)
        }

        var result = input

        if (formula.globalIntensity > 0.001f) {
            result = applyFormula(result, formula)
        }

        if (lut.lutData.isNotEmpty() && lut.intensity > 0.001f) {
            result = LutApplier.applyLut(result, lut.lutData, lut.lutSize, lut.intensity)
        }

        result = WatermarkRenderer.render(result, watermark, sourceUri?.path ?: sourceUri?.toString())

        return result
    }

    fun processExport(
        original: Bitmap,
        formula: ColorFormula,
        lut: LutPreset,
        watermark: WatermarkPreset,
        sourceUri: Uri? = null
    ): Bitmap {
        var result = original.copy(Bitmap.Config.ARGB_8888, true)

        if (formula.globalIntensity > 0.001f) {
            result = applyFormula(result, formula)
        }

        if (lut.lutData.isNotEmpty() && lut.intensity > 0.001f) {
            result = LutApplier.applyLut(result, lut.lutData, lut.lutSize, lut.intensity)
        }

        result = WatermarkRenderer.render(result, watermark, sourceUri?.path ?: sourceUri?.toString())

        return result
    }

    private fun applyFormula(input: Bitmap, formula: ColorFormula): Bitmap {
        val pipeline = FilterPipeline()

        if (needsBrightness(formula)) {
            pipeline.addStage(BrightnessFilter(
                formula.exposure, formula.contrast,
                formula.highlights, formula.shadows
            ), formula.brightnessIntensity)
        }

        if (needsCurves(formula)) {
            pipeline.addStage(CurvesFilter(
                formula.curvePointsW, formula.curvePointsR,
                formula.curvePointsG, formula.curvePointsB
            ), formula.curvesIntensity)
        }

        if (needsColor(formula)) {
            pipeline.addStage(ColorAdjustFilter(
                formula.saturation, formula.colorTemperature, formula.tint
            ), formula.colorIntensity)
        }

        if (needsRgb(formula)) {
            pipeline.addStage(RGBAdjustFilter(
                formula.redHue, formula.redSaturation,
                formula.greenHue, formula.greenSaturation,
                formula.blueHue, formula.blueSaturation
            ), formula.rgbIntensity)
        }

        if (needsLch(formula)) {
            pipeline.addStage(LCHAdjustFilter(
                formula.lchLightness, formula.lchChroma, formula.lchHue
            ), formula.lchIntensity)
        }

        if (formula.fade > 0.001f || formula.silverRetention > 0.001f) {
            pipeline.addStage(EffectsFilter(
                formula.fade, formula.silverRetention
            ), formula.effectsIntensity)
        }

        return pipeline.process(input, formula.globalIntensity)
    }

    private fun needsBrightness(f: ColorFormula): Boolean =
        abs(f.exposure) > 0.001f || abs(f.contrast) > 0.001f ||
        abs(f.highlights) > 0.001f || abs(f.shadows) > 0.001f

    private fun needsCurves(f: ColorFormula): Boolean =
        f.curvePointsW.any { abs(it.x - it.y) > 0.001f } ||
        f.curvePointsR.any { abs(it.x - it.y) > 0.001f } ||
        f.curvePointsG.any { abs(it.x - it.y) > 0.001f } ||
        f.curvePointsB.any { abs(it.x - it.y) > 0.001f }

    private fun needsColor(f: ColorFormula): Boolean =
        abs(f.saturation) > 0.001f || abs(f.colorTemperature) > 0.001f || abs(f.tint) > 0.001f

    private fun needsRgb(f: ColorFormula): Boolean =
        abs(f.redHue) > 0.001f || abs(f.redSaturation) > 0.001f ||
        abs(f.greenHue) > 0.001f || abs(f.greenSaturation) > 0.001f ||
        abs(f.blueHue) > 0.001f || abs(f.blueSaturation) > 0.001f

    private fun needsLch(f: ColorFormula): Boolean =
        abs(f.lchLightness) > 0.001f || abs(f.lchChroma) > 0.001f || abs(f.lchHue) > 0.001f

    private fun abs(v: Float) = kotlin.math.abs(v)
}
