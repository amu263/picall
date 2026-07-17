package com.picall.app.imageprocessing.watermark

import android.graphics.*
import android.location.Geocoder
import androidx.exifinterface.media.ExifInterface
import com.picall.app.data.model.*
import java.text.SimpleDateFormat
import java.util.*

object WatermarkRenderer {

    fun render(input: Bitmap, preset: WatermarkPreset, sourcePath: String? = null): Bitmap {
        if (preset.frameStyle == FrameStyle.NONE) return input

        val w = input.width; val h = input.height
        val radius = (minOf(w, h) * 0.02f).toInt().coerceIn(8, 40)

        val canvasW: Int; val canvasH: Int; val imgX: Int; val imgY: Int
        when (preset.frameStyle) {
            FrameStyle.CLASSIC_MATTE -> {
                val bgPad = maxOf((maxOf(w, h) * 0.06f).toInt(), 24)
                val bar = if (preset.showExif) (h * 0.07f).toInt().coerceIn(44, 72) else 0
                canvasW = w + bgPad * 2; canvasH = h + bgPad * 2 + bar; imgX = bgPad; imgY = bgPad
            }
            FrameStyle.MINIMAL_LINE -> {
                val p = maxOf((minOf(w, h) * 0.03f).toInt(), 10)
                val bar = if (preset.showExif) (h * 0.05f).toInt().coerceIn(32, 56) else 0
                canvasW = w + p * 2; canvasH = h + p * 2 + bar; imgX = p; imgY = p
            }
            FrameStyle.VIGNETTE -> {
                val bar = if (preset.showExif) (h * 0.06f).toInt().coerceIn(36, 60) else 0
                canvasW = w; canvasH = h + bar; imgX = 0; imgY = 0
            }
            FrameStyle.DOUBLE_PRESERVE -> {
                val p = maxOf((maxOf(w, h) * 0.08f).toInt(), 32)
                val bar = if (preset.showExif) (h * 0.06f).toInt().coerceIn(40, 68) else 0
                canvasW = w + p * 2; canvasH = h + p * 2 + bar; imgX = p; imgY = p
            }
            FrameStyle.PHOTO_PAPER -> {
                val side = maxOf((w * 0.05f).toInt(), 16)
                val bottom = maxOf((h * 0.14f).toInt(), 72)
                canvasW = w + side * 2; canvasH = h + side + bottom; imgX = side; imgY = side
            }
            FrameStyle.NONE -> { canvasW = w; canvasH = h; imgX = 0; imgY = 0 }
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

        if (preset.showExif) {
            val exifText = sourcePath?.let { readExif(it, preset) } ?: ""
            if (exifText.isNotEmpty()) {
                drawInfoBar(canvas, canvasW, canvasH, imgY, exifText)
            }
        }

        return output
    }

    private fun readExif(path: String, preset: WatermarkPreset): String {
        val parts = mutableListOf<String>()
        try {
            val exif = ExifInterface(path)

            if (preset.showDevice) {
                val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
                val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
                val device = listOfNotNull(make.takeIf { it.isNotEmpty() }, model.takeIf { it.isNotEmpty() })
                    .joinToString(" ")
                if (device.isNotEmpty()) parts.add(device)
            }

            if (preset.showParams) {
                val params = mutableListOf<String>()
                exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let {
                    val fl = it.toFloatOrNull()
                    if (fl != null) params.add("${fl.toInt()}mm")
                    else params.add(it)
                }
                exif.getAttribute(ExifInterface.TAG_APERTURE_VALUE)?.let {
                    val ap = it.toDoubleOrNull()
                    if (ap != null) {
                        val f = Math.pow(2.0, ap / 2.0)
                        params.add("f/${"%.1f".format(f)}")
                    }
                }
                exif.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE)?.let {
                    val ss = it.toDoubleOrNull()
                    if (ss != null) {
                        val sec = Math.pow(2.0, -ss)
                        params.add(if (sec >= 1) "${sec.toInt()}s" else "1/${(1.0 / sec).toInt()}s")
                    }
                }
                exif.getAttribute(ExifInterface.TAG_ISO_SPEED)?.let { params.add("ISO$it") }
                if (params.isNotEmpty()) parts.add(params.joinToString("  "))
            }

            if (preset.showDateTime) {
                exif.getAttribute(ExifInterface.TAG_DATETIME)?.let { dt ->
                    try {
                        val parsed = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).parse(dt)
                        if (parsed != null) {
                            parts.add(SimpleDateFormat("yyyy.MM.dd  HH:mm", Locale.getDefault()).format(parsed))
                        }
                    } catch (_: Exception) { parts.add(dt) }
                }
            }

            if (preset.showLocation) {
                val lat = exif.latLong
                if (lat != null && lat.size == 2) {
                    try {
                        val geocoder = Geocoder(java.util.Locale.getDefault())
                        val addr = geocoder.getFromLocation(lat[0], lat[1], 1)
                        if (!addr.isNullOrEmpty()) {
                            val locality = addr[0].locality ?: addr[0].subAdminArea ?: addr[0].adminArea
                            if (!locality.isNullOrEmpty()) parts.add(locality)
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}

        return parts.joinToString("  ·  ")
    }

    private fun drawInfoBar(canvas: Canvas, cw: Int, ch: Int, imgY: Int, text: String) {
        val barH = (ch - imgY - (imgY * 0.4f).toInt()).coerceIn(40, 68)
        val barTop = ch - barH

        val linePaint = Paint().apply { color = Color.argb(25, 0, 0, 0); strokeWidth = 0.5f }
        canvas.drawLine(cw * 0.15f, barTop.toFloat(), cw * 0.85f, barTop.toFloat(), linePaint)

        val textPaint = Paint().apply {
            isAntiAlias = true; color = Color.argb(160, 40, 40, 40)
            textSize = (cw * 0.022f).coerceIn(12f, 22f); textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(text, cw / 2f, (barTop + ch) / 2f + textPaint.textSize / 3f, textPaint)
    }

    private fun drawClassicMatte(canvas: Canvas, img: Bitmap, cw: Int, ch: Int, ix: Int, iy: Int, radius: Int) {
        val bg = Bitmap.createScaledBitmap(img, cw, ch, true)
        val blurPaint = Paint().apply {
            maskFilter = BlurMaskFilter((cw * 0.03f).coerceIn(10f, 60f), BlurMaskFilter.Blur.NORMAL)
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
            color = Color.argb(35, 0, 0, 0); isAntiAlias = true
        }
        canvas.drawRoundRect(ix.toFloat(), iy.toFloat(), (ix + img.width).toFloat(), (iy + img.height).toFloat(),
            radius.toFloat(), radius.toFloat(), borderPaint)
    }

    private fun drawVignette(canvas: Canvas, img: Bitmap, cw: Int, ch: Int) {
        canvas.drawBitmap(img, 0f, 0f, null)
        val vignettePaint = Paint().apply {
            shader = RadialGradient(cw / 2f, ch / 2f, maxOf(cw, ch) * 0.75f,
                intArrayOf(Color.TRANSPARENT, Color.argb(150, 0, 0, 0)),
                floatArrayOf(0.25f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, cw.toFloat(), ch.toFloat(), vignettePaint)
    }

    private fun drawDoublePreserve(canvas: Canvas, img: Bitmap, cw: Int, ch: Int, ix: Int, iy: Int, radius: Int) {
        canvas.drawColor(Color.argb(245, 250, 248, 245))
        drawRoundedImage(canvas, img, ix.toFloat(), iy.toFloat(), radius * 2)
        val gap = (cw * 0.015f).toInt().coerceIn(4, 16)
        val inner = Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = (cw * 0.002f).coerceIn(1f, 2f)
            color = Color.argb(60, 0, 0, 0); isAntiAlias = true
        }
        val outer = Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = (cw * 0.008f).coerceIn(2f, 6f)
            color = Color.argb(25, 0, 0, 0); isAntiAlias = true
        }
        canvas.drawRoundRect((ix - gap).toFloat(), (iy - gap).toFloat(), (ix + img.width + gap).toFloat(), (iy + img.height + gap).toFloat(), (radius * 2 + gap).toFloat(), (radius * 2 + gap).toFloat(), inner)
        canvas.drawRoundRect((ix - gap * 2).toFloat(), (iy - gap * 2).toFloat(), (ix + img.width + gap * 2).toFloat(), (iy + img.height + gap * 2).toFloat(), (radius * 2 + gap * 2).toFloat(), (radius * 2 + gap * 2).toFloat(), outer)
    }

    private fun drawPhotoPaper(canvas: Canvas, img: Bitmap, cw: Int, ch: Int, ix: Int, iy: Int, radius: Int) {
        canvas.drawColor(Color.WHITE)
        drawRoundedImage(canvas, img, ix.toFloat(), iy.toFloat(), radius.coerceAtMost(12))
        val shadowPaint = Paint().apply {
            setShadowLayer((cw * 0.012f).coerceIn(4f, 14f), 0f, (cw * 0.004f).coerceIn(1f, 5f), Color.argb(50, 0, 0, 0))
        }
        canvas.drawRoundRect(ix.toFloat(), iy.toFloat(), (ix + img.width).toFloat(), (iy + img.height).toFloat(),
            radius.toFloat(), radius.toFloat(), shadowPaint)
    }

    private fun drawRoundedImage(canvas: Canvas, img: Bitmap, x: Float, y: Float, radius: Int) {
        val path = Path().apply {
            addRoundRect(x, y, x + img.width, y + img.height, radius.toFloat(), radius.toFloat(), Path.Direction.CW)
        }
        canvas.save(); canvas.clipPath(path); canvas.drawBitmap(img, x, y, null); canvas.restore()
    }
}
