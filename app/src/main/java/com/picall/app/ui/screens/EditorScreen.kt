package com.picall.app.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.picall.app.data.local.PresetDatabase
import com.picall.app.data.model.*
import com.picall.app.data.repository.PresetRepository
import com.picall.app.ui.components.AdjustmentSlider
import com.picall.app.ui.components.BidirectionalSlider
import com.picall.app.ui.components.BUILTIN_FILM_PRESETS
import com.picall.app.ui.components.FilmRollCard
import com.picall.app.ui.components.FilterCategoryCard
import com.picall.app.ui.components.FilmPresetItem
import com.picall.app.ui.components.HistogramView
import com.picall.app.ui.components.PresetCard
import com.picall.app.ui.theme.*
import com.picall.app.viewmodel.EditorState
import com.picall.app.viewmodel.EditorTab
import com.picall.app.viewmodel.EditorViewModel
import com.picall.app.viewmodel.EditorViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onNavigateToPresets: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { PresetDatabase.getInstance(ctx) }
    val repo = remember { PresetRepository(db) }
    val vm: EditorViewModel = viewModel(factory = EditorViewModelFactory(repo))
    val s by vm.state.collectAsState()

    val imgPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                ctx.contentResolver.openInputStream(it)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { bmp -> vm.loadImage(bmp, it) }
                }
            } catch (_: Exception) {}
        }
    }

    val lutPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val txt = ctx.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: ""
                vm.importLut(txt, it.lastPathSegment ?: "unknown.cube")
            } catch (_: Exception) {}
        }
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Picall", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { vm.undo() }, enabled = s.canUndo) {
                        Icon(Icons.Default.Undo, "撤销")
                    }
                    IconButton(onClick = onNavigateToPresets) {
                        Icon(Icons.Outlined.Bookmarks, "预设管理")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, "设置")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabBtn("色彩配方", Icons.Outlined.ColorLens, Icons.Default.ColorLens,
                        s.activeTab == EditorTab.COLOR_FORMULA) { vm.setActiveTab(EditorTab.COLOR_FORMULA) }
                    TabBtn("预设", Icons.Outlined.Bookmarks, Icons.Default.Bookmarks,
                        s.activeTab == EditorTab.PRESETS) { vm.setActiveTab(EditorTab.PRESETS) }

                    FilledIconButton(onClick = {
                        scope.launch {
                            vm.exportAsync()?.let { bmp ->
                                saveToGallery(ctx, bmp)
                                Toast.makeText(ctx, "已保存到相册", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, enabled = !s.isExporting,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = SliderActive)
                    ) {
                        if (s.isExporting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.Default.SaveAlt, "导出", Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { pad ->
        if (s.originalBitmap == null) {
            EmptyState(onPick = { imgPicker.launch("image/*") })
        } else {
            Column(Modifier.fillMaxSize().padding(pad)) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    PreviewArea(s.previewBitmap, s.isProcessing)
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ imgPicker.launch("image/*") }) {
                        Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("换图", fontSize = 12.sp)
                    }
                    HistogramView(s.previewBitmap, Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
                when (s.activeTab) {
                    EditorTab.COLOR_FORMULA -> FormulaPanel(s, vm, { showSaveDialog = true }, { lutPicker.launch("*/*") }, Modifier.weight(1f))
                    EditorTab.PRESETS -> PresetsTab(vm, Modifier.weight(1f))
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false; presetName = "" },
            title = { Text("保存预设") },
            text = {
                OutlinedTextField(presetName, { if (it.length <= 8) presetName = it }, label = { Text("预设名称 (最多8字)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = {
                    if (presetName.isNotBlank() && presetName.length <= 8) {
                        vm.savePreset(presetName) { success, msg ->
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            if (success) { presetName = ""; showSaveDialog = false }
                        }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = SliderActive)) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false; presetName = "" }) { Text("取消") }
            }
        )
    }
}

// ═══ Preview ═══

@Composable
private fun PreviewArea(bitmap: android.graphics.Bitmap?, isProcessing: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.background(Color(0xFF111111)), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), "预览",
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit)
        }
        if (isProcessing) {
            CircularProgressIndicator(Modifier.size(28.dp).align(Alignment.TopEnd).padding(12.dp),
                color = SliderActive, strokeWidth = 2.dp)
        }
    }
}

// ═══ Color Formula Panel ═══

@Composable
private fun FormulaPanel(s: EditorState, vm: EditorViewModel, onSavePreset: () -> Unit, onPickLut: () -> Unit, modifier: Modifier = Modifier) {
    val f = s.colorFormula
    val lut = s.lutPreset
    var selectedFilmId by remember { mutableStateOf("none") }
    val customPresets by vm.colorFormulaPresets.collectAsState()
    val savedLuts by vm.lutPresets.collectAsState()

    val allPresets = remember(customPresets, savedLuts) {
        val items = mutableListOf<FilmPresetItem>()
        items.addAll(BUILTIN_FILM_PRESETS)
        for (p in customPresets) {
            val formula = p.toColorFormula() ?: continue
            items.add(FilmPresetItem("custom_${p.id}", p.name, formula, PresetType.COLOR_FORMULA, p.thumbnailPath))
        }
        for (p in savedLuts) {
            val l = p.toLutPreset() ?: continue
            // Generate formula from LUT data hash for unique card color
            val hash = l.lutData.hashCode()
            val r = ((hash and 0xFF0000) shr 16) / 255f
            val g = ((hash and 0x00FF00) shr 8) / 255f
            val b = (hash and 0x0000FF) / 255f
            val lutFormula = ColorFormula.DEFAULT.copy(
                colorTemperature = (r - 0.5f).coerceIn(-1f, 1f),
                tint = (g - 0.5f).coerceIn(-1f, 1f),
                saturation = (0.3f + b * 0.5f).coerceIn(0f, 1f))
            items.add(FilmPresetItem("lut_${p.id}", p.name, lutFormula, PresetType.LUT, l.lutData))
        }
        items
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
        // Film strip
        Text("胶片预设", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allPresets.size) { index ->
                val preset = allPresets[index]
                FilmRollCard(
                    item = preset,
                    isSelected = selectedFilmId == preset.id,
                    onClick = {
                        selectedFilmId = preset.id
                        when (preset.type) {
                            PresetType.LUT -> {
                                vm.importLutFromBase64(preset.lutData, preset.name)
                            }
                            else -> vm.selectFilmPreset(preset.formula, preset.id)
                        }
                    }
                )
            }
            item { Spacer(Modifier.width(4.dp)) }
        }

        // LUT card
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("LUT 预设", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (lut.lutData.isNotEmpty()) {
                        Text(lut.name.ifEmpty { "已导入" }, style = MaterialTheme.typography.bodySmall, color = SliderActive)
                    }
                }
                if (lut.lutData.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("强度", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                        Slider(lut.intensity, { vm.setLutIntensity(it) }, valueRange = 0f..1f, modifier = Modifier.weight(1f).height(20.dp),
                            colors = SliderDefaults.colors(thumbColor = SliderThumb, activeTrackColor = SliderActive, inactiveTrackColor = SliderTrack))
                        Text("${(lut.intensity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onPickLut, Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.FileOpen, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("导入 LUT", fontSize = 12.sp)
                    }
                    if (lut.lutData.isNotEmpty()) {
                        OutlinedButton({ vm.removeLut() }, Modifier.weight(1f), shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))) {
                            Text("移除 LUT", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Global intensity
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("配方总强度", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(80.dp))
            Slider(f.globalIntensity, { vm.updateFormula { copy(globalIntensity = it) } },
                valueRange = 0f..1f, modifier = Modifier.weight(1f).height(20.dp),
                colors = SliderDefaults.colors(thumbColor = SliderThumb, activeTrackColor = SliderActive, inactiveTrackColor = SliderTrack))
            Text("${(f.globalIntensity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp))
        }
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // Brightness
        FilterCategoryCard("亮度", s.expandedCategory == "brightness", { vm.toggleCategory("brightness") }, f.brightnessIntensity,
            { vm.updateFormula { copy(brightnessIntensity = it) } }) {
            AdjustmentSlider("曝光", f.exposure, { vm.updateFormula { copy(exposure = it) } })
            AdjustmentSlider("对比度", f.contrast, { vm.updateFormula { copy(contrast = it) } })
            AdjustmentSlider("高光", f.highlights, { vm.updateFormula { copy(highlights = it) } })
            AdjustmentSlider("阴影", f.shadows, { vm.updateFormula { copy(shadows = it) } })
        }

        // Color
        FilterCategoryCard("颜色", s.expandedCategory == "color", { vm.toggleCategory("color") }, f.colorIntensity,
            { vm.updateFormula { copy(colorIntensity = it) } }) {
            AdjustmentSlider("饱和度", f.saturation, { vm.updateFormula { copy(saturation = it) } })
            BidirectionalSlider("色温", f.colorTemperature, { vm.updateFormula { copy(colorTemperature = it) } }, negativeLabel = "冷", positiveLabel = "暖")
            BidirectionalSlider("色调", f.tint, { vm.updateFormula { copy(tint = it) } }, negativeLabel = "绿", positiveLabel = "紫")
        }

        // RGB
        FilterCategoryCard("RGB原色", s.expandedCategory == "rgb", { vm.toggleCategory("rgb") }, f.rgbIntensity,
            { vm.updateFormula { copy(rgbIntensity = it) } }) {
            Text("红色通道", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF4757))
            AdjustmentSlider("色相", f.redHue, { vm.updateFormula { copy(redHue = it) } })
            AdjustmentSlider("饱和度", f.redSaturation, { vm.updateFormula { copy(redSaturation = it) } })
            Text("绿色通道", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2ED573))
            AdjustmentSlider("色相", f.greenHue, { vm.updateFormula { copy(greenHue = it) } })
            AdjustmentSlider("饱和度", f.greenSaturation, { vm.updateFormula { copy(greenSaturation = it) } })
            Text("蓝色通道", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1E90FF))
            AdjustmentSlider("色相", f.blueHue, { vm.updateFormula { copy(blueHue = it) } })
            AdjustmentSlider("饱和度", f.blueSaturation, { vm.updateFormula { copy(blueSaturation = it) } })
        }

        // LCH
        FilterCategoryCard("LCH颜色", s.expandedCategory == "lch", { vm.toggleCategory("lch") }, f.lchIntensity,
            { vm.updateFormula { copy(lchIntensity = it) } }) {
            AdjustmentSlider("明度 L", f.lchLightness, { vm.updateFormula { copy(lchLightness = it) } })
            AdjustmentSlider("彩度 C", f.lchChroma, { vm.updateFormula { copy(lchChroma = it) } })
            AdjustmentSlider("色相 H", f.lchHue, { vm.updateFormula { copy(lchHue = it) } }, displayValue = "${(f.lchHue * 180).toInt()}°")
        }

        // Effects
        FilterCategoryCard("效果", s.expandedCategory == "effects", { vm.toggleCategory("effects") }, f.effectsIntensity,
            { vm.updateFormula { copy(effectsIntensity = it) } }) {
            AdjustmentSlider("褪色", f.fade, { vm.updateFormula { copy(fade = it) } }, valueRange = 0f..1f, displayValue = "${(f.fade * 100).toInt()}%")
            AdjustmentSlider("留银冲洗", f.silverRetention, { vm.updateFormula { copy(silverRetention = it) } }, valueRange = 0f..1f, displayValue = "${(f.silverRetention * 100).toInt()}%")
        }

        // Buttons
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton({ vm.resetFormula() }, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("重置")
            }
                Button(onSavePreset, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SliderActive)) {
                Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("保存预设")
            }
        }
    }
}

// ═══ Presets Tab ═══

@Composable
private fun PresetsTab(vm: EditorViewModel, modifier: Modifier = Modifier) {
    val colorP by vm.colorFormulaPresets.collectAsState()
    val lutP by vm.lutPresets.collectAsState()

    val allPresets = remember(colorP, lutP) {
        (colorP + lutP).sortedByDescending { it.updatedAt }
    }

    Column(modifier) {
        if (allPresets.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Bookmarks, null, Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))
                    Text("暂无预设", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("保存后出现在这里", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allPresets, key = { it.id }) { preset ->
                    val typeLabel = when (preset.type) {
                        PresetType.COLOR_FORMULA -> if (preset.thumbnailPath.isNotEmpty()) "含LUT" else "色彩配方"
                        PresetType.LUT -> "3D LUT"
                        else -> "预设"
                    }
                    Card(shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        onClick = { vm.loadPreset(preset) }
                    ) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(6.dp))
                                .background(SliderActive.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center) {
                                Icon(if (preset.type == PresetType.LUT) Icons.Outlined.Gradient else Icons.Outlined.Palette,
                                    null, Modifier.size(28.dp), tint = SliderActive.copy(alpha = 0.3f))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(preset.name, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(typeLabel, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(2.dp))
                            TextButton({ vm.deletePreset(preset.id) },
                                Modifier.fillMaxWidth(), contentPadding = PaddingValues(2.dp)) {
                                Text("删除", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══ Helpers ═══

@Composable
private fun TabBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                   selIcon: androidx.compose.ui.graphics.vector.ImageVector, sel: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Icon(if (sel) selIcon else icon, label,
            tint = if (sel) SliderActive else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp))
        Text(label, fontSize = 10.sp, color = if (sel) SliderActive else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(onPick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.Image, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        Spacer(Modifier.height(24.dp))
        Text("选择一张照片开始编辑", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("色彩配方 · LUT导入", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Spacer(Modifier.height(32.dp))
        Button(onPick, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SliderActive)) {
            Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("选择图片")
        }
    }
}

private fun posSymbol(pos: WatermarkPosition) = when (pos) {
    WatermarkPosition.TOP_LEFT -> "↖"; WatermarkPosition.TOP_CENTER -> "↑"; WatermarkPosition.TOP_RIGHT -> "↗"
    WatermarkPosition.CENTER_LEFT -> "←"; WatermarkPosition.CENTER -> "·"; WatermarkPosition.CENTER_RIGHT -> "→"
    WatermarkPosition.BOTTOM_LEFT -> "↙"; WatermarkPosition.BOTTOM_CENTER -> "↓"; WatermarkPosition.BOTTOM_RIGHT -> "↘"
    else -> "·"
}

private fun frameLabel(s: FrameStyle) = when (s) {
    FrameStyle.NONE -> "无"
    FrameStyle.CLASSIC_MATTE -> "经典留白"
    FrameStyle.MINIMAL_LINE -> "极简线框"
    FrameStyle.VIGNETTE -> "暗角光影"
    FrameStyle.DOUBLE_PRESERVE -> "双框珍藏"
    FrameStyle.PHOTO_PAPER -> "相纸印记"
}

private fun saveToGallery(ctx: android.content.Context, bitmap: android.graphics.Bitmap) {
    try {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "Picall_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Picall")
        }
        ctx.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let {
            ctx.contentResolver.openOutputStream(it)?.use { os -> bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, os) }
        }
    } catch (_: Exception) {}
}
