package com.picall.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun HistogramView(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val bins = remember(bitmap) { computeHistogram(bitmap) }

    Canvas(modifier
        .clip(RoundedCornerShape(4.dp))
        .background(Color(0x22000000))
        .then(modifier)
        .height(48.dp)
        .fillMaxWidth()
    ) {
        if (bins == null) return@Canvas
        val w = size.width
        val h = size.height
        val maxCount = bins.max().coerceAtLeast(1).toFloat()

        // RGB curves
        val pathR = Path()
        val pathG = Path()
        val pathB = Path()
        val pathL = Path()

        for (i in 0 until 256) {
            val x = (i / 255f) * w
            val yr = h - (bins[i * 4 + 1] / maxCount) * h
            val yg = h - (bins[i * 4 + 2] / maxCount) * h
            val yb = h - (bins[i * 4 + 3] / maxCount) * h
            val yl = h - (bins[i * 4 + 0] / maxCount) * h

            if (i == 0) {
                pathR.moveTo(x, yr); pathG.moveTo(x, yg)
                pathB.moveTo(x, yb); pathL.moveTo(x, yl)
            } else {
                pathR.lineTo(x, yr); pathG.lineTo(x, yg)
                pathB.lineTo(x, yb); pathL.lineTo(x, yl)
            }
        }

        // Luminance fill
        val fillPath = Path().apply {
            addPath(pathL)
            for (i in 255 downTo 0) lineTo((i / 255f) * w, h)
            close()
        }
        drawPath(fillPath, Color.White.copy(alpha = 0.12f))

        drawPath(pathR, Color(0xFFFF4757).copy(alpha = 0.7f), style = Stroke(1.5f))
        drawPath(pathG, Color(0xFF2ED573).copy(alpha = 0.7f), style = Stroke(1.5f))
        drawPath(pathB, Color(0xFF1E90FF).copy(alpha = 0.7f), style = Stroke(1.5f))
        drawPath(pathL, Color.White.copy(alpha = 0.4f), style = Stroke(1f))
    }
}

private fun computeHistogram(bitmap: Bitmap?): IntArray? {
    if (bitmap == null) return null
    val bins = IntArray(256 * 4)
    val w = minOf(bitmap.width, 256)
    val h = minOf(bitmap.height, 256)
    val stepX = bitmap.width / w.coerceAtLeast(1)
    val stepY = bitmap.height / h.coerceAtLeast(1)

    for (sy in 0 until h) {
        for (sx in 0 until w) {
            val px = bitmap.getPixel(sx * stepX, sy * stepY)
            val r = px shr 16 and 0xFF
            val g = px shr 8 and 0xFF
            val b = px and 0xFF
            val l = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
            bins[l * 4 + 0]++
            bins[r * 4 + 1]++
            bins[g * 4 + 2]++
            bins[b * 4 + 3]++
        }
    }
    return bins
}
