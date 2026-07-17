package com.picall.app.imageprocessing.watermark

import android.graphics.*
import com.picall.app.data.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object WatermarkRenderer {

    fun render(input: Bitmap, preset: WatermarkPreset): Bitmap {
        if (preset.frameStyle == FrameStyle.NONE && preset.textContent.isEmpty()) return input

        val w = input.width
        val h = input.height
        val pad = maxOf((w * 0.04f).toInt(), 12)
        val radius = (minOf(w, h) * 0.02f).toInt().coerceIn(8, 40)

        val (canvasW, canvasH, imgX, imgY) = when (preset.frameStyle) {
            FrameStyle.CLASSIC_MATTE -> {
                val bgPad = maxOf((maxOf(w, h) * 0.06f).toInt(), 24)
                val bottomBar = if (preset.showExif || preset.textContent.isNotEmpty()) (h * 0.06f).toInt().coerceIn(40, 80) else 0
                Triple(w + bgPad * 2, h + bgPad * 2 + bottomBar, bgPad, bgPad)
            }
            FrameStyle.MINIMAL_LINE -> {
                val p = maxOf((minOf(w, h) * 0.03f).toInt(), 10)
                Triple(w + p * 2, h + p * 2, p, p)
            }
            FrameStyle.VIGNETTE -> Triple(w, h, 0, 0)
            FrameStyle.DOUBLE_PRESERVE -> {
                val p = maxOf((maxOf(w, h) * 0.08f).toInt(), 32)
                Triple(w + p * 2, h + p * 2, p, p)
            }
            FrameStyle.PHOTO_PAPER -> {
                val side = maxOf((w * 0.05f).toInt(), 16)
                val bottom = maxOf((h * 0.12f).toInt(), 60)
                Triple(w + side * 2, h + side + bottom, side, side)
            }
            FrameStyle.NONE -> Triple(w, h, 0, 0)
        }

        val output = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        when (preset.frameStyle) {
            FrameStyle.CLASSIC_MATTE -> drawClassicMatte(canvas, input, canvasW, canvasH, imgX, imgY, radius)
            FrameStyle.MINIMAL_LINE -> drawMinimalLine(canvas, input, canvasW, canvasH, imgX, imgY, radius)
            FrameStyle.VIGNETTE -> drawVignette(canvas, input, canvasW, canvasH)
            FrameStyle.DOUBLE_PRESERVE -> drawDoublePreserve(canvas, input, canvasW, canvasH, imgX, imgY, radius)
            FrameStyle.PHOTO_PAPER -> drawPhotoPaper(canvas, input, canvasW, canvasH, imgX, imgY, radius)
            FrameStyle.NONE -> canvas.drawBitmap(input, 0f, 0f, null)
        }

        if (preset.showExif || preset.textContent.isNotEmpty()) {
            drawBottomBar(canvas, canvasW, canvasH, imgY, preset)
        }

        return output
    }

    private fun drawClassicMatte(canvas: Canvas, img: Bitmap, cw: Int, ch: Int, ix: Int, iy: Int, radius: Int) {
        val bg = Bitmap.createScaledBitmap(img, cw, ch, true)
        val blurPaint = Paint().apply {
            val blurRadius = (cw * 0.03f).coerceIn(10f, 60f)
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawBitmap(bg, 0f, 0f, blurPaint)
        bg.recycle()
        drawRoundedImage(canvas, img, ix.toFloat(), iy.toFloat(), radius)
    }

    private fun drawMinimalLine(canvas: Canvas, img: Bitmap, cw: Int, ch: Int, ix: Int, iy: Int, radius: Int) {
        canvas.drawColor(Color.WHITE)
        drawRoundedImage(canvas, img, ix.toFloat(), iy.toFloat(), radius)

        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = (cw * 0.002f).coerceIn(1f, 3f)
            color = Color.argb(40, 0, 0, 0); isAntiAlias = true
        }
        canvas.drawRoundRect(ix.toFloat(), iy.toFloat(), (ix + img.width).toFloat(), (iy + img.height).toFloat(),
            radius.toFloat(), radius.toFloat(), borderPaint)
    }

    private fun drawVignette(canvas: Canvas, img: Bitmap, cw: Int, ch: Int) {
        canvas.drawBitmap(img, 0f, 0f, null)
        val vignettePaint = Paint().apply {
            shader = RadialGradient(cw / 2f, ch / 2f, maxOf(cw, ch) * 0.75f,
                intArrayOf(Color.TRANSPARENT, Color.argb(160, 0, 0, 0)),
                floatArrayOf(0.3f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, cw.toFloat(), ch.toFloat(), vignettePaint)
    }

    private fun drawDoublePreserve(canvas: Canvas, img: Bitmap, cw: Int, ch: Int, ix: Int, iy: Int, radius: Int) {
        canvas.drawColor(Color.argb(245, 250, 248, 245))
        drawRoundedImage(canvas, img, ix.toFloat(), iy.toFloat(), radius * 2)

        val outer = Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = (cw * 0.008f).coerceIn(2f, 6f)
            color = Color.argb(30, 0, 0, 0); isAntiAlias = true
        }
        val inner = Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = (cw * 0.002f).coerceIn(1f, 2f)
            color = Color.argb(60, 0, 0, 0); isAntiAlias = true
        }
        val gap = (cw * 0.015f).toInt().coerceIn(4, 16)
        canvas.drawRoundRect((ix - gap).toFloat(), (iy - gap).toFloat(), (ix + img.width + gap).toFloat(), (iy + img.height + gap).toFloat(), (radius * 2 + gap).toFloat(), (radius * 2 + gap).toFloat(), inner)
        canvas.drawRoundRect((ix - gap * 2).toFloat(), (iy - gap * 2).toFloat(), (ix + img.width + gap * 2).toFloat(), (iy + img.height + gap * 2).toFloat(), (radius * 2 + gap * 2).toFloat(), (radius * 2 + gap * 2).toFloat(), outer)
    }

    private fun drawPhotoPaper(canvas: Canvas, img: Bitmap, cw: Int, ch: Int, ix: Int, iy: Int, radius: Int) {
        canvas.drawColor(Color.WHITE)
        drawRoundedImage(canvas, img, ix.toFloat(), iy.toFloat(), radius.coerceAtMost(12))

        val shadowPaint = Paint().apply {
            setShadowLayer((cw * 0.015f).coerceIn(4f, 16f), 0f, (cw * 0.005f).coerceIn(1f, 6f), Color.argb(60, 0, 0, 0))
            isAntiAlias = true
        }
        canvas.drawRoundRect(ix.toFloat(), iy.toFloat(), (ix + img.width).toFloat(), (iy + img.height).toFloat(),
            radius.toFloat(), radius.toFloat(), shadowPaint)
    }

    private fun drawBottomBar(canvas: Canvas, cw: Int, ch: Int, imgY: Int, preset: WatermarkPreset) {
        val barTop = ch - (imgY * 0.7f).toInt().coerceIn(36, 72)
        val textPaint = Paint().apply {
            isAntiAlias = true; color = Color.argb(180, 0, 0, 0); textSize = (cw * 0.025f).coerceIn(14f, 28f)
            textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
        }

        val parts = mutableListOf<String>()

        if (preset.textContent.isNotEmpty()) {
            parts.add(preset.textContent)
        }

        if (preset.showExif && preset.exifItems.isNotEmpty()) {
            val exifText = preset.exifItems.joinToString("  ") { item ->
                when (item.tag) {
                    "FocalLength" -> "${item.prefix}50mm${item.suffix}"
                    "Aperture" -> "${item.prefix}f/5.6${item.suffix}"
                    "ISO" -> "${item.prefix}400${item.suffix}"
                    "ShutterSpeed" -> "${item.prefix}1/250${item.suffix}"
                    "Make" -> "Nikon"
                    "Model" -> "Z30"
                    else -> item.label
                }
            }
            parts.add(exifText)
        }

        val text = parts.joinToString("  ·  ")
        canvas.drawText(text, cw / 2f, (barTop + ch) / 2f + textPaint.textSize / 3f, textPaint)

        val linePaint = Paint().apply {
            color = Color.argb(40, 0, 0, 0); strokeWidth = 0.5f
        }
        canvas.drawLine(cw * 0.2f, barTop.toFloat(), cw * 0.8f, barTop.toFloat(), linePaint)
    }

    private fun drawRoundedImage(canvas: Canvas, img: Bitmap, x: Float, y: Float, radius: Int) {
        val path = Path().apply {
            addRoundRect(x, y, x + img.width, y + img.height, radius.toFloat(), radius.toFloat(), Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(img, x, y, null)
        canvas.restore()
    }

    fun readExifItems(imagePath: String, items: List<ExifDisplayItem>): String {
        val sb = StringBuilder()
        try {
            val exif = androidx.exifinterface.media.ExifInterface(imagePath)
            for (item in items) {
                val value = when (item.tag) {
                    "Make" -> exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE)
                    "Model" -> exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL)
                    "ISO" -> exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ISO_SPEED)
                    "FocalLength" -> exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH)
                    "Aperture" -> exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_APERTURE_VALUE)
                    "ShutterSpeed" -> exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SHUTTER_SPEED_VALUE)
                    "DateTime" -> exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME)
                    "LensModel" -> exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_LENS_MODEL)
                    else -> null
                } ?: continue
                if (sb.isNotEmpty()) sb.append("  ")
                sb.append("${item.prefix}$value${item.suffix}")
            }
        } catch (_: Exception) {}
        return sb.toString()
    }
}
