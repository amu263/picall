package com.picall.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * LUT 预设 — 导入的 3D LUT 文件及其参数
 */
data class LutPreset(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "未命名LUT",
    @SerializedName("lut_data") val lutData: String = "",      // Base64 编码的 LUT 数据
    @SerializedName("lut_size") val lutSize: Int = 33,         // LUT 立方体大小 (e.g. 33)
    @SerializedName("source_file") val sourceFile: String = "", // 原始文件名
    @SerializedName("intensity") val intensity: Float = 1f,     // 0.0 .. 1.0
    @SerializedName("created_at") val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        val DEFAULT = LutPreset()
    }
}
