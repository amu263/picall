package com.picall.app.data.local

import androidx.room.*
import com.picall.app.data.model.Preset
import com.picall.app.data.model.PresetType
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets WHERE type = :type ORDER BY updated_at DESC")
    fun getPresetsByType(type: PresetType): Flow<List<Preset>>

    @Query("SELECT * FROM presets ORDER BY updated_at DESC")
    fun getAllPresets(): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE is_favorite = 1 ORDER BY updated_at DESC")
    fun getFavoritePresets(): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE category = :category ORDER BY updated_at DESC")
    fun getPresetsByCategory(category: String): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getPresetById(id: Long): Preset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: Preset): Long

    @Update
    suspend fun updatePreset(preset: Preset)

    @Delete
    suspend fun deletePreset(preset: Preset)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)

    @Query("UPDATE presets SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT DISTINCT category FROM presets WHERE category != ''")
    suspend fun getAllCategories(): List<String>
}
