package com.picall.app.imageprocessing.lut

import android.util.Base64

/**
 * LUT 文件解析器 — 支持 .cube 和 .3dl 格式
 *
 * .cube 格式示例:
 * ```
 * TITLE "My LUT"
 * LUT_3D_SIZE 33
 * DOMAIN_MIN 0.0 0.0 0.0
 * DOMAIN_MAX 1.0 1.0 1.0
 * 0.000000 0.000000 0.000000
 * 0.000000 0.000000 0.031250
 * ...
 * ```
 *
 * .3dl 格式类似，但行前缀包含索引号
 */
object LutParser {

    data class LutData(
        val title: String = "",
        val size: Int = 33,
        val domainMin: FloatArray = floatArrayOf(0f, 0f, 0f),
        val domainMax: FloatArray = floatArrayOf(1f, 1f, 1f),
        val values: FloatArray = floatArrayOf()  // R0,G0,B0,R1,G1,B1,... (size^3 * 3 entries)
    )

    /**
     * 从文本内容解析 LUT
     */
    fun parse(content: String): LutData {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }

        var title = ""
        var size = 33
        var domainMin = floatArrayOf(0f, 0f, 0f)
        var domainMax = floatArrayOf(1f, 1f, 1f)
        val valueList = mutableListOf<Float>()

        for (line in lines) {
            when {
                line.startsWith("TITLE", ignoreCase = true) -> {
                    title = line.substringAfter("TITLE").trim().trim('"', '\'')
                }
                line.startsWith("LUT_3D_SIZE", ignoreCase = true) -> {
                    size = line.substringAfter("LUT_3D_SIZE").trim().toIntOrNull() ?: 33
                }
                line.startsWith("DOMAIN_MIN", ignoreCase = true) -> {
                    domainMin = parseFloats(line.substringAfter("DOMAIN_MIN"))
                }
                line.startsWith("DOMAIN_MAX", ignoreCase = true) -> {
                    domainMax = parseFloats(line.substringAfter("DOMAIN_MAX"))
                }
                // 数字行：RGB 值
                line.firstOrNull()?.isDigit() == true || line.startsWith("-") || line.startsWith(".") -> {
                    valueList.addAll(parseFloats(line).toList())
                }
            }
        }

        return LutData(
            title = title,
            size = size,
            domainMin = domainMin,
            domainMax = domainMax,
            values = valueList.toFloatArray()
        )
    }

    /**
     * 将 LUT 数据编码为 Base64 字符串 (供存储)
     */
    fun encodeToBase64(lutData: LutData): String {
        // 格式: size:r0,g0,b0,r1,g1,b1,...
        val sb = StringBuilder()
        sb.append(lutData.size).append(":")
        sb.append(lutData.values.joinToString(",") { "%.6f".format(it) })
        return Base64.encodeToString(sb.toString().toByteArray(), Base64.NO_WRAP)
    }

    /**
     * 从 Base64 字符串解码 LUT 数据
     */
    fun decodeFromBase64(base64: String): LutData {
        val text = String(Base64.decode(base64, Base64.NO_WRAP))
        val parts = text.split(":")
        val size = parts[0].toIntOrNull() ?: 33
        val values = parts.getOrElse(1) { "" }
            .split(",")
            .mapNotNull { it.toFloatOrNull() }
            .toFloatArray()

        return LutData(size = size, values = values)
    }

    private fun parseFloats(s: String): FloatArray {
        return s.trim().split("\\s+".toRegex())
            .mapNotNull { it.toFloatOrNull() }
            .toFloatArray()
    }
}
