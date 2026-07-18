package com.picall.app.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.picall.app.data.model.*
import com.picall.app.data.repository.PresetRepository
import com.picall.app.imageprocessing.ImageProcessor
import com.picall.app.imageprocessing.lut.LutParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EditorState(
    val originalBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val sourceUri: Uri? = null,
    val isProcessing: Boolean = false,
    val isExporting: Boolean = false,

    val colorFormula: ColorFormula = ColorFormula.DEFAULT,
    val lutPreset: LutPreset = LutPreset.DEFAULT,

    val activeTab: EditorTab = EditorTab.COLOR_FORMULA,
    val expandedCategory: String? = null,
    val canUndo: Boolean = false,

    val selectedCurveChannel: String = "W"
)

enum class EditorTab { COLOR_FORMULA, PRESETS }

class EditorViewModel(
    private val repository: PresetRepository
) : ViewModel() {

    private val processor = ImageProcessor()
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    val colorFormulaPresets: StateFlow<List<Preset>> = repository.getColorFormulas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lutPresets: StateFlow<List<Preset>> = repository.getLuts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val history = mutableListOf<ColorFormula>()
    private var previewJob: Job? = null

    fun loadImage(bitmap: Bitmap, uri: Uri? = null) {
        history.clear()
        _state.update { EditorState(originalBitmap = bitmap, previewBitmap = bitmap, sourceUri = uri) }
        triggerPreview()
    }

    fun setActiveTab(tab: EditorTab) {
        _state.update { it.copy(activeTab = tab) }
    }

    fun toggleCategory(cat: String) {
        _state.update {
            it.copy(expandedCategory = if (it.expandedCategory == cat) null else cat)
        }
    }

    fun setCurveChannel(channel: String) {
        _state.update { it.copy(selectedCurveChannel = channel) }
    }

    // ── Color Formula ──

    fun updateFormula(transform: ColorFormula.() -> ColorFormula) {
        val current = _state.value.colorFormula
        history.add(current)
        if (history.size > 20) history.removeFirst()
        _state.update { it.copy(colorFormula = current.transform(), canUndo = history.isNotEmpty()) }
        triggerPreview()
    }

    fun undo() {
        if (history.isNotEmpty()) {
            val prev = history.removeLast()
            _state.update { it.copy(colorFormula = prev, canUndo = history.isNotEmpty()) }
            triggerPreview()
        }
    }

    fun resetFormula() {
        history.add(_state.value.colorFormula)
        _state.update { it.copy(colorFormula = ColorFormula.DEFAULT, lutPreset = LutPreset.DEFAULT, canUndo = true) }
        triggerPreview()
    }

    fun selectFilmPreset(formula: ColorFormula, presetId: String) {
        if (presetId == "none") {
            // Hard reset: force all params to zero, clear LUT
            _state.update { it.copy(
                colorFormula = ColorFormula.DEFAULT,
                lutPreset = LutPreset.DEFAULT,
                canUndo = true
            )}
        } else {
            _state.update { it.copy(colorFormula = formula) }
        }
        triggerPreview()
    }

    // ── LUT ──

    fun importLut(content: String, fileName: String) {
        val data = LutParser.parse(content)
        val b64 = LutParser.encodeToBase64(data)
        _state.update {
            it.copy(lutPreset = LutPreset(
                lutData = b64, lutSize = data.size,
                sourceFile = fileName, name = data.title.ifEmpty { fileName }
            ))
        }
        triggerPreview()
    }

    fun setLutIntensity(v: Float) {
        _state.update { it.copy(lutPreset = it.lutPreset.copy(intensity = v)) }
        triggerPreview()
    }

    fun removeLut() {
        _state.update { it.copy(lutPreset = LutPreset.DEFAULT) }
        triggerPreview()
    }

    fun importLutFromBase64(data: String, name: String) {
        _state.update { it.copy(lutPreset = LutPreset(lutData = data, name = name, intensity = 1f)) }
        triggerPreview()
    }

    // ── Presets ──

    private val _presetSaveResult = MutableStateFlow<String?>(null)
    val presetSaveResult: StateFlow<String?> = _presetSaveResult.asStateFlow()

    fun savePreset(name: String, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = _state.value
            val existing = repository.getAllPresets().first().any { it.name == name }
            if (existing) {
                withContext(Dispatchers.Main) { onResult?.invoke(false, "预设名称已存在") }
                return@launch
            }
            val lutData = s.lutPreset.lutData
            repository.saveColorFormula(name, s.colorFormula, lutData)
            withContext(Dispatchers.Main) { onResult?.invoke(true, "预设已保存") }
        }
    }

    fun loadPreset(preset: Preset) {
        viewModelScope.launch {
            preset.toColorFormula()?.let { formula ->
                val lutData = preset.thumbnailPath
                _state.update { s -> s.copy(
                    colorFormula = formula,
                    lutPreset = if (lutData.isNotEmpty()) LutPreset(lutData = lutData, intensity = 1f, name = preset.name)
                                else LutPreset.DEFAULT
                )}
                triggerPreview()
            }
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch { repository.deletePreset(id) }
    }

    // ── Export ──

    suspend fun exportAsync(): Bitmap? {
        val s = _state.value
        val original = s.originalBitmap ?: return null
        _state.update { it.copy(isExporting = true) }
        return try {
            withContext(Dispatchers.Default) {
                processor.processExport(original, s.colorFormula, s.lutPreset, s.sourceUri)
            }
        } finally {
            _state.update { it.copy(isExporting = false) }
        }
    }

    // ── Internal ──

    private fun triggerPreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(80) // debounce: balance responsiveness vs curve drag smoothness
            val s = _state.value
            val original = s.originalBitmap ?: return@launch
            _state.update { it.copy(isProcessing = true) }

            val result = withContext(Dispatchers.Default) {
                processor.processPreview(original, s.colorFormula, s.lutPreset, s.sourceUri)
            }
            _state.update { it.copy(previewBitmap = result, isProcessing = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _state.value.originalBitmap?.recycle()
        _state.value.previewBitmap?.recycle()
    }
}

class EditorViewModelFactory(
    private val repo: PresetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditorViewModel(repo) as T
    }
}
