package com.picall.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.picall.app.data.model.ColorFormula
import com.picall.app.data.model.Preset

data class FilmPreset(
    val name: String,
    val formula: ColorFormula,
    val lutData: String = ""
)

@Composable
fun FilmStripRow(
    presets: List<FilmPreset>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (presets.isEmpty()) return

    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex.coerceIn(0, presets.size - 1))
    }

    Column(modifier) {
        Text(
            "胶片预设",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(presets.size) { index ->
                val preset = presets[index]
                FilmCard(
                    name = preset.name,
                    formula = preset.formula,
                    isSelected = index == selectedIndex,
                    onClick = { onSelect(index) }
                )
            }

            item {
                Spacer(Modifier.width(4.dp))
            }
        }

        if (selectedIndex in presets.indices) {
            Text(
                presets[selectedIndex].name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

fun defaultFilmPresets(): List<FilmPreset> = listOf(
    FilmPreset("无", ColorFormula.DEFAULT),
    FilmPreset("暖日", ColorFormula.DEFAULT.copy(
        colorTemperature = 0.5f, saturation = 0.2f, exposure = 0.15f, contrast = 0.1f)),
    FilmPreset("清冷", ColorFormula.DEFAULT.copy(
        colorTemperature = -0.4f, tint = 0.1f, saturation = -0.1f, highlights = -0.1f)),
    FilmPreset("复古", ColorFormula.DEFAULT.copy(
        saturation = -0.3f, fade = 0.4f, contrast = -0.1f, shadows = 0.2f, colorTemperature = 0.3f)),
    FilmPreset("胶片", ColorFormula.DEFAULT.copy(
        saturation = 0.15f, contrast = 0.25f, fade = 0.15f, silverRetention = 0.3f, shadows = 0.1f)),
    FilmPreset("黑白", ColorFormula.DEFAULT.copy(
        saturation = -1f, contrast = 0.3f, exposure = 0.1f)),
    FilmPreset("鲜明", ColorFormula.DEFAULT.copy(
        saturation = 0.5f, contrast = 0.2f, exposure = 0.1f, highlights = -0.2f)),
    FilmPreset("暗调", ColorFormula.DEFAULT.copy(
        exposure = -0.3f, contrast = 0.15f, shadows = -0.2f, saturation = -0.2f)),
    FilmPreset("清新", ColorFormula.DEFAULT.copy(
        exposure = 0.2f, saturation = 0.1f, highlights = -0.15f, shadows = 0.15f, colorTemperature = -0.1f)),
    FilmPreset("暖金", ColorFormula.DEFAULT.copy(
        colorTemperature = 0.7f, tint = 0.15f, saturation = 0.25f, exposure = 0.1f))
)
