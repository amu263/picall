package com.picall.app.data.model

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 统一预设数据模型 — 可保存色彩配方 / LUT / 水印相框三种类型的预设
 */
@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String = "未命名预设",

    @ColumnInfo(name = "type")
    val type: PresetType = PresetType.COLOR_FORMULA,

    @ColumnInfo(name = "description")
    val description: String = "",

    // JSON 序列化的预设数据
    @ColumnInfo(name = "data_json")
    val dataJson: String = "",

    // 缩略图路径
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "category")
    val category: String = ""
)

enum class PresetType {
    @SerializedName("color_formula")
    COLOR_FORMULA,

    @SerializedName("lut")
    LUT,

    @SerializedName("watermark")
    WATERMARK
}

/**
 * 将 Preset 转换为对应的数据类型
 */
fun Preset.toColorFormula(): ColorFormula? = try {
    Gson().fromJson(dataJson, ColorFormula::class.java)
} catch (e: Exception) { null }

fun Preset.toLutPreset(): LutPreset? = try {
    Gson().fromJson(dataJson, LutPreset::class.java)
} catch (e: Exception) { null }

fun Preset.toWatermarkPreset(): WatermarkPreset? = try {
    Gson().fromJson(dataJson, WatermarkPreset::class.java)
} catch (e: Exception) { null }
