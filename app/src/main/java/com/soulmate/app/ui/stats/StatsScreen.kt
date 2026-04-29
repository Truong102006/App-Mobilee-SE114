package com.soulmate.app.ui.stats


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soulmate.app.ui.theme.MoodColors

enum class TimeRange(val label: String) {
    WEEK("Tuần này"),
    MONTH("Tháng này"),
    YEAR("Năm nay")
}

@Composable
fun StatsScreen() {
    var selectedTimeRange by remember { mutableStateOf(TimeRange.WEEK) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colors.surface)) {
                Spacer(modifier = Modifier.height(30.dp))
                TopAppBar(
                    title = { Text("Thống kê cảm xúc", fontWeight = FontWeight.Bold) },
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.primary,
                    elevation = 0.dp
                )
            }
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            TimeFilterSelector(
                selectedRange = selectedTimeRange,
                onRangeSelected = { selectedTimeRange = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .border(1.5.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Tỉ lệ cảm xúc",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = "Dữ liệu: ${selectedTimeRange.label}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val mockDonutData = listOf(
                        DonutData("Happy", 45f, MoodColors[0]),
                        DonutData("Satisfied", 25f, MoodColors[1]),
                        DonutData("Neutral", 15f, MoodColors[2]),
                        DonutData("Sad", 10f, MoodColors[3]),
                        DonutData("Angry", 5f, MoodColors[4])
                    )
                    MoodDonutChart(data = mockDonutData)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .border(1.5.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Biến động tâm lý",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = "Dữ liệu: ${selectedTimeRange.label}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val mockLineData = listOf(
                        DailyMoodData("Mon", 3f),
                        DailyMoodData("Tue", 5f),
                        DailyMoodData("Wed", 4f),
                        DailyMoodData("Thu", 2f),
                        DailyMoodData("Fri", 4f),
                        DailyMoodData("Sat", 5f),
                        DailyMoodData("Sun", 5f)
                    )
                    MoodLineChart(data = mockLineData)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun TimeFilterSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            TimeRange.entries.forEach { range ->
                val isSelected = selectedRange == range

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colors.primary else Color.Transparent,
                    animationSpec = tween(300), label = "bgColor"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.Gray,
                    animationSpec = tween(300), label = "textColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgColor)
                        .clickable { onRangeSelected(range) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.label,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}