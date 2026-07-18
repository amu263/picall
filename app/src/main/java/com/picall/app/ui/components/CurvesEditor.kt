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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picall.app.data.model.CurvePoint
import com.picall.app.ui.theme.*

@Composable
fun CurvesEditor(
    channelLabel: String,
    points: List<CurvePoint>,
    onPointsChanged: (List<CurvePoint>) -> Unit,
    selectedChannel: String = "W",
    onChannelChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedPointIndex by remember { mutableStateOf(-1) }
    var dragValue by remember { mutableStateOf("") }
    val sorted = remember(points) { points.sortedBy { it.x } }

    // Stable refs to prevent pointerInput restart during drag
    val currentSorted by rememberUpdatedState(sorted)
    val currentSelectedIndex by rememberUpdatedState(selectedPointIndex)

    Column(modifier) {
        // Channel buttons
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("W" to CurveChannelW, "R" to CurveChannelR, "G" to CurveChannelG, "B" to CurveChannelB).forEach { (ch, color) ->
                FilterChip(selectedChannel == ch, { onChannelChanged(ch) },
                    label = { Text(ch, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f).height(28.dp))
            }
        }

        // Canvas — pointerInput(Unit) + rememberUpdatedState prevents gesture restart
        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF111111))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = currentSorted
                    val ptSize = 20f
                    val hit = s.indexOfFirst { pt ->
                        val px = pt.x * size.width; val py = (1f - pt.y) * size.height
                        (offset - Offset(px, py)).getDistance() < ptSize
                    }
                    if (hit >= 0) selectedPointIndex = hit
                    else {
                        val nx = (offset.x / size.width).coerceIn(0f, 1f)
                        val ny = (1f - offset.y / size.height).coerceIn(0f, 1f)
                        val updated = (s + CurvePoint(nx, ny)).sortedBy { it.x }
                        selectedPointIndex = updated.indexOfFirst { it.x == nx }
                        onPointsChanged(updated)
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val s = currentSorted
                        val ptSize = 20f
                        selectedPointIndex = s.indexOfFirst { pt ->
                            val px = pt.x * size.width; val py = (1f - pt.y) * size.height
                            (offset - Offset(px, py)).getDistance() < ptSize
                        }
                    },
                    onDragEnd = { selectedPointIndex = -1; dragValue = "" },
                    onDragCancel = { selectedPointIndex = -1; dragValue = "" }
                ) { change, _ ->
                    val idx = currentSelectedIndex
                    val s = currentSorted
                    if (idx in s.indices) {
                        change.consume()
                        val isEndpoint = idx == 0 || idx == s.size - 1
                        val nx = if (isEndpoint) s[idx].x
                        else (change.position.x / size.width).coerceIn(
                            s.getOrElse(idx - 1) { s.first() }.x + 0.02f,
                            s.getOrElse(idx + 1) { s.last() }.x - 0.02f)
                        val ny = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        val updated = s.toMutableList()
                        updated[idx] = CurvePoint(nx, ny)
                        onPointsChanged(updated)
                        dragValue = "X: ${"%.2f".format(nx)} Y: ${"%.2f".format(ny)}"
                    }
                }
            }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width; val ch = size.height

            // Grid
            for (i in 1..3) {
                val x = cw * i / 4f; drawLine(Color(0xFF333333), Offset(x, 0f), Offset(x, ch), 1f)
                val y = ch * i / 4f; drawLine(Color(0xFF333333), Offset(0f, y), Offset(cw, y), 1f)
            }
            drawLine(Color(0xFF444444), Offset(0f, ch), Offset(cw, 0f), 1f)

            // Spline curve
            if (sorted.size >= 2 && cw > 0) {
                val path = Path()
                for (i in 0 until 256) {
                    val t = i / 255f; val y = evaluateSpline(sorted, t)
                    val px = t * cw; val py = (1f - y) * ch
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(path, CurveLine, style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            // Control points
            sorted.forEachIndexed { index, pt ->
                val cx = pt.x * cw; val cy = (1f - pt.y) * ch
                val sel = index == selectedPointIndex; val r = if (sel) 10f else 7f; val a = if (sel) 1f else 0.8f
                drawCircle(CurvePointColor.copy(alpha = a), r + 2f, Offset(cx, cy))
                drawCircle(Color.White.copy(alpha = a), r, Offset(cx, cy))
            }

            // Value indicator
            if (dragValue.isNotEmpty()) {
                drawRoundRect(Color.Black.copy(alpha = 0.7f), topLeft = Offset(4f, 4f),
                    size = Size(dragValue.length * 8f + 16f, 20f), cornerRadius = CornerRadius(4f))
            }
            } // Canvas closes
        } // Box closes

        // Drag value
        if (dragValue.isNotEmpty()) {
            Text(dragValue, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 4.dp))
        }

        // Preset buttons + reset
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton({ onPointsChanged(sCurvePreset()) }, Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.textButtonColors(contentColor = SliderActive)) {
                Text("S曲线", fontSize = 10.sp)
            }
            TextButton({ onPointsChanged(liftShadowsPreset()) }, Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp)) {
                Text("提亮暗部", fontSize = 10.sp)
            }
            TextButton({ onPointsChanged(crushHighlightsPreset()) }, Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp)) {
                Text("压暗高光", fontSize = 10.sp)
            }
            TextButton({
                onPointsChanged(listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)))
            }, Modifier.weight(1f), shape = RoundedCornerShape(6.dp)) {
                Text("重置", fontSize = 10.sp)
            }
        }
    }
}

fun sCurvePreset() = listOf(CurvePoint(0f, 0f), CurvePoint(0.25f, 0.15f), CurvePoint(0.5f, 0.5f), CurvePoint(0.75f, 0.85f), CurvePoint(1f, 1f))
fun liftShadowsPreset() = listOf(CurvePoint(0f, 0f), CurvePoint(0.15f, 0.22f), CurvePoint(0.5f, 0.5f), CurvePoint(1f, 1f))
fun crushHighlightsPreset() = listOf(CurvePoint(0f, 0f), CurvePoint(0.5f, 0.5f), CurvePoint(0.75f, 0.68f), CurvePoint(1f, 1f))

fun evaluateSpline(points: List<CurvePoint>, t: Float): Float {
    val sorted = points.sortedBy { it.x }
    if (sorted.isEmpty()) return t
    if (sorted.size == 1) return sorted[0].y
    if (t <= sorted.first().x) return sorted.first().y
    if (t >= sorted.last().x) return sorted.last().y

    var seg = 0
    for (i in 0 until sorted.size - 1) if (t in sorted[i].x..sorted[i + 1].x) { seg = i; break }

    val p0 = sorted.getOrElse(seg - 1) { sorted[0] }; val p1 = sorted[seg]
    val p2 = sorted[seg + 1]; val p3 = sorted.getOrElse(seg + 2) { sorted.last() }
    val lt = if (p2.x != p1.x) (t - p1.x) / (p2.x - p1.x) else 0f
    val t2 = lt * lt; val t3 = t2 * lt
    return (0.5f * ((2f * p1.y) + (-p0.y + p2.y) * lt +
            (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
            (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3)).coerceIn(0f, 1f)
}

private val CurvePointColor = Color(0xFFE94560)
