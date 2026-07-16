package com.picall.app.data.model

import android.graphics.Typeface
import com.google.gson.annotations.SerializedName

/**
 * 水印相框预设 — 自定义水印和相框的所有参数
 */
data class WatermarkPreset(
    // ── 文字设置 ──
    @SerializedName("text_content") val textContent: String = "",
    @SerializedName("text_lines") val textLines: List<WatermarkTextLine> = emptyList(),
    @SerializedName("font_size") val fontSize: Float = 36f,            // sp
    @SerializedName("font_path") val fontPath: String = "",             // 自定义字体路径
    @SerializedName("text_color") val textColor: Long = 0xFFFFFFFF,    // ARGB
    @SerializedName("text_opacity") val textOpacity: Float = 0.9f,     // 0.0 .. 1.0
    @SerializedName("text_shadow") val textShadow: Boolean = true,
    @SerializedName("text_shadow_color") val textShadowColor: Long = 0x80000000,
    @SerializedName("text_shadow_radius") val textShadowRadius: Float = 4f,
    @SerializedName("letter_spacing") val letterSpacing: Float = 1f,   // sp

    // ── 位置 ──
    @SerializedName("position") val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    @SerializedName("offset_x") val offsetX: Float = 16f,    // dp 偏移
    @SerializedName("offset_y") val offsetY: Float = 16f,
    @SerializedName("alignment_lines") val alignmentLines: Int = 1,     // 行数

    // ── EXIF 参数显示 ──
    @SerializedName("show_exif") val showExif: Boolean = false,
    @SerializedName("exif_items") val exifItems: List<ExifDisplayItem> = emptyList(),
    @SerializedName("exif_separator") val exifSeparator: String = " | ",
    @SerializedName("exif_font_size") val exifFontSize: Float = 14f,

    // ── 相框样式 ──
    @SerializedName("frame_style") val frameStyle: FrameStyle = FrameStyle.NONE,
    @SerializedName("frame_color") val frameColor: Long = 0xFFFFFFFF,
    @SerializedName("frame_width") val frameWidth: Float = 8f,          // dp
    @SerializedName("frame_padding") val framePadding: Float = 20f,     // dp 内边距
    @SerializedName("frame_radius") val frameRadius: Float = 0f,        // dp 圆角
    @SerializedName("frame_bg_color") val frameBgColor: Long = 0xFFFFFFFF,
    @SerializedName("frame_bg_opacity") val frameBgOpacity: Float = 0.15f,

    // ── 全局强度 ──
    @SerializedName("global_intensity") val globalIntensity: Float = 1f  // 水印整体不透明度
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
    NONE,           // 无相框
    SIMPLE,         // 简洁线条框
    DOUBLE,         // 双线框
    FILM_STRIP,     // 胶片边框
    CLASSIC_MATTE,  // 经典留白卡纸框
    POLAROID,       // 拍立得风格
    VIGNETTE,       // 暗角相框
    SHADOW_BORDER   // 阴影边框
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
