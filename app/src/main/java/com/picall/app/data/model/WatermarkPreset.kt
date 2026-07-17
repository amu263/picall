package com.picall.app.data.model

import android.graphics.Typeface
import com.google.gson.annotations.SerializedName

/**
 * 水印相框预设 — 自定义水印和相框的所有参数
 */
data class WatermarkPreset(
    // ── 相框样式 ──
    @SerializedName("frame_style") val frameStyle: FrameStyle = FrameStyle.CLASSIC_MATTE,

    // ── EXIF 参数显示 ──
    @SerializedName("show_exif") val showExif: Boolean = true,
    @SerializedName("show_device") val showDevice: Boolean = true,
    @SerializedName("show_params") val showParams: Boolean = true,
    @SerializedName("show_datetime") val showDateTime: Boolean = true,
    @SerializedName("show_location") val showLocation: Boolean = false,

    // ── 全局强度 ──
    @SerializedName("global_intensity") val globalIntensity: Float = 1f
) {
    companion object {
        val DEFAULT = WatermarkPreset()
    }
}

/**
 * 水印文字行 — 每行的内容和设置
 */
data class WatermarkTextLine(
    @SerializedName("content") val content: String = "",
    @SerializedName("font_size") val fontSize: Float = 36f,
    @SerializedName("is_exif") val isExif: Boolean = false,     // 是否为 EXIF 数据占位符
    @SerializedName("exif_tag") val exifTag: String = ""        // EXIF 标签名
)

/**
 * 水印位置枚举
 */
enum class WatermarkPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT,
    CUSTOM
}

/**
 * 相框样式枚举
 */
enum class FrameStyle {
    NONE,                // 无相框
    CLASSIC_MATTE,       // 经典留白 - 模糊背景+居中锐图+圆角阴影
    MINIMAL_LINE,        // 极简线框 - 单线细框+微妙外阴影
    VIGNETTE,            // 暗角光影 - 径向渐变暗角
    DOUBLE_PRESERVE,     // 双框珍藏 - 双同心边框+宽留白
    PHOTO_PAPER          // 相纸印记 - 拍立得/相纸风格
}

/**
 * EXIF 显示项 — 选择要显示的拍摄参数
 */
data class ExifDisplayItem(
    @SerializedName("tag") val tag: String = "",        // EXIF 标签: Make, Model, ISO, Aperture, ShutterSpeed, FocalLength, DateTime, LensModel
    @SerializedName("label") val label: String = "",     // 自定义显示标签
    @SerializedName("prefix") val prefix: String = "",   // 前缀 (如 "ISO ")
    @SerializedName("suffix") val suffix: String = ""    // 后缀 (如 "mm")
) {
    companion object {
        /** 可用 EXIF 标签列表 */
        val AVAILABLE_TAGS = listOf(
            "Make" to "相机制造商",
            "Model" to "相机型号",
            "ISO" to "ISO感光度",
            "Aperture" to "光圈值",
            "ShutterSpeed" to "快门速度",
            "FocalLength" to "焦距",
            "DateTime" to "拍摄时间",
            "LensModel" to "镜头型号",
            "ExposureBias" to "曝光补偿",
            "Flash" to "闪光灯",
            "WhiteBalance" to "白平衡",
            "GPSLatitude" to "GPS纬度",
            "GPSLongitude" to "GPS经度"
        )
    }
}
