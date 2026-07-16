package com.picall.app.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import com.picall.app.ui.components.*
import com.picall.app.ui.theme.*
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { PresetDatabase.getInstance(context) }
    val repo = remember { PresetRepository(db) }
    val vm: EditorViewModel = viewModel(factory = EditorViewModelFactory(repo))
    val editorState by vm.state.collectAsState()

    val colorFormulaPresets by vm.colorFormulaPresets.collectAsState()
    val lutPresets by vm.lutPresets.collectAsState()
    val watermarkPresets by vm.watermarkPresets.collectAsState()

    // 保存预设对话框
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        vm.loadImage(bitmap, it)
            }
        }
    }

    // 保存预设对话框
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存预设") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("预设名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            when (editorState.activeTab) {
                                EditorTab.COLOR_FORMULA -> vm.saveColorFormulaPreset(presetName)
                                EditorTab.LUT -> vm.saveLutPreset(presetName)
                                EditorTab.WATERMARK -> vm.saveWatermarkPreset(presetName)
                                else -> {}
                            }
                            presetName = ""
                            showSaveDialog = false
                            Toast.makeText(context, "预设已保存", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SliderActive)
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false; presetName = "" }) {
                    Text("取消")
                }
            }
        )
    }
}
    }

    // LUT 文件选择器
    val lutPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: ""
                val fileName = it.lastPathSegment ?: "unknown.cube"
                vm.importLut(content, fileName)
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Picall",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // 撤消按钮
                    IconButton(
                        onClick = { vm.undo() },
                        enabled = editorState.canUndo
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "撤消")
                    }
                    // 预设
                    IconButton(onClick = onNavigateToPresets) {
                        Icon(Icons.Outlined.Bookmarks, contentDescription = "预设")
                    }
                    // 设置
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            // 底部工具栏：4 个标签 + 导出
            BottomToolbar(
                activeTab = editorState.activeTab,
                onTabSelected = { vm.setActiveTab(it) },
                onExport = {
                    scope.launch {
                        vm.exportImageAsync()?.let { bitmap ->
                            saveBitmapToGallery(context, bitmap)
                            Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                isExporting = editorState.isExporting
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (editorState.originalBitmap == null) {
                // ── 空状态：提示选择图片 ──
                EmptyImagePlaceholder(
                    onSelectImage = { imagePickerLauncher.launch("image/*") },
                    onTakePhoto = { /* 相机拍照 */ }
                )
            } else {
                // ── 图片预览 ──
                ImagePreviewSection(
                    bitmap = editorState.previewBitmap,
                    isProcessing = editorState.isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f)
                )

                // ── 调节面板 ──
                when (editorState.activeTab) {
                    EditorTab.COLOR_FORMULA -> ColorFormulaPanel(
                        formula = editorState.colorFormula,
                        expandedCategory = editorState.expandedCategory,
                        onToggleCategory = { vm.toggleCategory(it) },
                        onUpdate = { vm.updateColorFormula(it) }
                    )
                    EditorTab.LUT -> LutPanel(
                        lutPreset = editorState.lutPreset,
                        onImportLut = { lutPickerLauncher.launch("*/*") },
                        onIntensityChange = { vm.setLutIntensity(it) },
                        onSavePreset = { showSaveDialog = true }
                    )
                    EditorTab.WATERMARK -> WatermarkPanel(
                        watermark = editorState.watermarkPreset,
                        onUpdate = { vm.updateWatermark(it) },
                        onSavePreset = { showSaveDialog = true }
                    )
                    EditorTab.PRESETS -> PresetsPanel(
                        colorFormulas = colorFormulaPresets,
                        lutPresets = lutPresets,
                        watermarkPresets = watermarkPresets,
                        onLoadPreset = { vm.loadPreset(it) },
                        onDeletePreset = { vm.deletePreset(it) }
                    )
                }
            }
        }
    }

    // 保存预设对话框
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存预设") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("预设名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            when (editorState.activeTab) {
                                EditorTab.COLOR_FORMULA -> vm.saveColorFormulaPreset(presetName)
                                EditorTab.LUT -> vm.saveLutPreset(presetName)
                                EditorTab.WATERMARK -> vm.saveWatermarkPreset(presetName)
                                else -> {}
                            }
                            presetName = ""
                            showSaveDialog = false
                            Toast.makeText(context, "预设已保存", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SliderActive)
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false; presetName = "" }) {
                    Text("取消")
                }
            }
        )
    }
}

// ═══════════════════════════════════════════
//  空状态占位
// ═══════════════════════════════════════════

@Composable
private fun EmptyImagePlaceholder(
    onSelectImage: () -> Unit,
    onTakePhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "选择一张照片开始编辑",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "支持色彩配方、LUT导入、水印相框",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onSelectImage,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SliderActive)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("选择图片")
            }

            OutlinedButton(
                onClick = onTakePhoto,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("拍照")
            }
        }
    }
}

// ═══════════════════════════════════════════
//  图片预览
// ═══════════════════════════════════════════

@Composable
private fun ImagePreviewSection(
    bitmap: android.graphics.Bitmap?,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "预览",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offset = Offset(
                                offset.x + pan.x,
                                offset.y + pan.y
                            )
                        }
                    },
                contentScale = ContentScale.Fit
            )

            // 处理中遮罩
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    color = SliderActive,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
//  底部工具栏
// ═══════════════════════════════════════════

@Composable
private fun BottomToolbar(
    activeTab: EditorTab,
    onTabSelected: (EditorTab) -> Unit,
    onExport: () -> Unit,
    isExporting: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarTab(
                icon = Icons.Outlined.ColorLens,
                selectedIcon = Icons.Default.ColorLens,
                label = "色彩配方",
                isSelected = activeTab == EditorTab.COLOR_FORMULA,
                onClick = { onTabSelected(EditorTab.COLOR_FORMULA) }
            )

            ToolbarTab(
                icon = Icons.Outlined.Gradient,
                selectedIcon = Icons.Default.Gradient,
                label = "LUT",
                isSelected = activeTab == EditorTab.LUT,
                onClick = { onTabSelected(EditorTab.LUT) }
            )

            ToolbarTab(
                icon = Icons.Outlined.WaterDrop,
                selectedIcon = Icons.Default.WaterDrop,
                label = "水印相框",
                isSelected = activeTab == EditorTab.WATERMARK,
                onClick = { onTabSelected(EditorTab.WATERMARK) }
            )

            ToolbarTab(
                icon = Icons.Outlined.Bookmarks,
                selectedIcon = Icons.Default.Bookmarks,
                label = "预设",
                isSelected = activeTab == EditorTab.PRESETS,
                onClick = { onTabSelected(EditorTab.PRESETS) }
            )

            // 导出按钮
            FilledIconButton(
                onClick = onExport,
                enabled = !isExporting,
                modifier = Modifier.padding(start = 8.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = SliderActive,
                    contentColor = Color.White
                )
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.SaveAlt, contentDescription = "导出", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ToolbarTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isSelected) selectedIcon else icon,
            contentDescription = label,
            tint = if (isSelected) SliderActive else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) SliderActive else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

// ═══════════════════════════════════════════
//  色彩配方面板
// ═══════════════════════════════════════════

@Composable
private fun ColorFormulaPanel(
    formula: ColorFormula,
    expandedCategory: String?,
    onToggleCategory: (String) -> Unit,
    onUpdate: (ColorFormula.() -> ColorFormula) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(0.55f)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp)
    ) {
        // 全局强度
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("全局配方强度", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(96.dp))
            Slider(
                value = formula.globalIntensity,
                onValueChange = { onUpdate { copy(globalIntensity = it) } },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f).height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = SliderThumb,
                    activeTrackColor = SliderActive,
                    inactiveTrackColor = SliderTrack
                )
            )
            Text("${(formula.globalIntensity * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(36.dp))
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // 亮度
        FilterCategoryCard(
            title = "亮度",
            isExpanded = expandedCategory == "brightness",
            onToggle = { onToggleCategory("brightness") },
            intensity = formula.brightnessIntensity,
            onIntensityChange = { onUpdate { copy(brightnessIntensity = it) } }
        ) {
            AdjustmentSlider("曝光", formula.exposure, { onUpdate { copy(exposure = it) } })
            AdjustmentSlider("对比度", formula.contrast, { onUpdate { copy(contrast = it) } })
            AdjustmentSlider("高光", formula.highlights, { onUpdate { copy(highlights = it) } })
            AdjustmentSlider("阴影", formula.shadows, { onUpdate { copy(shadows = it) } })
        }

        // WRGB 曲线
        FilterCategoryCard(
            title = "WRGB曲线",
            isExpanded = expandedCategory == "curves",
            onToggle = { onToggleCategory("curves") },
            intensity = formula.curvesIntensity,
            onIntensityChange = { onUpdate { copy(curvesIntensity = it) } }
        ) {
            var selectedChannel by remember { mutableStateOf("W") }
            val channels = listOf("W" to "亮度", "R" to "红色", "G" to "绿色", "B" to "蓝色")
            val channelColors = mapOf("W" to CurveChannelW, "R" to CurveChannelR, "G" to CurveChannelG, "B" to CurveChannelB)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                channels.forEach { (ch, label) ->
                    FilterChip(
                        selected = selectedChannel == ch,
                        onClick = { selectedChannel = ch },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = channelColors[ch]?.copy(alpha = 0.2f) ?: SliderActive.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val points = when (selectedChannel) {
                "W" -> formula.curvePointsW
                "R" -> formula.curvePointsR
                "G" -> formula.curvePointsG
                "B" -> formula.curvePointsB
                else -> formula.curvePointsW
            }

            CurvesEditor(
                channelLabel = "通道: $selectedChannel",
                points = points,
                onPointsChanged = { newPoints ->
                    when (selectedChannel) {
                        "W" -> onUpdate { copy(curvePointsW = newPoints) }
                        "R" -> onUpdate { copy(curvePointsR = newPoints) }
                        "G" -> onUpdate { copy(curvePointsG = newPoints) }
                        "B" -> onUpdate { copy(curvePointsB = newPoints) }
                    }
                },
                lineColor = channelColors[selectedChannel] ?: CurveLine
            )
        }

        // 颜色
        FilterCategoryCard(
            title = "颜色",
            isExpanded = expandedCategory == "color",
            onToggle = { onToggleCategory("color") },
            intensity = formula.colorIntensity,
            onIntensityChange = { onUpdate { copy(colorIntensity = it) } }
        ) {
            AdjustmentSlider("饱和度", formula.saturation, { onUpdate { copy(saturation = it) } })
            BidirectionalSlider("色温", formula.colorTemperature,
                { onUpdate { copy(colorTemperature = it) } }, negativeLabel = "冷", positiveLabel = "暖")
            BidirectionalSlider("色调", formula.tint,
                { onUpdate { copy(tint = it) } }, negativeLabel = "绿", positiveLabel = "洋红")
        }

        // RGB 原色
        FilterCategoryCard(
            title = "RGB原色",
            isExpanded = expandedCategory == "rgb",
            onToggle = { onToggleCategory("rgb") },
            intensity = formula.rgbIntensity,
            onIntensityChange = { onUpdate { copy(rgbIntensity = it) } }
        ) {
            Text("红色", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF4757))
            AdjustmentSlider("色相", formula.redHue, { onUpdate { copy(redHue = it) } })
            AdjustmentSlider("饱和度", formula.redSaturation, { onUpdate { copy(redSaturation = it) } })

            Text("绿色", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2ED573))
            AdjustmentSlider("色相", formula.greenHue, { onUpdate { copy(greenHue = it) } })
            AdjustmentSlider("饱和度", formula.greenSaturation, { onUpdate { copy(greenSaturation = it) } })

            Text("蓝色", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1E90FF))
            AdjustmentSlider("色相", formula.blueHue, { onUpdate { copy(blueHue = it) } })
            AdjustmentSlider("饱和度", formula.blueSaturation, { onUpdate { copy(blueSaturation = it) } })
        }

        // LCH 颜色
        FilterCategoryCard(
            title = "LCH颜色",
            isExpanded = expandedCategory == "lch",
            onToggle = { onToggleCategory("lch") },
            intensity = formula.lchIntensity,
            onIntensityChange = { onUpdate { copy(lchIntensity = it) } }
        ) {
            AdjustmentSlider("明度 (L)", formula.lchLightness, { onUpdate { copy(lchLightness = it) } })
            AdjustmentSlider("彩度 (C)", formula.lchChroma, { onUpdate { copy(lchChroma = it) } })
            AdjustmentSlider("色相 (H)", formula.lchHue, { onUpdate { copy(lchHue = it) } },
                displayValue = "${(formula.lchHue * 180).toInt()}°")
        }

        // 效果
        FilterCategoryCard(
            title = "效果",
            isExpanded = expandedCategory == "effects",
            onToggle = { onToggleCategory("effects") },
            intensity = formula.effectsIntensity,
            onIntensityChange = { onUpdate { copy(effectsIntensity = it) } }
        ) {
            AdjustmentSlider("褪色", formula.fade, { onUpdate { copy(fade = it) } },
                valueRange = 0f..1f, displayValue = "${(formula.fade * 100).toInt()}%")
            AdjustmentSlider("留银冲洗", formula.silverRetention, { onUpdate { copy(silverRetention = it) } },
                valueRange = 0f..1f, displayValue = "${(formula.silverRetention * 100).toInt()}%")
        }

        // 底部操作
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { onUpdate { ColorFormula.DEFAULT } },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("重置配方")
            }

            Button(
                onClick = { showSaveDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SliderActive)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("保存为预设")
            }
        }
    }
}

// ═══════════════════════════════════════════
//  LUT 面板
// ═══════════════════════════════════════════

@Composable
private fun LutPanel(
    lutPreset: LutPreset,
    onImportLut: () -> Unit,
    onIntensityChange: (Float) -> Unit,
    onSavePreset: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(0.55f)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (lutPreset.lutData.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Gradient,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("导入 LUT 文件", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "支持 .cube / .3dl 格式",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onImportLut,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("导入 LUT")
                    }
                }
            }
        } else {
            // 已导入 LUT
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(lutPreset.name.ifEmpty { "未命名LUT" },
                                style = MaterialTheme.typography.titleSmall)
                            Text("大小: ${lutPreset.lutSize}³",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("LUT 强度", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = lutPreset.intensity,
                            onValueChange = onIntensityChange,
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f).height(24.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = SliderThumb,
                                activeTrackColor = SliderActive,
                                inactiveTrackColor = SliderTrack
                            )
                        )
                        Text("${(lutPreset.intensity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(36.dp))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
//  水印相框面板
// ═══════════════════════════════════════════

@Composable
private fun WatermarkPanel(
    watermark: WatermarkPreset,
    onUpdate: (WatermarkPreset.() -> WatermarkPreset) -> Unit,
    onSavePreset: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(0.55f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // 全局不透明度
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("水印不透明度", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(96.dp))
            Slider(
                value = watermark.globalIntensity,
                onValueChange = { onUpdate { copy(globalIntensity = it) } },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f).height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = SliderThumb, activeTrackColor = SliderActive, inactiveTrackColor = SliderTrack
                )
            )
            Text("${(watermark.globalIntensity * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp))
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // 文字内容
        FilterCategoryCard(
            title = "自定义文字",
            isExpanded = true,
            onToggle = { },
            intensity = watermark.textOpacity,
            onIntensityChange = { onUpdate { copy(textOpacity = it) } }
        ) {
            OutlinedTextField(
                value = watermark.textContent,
                onValueChange = { onUpdate { copy(textContent = it) } },
                label = { Text("水印文字内容") },
                placeholder = { Text("输入水印文字，支持多行") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(Modifier.height(8.dp))

            AdjustmentSlider("字体大小", watermark.fontSize / 72f,
                { onUpdate { copy(fontSize = it * 72f) } },
                valueRange = 0.1f..1f, displayValue = "${watermark.fontSize.toInt()}sp")

            AdjustmentSlider("字间距", watermark.letterSpacing / 10f,
                { onUpdate { copy(letterSpacing = it * 10f) } },
                valueRange = 0f..1f)
        }

        // 位置设置
        FilterCategoryCard(
            title = "位置设置",
            isExpanded = false,
            onToggle = { },
            intensity = watermark.globalIntensity,
            onIntensityChange = { onUpdate { copy(globalIntensity = it) } }
        ) {
            // 9宫格位置选择
            val positions = listOf(
                listOf(WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_CENTER, WatermarkPosition.TOP_RIGHT),
                listOf(WatermarkPosition.CENTER_LEFT, WatermarkPosition.CENTER, WatermarkPosition.CENTER_RIGHT),
                listOf(WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_CENTER, WatermarkPosition.BOTTOM_RIGHT)
            )

            positions.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { pos ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (watermark.position == pos) SliderActive.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onUpdate { copy(position = pos) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (pos) {
                                    WatermarkPosition.TOP_LEFT -> "↖"
                                    WatermarkPosition.TOP_CENTER -> "↑"
                                    WatermarkPosition.TOP_RIGHT -> "↗"
                                    WatermarkPosition.CENTER_LEFT -> "←"
                                    WatermarkPosition.CENTER -> "·"
                                    WatermarkPosition.CENTER_RIGHT -> "→"
                                    WatermarkPosition.BOTTOM_LEFT -> "↙"
                                    WatermarkPosition.BOTTOM_CENTER -> "↓"
                                    WatermarkPosition.BOTTOM_RIGHT -> "↘"
                                    else -> "·"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        // 相框样式
        FilterCategoryCard(
            title = "相框样式",
            isExpanded = false,
            onToggle = { },
            intensity = watermark.frameWidth / 20f,
            onIntensityChange = { onUpdate { copy(frameWidth = it * 20f) } }
        ) {
            val frameStyles = listOf(
                FrameStyle.NONE to "无",
                FrameStyle.SIMPLE to "简洁线框",
                FrameStyle.DOUBLE to "双线框",
                FrameStyle.FILM_STRIP to "胶片边框",
                FrameStyle.CLASSIC_MATTE to "卡纸框",
                FrameStyle.POLAROID to "拍立得",
                FrameStyle.VIGNETTE to "暗角",
                FrameStyle.SHADOW_BORDER to "阴影边框"
            )

            frameStyles.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (style, label) ->
                        FilterChip(
                            selected = watermark.frameStyle == style,
                            onClick = { onUpdate { copy(frameStyle = style) } },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).height(32.dp)
                        )
                    }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(4.dp))
            }

            if (watermark.frameStyle != FrameStyle.NONE) {
                AdjustmentSlider("边框宽度", watermark.frameWidth / 20f,
                    { onUpdate { copy(frameWidth = it * 20f) } },
                    valueRange = 0.1f..1f, displayValue = "${watermark.frameWidth.toInt()}dp")
            }
        }

        // EXIF 显示
        FilterCategoryCard(
            title = "照片参数 (EXIF)",
            isExpanded = false,
            onToggle = { },
            intensity = watermark.globalIntensity,
            onIntensityChange = { onUpdate { copy(globalIntensity = it) } }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("显示EXIF参数", modifier = Modifier.weight(1f))
                Switch(
                    checked = watermark.showExif,
                    onCheckedChange = { onUpdate { copy(showExif = it) } }
                )
            }

            if (watermark.showExif) {
                Text("选择要显示的参数:", style = MaterialTheme.typography.bodySmall)

                ExifDisplayItem.AVAILABLE_TAGS.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (tag, label) ->
                            val isSelected = watermark.exifItems.any { it.tag == tag }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val current = watermark.exifItems.toMutableList()
                                    if (isSelected) current.removeAll { it.tag == tag }
                                    else current.add(ExifDisplayItem(tag = tag, label = label))
                                    onUpdate { copy(exifItems = current) }
                                },
                                label = { Text(label, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f).height(28.dp)
                            )
                    }
                }
            }

            // 保存预设按钮
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSavePreset,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SliderActive),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("保存为预设")
            }
        }
    }
}
}

// ═══════════════════════════════════════════
//  预设面板
// ═══════════════════════════════════════════

@Composable
private fun PresetsPanel(
    colorFormulas: List<Preset>,
    lutPresets: List<Preset>,
    watermarkPresets: List<Preset>,
    onLoadPreset: (Preset) -> Unit = {},
    onDeletePreset: (Long) -> Unit = {}
) {
    var selectedType by remember { mutableStateOf(PresetType.COLOR_FORMULA) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(0.55f)
    ) {
        // 类型切换
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                PresetType.COLOR_FORMULA to "色彩配方",
                PresetType.LUT to "LUT",
                PresetType.WATERMARK to "水印相框"
            ).forEach { (type, label) ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(label, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val presets = when (selectedType) {
            PresetType.COLOR_FORMULA -> colorFormulas
            PresetType.LUT -> lutPresets
            PresetType.WATERMARK -> watermarkPresets
        }

        if (presets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Bookmarks,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Text("暂无保存的预设",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            presets.forEach { preset ->
                PresetCard(
                    preset = preset,
                    onClick = { onLoadPreset(preset) },
                    onDelete = { onDeletePreset(preset.id) }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
//  辅助函数
// ═══════════════════════════════════════════

private fun saveBitmapToGallery(context: android.content.Context, bitmap: android.graphics.Bitmap) {
    // 使用 MediaStore API 保存到相册 (Android 10+)
    try {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "Picall_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Picall")
        }
        val uri = context.contentResolver.insert(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, stream)
            }
        }
    } catch (_: Exception) {}
}
