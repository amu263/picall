package com.picall.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.picall.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── 常规设置 ──
            SettingsSection("常规")

            SettingsItem(
                icon = Icons.Outlined.Palette,
                title = "主题",
                subtitle = "深色 / 浅色 / 跟随系统",
                trailing = { Text("跟随系统", style = MaterialTheme.typography.bodySmall) }
            )

            SettingsItem(
                icon = Icons.Outlined.Language,
                title = "语言",
                subtitle = "界面语言设置",
                trailing = { Text("中文", style = MaterialTheme.typography.bodySmall) }
            )

            // ── 导出设置 ──
            SettingsSection("导出")

            SettingsItem(
                icon = Icons.Outlined.PhotoSizeSelectLarge,
                title = "导出质量",
                subtitle = "JPEG 压缩质量",
                trailing = { Text("95%", style = MaterialTheme.typography.bodySmall) }
            )

            SettingsItem(
                icon = Icons.Outlined.AspectRatio,
                title = "最大分辨率",
                subtitle = "导出图片的最大边长",
                trailing = { Text("4096px", style = MaterialTheme.typography.bodySmall) }
            )

            SettingsItem(
                icon = Icons.Outlined.Watermark,
                title = "保留EXIF信息",
                subtitle = "导出时保留原始照片的EXIF数据",
                trailing = {
                    var checked by remember { mutableStateOf(true) }
                    Switch(checked = checked, onCheckedChange = { checked = it })
                }
            )

            // ── 编辑设置 ──
            SettingsSection("编辑")

            SettingsItem(
                icon = Icons.Outlined.TouchApp,
                title = "实时预览",
                subtitle = "调节参数时实时更新预览图像",
                trailing = {
                    var checked by remember { mutableStateOf(true) }
                    Switch(checked = checked, onCheckedChange = { checked = it })
                }
            )

            SettingsItem(
                icon = Icons.Outlined.Speed,
                title = "预览分辨率",
                subtitle = "实时预览时使用的图像分辨率",
                trailing = { Text("1024px", style = MaterialTheme.typography.bodySmall) }
            )

            // ── 关于 ──
            SettingsSection("关于")

            SettingsItem(
                icon = Icons.Outlined.Info,
                title = "版本",
                subtitle = "Picall v1.0.0",
                trailing = {}
            )

            SettingsItem(
                icon = Icons.Outlined.Description,
                title = "开源许可",
                subtitle = "查看使用的开源库许可信息",
                trailing = {}
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = SliderActive,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            trailing()
        }
    }
}
