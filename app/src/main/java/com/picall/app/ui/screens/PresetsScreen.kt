package com.picall.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picall.app.data.local.PresetDatabase
import com.picall.app.data.model.Preset
import com.picall.app.data.model.PresetType
import com.picall.app.data.repository.PresetRepository
import com.picall.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsScreen(onNavigateBack: () -> Unit, onSelectPreset: ((Preset) -> Unit)? = null) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { PresetDatabase.getInstance(ctx) }
    val repo = remember { PresetRepository(db) }
    var type by remember { mutableStateOf(PresetType.COLOR_FORMULA) }
    var query by remember { mutableStateOf("") }

    val all by repo.getAllPresets().collectAsState(initial = emptyList())
    val filtered = remember(all, type, query) {
        all.filter {
            (type == PresetType.COLOR_FORMULA || it.type == type) &&
            (query.isBlank() || it.name.contains(query, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("胶卷库", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("搜索预设...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true, shape = RoundedCornerShape(12.dp))

            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(type == PresetType.COLOR_FORMULA, { type = PresetType.COLOR_FORMULA },
                    label = { Text("色彩配方", fontSize = 12.sp) }, modifier = Modifier.weight(1f))
                FilterChip(type == PresetType.LUT, { type = PresetType.LUT },
                    label = { Text("LUT", fontSize = 12.sp) }, modifier = Modifier.weight(1f))
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Bookmarks, null, Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        Text("暂无预设", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("去编辑器创建一个吧", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { preset ->
                        Card(shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            onClick = { onSelectPreset?.invoke(preset) }
                        ) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                // Icon placeholder
                                Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp))
                                    .background(SliderActive.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (preset.type == PresetType.LUT) Icons.Outlined.Gradient else Icons.Outlined.Palette,
                                        null, Modifier.size(36.dp),
                                        tint = if (preset.type == PresetType.LUT) SliderActive.copy(alpha = 0.3f) else Color(0xFFE94560).copy(alpha = 0.3f)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(preset.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (preset.type == PresetType.LUT) "3D LUT文件" else "色彩配方",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End) {
                                    TextButton({ scope.launch { repo.deletePreset(preset.id) } },
                                        contentPadding = PaddingValues(2.dp)) {
                                        Icon(Icons.Default.Delete, null, Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
