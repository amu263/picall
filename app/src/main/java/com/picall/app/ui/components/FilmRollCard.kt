package com.picall.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picall.app.data.model.ColorFormula
import com.picall.app.data.model.PresetType
import com.picall.app.ui.theme.SliderActive

data class FilmPresetItem(
    val id: String,
    val name: String,
    val formula: ColorFormula,
    val type: PresetType = PresetType.COLOR_FORMULA,
    val lutData: String = "",
    val thumbnailBmp: Bitmap? = null,
    val isBuiltin: Boolean = false
) {
    val iso get() = when {
        type == PresetType.LUT -> "3D LUT"
        formula.saturation < -0.8f -> "ISO 100 · 黑白"
        formula.saturation < -0.3f -> "ISO 200 · 彩色负片"
        formula.saturation > 0.3f || formula.fade > 0.3f -> "ISO 400 · 彩色正片"
        formula.silverRetention > 0.2f -> "ISO 400 · 留银冲洗"
        else -> "ISO 200 · 彩色负片"
    }
}

@Composable
fun FilmRollCard(
    item: FilmPresetItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(76.dp)
            .shadow(if (isSelected) 4.dp else 2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0EDE8))
            .then(if (isSelected) Modifier.border(1.5.dp, SliderActive, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(60.dp).padding(3.dp), contentAlignment = Alignment.Center) {
            if (item.thumbnailBmp != null) {
                Image(item.thumbnailBmp.asImageBitmap(), null, Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)))
            } else {
                CanisterGraphic(item)
            }
        }

        Column(Modifier.fillMaxWidth().background(Color(0xFFF0EDE8)).padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.name, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C2C2C),
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(item.iso, fontSize = 7.sp, color = Color(0xFF888888), maxLines = 1, textAlign = TextAlign.Center)
        }

        if (isSelected) Box(Modifier.fillMaxWidth().height(2.dp).background(SliderActive))
    }
}

@Composable
private fun CanisterGraphic(item: FilmPresetItem) {
    val colors = remember(item.formula) { formulaToCanisterColors(item.formula) }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val nc = drawContext.canvas.nativeCanvas
        val w = size.width; val h = size.height
        val canW = w * 0.52f; val canH = h * 0.82f
        val canX = (w - canW) / 2f; val canY = (h - canH) / 2f

        val canPaint = android.graphics.Paint().apply {
            shader = android.graphics.LinearGradient(canX, 0f, canX + canW, 0f,
                intArrayOf(colors[0].hashCode(), colors[1].hashCode(), colors[0].hashCode()),
                floatArrayOf(0f, 0.5f, 1f), android.graphics.Shader.TileMode.CLAMP)
            isAntiAlias = true
        }
        nc.drawRoundRect(canX, canY, canX + canW, canY + canH, w * 0.06f, w * 0.06f, canPaint)

        val hl = android.graphics.Paint().apply { color = 0x50FFFFFF.toInt(); isAntiAlias = true }
        nc.drawRoundRect(canX + 3f, canY + 2f, canX + 12f, canY + 12f, 3f, 3f, hl)

        val out = android.graphics.Paint().apply { color = 0xDD141414.toInt(); isAntiAlias = true }
        nc.drawRoundRect(canX + canW * 0.2f, canY - 1f, canX + canW * 0.8f, canY + 10f, 2f, 2f, out)

        val lw = canW * 0.7f; val lh = canH * 0.3f
        val lx = canX + (canW - lw) / 2f; val ly = canY + canH * 0.3f
        val lp = android.graphics.Paint().apply { color = 0xF0FFFFFF.toInt(); isAntiAlias = true }
        nc.drawRoundRect(lx, ly, lx + lw, ly + lh, 3f, 3f, lp)

        val cp = android.graphics.Paint().apply { color = 0xDD1E1E1E.toInt(); isAntiAlias = true }
        nc.drawRoundRect(w * 0.86f, canY + canH * 0.08f, w * 0.99f, canY + canH * 0.92f, 4f, 4f, cp)

        // Perforation strip in dominant color
        val stripColor = colors[0].hashCode()
        val stripPaint = android.graphics.Paint().apply { color = stripColor; isAntiAlias = true }
        nc.drawRoundRect(w * 0.91f, canY + canH * 0.06f, w * 0.97f, canY + canH * 0.94f, 2f, 2f, stripPaint)

        // Perforation holes (white)
        val dp = android.graphics.Paint().apply { color = 0xFFFFFFFF.toInt() }
        for (i in 0..4) nc.drawCircle(w * 0.94f, canY + canH * 0.15f + i * canH * 0.17f, w * 0.018f, dp)
    }
}

fun formulaToCanisterColors(formula: ColorFormula): List<Color> {
    val sat = formula.saturation; val temp = formula.colorTemperature
    if (sat < -0.8f) return listOf(Color(0xFF333333), Color(0xFF777777), Color(0xFF222222))
    return when {
        temp > 0.3f -> listOf(Color(0xFFE67451), Color(0xFFD8A37E), Color(0xFFC0392B))
        temp < -0.3f -> listOf(Color(0xFF4A90E2), Color(0xFF50E3C2), Color(0xFF2E86C1))
        else -> listOf(Color(0xFFF5A623), Color(0xFFF8C471), Color(0xFFD0021B))
    }
}

fun generateThumbnailBitmap(formula: ColorFormula, name: String, width: Int = 160, height: Int = 200): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val colors = formulaToCanisterColors(formula)
    val c0 = colors[0].hashCode(); val c1 = colors[1].hashCode(); val c2 = colors[2].hashCode()

    val canW = width * 0.48f; val canH = height * 0.6f
    val canX = (width - canW) / 2f; val canY = height * 0.12f

    canvas.drawColor(0xF5F0EDE8.toInt())

    val canPaint = android.graphics.Paint().apply {
        shader = android.graphics.LinearGradient(canX, 0f, canX + canW, 0f, intArrayOf(c0, c1, c2),
            floatArrayOf(0f, 0.5f, 1f), android.graphics.Shader.TileMode.CLAMP)
        isAntiAlias = true
    }
    canvas.drawRoundRect(canX, canY, canX + canW, canY + canH, width * 0.05f, width * 0.05f, canPaint)

    val hl = android.graphics.Paint().apply { color = 0x60FFFFFF.toInt(); isAntiAlias = true }
    canvas.drawRoundRect(canX + 6f, canY + 4f, canX + 24f, canY + 28f, 4f, 4f, hl)

    val out = android.graphics.Paint().apply { color = 0xDD141414.toInt(); isAntiAlias = true }
    canvas.drawRoundRect(canX + canW * 0.18f, canY - 2f, canX + canW * 0.82f, canY + 20f, 4f, 4f, out)

    val lw = canW * 0.7f; val lh = canH * 0.28f
    val lx = canX + (canW - lw) / 2f; val ly = canY + canH * 0.32f
    val lp = android.graphics.Paint().apply {
        color = 0xF5FFFFFF.toInt(); isAntiAlias = true
        setShadowLayer(2f, 0f, 1f, 0x50000000.toInt())
    }
    canvas.drawRoundRect(lx, ly, lx + lw, ly + lh, 4f, 4f, lp)

    val tp = android.graphics.Paint().apply {
        color = 0xEE1E1E1E.toInt(); textSize = width * 0.11f
        isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true
    }
    canvas.drawText(name, width / 2f, ly + lh * 0.62f, tp)

    val cp = android.graphics.Paint().apply { color = 0xDD1E1E1E.toInt(); isAntiAlias = true }
    canvas.drawRoundRect(width * 0.86f, canY + canH * 0.06f, width * 0.99f, canY + canH * 0.94f, 6f, 6f, cp)

    // Perforation strip in dominant color
    val sp = android.graphics.Paint().apply { color = c0; isAntiAlias = true }
    canvas.drawRoundRect(width * 0.91f, canY + canH * 0.04f, width * 0.97f, canY + canH * 0.96f, 3f, 3f, sp)

    // Perforation holes (white)
    val dp = android.graphics.Paint().apply { color = 0xFFFFFFFF.toInt() }
    for (i in 0..5) canvas.drawCircle(width * 0.94f, canY + canH * 0.15f + i * canH * 0.13f, width * 0.016f, dp)

    return bmp
}

val BUILTIN_FILM_PRESETS = listOf(
    FilmPresetItem("none", "无", ColorFormula.DEFAULT, isBuiltin = true),
    FilmPresetItem("warm_sun", "暖日", ColorFormula.DEFAULT.copy(colorTemperature = 0.5f, saturation = 0.2f, exposure = 0.15f, contrast = 0.1f), isBuiltin = true),
    FilmPresetItem("cool_air", "清冷", ColorFormula.DEFAULT.copy(colorTemperature = -0.4f, tint = 0.1f, saturation = -0.1f, highlights = -0.1f), isBuiltin = true),
    FilmPresetItem("retro", "复古", ColorFormula.DEFAULT.copy(saturation = -0.3f, fade = 0.4f, contrast = -0.1f, shadows = 0.2f, colorTemperature = 0.3f), isBuiltin = true),
    FilmPresetItem("film_like", "胶片", ColorFormula.DEFAULT.copy(saturation = 0.15f, contrast = 0.25f, fade = 0.15f, silverRetention = 0.3f, shadows = 0.1f), isBuiltin = true),
    FilmPresetItem("bw", "黑白", ColorFormula.DEFAULT.copy(saturation = -1f, contrast = 0.3f, exposure = 0.1f), isBuiltin = true),
    FilmPresetItem("vivid", "鲜明", ColorFormula.DEFAULT.copy(saturation = 0.5f, contrast = 0.2f, exposure = 0.1f, highlights = -0.2f), isBuiltin = true),
    FilmPresetItem("dark", "暗调", ColorFormula.DEFAULT.copy(exposure = -0.3f, contrast = 0.15f, shadows = -0.2f, saturation = -0.2f), isBuiltin = true),
    FilmPresetItem("fresh", "清新", ColorFormula.DEFAULT.copy(exposure = 0.2f, saturation = 0.1f, highlights = -0.15f, shadows = 0.15f, colorTemperature = -0.1f), isBuiltin = true),
    FilmPresetItem("golden", "暖金", ColorFormula.DEFAULT.copy(colorTemperature = 0.7f, tint = 0.15f, saturation = 0.25f, exposure = 0.1f), isBuiltin = true)
)
