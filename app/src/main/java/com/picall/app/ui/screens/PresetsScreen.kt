package com.picall.app.ui.screens

import androidx.compose.foundation.background
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
fun PresetsScreen(onNavigateBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { PresetDatabase.getInstance(ctx) }
    val repo = remember { PresetRepository(db) }
    var query by remember { mutableStateOf("") }

    val all by repo.getAllPresets().collectAsState(initial = emptyList())
    val filtered = remember(all, query) {
        all.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
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

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Bookmarks, null, Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        Text("暂无预设", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("在编辑器中保存的预设将显示在这里", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
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
                        val typeLabel = when (preset.type) {
                            PresetType.COLOR_FORMULA -> if (preset.thumbnailPath.isNotEmpty()) "含LUT" else "色彩配方"
                            PresetType.LUT -> "3D LUT"
                            PresetType.WATERMARK -> "水印相框"
                        }
                        Card(shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp))
                                    .background(SliderActive.copy(alpha = 0.06f)),
                                    contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (preset.type == PresetType.LUT) Icons.Outlined.Gradient else Icons.Outlined.Palette,
                                        null, Modifier.size(36.dp),
                                        tint = SliderActive.copy(alpha = 0.35f)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(preset.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(typeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                TextButton(
                                    { scope.launch { repo.deletePreset(preset.id) } },
                                    Modifier.fillMaxWidth(), contentPadding = PaddingValues(2.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                    Spacer(Modifier.width(4.dp))
                                    Text("删除", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
