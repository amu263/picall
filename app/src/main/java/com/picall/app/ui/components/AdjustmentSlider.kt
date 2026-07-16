package com.picall.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picall.app.ui.theme.*

/**
 * 通用调节滑块 — 带有标签、数值显示和重置按钮
 *
 * @param label 滑块标签
 * @param value 当前值 (-1.0 .. 1.0)
 * @param onValueChange 值变化回调
 * @param valueRange 值范围
 * @param displayValue 显示值文本 (可选，默认显示百分比)
 * @param onReset 重置回调 (可选)
 * @param enabled 是否启用
 */
@Composable
fun AdjustmentSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = -1f..1f,
    displayValue: String? = null,
    onReset: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val sliderColor by animateColorAsState(
        if (enabled) SliderActive else SliderTrack,
        label = "sliderColor"
    )

    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayValue ?: formatValue(value, valueRange),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp)
                )

                if (onReset != null && value != 0f) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重置",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = SliderThumb,
                activeTrackColor = sliderColor,
                inactiveTrackColor = SliderTrack
            )
        )
    }
}

/**
 * 强度百分比滑块 — 专门用于调节各组的总强度
 */
@Composable
fun IntensitySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "强度"
) {
    AdjustmentSlider(
        label = label,
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..1f,
        displayValue = "${(value * 100).toInt()}%",
        modifier = modifier
    )
}

/**
 * 双端调节滑块 — 支持负值和正值 (如色温：冷-暖)
 */
@Composable
fun BidirectionalSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    negativeLabel: String = "",
    positiveLabel: String = "",
    onReset: (() -> Unit)? = null
) {
    val sliderColor by animateColorAsState(
        if (value != 0f) SliderActive else SliderTrack,
        label = "biSliderColor"
    )

    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${(value * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp)
                )

                if (onReset != null && value != 0f) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重置",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (negativeLabel.isNotEmpty()) {
                Text(
                    text = negativeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.width(32.dp)
                )
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = -1f..1f,
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = SliderThumb,
                    activeTrackColor = sliderColor,
                    inactiveTrackColor = SliderTrack
                )
            )

            if (positiveLabel.isNotEmpty()) {
                Text(
                    text = positiveLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.width(32.dp)
                )
            }
        }
    }
}

private fun formatValue(value: Float, range: ClosedFloatingPointRange<Float>): String {
    val maxAbs = maxOf(kotlin.math.abs(range.start), kotlin.math.abs(range.endInclusive))
    return "${(value / maxAbs * 100).toInt()}%"
}
