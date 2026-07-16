package com.picall.app.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.picall.app.data.model.*
import com.picall.app.data.repository.PresetRepository
import com.picall.app.imageprocessing.ImageProcessor
import com.picall.app.imageprocessing.lut.LutParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 编辑状态 — 当前所有调节参数的即时快照
 */
data class EditorState(
    val originalBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val sourceUri: Uri? = null,
    val isProcessing: Boolean = false,

    // 各个功能的状态
    val colorFormula: ColorFormula = ColorFormula.DEFAULT,
    val lutPreset: LutPreset = LutPreset.DEFAULT,
    val watermarkPreset: WatermarkPreset = WatermarkPreset.DEFAULT,

    // UI 状态
    val activeTab: EditorTab = EditorTab.COLOR_FORMULA,
    val expandedCategory: String? = null,  // 当前展开的调节分类
    val isExporting: Boolean = false,

    // 操作历史
    val canUndo: Boolean = false,
    val historyIndex: Int = -1
)

enum class EditorTab {
    COLOR_FORMULA, LUT, WATERMARK, PRESETS
}

class EditorViewModel(
    private val presetRepository: PresetRepository
) : ViewModel() {

    private val imageProcessor = ImageProcessor()

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    // 预设流
    val colorFormulaPresets: StateFlow<List<Preset>> = presetRepository.getColorFormulas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lutPresets: StateFlow<List<Preset>> = presetRepository.getLuts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watermarkPresets: StateFlow<List<Preset>> = presetRepository.getWatermarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 历史记录 (用于撤消)
    private val history = mutableListOf<ColorFormula>()
    private val maxHistory = 20

    // 预览防抖 Job
    private var previewJob: Job? = null

    /**
     * 加载图片
     */
    fun loadImage(bitmap: Bitmap, uri: Uri? = null) {
        _state.update {
            it.copy(
                originalBitmap = bitmap,
                previewBitmap = bitmap,
                sourceUri = uri,
                colorFormula = ColorFormula.DEFAULT,
                lutPreset = LutPreset.DEFAULT,
                watermarkPreset = WatermarkPreset.DEFAULT,
                historyIndex = -1
            )
        }
        history.clear()
        updatePreview()
    }

    /**
     * 切换标签
     */
    fun setActiveTab(tab: EditorTab) {
        _state.update { it.copy(activeTab = tab, expandedCategory = null) }
    }

    /**
     * 展开/折叠调节分类
     */
    fun toggleCategory(category: String) {
        _state.update {
            it.copy(
                expandedCategory = if (it.expandedCategory == category) null else category
            )
        }
    }

    // ═══════════════════════════════════════
    //  色彩配方调节
    // ═══════════════════════════════════════

    fun updateColorFormula(update: ColorFormula.() -> ColorFormula) {
        val current = _state.value.colorFormula
        val newFormula = current.update()
        saveHistory(current)
        _state.update { it.copy(colorFormula = newFormula) }
        schedulePreviewUpdate()
    }

    fun setExposure(value: Float) = updateAdjustment { copy(exposure = value) }
    fun setContrast(value: Float) = updateAdjustment { copy(contrast = value) }
    fun setHighlights(value: Float) = updateAdjustment { copy(highlights = value) }
    fun setShadows(value: Float) = updateAdjustment { copy(shadows = value) }
    fun setBrightnessIntensity(value: Float) = updateAdjustment { copy(brightnessIntensity = value) }

    fun setSaturation(value: Float) = updateAdjustment { copy(saturation = value) }
    fun setColorTemperature(value: Float) = updateAdjustment { copy(colorTemperature = value) }
    fun setTint(value: Float) = updateAdjustment { copy(tint = value) }
    fun setColorIntensity(value: Float) = updateAdjustment { copy(colorIntensity = value) }

    fun setRedHue(value: Float) = updateAdjustment { copy(redHue = value) }
    fun setRedSaturation(value: Float) = updateAdjustment { copy(redSaturation = value) }
    fun setGreenHue(value: Float) = updateAdjustment { copy(greenHue = value) }
    fun setGreenSaturation(value: Float) = updateAdjustment { copy(greenSaturation = value) }
    fun setBlueHue(value: Float) = updateAdjustment { copy(blueHue = value) }
    fun setBlueSaturation(value: Float) = updateAdjustment { copy(blueSaturation = value) }
    fun setRgbIntensity(value: Float) = updateAdjustment { copy(rgbIntensity = value) }

    fun setLchLightness(value: Float) = updateAdjustment { copy(lchLightness = value) }
    fun setLchChroma(value: Float) = updateAdjustment { copy(lchChroma = value) }
    fun setLchHue(value: Float) = updateAdjustment { copy(lchHue = value) }
    fun setLchIntensity(value: Float) = updateAdjustment { copy(lchIntensity = value) }

    fun setCurvePoints(channel: String, points: List<CurvePoint>) {
        updateColorFormula {
            when (channel) {
                "W" -> copy(curvePointsW = points)
                "R" -> copy(curvePointsR = points)
                "G" -> copy(curvePointsG = points)
                "B" -> copy(curvePointsB = points)
                else -> this
            }
        }
    }
    fun setCurvesIntensity(value: Float) = updateAdjustment { copy(curvesIntensity = value) }

    fun setFade(value: Float) = updateAdjustment { copy(fade = value) }
    fun setSilverRetention(value: Float) = updateAdjustment { copy(silverRetention = value) }
    fun setEffectsIntensity(value: Float) = updateAdjustment { copy(effectsIntensity = value) }

    fun setGlobalIntensity(value: Float) = updateAdjustment { copy(globalIntensity = value) }

    private fun updateAdjustment(transform: ColorFormula.() -> ColorFormula) {
        val current = _state.value.colorFormula
        val newFormula = current.transform()
        saveHistory(current)
        _state.update { it.copy(colorFormula = newFormula) }
        schedulePreviewUpdate()
    }

    // ═══════════════════════════════════════
    //  LUT 操作
    // ═══════════════════════════════════════

    /**
     * 从文本导入 LUT 文件
     */
    fun importLut(content: String, fileName: String) {
        val lutData = LutParser.parse(content)
        val base64 = LutParser.encodeToBase64(lutData)

        _state.update {
            it.copy(
                lutPreset = it.lutPreset.copy(
                    lutData = base64,
                    lutSize = lutData.size,
                    sourceFile = fileName,
                    name = lutData.title.ifEmpty { fileName }
                )
            )
        }
        schedulePreviewUpdate()
    }

    fun setLutIntensity(value: Float) {
        _state.update { it.copy(lutPreset = it.lutPreset.copy(intensity = value)) }
        schedulePreviewUpdate()
    }

    // ═══════════════════════════════════════
    //  水印相框操作
    // ═══════════════════════════════════════

    fun updateWatermark(update: WatermarkPreset.() -> WatermarkPreset) {
        val current = _state.value.watermarkPreset
        _state.update { it.copy(watermarkPreset = current.update()) }
        schedulePreviewUpdate()
    }

    fun setWatermarkText(text: String) = updateWatermark { copy(textContent = text) }
    fun setWatermarkFontSize(size: Float) = updateWatermark { copy(fontSize = size) }
    fun setWatermarkPosition(position: WatermarkPosition) = updateWatermark { copy(position = position) }
    fun setWatermarkColor(color: Long) = updateWatermark { copy(textColor = color) }
    fun setWatermarkOpacity(opacity: Float) = updateWatermark { copy(textOpacity = opacity) }
    fun setFrameStyle(style: FrameStyle) = updateWatermark { copy(frameStyle = style) }
    fun setFrameWidth(width: Float) = updateWatermark { copy(frameWidth = width) }
    fun setWatermarkGlobalIntensity(value: Float) = updateWatermark { copy(globalIntensity = value) }
    fun setShowExif(show: Boolean) = updateWatermark { copy(showExif = show) }
    fun setExifItems(items: List<ExifDisplayItem>) = updateWatermark { copy(exifItems = items) }
    fun setFontPath(path: String) = updateWatermark { copy(fontPath = path) }

    // ═══════════════════════════════════════
    //  预设管理
    // ═══════════════════════════════════════

    fun saveColorFormulaPreset(name: String, category: String = "") {
        viewModelScope.launch {
            val formula = _state.value.colorFormula
            // 在实际应用中，先生成缩略图
            presetRepository.saveColorFormula(name, formula)
        }
    }

    fun saveLutPreset(name: String, category: String = "") {
        viewModelScope.launch {
            val lut = _state.value.lutPreset
            presetRepository.saveLut(name, lut)
        }
    }

    fun saveWatermarkPreset(name: String, category: String = "") {
        viewModelScope.launch {
            val watermark = _state.value.watermarkPreset
            presetRepository.saveWatermark(name, watermark)
        }
    }

    fun loadPreset(preset: Preset) {
        viewModelScope.launch {
            when (preset.type) {
                PresetType.COLOR_FORMULA -> {
                    val formula = preset.toColorFormula()
                    if (formula != null) {
                        _state.update { it.copy(colorFormula = formula) }
                        updatePreview()
                    }
                }
                PresetType.LUT -> {
                    val lut = preset.toLutPreset()
                    if (lut != null) {
                        _state.update { it.copy(lutPreset = lut) }
                        updatePreview()
                    }
                }
                PresetType.WATERMARK -> {
                    val watermark = preset.toWatermarkPreset()
                    if (watermark != null) {
                        _state.update { it.copy(watermarkPreset = watermark) }
                        updatePreview()
                    }
                }
            }
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch {
            presetRepository.deletePreset(id)
        }
    }

    // ═══════════════════════════════════════
    //  重置
    // ═══════════════════════════════════════

    fun resetColorFormula() {
        saveHistory(_state.value.colorFormula)
        _state.update { it.copy(colorFormula = ColorFormula.DEFAULT) }
        schedulePreviewUpdate()
    }

    fun resetLut() {
        _state.update { it.copy(lutPreset = LutPreset.DEFAULT) }
        schedulePreviewUpdate()
    }

    fun resetWatermark() {
        _state.update { it.copy(watermarkPreset = WatermarkPreset.DEFAULT) }
        schedulePreviewUpdate()
    }

    fun resetAll() {
        history.clear()
        _state.update {
            it.copy(
                colorFormula = ColorFormula.DEFAULT,
                lutPreset = LutPreset.DEFAULT,
                watermarkPreset = WatermarkPreset.DEFAULT,
                historyIndex = -1
            )
        }
        updatePreview()
    }

    // ═══════════════════════════════════════
    //  撤消
    // ═══════════════════════════════════════

    fun undo() {
        if (history.isNotEmpty()) {
            val previous = history.removeLast()
            _state.update {
                it.copy(
                    colorFormula = previous,
                    historyIndex = it.historyIndex - 1,
                    canUndo = history.isNotEmpty()
                )
            }
            schedulePreviewUpdate()
        }
    }

    // ═══════════════════════════════════════
    //  内部方法
    // ═══════════════════════════════════════

    private fun saveHistory(formula: ColorFormula) {
        history.add(formula)
        if (history.size > maxHistory) {
            history.removeFirst()
        }
        _state.update { it.copy(canUndo = history.isNotEmpty(), historyIndex = history.size - 1) }
    }

    private fun schedulePreviewUpdate() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            // 150ms 防抖
            delay(150)
            updatePreview()
        }
    }

    private fun updatePreview() {
        val state = _state.value
        val original = state.originalBitmap ?: return

        _state.update { it.copy(isProcessing = true) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                var result = original.copy(Bitmap.Config.ARGB_8888, true)

                // 1. 应用色彩配方
                result = imageProcessor.applyColorFormulaPreview(result, state.colorFormula)

                // 2. 应用 LUT
                if (state.lutPreset.lutData.isNotEmpty()) {
                    result = imageProcessor.applyLut(result, state.lutPreset)
                }

                // 3. 应用水印相框
                result = imageProcessor.applyWatermark(result, state.watermarkPreset)

                withContext(Dispatchers.Main) {
                    val oldPreview = _state.value.previewBitmap
                    _state.update { it.copy(previewBitmap = result, isProcessing = false) }
                    // 释放旧预览
                    if (oldPreview !== original && oldPreview !== result) {
                        oldPreview?.recycle()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isProcessing = false) }
                }
            }
        }
    }

    /**
     * 最终导出 — 全分辨率处理
     */
    fun exportImage(): Bitmap? {
        val state = _state.value
        val original = state.originalBitmap ?: return null

        _state.update { it.copy(isExporting = true) }

        return try {
            var result = original.copy(Bitmap.Config.ARGB_8888, true)
            result = imageProcessor.applyColorFormula(result, state.colorFormula)
            if (state.lutPreset.lutData.isNotEmpty()) {
                result = imageProcessor.applyLut(result, state.lutPreset)
            }
            result = imageProcessor.applyWatermark(result, state.watermarkPreset)
            result
        } finally {
            _state.update { it.copy(isExporting = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _state.value.originalBitmap?.recycle()
        _state.value.previewBitmap?.recycle()
    }
}
