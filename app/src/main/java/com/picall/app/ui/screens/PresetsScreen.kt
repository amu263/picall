package com.picall.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.picall.app.data.local.PresetDatabase
import com.picall.app.data.model.ColorFormula
import com.picall.app.data.model.Preset
import com.picall.app.data.model.PresetType
import com.picall.app.data.repository.PresetRepository
import com.picall.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsScreen(onNavigateBack: () -> Unit, onLoadPreset: (Long) -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { PresetDatabase.getInstance(ctx) }
    val repo = remember { PresetRepository(db) }
    var query by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Preset?>(null) }

    val all by repo.getAllPresets().collectAsState(initial = emptyList())
    val filtered = remember(all, query) {
        all.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("胶卷库", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("搜索预设...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true, shape = RoundedCornerShape(12.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Bookmarks, null, Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        Text("暂无预设", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("在编辑器中保存的预设将显示在这里", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { preset ->
                        PresetGridCard(
                            preset = preset,
                            onClick = { onLoadPreset(preset.id) },
                            onDelete = { deleteTarget = preset }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除预设") },
            text = { Text("确定要删除「${deleteTarget!!.name}」吗？此操作不可撤销。") },
            confirmButton = {
                Button(onClick = {
                    scope.launch { repo.deletePreset(deleteTarget!!.id) }
                    deleteTarget = null
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun PresetGridCard(preset: Preset, onClick: () -> Unit, onDelete: () -> Unit) {
    val typeLabel = when (preset.type) {
        PresetType.COLOR_FORMULA -> if (preset.thumbnailPath.isNotEmpty()) "含LUT" else "色彩配方"
        PresetType.LUT -> "3D LUT"
        PresetType.WATERMARK -> "水印相框"
    }

    // Parse formula for canister colors
    val formula = remember(preset.dataJson) {
        if (preset.type == PresetType.COLOR_FORMULA) {
            try { Gson().fromJson(preset.dataJson, ColorFormula::class.java) } catch (_: Exception) { null }
        } else null
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Thumbnail area — large film canister graphic
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                if (formula != null) {
                    FilmCanisterLarge(formula, preset.name)
                } else {
                    Icon(
                        if (preset.type == PresetType.LUT) Icons.Outlined.Gradient else Icons.Outlined.Palette,
                        null, Modifier.size(48.dp),
                        tint = SliderActive.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Preset name
            Text(
                preset.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            // Type label
            Text(
                typeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))

            // Delete button
            OutlinedButton(
                onClick = onDelete,
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Delete, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("删除", fontSize = 11.sp)
            }
        }
    }
}

/**
 * Large film canister graphic drawn on Canvas for the preset grid card.
 * Shows a warm/cool tinted canister with film strip and perforations.
 */
@Composable
private fun FilmCanisterLarge(formula: ColorFormula, name: String) {
    val colors = remember(formula) { formulaToCanisterColors(formula) }

    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        val nc = drawContext.canvas.nativeCanvas
        val w = size.width; val h = size.height
        val canW = w * 0.55f; val canH = h * 0.78f
        val canX = (w - canW) / 2f; val canY = (h - canH) / 2f + 6f

        // Canister body with gradient
        val canPaint = android.graphics.Paint().apply {
            shader = android.graphics.LinearGradient(
                canX, 0f, canX + canW, 0f,
                intArrayOf(colors[0].toArgb(), colors[1].toArgb(), colors[2].toArgb()),
                floatArrayOf(0f, 0.5f, 1f), android.graphics.Shader.TileMode.CLAMP
            )
            isAntiAlias = true
        }
        nc.drawRoundRect(canX, canY, canX + canW, canY + canH, w * 0.06f, w * 0.06f, canPaint)

        // Canister outline
        val outlinePaint = android.graphics.Paint().apply {
            color = 0x40FFFFFF.toInt(); style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1.5f; isAntiAlias = true
        }
        nc.drawRoundRect(canX, canY, canX + canW, canY + canH, w * 0.06f, w * 0.06f, outlinePaint)

        // Highlight reflection
        val hl = android.graphics.Paint().apply {
            color = 0x40FFFFFF.toInt(); isAntiAlias = true
        }
        nc.drawRoundRect(canX + 6f, canY + 6f, canX + 18f, canY + 18f, 4f, 4f, hl)

        // Top rim / spool
        val out = android.graphics.Paint().apply {
            color = 0xDD1A1A1A.toInt(); isAntiAlias = true
        }
        nc.drawRoundRect(canX + canW * 0.15f, canY - 4f, canX + canW * 0.85f, canY + 14f, 4f, 4f, out)

        // Label area
        val lw = canW * 0.72f; val lh = canH * 0.32f
        val lx = canX + (canW - lw) / 2f; val ly = canY + canH * 0.28f
        val lp = android.graphics.Paint().apply {
            color = 0xEEFFFFFF.toInt(); isAntiAlias = true
            setShadowLayer(3f, 0f, 1f, 0x50000000.toInt())
        }
        nc.drawRoundRect(lx, ly, lx + lw, ly + lh, 5f, 5f, lp)

        // Label text
        val tp = android.graphics.Paint().apply {
            color = 0xEE1A1A1A.toInt(); textSize = w * 0.10f
            isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true
        }
        nc.drawText(name, w / 2f, ly + lh * 0.62f, tp)

        // Right edge cap
        val cp = android.graphics.Paint().apply {
            color = 0xDD1A1A1A.toInt(); isAntiAlias = true
        }
        nc.drawRoundRect(w * 0.84f, canY + canH * 0.05f, w * 0.99f, canY + canH * 0.95f, 6f, 6f, cp)

        // Perforation strip in dominant color
        val sp = android.graphics.Paint().apply {
            color = colors[0].toArgb(); isAntiAlias = true
        }
        nc.drawRoundRect(w * 0.90f, canY + canH * 0.03f, w * 0.97f, canY + canH * 0.97f, 4f, 4f, sp)

        // Perforation holes
        val dp = android.graphics.Paint().apply { color = 0xFFFFFFFF.toInt() }
        for (i in 0..4) {
            nc.drawCircle(w * 0.935f, canY + canH * 0.14f + i * canH * 0.18f, w * 0.016f, dp)
        }

        // Bottom spool
        nc.drawRoundRect(canX + canW * 0.15f, canY + canH - 10f, canX + canW * 0.85f, canY + canH + 4f, 3f, 3f, out)
    }
}

/**
 * Map formula parameters to canister color palette.
 * Warm → orange/red, Cool → blue, Neutral → amber gold.
 */
private fun formulaToCanisterColors(formula: ColorFormula): List<Color> {
    val sat = formula.saturation; val temp = formula.colorTemperature
    if (sat < -0.8f) return listOf(Color(0xFF333333), Color(0xFF777777), Color(0xFF222222))
    return when {
        temp > 0.3f -> listOf(Color(0xFFE67451), Color(0xFFD8A37E), Color(0xFFC0392B))
        temp < -0.3f -> listOf(Color(0xFF4A90E2), Color(0xFF50E3C2), Color(0xFF2E86C1))
        else -> listOf(Color(0xFFF5A623), Color(0xFFF8C471), Color(0xFFD0021B))
    }
}
