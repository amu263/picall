package com.picall.app.data.repository

import com.picall.app.data.local.PresetDatabase
import com.picall.app.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow

class PresetRepository(private val database: PresetDatabase) {

    private val dao = database.presetDao()
    private val gson = Gson()

    // ── 色彩配方预设 ──
    fun getColorFormulas(): Flow<List<Preset>> =
        dao.getPresetsByType(PresetType.COLOR_FORMULA)

    suspend fun saveColorFormula(name: String, formula: ColorFormula, thumbnailPath: String = ""): Long {
        val json = gson.toJson(formula)
        return dao.insertPreset(
            Preset(
                name = name,
                type = PresetType.COLOR_FORMULA,
                dataJson = json,
                thumbnailPath = thumbnailPath,
                description = "色彩配方"
            )
        )
    }

    suspend fun loadColorFormula(presetId: Long): ColorFormula? {
        val preset = dao.getPresetById(presetId) ?: return null
        return preset.toColorFormula()
    }

    // ── LUT 预设 ──
    fun getLuts(): Flow<List<Preset>> =
        dao.getPresetsByType(PresetType.LUT)

    suspend fun saveLut(name: String, lut: LutPreset, thumbnailPath: String = ""): Long {
        val json = gson.toJson(lut)
        return dao.insertPreset(
            Preset(
                name = name,
                type = PresetType.LUT,
                dataJson = json,
                thumbnailPath = thumbnailPath,
                description = "LUT预设 - ${lut.sourceFile}"
            )
        )
    }

    suspend fun loadLut(presetId: Long): LutPreset? {
        val preset = dao.getPresetById(presetId) ?: return null
        return preset.toLutPreset()
    }

    // ── 水印相框预设 ──
    fun getWatermarks(): Flow<List<Preset>> =
        dao.getPresetsByType(PresetType.WATERMARK)

    suspend fun saveWatermark(name: String, watermark: WatermarkPreset, thumbnailPath: String = ""): Long {
        val json = gson.toJson(watermark)
        return dao.insertPreset(
            Preset(
                name = name,
                type = PresetType.WATERMARK,
                dataJson = json,
                thumbnailPath = thumbnailPath,
                description = "水印相框预设"
            )
        )
    }

    suspend fun loadWatermark(presetId: Long): WatermarkPreset? {
        val preset = dao.getPresetById(presetId) ?: return null
        return preset.toWatermarkPreset()
    }

    // ── 通用操作 ──
    fun getAllPresets(): Flow<List<Preset>> = dao.getAllPresets()
    fun getFavorites(): Flow<List<Preset>> = dao.getFavoritePresets()

    suspend fun deletePreset(id: Long) = dao.deletePresetById(id)
    suspend fun toggleFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)
    suspend fun getCategories(): List<String> = dao.getAllCategories()
}
