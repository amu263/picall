package com.picall.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picall.app.data.model.ColorFormula
import com.picall.app.ui.theme.SliderActive

@Composable
fun FilmCard(
    name: String,
    formula: ColorFormula,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = remember(formula) { formulaToGradient(formula) }

    Column(
        modifier = modifier.width(72.dp).clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Film strip gradient
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            // Perforation dots
            Column(Modifier.fillMaxHeight().padding(vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceEvenly) {
                repeat(6) {
                    Box(Modifier.size(5.dp).clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.3f)))
                }
            }
        }

        // Name area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) SliderActive.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                )
                .padding(horizontal = 4.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) SliderActive else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun formulaToGradient(formula: ColorFormula): List<Color> {
    val temp = formula.colorTemperature
    val tint = formula.tint
    val sat = formula.saturation
    val exp = formula.exposure
    val fade = formula.fade

    val baseR = (128 + temp * 40 - tint * 20 + exp * 30).toInt().coerceIn(0, 255)
    val baseG = (128 + tint * 30 - sat * 20 + exp * 20).toInt().coerceIn(0, 255)
    val baseB = (128 - temp * 40 + tint * 20 + exp * 30).toInt().coerceIn(0, 255)

    val fadeMix = (fade * 180).toInt().coerceIn(0, 255)

    val midR = (baseR + (255 - baseR) * 0.4f).toInt()
    val midG = (baseG + (255 - baseG) * 0.4f).toInt()
    val midB = (baseB + (255 - baseB) * 0.4f).toInt()

    return listOf(
        Color(baseR, baseG, baseB),
        Color(midR, midG, midB),
        Color((baseR + fadeMix).coerceIn(0, 255), (baseG + fadeMix).coerceIn(0, 255), (baseB + fadeMix).coerceIn(0, 255)),
        Color(midR, midG, midB),
        Color(baseR, baseG, baseB)
    )
}
