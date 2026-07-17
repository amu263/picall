package com.picall.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.picall.app.data.model.CurvePoint
import com.picall.app.ui.theme.*

/**
 * WRGB 曲线编辑器 — 支持 4 通道 (W/R/G/B) 交互式曲线编辑
 *
 * 用户可以在曲线上点击添加控制点，拖拽移动控制点，
 * 使用 Catmull-Rom 样条在控制点之间平滑插值
 */
@Composable
fun CurvesEditor(
    channelLabel: String,
    points: List<CurvePoint>,
    onPointsChanged: (List<CurvePoint>) -> Unit,
    modifier: Modifier = Modifier,
    lineColor: Color = CurveLine,
    gridColor: Color = CurveGrid
) {
    var selectedPointIndex by remember { mutableStateOf(-1) }
    val sortedPoints = remember(points) { points.sortedBy { it.x } }

    Column(modifier = modifier) {
        // 通道标签
        Text(
            text = channelLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CurveBackground)
                .pointerInput(sortedPoints) {
                    detectTapGestures { offset ->
                        // 检查是否点击了现有点
                        val pointSize = 24f
                        val clickedIndex = sortedPoints.indexOfFirst { pt ->
                            val px = pt.x * size.width
                            val py = (1f - pt.y) * size.height
                            (offset - Offset(px, py)).getDistance() < pointSize
                        }
                        if (clickedIndex >= 0) {
                            selectedPointIndex = clickedIndex
                        } else {
                            // 添加新控制点
                            val newX = (offset.x / size.width).coerceIn(0f, 1f)
                            val newY = (1f - offset.y / size.height).coerceIn(0f, 1f)
                            val newPoint = CurvePoint(newX, newY)
                            val newPoints = (sortedPoints + newPoint).sortedBy { it.x }
                            selectedPointIndex = newPoints.indexOf(newPoint)
                            onPointsChanged(newPoints)
                        }
                    }
                }
                .pointerInput(sortedPoints, selectedPointIndex) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val pointSize = 24f
                            selectedPointIndex = sortedPoints.indexOfFirst { pt ->
                                val px = pt.x * size.width
                                val py = (1f - pt.y) * size.height
                                (offset - Offset(px, py)).getDistance() < pointSize
                            }
                        },
                        onDragEnd = { selectedPointIndex = -1 },
                        onDragCancel = { selectedPointIndex = -1 }
                    ) { change, _ ->
                        if (selectedPointIndex in sortedPoints.indices) {
                            change.consume()
                            // 不能移动首尾点的 X 坐标
                            val isEndpoint = selectedPointIndex == 0 || selectedPointIndex == sortedPoints.size - 1
                            val newX = if (isEndpoint) {
                                sortedPoints[selectedPointIndex].x
                            } else {
                                (change.position.x / size.width)
                                    .coerceIn(
                                        sortedPoints.getOrElse(selectedPointIndex - 1) { sortedPoints.first() }.x + 0.02f,
                                        sortedPoints.getOrElse(selectedPointIndex + 1) { sortedPoints.last() }.x - 0.02f
                                    )
                            }
                            val newY = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                            val updated = sortedPoints.toMutableList()
                            updated[selectedPointIndex] = CurvePoint(newX, newY)
                            onPointsChanged(updated)
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 网格线
            for (i in 1..3) {
                val x = canvasWidth * i / 4f
                drawLine(gridColor, Offset(x, 0f), Offset(x, canvasHeight), 1f)
                val y = canvasHeight * i / 4f
                drawLine(gridColor, Offset(0f, y), Offset(canvasWidth, y), 1f)
            }

            // 对角线参考线
            drawLine(
                gridColor.copy(alpha = 0.3f),
                Offset(0f, canvasHeight),
                Offset(canvasWidth, 0f),
                1f
            )

            // 样条曲线
            if (sortedPoints.size >= 2 && canvasWidth > 0) {
                val path = Path()
                for (i in 0 until 256) {
                    val t = i / 255f
                    val y = evaluateSpline(sortedPoints, t)
                    val px = t * canvasWidth
                    val py = (1f - y) * canvasHeight
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(path, lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            // 控制点
            sortedPoints.forEachIndexed { index, point ->
                val cx = point.x * canvasWidth
                val cy = (1f - point.y) * canvasHeight
                val isSelected = index == selectedPointIndex
                val radius = if (isSelected) 10f else 7f
                val alpha = if (isSelected) 1f else 0.8f

                drawCircle(
                    color = CurvePoint.copy(alpha = alpha),
                    radius = radius + 2f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(cx, cy)
                )
            }
        }

        // 重置按钮
        TextButton(
            onClick = {
                onPointsChanged(
                    listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
                )
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("重置曲线", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * 使用 Catmull-Rom 样条计算曲线上指定 x 处的 y 值
 */
private fun evaluateSpline(points: List<CurvePoint>, t: Float): Float {
    val sorted = points.sortedBy { it.x }
    if (sorted.isEmpty()) return t
    if (sorted.size == 1) return sorted[0].y

    // 找到 t 所在的段
    var seg = 0
    for (i in 0 until sorted.size - 1) {
        if (t >= sorted[i].x && t <= sorted[i + 1].x) {
            seg = i
            break
        }
    }
    if (t <= sorted.first().x) return sorted.first().y
    if (t >= sorted.last().x) return sorted.last().y

    val p0 = sorted.getOrElse(seg - 1) { sorted[0] }
    val p1 = sorted[seg]
    val p2 = sorted[seg + 1]
    val p3 = sorted.getOrElse(seg + 2) { sorted.last() }

    val localT = if (p2.x != p1.x) (t - p1.x) / (p2.x - p1.x) else 0f

    // Catmull-Rom
    val t2 = localT * localT
    val t3 = t2 * localT
    val result = 0.5f * (
        (2f * p1.y) +
        (-p0.y + p2.y) * localT +
        (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
        (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3
    )

    return result.coerceIn(0f, 1f)
}

private fun DrawScope.drawLine(color: Color, start: Offset, end: Offset, width: Float) {
    drawLine(color, start, end, width)
}
