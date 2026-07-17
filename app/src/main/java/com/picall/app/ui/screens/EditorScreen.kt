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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.picall.app.data.local.PresetDatabase
import com.picall.app.data.model.*
import com.picall.app.data.repository.PresetRepository
import com.picall.app.ui.components.AdjustmentSlider
import com.picall.app.ui.components.BidirectionalSlider
import com.picall.app.ui.components.FilterCategoryCard
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
                    TabBtn("LUT", Icons.Outlined.Gradient, Icons.Default.Gradient,
                        s.activeTab == EditorTab.LUT) { vm.setActiveTab(EditorTab.LUT) }
                    TabBtn("水印相框", Icons.Outlined.WaterDrop, Icons.Default.WaterDrop,
                        s.activeTab == EditorTab.WATERMARK) { vm.setActiveTab(EditorTab.WATERMARK) }
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
                PreviewArea(s.previewBitmap, s.isProcessing, Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                when (s.activeTab) {
                    EditorTab.COLOR_FORMULA -> FormulaPanel(s, vm, { showSaveDialog = true }, Modifier.weight(1f))
                    EditorTab.LUT -> LutTab(s, vm, { lutPicker.launch("*/*") }, { showSaveDialog = true }, Modifier.weight(1f))
                    EditorTab.WATERMARK -> WatermarkTab(s, vm, { showSaveDialog = true }, Modifier.weight(1f))
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
                OutlinedTextField(presetName, { presetName = it }, label = { Text("预设名称") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = {
                    if (presetName.isNotBlank()) {
                        when (s.activeTab) {
                            EditorTab.COLOR_FORMULA -> vm.saveColorPreset(presetName)
                            EditorTab.LUT -> vm.saveLutPreset(presetName)
                            EditorTab.WATERMARK -> vm.saveWatermarkPreset(presetName)
                            else -> {}
                        }
                        presetName = ""; showSaveDialog = false
                        Toast.makeText(ctx, "已保存", Toast.LENGTH_SHORT).show()
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
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Column(modifier) {
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF111111)), contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), "预览", Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offset = Offset(offset.x + pan.x, offset.y + pan.y)
                        }
                    },
                    contentScale = ContentScale.Fit)
            }
            if (isProcessing) {
                CircularProgressIndicator(Modifier.size(28.dp).align(Alignment.TopEnd).padding(12.dp),
                    color = SliderActive, strokeWidth = 2.dp)
            }
        }
        HistogramView(bitmap, Modifier.fillMaxWidth())
    }
}

// ═══ Color Formula Panel ═══

@Composable
private fun FormulaPanel(s: EditorState, vm: EditorViewModel, onSavePreset: () -> Unit, modifier: Modifier = Modifier) {
    val f = s.colorFormula
    Column(modifier.verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
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

// ═══ LUT Tab ═══

@Composable
private fun LutTab(s: EditorState, vm: EditorViewModel, onImport: () -> Unit, onSavePreset: () -> Unit, modifier: Modifier = Modifier) {
    val lut = s.lutPreset
    Column(modifier.padding(16.dp)) {
        if (lut.lutData.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Gradient, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("导入 LUT 文件", style = MaterialTheme.typography.titleMedium)
                    Text("支持 .cube / .3dl 格式", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Button(onImport, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.FileOpen, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("导入 LUT")
                    }
                }
            }
        } else {
            Card(shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(lut.name.ifEmpty { "LUT" }, style = MaterialTheme.typography.titleSmall)
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("强度 ${(lut.intensity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(lut.intensity, { vm.setLutIntensity(it) }, valueRange = 0f..1f, modifier = Modifier.height(24.dp),
                        colors = SliderDefaults.colors(thumbColor = SliderThumb, activeTrackColor = SliderActive, inactiveTrackColor = SliderTrack))
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton({ vm.removeLut() }, Modifier.fillMaxWidth()) {
                        Text("移除 LUT")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onSavePreset, Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = SliderActive)) {
                Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("保存为预设")
            }
        }
    }
}

// ═══ Watermark Tab ═══

@Composable
private fun WatermarkTab(s: EditorState, vm: EditorViewModel, onSavePreset: () -> Unit, modifier: Modifier = Modifier) {
    val w = s.watermarkPreset
    var frameExpanded by remember { mutableStateOf(true) }
    var exifExpanded by remember { mutableStateOf(true) }

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)) {
        // Frame style selection
        Card(shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column {
                Row(Modifier.fillMaxWidth().clickable { frameExpanded = !frameExpanded }.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("相框样式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Icon(if (frameExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AnimatedVisibility(frameExpanded) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        val styles = FrameStyle.entries.toList()
                        styles.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { style ->
                                    val selected = w.frameStyle == style
                                    Surface(
                                        modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(8.dp))
                                            .clickable { vm.updateWatermark { copy(frameStyle = style) } },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) SliderActive.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, SliderActive) else null
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(frameLabel(style), fontSize = 11.sp,
                                                color = if (selected) SliderActive else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                                if (row.size < 2) Spacer(Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // EXIF info toggles
        Card(shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column {
                Row(Modifier.fillMaxWidth().clickable { exifExpanded = !exifExpanded }.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("照片参数", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Switch(w.showExif, { vm.updateWatermark { copy(showExif = it) } }, Modifier.height(24.dp))
                    }
                    Icon(if (exifExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AnimatedVisibility(exifExpanded) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        ExifToggle("设备名称", w.showDevice) { vm.updateWatermark { copy(showDevice = it) } }
                        ExifToggle("拍摄参数", w.showParams) { vm.updateWatermark { copy(showParams = it) } }
                        ExifToggle("拍摄时间", w.showDateTime) { vm.updateWatermark { copy(showDateTime = it) } }
                        ExifToggle("拍摄位置", w.showLocation) { vm.updateWatermark { copy(showLocation = it) } }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(onSavePreset, Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = SliderActive)) {
            Icon(Icons.Default.Save, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("保存为预设")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ExifToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked, onChange, Modifier.height(24.dp))
    }
}

// ═══ Presets Tab ═══

@Composable
private fun PresetsTab(vm: EditorViewModel, modifier: Modifier = Modifier) {
    val colorP by vm.colorFormulaPresets.collectAsState()
    val lutP by vm.lutPresets.collectAsState()
    val wmP by vm.watermarkPresets.collectAsState()
    var type by remember { mutableStateOf(PresetType.COLOR_FORMULA) }

    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(type == PresetType.COLOR_FORMULA, { type = PresetType.COLOR_FORMULA },
                label = { Text("色彩", fontSize = 12.sp) }, modifier = Modifier.weight(1f))
            FilterChip(type == PresetType.LUT, { type = PresetType.LUT },
                label = { Text("LUT", fontSize = 12.sp) }, modifier = Modifier.weight(1f))
            FilterChip(type == PresetType.WATERMARK, { type = PresetType.WATERMARK },
                label = { Text("水印", fontSize = 12.sp) }, modifier = Modifier.weight(1f))
        }
        val list = when (type) {
            PresetType.COLOR_FORMULA -> colorP
            PresetType.LUT -> lutP
            PresetType.WATERMARK -> wmP
        }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无预设", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                list.forEach { p ->
                    PresetCard(p, { vm.loadPreset(p) }, { vm.deletePreset(p.id) })
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
        Text("色彩配方 · LUT导入 · 水印相框", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
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
