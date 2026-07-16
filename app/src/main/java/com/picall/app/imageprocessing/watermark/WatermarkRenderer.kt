package com.picall.app.imageprocessing.watermark

import android.graphics.*
import android.graphics.Paint.Align
import androidx.exifinterface.media.ExifInterface
import com.picall.app.data.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 水印渲染器 — 在图片上绘制自定义水印文字、EXIF 数据和相框
 */
object WatermarkRenderer {

    private const val DP_SCALE = 3f  // 简化的 dp→px 转换因子

    fun render(input: Bitmap, preset: WatermarkPreset): Bitmap {
        if (preset.globalIntensity < 0.001f && preset.frameStyle == FrameStyle.NONE) {
            return input
        }

        val padding = (preset.framePadding * DP_SCALE).toInt()
        val frameWidth = (preset.frameWidth * DP_SCALE).toInt()

        // 计算输出画布大小 (包含相框)
        val totalWidth = input.width + padding * 2 + frameWidth * 2
        val totalHeight = input.height + padding * 2 + frameWidth * 2

        val output = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 相框背景
        drawFrameBackground(canvas, totalWidth, totalHeight, preset)

        // 绘制相框
        drawFrame(canvas, totalWidth, totalHeight, padding, frameWidth, preset)

        // 绘制图片
        val imageX = padding + frameWidth
        val imageY = padding + frameWidth
        canvas.drawBitmap(input, imageX.toFloat(), imageY.toFloat(), null)

        // 绘制水印文字
        if (preset.textContent.isNotEmpty() || preset.textLines.isNotEmpty()) {
            drawWatermarkText(canvas, totalWidth, totalHeight, preset, input)
        }

        // 绘制 EXIF 参数
        if (preset.showExif && preset.exifItems.isNotEmpty()) {
            drawExifInfo(canvas, totalWidth, totalHeight, preset, input)
        }

        // 全局不透明度
        if (preset.globalIntensity < 0.999f) {
            val alphaPaint = Paint().apply {
                alpha = (preset.globalIntensity * 255).toInt()
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            canvas.drawRect(0f, 0f, totalWidth.toFloat(), totalHeight.toFloat(), alphaPaint)
        }

        return output
    }

    private fun drawFrameBackground(canvas: Canvas, w: Int, h: Int, preset: WatermarkPreset) {
        when (preset.frameStyle) {
            FrameStyle.NONE -> {}
            FrameStyle.POLAROID -> {
                // 白色背景
                canvas.drawColor(Color.WHITE)
            }
            FrameStyle.CLASSIC_MATTE -> {
                val bgPaint = Paint().apply {
                    color = preset.frameBgColor.toInt()
                    alpha = (preset.frameBgOpacity * 255).toInt()
                }
                canvas.drawColor(bgPaint.color)
            }
            else -> {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            }
        }
    }

    private fun drawFrame(
        canvas: Canvas, w: Int, h: Int,
        padding: Int, frameWidth: Int, preset: WatermarkPreset
    ) {
        val framePaint = Paint().apply {
            color = preset.frameColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = frameWidth.toFloat()
            isAntiAlias = true
        }

        val innerRect = RectF(
            padding.toFloat(),
            padding.toFloat(),
            (w - padding).toFloat(),
            (h - padding).toFloat()
        )

        when (preset.frameStyle) {
            FrameStyle.NONE -> { /* 无相框 */ }

            FrameStyle.SIMPLE -> {
                // 简洁单线框
                canvas.drawRoundRect(
                    innerRect,
                    preset.frameRadius * DP_SCALE,
                    preset.frameRadius * DP_SCALE,
                    framePaint
                )
            }

            FrameStyle.DOUBLE -> {
                // 双线框
                framePaint.strokeWidth = frameWidth.toFloat()
                canvas.drawRoundRect(
                    innerRect,
                    preset.frameRadius * DP_SCALE,
                    preset.frameRadius * DP_SCALE,
                    framePaint
                )
                framePaint.strokeWidth = (frameWidth * 0.4f)
                val innerMargin = frameWidth * 2f
                canvas.drawRoundRect(
                    RectF(
                        innerRect.left + innerMargin,
                        innerRect.top + innerMargin,
                        innerRect.right - innerMargin,
                        innerRect.bottom - innerMargin
                    ),
                    preset.frameRadius * DP_SCALE * 0.5f,
                    preset.frameRadius * DP_SCALE * 0.5f,
                    framePaint
                )
            }

            FrameStyle.FILM_STRIP -> {
                // 胶片边框 — 上下较宽+齿孔装饰
                val topBottomWidth = frameWidth * 2f
                val sideWidth = frameWidth * 0.8f

                // 上下宽框
                val topRect = RectF(0f, 0f, w.toFloat(), topBottomWidth)
                val bottomRect = RectF(0f, h - topBottomWidth, w.toFloat(), h.toFloat())
                framePaint.style = Paint.Style.FILL
                framePaint.color = Color.rgb(30, 30, 30)
                canvas.drawRect(topRect, framePaint)
                canvas.drawRect(bottomRect, framePaint)

                // 齿孔装饰
                val holePaint = Paint().apply {
                    color = Color.rgb(60, 60, 60)
                    style = Paint.Style.FILL
                }
                val holeRadius = topBottomWidth * 0.2f
                val holeSpacing = holeRadius * 4f
                var x = holeSpacing
                while (x < w) {
                    canvas.drawCircle(x, topBottomWidth / 2f, holeRadius, holePaint)
                    canvas.drawCircle(x, h - topBottomWidth / 2f, holeRadius, holePaint)
                    x += holeSpacing
                }
            }

            FrameStyle.CLASSIC_MATTE -> {
                // 经典卡纸框 — 宽白边
                framePaint.style = Paint.Style.STROKE
                framePaint.strokeWidth = frameWidth.toFloat()
                canvas.drawRect(innerRect, framePaint)
            }

            FrameStyle.POLAROID -> {
                // 拍立得 — 下方宽白边
                val bottomExtra = (h - padding) / 5  // 下方多 20% 空间
                framePaint.style = Paint.Style.STROKE
                framePaint.strokeWidth = 1f
                framePaint.color = Color.rgb(220, 220, 220)
                canvas.drawRect(
                    RectF(innerRect.left, innerRect.top, innerRect.right, innerRect.bottom.toFloat()),
                    framePaint
                )
            }

            FrameStyle.VIGNETTE -> {
                // 暗角效果 — 在图片区域绘制径向渐变
                val vignettePaint = Paint().apply {
                    shader = RadialGradient(
                        w / 2f, h / 2f, maxOf(w, h) * 0.7f,
                        intArrayOf(Color.TRANSPARENT, Color.argb(180, 0, 0, 0)),
                        floatArrayOf(0.3f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(innerRect, vignettePaint)

                // 细边框
                framePaint.style = Paint.Style.STROKE
                framePaint.strokeWidth = frameWidth * 0.5f
                framePaint.color = Color.argb(80, 255, 255, 255)
                canvas.drawRect(innerRect, framePaint)
            }

            FrameStyle.SHADOW_BORDER -> {
                // 阴影边框
                val shadowPaint = Paint().apply {
                    setShadowLayer(
                        frameWidth * 2f,
                        frameWidth * 0.5f,
                        frameWidth * 0.5f,
                        Color.argb(120, 0, 0, 0)
                    )
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRect(innerRect, shadowPaint)

                framePaint.style = Paint.Style.STROKE
                framePaint.strokeWidth = frameWidth * 0.3f
                framePaint.color = Color.rgb(200, 200, 200)
                canvas.drawRect(innerRect, framePaint)
            }
        }
    }

    private fun drawWatermarkText(
        canvas: Canvas, w: Int, h: Int,
        preset: WatermarkPreset, image: Bitmap
    ) {
        val lines = if (preset.textLines.isNotEmpty()) {
            preset.textLines
        } else {
            preset.textContent.split("\n").map { WatermarkTextLine(content = it) }
        }

        if (lines.isEmpty()) return

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = preset.textColor.toInt()
            alpha = (preset.textOpacity * 255).toInt()
            textSize = preset.fontSize * DP_SCALE
            letterSpacing = preset.letterSpacing / preset.fontSize

            // 加载自定义字体
            if (preset.fontPath.isNotEmpty()) {
                try {
                    typeface = Typeface.createFromFile(File(preset.fontPath))
                } catch (_: Exception) {}
            }

            // 文字阴影
            if (preset.textShadow) {
                setShadowLayer(
                    preset.textShadowRadius * DP_SCALE,
                    1f, 1f,
                    preset.textShadowColor.toInt()
                )
            }
        }

        // 根据 position 计算起始 Y 坐标
        val lineHeight = textPaint.textSize * 1.5f
        val totalTextHeight = lineHeight * lines.size
        val margin = (16f * DP_SCALE).toInt()

        val startX = when (preset.position) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.CENTER_LEFT, WatermarkPosition.BOTTOM_LEFT ->
                margin.toFloat() + preset.offsetX * DP_SCALE
            WatermarkPosition.TOP_CENTER, WatermarkPosition.CENTER, WatermarkPosition.BOTTOM_CENTER ->
                w / 2f + preset.offsetX * DP_SCALE
            WatermarkPosition.TOP_RIGHT, WatermarkPosition.CENTER_RIGHT, WatermarkPosition.BOTTOM_RIGHT ->
                w - margin - preset.offsetX * DP_SCALE
            else -> w / 2f
        }

        val startY = when (preset.position) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_CENTER, WatermarkPosition.TOP_RIGHT ->
                margin.toFloat() + preset.offsetY * DP_SCALE + lineHeight
            WatermarkPosition.CENTER_LEFT, WatermarkPosition.CENTER, WatermarkPosition.CENTER_RIGHT ->
                h / 2f - totalTextHeight / 2f + lineHeight + preset.offsetY * DP_SCALE
            WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_CENTER, WatermarkPosition.BOTTOM_RIGHT ->
                h - margin - totalTextHeight + lineHeight - preset.offsetY * DP_SCALE
            else -> h / 2f
        }

        textPaint.textAlign = when (preset.position) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.CENTER_LEFT, WatermarkPosition.BOTTOM_LEFT -> Align.LEFT
            WatermarkPosition.TOP_CENTER, WatermarkPosition.CENTER, WatermarkPosition.BOTTOM_CENTER -> Align.CENTER
            WatermarkPosition.TOP_RIGHT, WatermarkPosition.CENTER_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> Align.RIGHT
            else -> Align.CENTER
        }

        val adjustedStartX = when (textPaint.textAlign) {
            Align.CENTER -> w / 2f + preset.offsetX * DP_SCALE
            Align.RIGHT -> w - margin.toFloat() - preset.offsetX * DP_SCALE
            else -> margin.toFloat() + preset.offsetX * DP_SCALE
        }

        // 绘制每一行
        lines.forEachIndexed { index, line ->
            val y = startY + index * lineHeight
            val text = if (line.isExif && line.exifTag.isNotEmpty()) {
                // 在实际应用中，这里读取 EXIF 数据
                "[${line.exifTag}]"
            } else {
                line.content
            }
            canvas.drawText(text, adjustedStartX, y, textPaint)
        }
    }

    private fun drawExifInfo(
        canvas: Canvas, w: Int, h: Int,
        preset: WatermarkPreset, image: Bitmap
    ) {
        // EXIF 文字通常绘制在底部
        val exifPaint = Paint().apply {
            isAntiAlias = true
            color = preset.textColor.toInt()
            alpha = (preset.textOpacity * 0.7f * 255).toInt()
            textSize = preset.exifFontSize * DP_SCALE
            textAlign = Align.CENTER
        }

        val exifText = preset.exifItems.joinToString(preset.exifSeparator) { item ->
            "${item.prefix}[${item.tag}]${item.suffix}"
        }

        val y = h - (16f * DP_SCALE) - exifPaint.textSize
        canvas.drawText(exifText, w / 2f, y, exifPaint)
    }

    /**
     * 从文件读取 EXIF 信息并格式化为显示文本
     */
    fun readExifTags(imagePath: String, items: List<ExifDisplayItem>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val exif = ExifInterface(imagePath)
            for (item in items) {
                val value = when (item.tag) {
                    "Make" -> exif.getAttribute(ExifInterface.TAG_MAKE)
                    "Model" -> exif.getAttribute(ExifInterface.TAG_MODEL)
                    "ISO" -> exif.getAttribute(ExifInterface.TAG_ISO_SPEED)
                    "Aperture" -> exif.getAttribute(ExifInterface.TAG_APERTURE_VALUE)
                    "ShutterSpeed" -> exif.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE)
                    "FocalLength" -> exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                    "DateTime" -> exif.getAttribute(ExifInterface.TAG_DATETIME)
                    "LensModel" -> exif.getAttribute(ExifInterface.TAG_LENS_MODEL)
                    "ExposureBias" -> exif.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE)
                    "Flash" -> exif.getAttribute(ExifInterface.TAG_FLASH)
                    "WhiteBalance" -> exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)
                    "GPSLatitude" -> exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                    "GPSLongitude" -> exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
                    else -> null
                }
                if (value != null) {
                    result[item.tag] = "${item.prefix}$value${item.suffix}"
                }
            }
        } catch (_: Exception) {}
        return result
    }
}
