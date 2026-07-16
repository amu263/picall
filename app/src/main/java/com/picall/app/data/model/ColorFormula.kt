package com.picall.app.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 色彩配方 — 包含所有图像调色参数
 * 每个调节项的值范围通常为 -1.0 到 1.0 (或 0.0 到 1.0)
 */
data class ColorFormula(
    // ── 调色盘 ──
    @SerializedName("palette_type") val paletteType: String = "none",  // none, warm, cool, vintage, moody, custom
    @SerializedName("palette_intensity") val paletteIntensity: Float = 0.5f,

    // ── 亮度 ──
    @SerializedName("exposure") val exposure: Float = 0f,           // -1.0 .. 1.0
    @SerializedName("contrast") val contrast: Float = 0f,           // -1.0 .. 1.0
    @SerializedName("highlights") val highlights: Float = 0f,       // -1.0 .. 1.0
    @SerializedName("shadows") val shadows: Float = 0f,             // -1.0 .. 1.0
    @SerializedName("brightness_intensity") val brightnessIntensity: Float = 1f,  // 0.0 .. 1.0 总强度

    // ── WRGB 曲线 ──
    @SerializedName("curve_points_w") val curvePointsW: List<CurvePoint> = defaultCurvePoints(),
    @SerializedName("curve_points_r") val curvePointsR: List<CurvePoint> = defaultCurvePoints(),
    @SerializedName("curve_points_g") val curvePointsG: List<CurvePoint> = defaultCurvePoints(),
    @SerializedName("curve_points_b") val curvePointsB: List<CurvePoint> = defaultCurvePoints(),
    @SerializedName("curves_intensity") val curvesIntensity: Float = 1f,  // 0.0 .. 1.0

    // ── 颜色 ──
    @SerializedName("saturation") val saturation: Float = 0f,       // -1.0 .. 1.0
    @SerializedName("color_temperature") val colorTemperature: Float = 0f,  // -1.0(cool) .. 1.0(warm)
    @SerializedName("tint") val tint: Float = 0f,                   // -1.0(green) .. 1.0(magenta)
    @SerializedName("color_effect") val colorEffect: Float = 0f,    // 色彩效果强度
    @SerializedName("color_intensity") val colorIntensity: Float = 1f,  // 0.0 .. 1.0

    // ── RGB 原色调整 ──
    @SerializedName("red_hue") val redHue: Float = 0f,             // -1.0 .. 1.0
    @SerializedName("red_saturation") val redSaturation: Float = 0f, // -1.0 .. 1.0
    @SerializedName("green_hue") val greenHue: Float = 0f,
    @SerializedName("green_saturation") val greenSaturation: Float = 0f,
    @SerializedName("blue_hue") val blueHue: Float = 0f,
    @SerializedName("blue_saturation") val blueSaturation: Float = 0f,
    @SerializedName("rgb_intensity") val rgbIntensity: Float = 1f,  // 0.0 .. 1.0

    // ── LCH 颜色空间调节 ──
    @SerializedName("lch_lightness") val lchLightness: Float = 0f,   // -1.0 .. 1.0
    @SerializedName("lch_chroma") val lchChroma: Float = 0f,         // -1.0 .. 1.0
    @SerializedName("lch_hue") val lchHue: Float = 0f,               // -1.0 .. 1.0 (对应 -180° .. 180°)
    @SerializedName("lch_intensity") val lchIntensity: Float = 1f,  // 0.0 .. 1.0

    // ── 效果 ──
    @SerializedName("fade") val fade: Float = 0f,                   // 0.0 .. 1.0 褪色
    @SerializedName("silver_retention") val silverRetention: Float = 0f,  // 0.0 .. 1.0 留银冲洗
    @SerializedName("effects_intensity") val effectsIntensity: Float = 1f,  // 0.0 .. 1.0

    // ── 全局强度 ──
    @SerializedName("global_intensity") val globalIntensity: Float = 1f  // 0.0 .. 1.0 整体配方强度
) {
    companion object {
        fun defaultCurvePoints() = listOf(
            CurvePoint(0f, 0f),
            CurvePoint(0.25f, 0.25f),
            CurvePoint(0.5f, 0.5f),
            CurvePoint(0.75f, 0.75f),
            CurvePoint(1f, 1f)
        )

        val DEFAULT = ColorFormula()

        fun fromJson(json: String): ColorFormula =
            Gson().fromJson(json, ColorFormula::class.java)

        fun toJson(formula: ColorFormula): String =
            Gson().toJson(formula)
    }
}

/**
 * 曲线控制点 — 归一化坐标 (0.0 .. 1.0)
 */
data class CurvePoint(
    @SerializedName("x") val x: Float,
    @SerializedName("y") val y: Float
)
