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
import androidx.compose.ui.graphics.Brush
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
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf

data class DonutData(val label: String, val percentage: Float, val color: Color)
data class DailyMoodData(val dayLabel: String, val moodScore: Float)

@Composable
fun MoodDonutChart(
    data: List<DonutData>,
    emptyMessage: String = "Chưa có dữ liệu"
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val nonEmptyData = data.filter { it.percentage > 0f }

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
            if (nonEmptyData.isEmpty()) {
                Text(
                    text = emptyMessage,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            } else {
                Canvas(modifier = Modifier.size(160.dp)) {
                    var startAngle = -90f

                    nonEmptyData.forEach { item ->
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
                    Text(
                        text = nonEmptyData.maxByOrNull { it.percentage }?.label ?: "--",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colors.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            nonEmptyData.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(item.color)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "${item.label} ${item.percentage.toInt()}%",
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MoodLineChart(
    data: List<DailyMoodData>,
    emptyMessage: String = "Chưa có dữ liệu"
) {
    val hasRealData = data.any { it.moodScore > 0f }
    if (data.isEmpty() || !hasRealData) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
        return
    }

    val entries = data.mapIndexed { index, dailyMood ->
        FloatEntry(x = index.toFloat(), y = dailyMood.moodScore)
    }
    val chartEntryModel = entryModelOf(entries)

    val primaryColor = MaterialTheme.colors.primary

    Chart(
        chart = lineChart(
            axisValuesOverrider = AxisValuesOverrider.fixed(minY = 0f, maxY = 5f),
            lines = listOf(
                lineSpec(
                    lineColor = primaryColor,
                    lineThickness = 3.dp,
                    lineBackgroundShader = DynamicShaders.fromBrush(
                        brush = Brush.verticalGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.4f), 
                                primaryColor.copy(alpha = 0.0f) 
                            )
                        )
                    )
                )
            )
        ),
        model = chartEntryModel,
        startAxis = rememberStartAxis(
            valueFormatter = { value, _ ->
                when (value.toInt()) {
                    0 -> ""
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
                val index = value.toInt()
                if (index >= 0 && index < data.size) {
                    data[index].dayLabel
                } else {
                    ""
                }
            }
        ),
        modifier = Modifier.fillMaxWidth().height(220.dp)
    )
}

