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
    val canUndo: Boolean = false
)

enum class EditorTab { COLOR_FORMULA, LUT, PRESETS }

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
        _state.update { it.copy(colorFormula = ColorFormula.DEFAULT, canUndo = true) }
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

    // ── Presets ──

    fun saveColorPreset(name: String) {
        viewModelScope.launch { repository.saveColorFormula(name, _state.value.colorFormula) }
    }

    fun saveLutPreset(name: String) {
        viewModelScope.launch { repository.saveLut(name, _state.value.lutPreset) }
    }

    fun loadPreset(preset: Preset) {
        viewModelScope.launch {
            when (preset.type) {
                PresetType.COLOR_FORMULA -> preset.toColorFormula()?.let {
                    _state.update { s -> s.copy(colorFormula = it) }
                    triggerPreview()
                }
                PresetType.LUT -> preset.toLutPreset()?.let {
                    _state.update { s -> s.copy(lutPreset = it) }
                    triggerPreview()
                }
                else -> {}
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
                processor.processExport(original, s.colorFormula, s.lutPreset, WatermarkPreset.DEFAULT, s.sourceUri)
            }
        } finally {
            _state.update { it.copy(isExporting = false) }
        }
    }

    // ── Internal ──

    private fun triggerPreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(80)
            val s = _state.value
            val original = s.originalBitmap ?: return@launch
            _state.update { it.copy(isProcessing = true) }

            val result = withContext(Dispatchers.Default) {
                processor.processPreview(original, s.colorFormula, s.lutPreset, WatermarkPreset.DEFAULT, s.sourceUri)
            }

            val old = _state.value.previewBitmap
            _state.update { it.copy(previewBitmap = result, isProcessing = false) }
            if (old !== original && old !== result) old?.recycle()
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
