package com.soulmate.app.ui.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.entryModelOf


data class DonutData(val label: String, val percentage: Float, val color: Color)

@Composable
fun MoodDonutChart(data: List<DonutData>) {
    var animationPlayed by remember { mutableStateOf(false) }

    val animateSweep by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "DonutAnimation"
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(160.dp)) {
                var startAngle = -90f

                data.forEach { item ->
                    val sweepAngle = (item.percentage / 100f) * 360f * animateSweep

                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 32.dp.toPx(), cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Chủ yếu", color = Color.Gray, fontSize = 12.sp)
                Text(text = data.maxByOrNull { it.percentage }?.label ?: "", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colors.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { item ->
                if (item.percentage > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(item.color))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${item.label} ${item.percentage.toInt()}%", fontSize = 12.sp, color = MaterialTheme.colors.surface.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
fun MoodLineChart() {
    // Data mẫu: Trục X là ngày, Trục Y là điểm cảm xúc (1: Giận dữ -> 5: Vui vẻ)
    // entryModelOf( x to y, x to y... )
    val chartEntryModel = entryModelOf(
        1f to 3f, // Thứ 2: Neutral
        2f to 5f, // Thứ 3: Happy
        3f to 4f, // Thứ 4: Satisfied
        4f to 2f, // Thứ 5: Sad
        5f to 4f, // Thứ 6: Satisfied
        6f to 5f, // Thứ 7: Happy
        7f to 5f  // CN: Happy
    )

    Chart(
        chart = lineChart(
            axisValuesOverrider = AxisValuesOverrider.fixed(minY = 1f, maxY = 5f)
        ),
        model = chartEntryModel,
        startAxis = rememberStartAxis(
            valueFormatter = { value, _ ->
                when (value.toInt()) {
                    1 -> "Angry"
                    2 -> "Sad"
                    3 -> "Neutral"
                    4 -> "Satisfied"
                    5 -> "Happy"
                    else -> ""
                }
            }
        ),
        bottomAxis = rememberBottomAxis(
            valueFormatter = { value, _ ->
                "T${value.toInt() + 1}"
            }
        ),
        modifier = Modifier.fillMaxWidth().height(220.dp)
    )
}