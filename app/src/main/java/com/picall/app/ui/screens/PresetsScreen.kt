package com.picall.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.picall.app.data.local.PresetDatabase
import com.picall.app.data.model.Preset
import com.picall.app.data.model.PresetType
import com.picall.app.data.repository.PresetRepository
import com.picall.app.ui.components.PresetCard
import com.picall.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsScreen(onNavigateBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { PresetDatabase.getInstance(ctx) }
    val repo = remember { PresetRepository(db) }
    var type by remember { mutableStateOf(PresetType.COLOR_FORMULA) }

    val all by repo.getAllPresets().collectAsState(initial = emptyList())
    val filtered = all.filter { it.type == type }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预设管理", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(type == PresetType.COLOR_FORMULA, { type = PresetType.COLOR_FORMULA },
                    label = { Text("色彩配方") }, modifier = Modifier.weight(1f))
                FilterChip(type == PresetType.LUT, { type = PresetType.LUT },
                    label = { Text("LUT") }, modifier = Modifier.weight(1f))
                FilterChip(type == PresetType.WATERMARK, { type = PresetType.WATERMARK },
                    label = { Text("水印相框") }, modifier = Modifier.weight(1f))
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Bookmarks, null, Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        Text("暂无保存的预设", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(filtered, key = { it.id }) { preset ->
                        PresetCard(preset, {}, { scope.launch { repo.deletePreset(it.id) } },
                            { scope.launch { repo.toggleFavorite(preset.id, !preset.isFavorite) } })
                    }
                }
            }
        }
    }
}
