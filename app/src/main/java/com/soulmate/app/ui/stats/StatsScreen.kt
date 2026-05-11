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
import androidx.hilt.navigation.compose.hiltViewModel
import com.soulmate.app.domain.model.Diary
import com.soulmate.app.ui.theme.MoodColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimeRange(val label: String) {
    WEEK("Tuần này"),
    MONTH("Tháng này"),
    YEAR("Năm nay")
}

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    var selectedTimeRange by remember { mutableStateOf(TimeRange.WEEK) }
    val uiState by viewModel.uiState
    val chartDataByRange = remember(uiState.diaries) {
        TimeRange.entries.associateWith { range ->
            buildChartDataForRange(diaries = uiState.diaries, range = range)
        }
    }
    val selectedChartData = chartDataByRange[selectedTimeRange] ?: ChartData(emptyList(), emptyList())
    val donutData = selectedChartData.donutData
    val lineData = selectedChartData.lineData
    val emptyChartMessage = "Chưa có dữ liệu ${selectedTimeRange.label.lowercase(Locale.getDefault())}"

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

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Không thể tải dữ liệu thống kê: ${uiState.errorMessage}",
                    color = MaterialTheme.colors.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
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

                    if (uiState.isLoading) {
                        LoadingChartPlaceholder()
                    } else {
                        MoodDonutChart(
                            data = donutData,
                            emptyMessage = emptyChartMessage
                        )
                    }
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

                    if (uiState.isLoading) {
                        LoadingChartPlaceholder()
                    } else {
                        MoodLineChart(
                            data = lineData,
                            emptyMessage = emptyChartMessage
                        )
                    }
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

@Composable
private fun LoadingChartPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colors.primary)
    }
}

private data class ChartData(
    val donutData: List<DonutData>,
    val lineData: List<DailyMoodData>
)

private data class TimeBucket(
    val key: String,
    val label: String
)

private fun buildChartDataForRange(
    diaries: List<Diary>,
    range: TimeRange,
    nowMillis: Long = System.currentTimeMillis()
): ChartData {
    val buckets = when (range) {
        TimeRange.WEEK -> buildWeekBuckets(nowMillis)
        TimeRange.MONTH -> buildMonthDayBuckets(nowMillis)
        TimeRange.YEAR -> buildYearMonthBuckets(nowMillis)
    }
    val moodsByBucket = buckets.associate { it.key to mutableListOf<String>() }.toMutableMap()
    val allMoodsInRange = mutableListOf<String>()
    val dayKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val monthKeyFormatter = SimpleDateFormat("yyyy-MM", Locale.US)

    diaries.forEach { diary ->
        val bucketKey = when (range) {
            TimeRange.YEAR -> monthKeyFormatter.format(Date(diary.createdAt))
            TimeRange.WEEK, TimeRange.MONTH -> dayKeyFormatter.format(Date(diary.createdAt))
        }
        val mood = normalizeMoodForStats(diary.moodTag)
        if (moodsByBucket.containsKey(bucketKey)) {
            moodsByBucket[bucketKey]?.add(mood)
            allMoodsInRange.add(mood)
        }
    }

    val lineData = buckets.map { bucket ->
        val scores = moodsByBucket[bucket.key].orEmpty().map { moodToScore(it) }
        val averageScore = if (scores.isEmpty()) 0f else scores.average().toFloat()
        DailyMoodData(bucket.label, averageScore)
    }

    return ChartData(
        donutData = buildDonutData(allMoodsInRange),
        lineData = lineData
    )
}

private fun buildDonutData(moods: List<String>): List<DonutData> {
    val moodOrder = listOf("Happy", "Satisfied", "Neutral", "Sad", "Angry")
    val totalMoodCount = moods.size
    val moodCounts = moods.groupingBy { it }.eachCount()

    return moodOrder.mapIndexed { index, mood ->
        val count = moodCounts[mood] ?: 0
        val percentage = if (totalMoodCount == 0) 0f else (count * 100f / totalMoodCount)
        DonutData(mood, percentage, MoodColors[index])
    }
}

private fun buildWeekBuckets(nowMillis: Long): List<TimeBucket> {
    val dayKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        timeInMillis = nowMillis
    }
    val offsetToMonday = if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
        -6
    } else {
        Calendar.MONDAY - calendar.get(Calendar.DAY_OF_WEEK)
    }
    calendar.add(Calendar.DAY_OF_MONTH, offsetToMonday)
    clearTime(calendar)

    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return buildList {
        repeat(7) { index ->
            add(
                TimeBucket(
                    key = dayKeyFormatter.format(calendar.time),
                    label = labels[index]
                )
            )
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}

private fun buildMonthDayBuckets(nowMillis: Long): List<TimeBucket> {
    val dayKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    clearTime(calendar)

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    return buildList {
        repeat(daysInMonth) { index ->
            val dayNumber = index + 1
            val label = when {
                dayNumber == 1 || dayNumber == daysInMonth -> dayNumber.toString()
                dayNumber % 5 == 0 -> dayNumber.toString()
                else -> ""
            }
            add(
                TimeBucket(
                    key = dayKeyFormatter.format(calendar.time),
                    label = label
                )
            )
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}

private fun buildYearMonthBuckets(nowMillis: Long): List<TimeBucket> {
    val monthKeyFormatter = SimpleDateFormat("yyyy-MM", Locale.US)
    val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    calendar.set(Calendar.DAY_OF_YEAR, 1)
    clearTime(calendar)

    val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return buildList {
        repeat(12) { monthIndex ->
            add(
                TimeBucket(
                    key = monthKeyFormatter.format(calendar.time),
                    label = monthLabels[monthIndex]
                )
            )
            calendar.add(Calendar.MONTH, 1)
        }
    }
}

private fun clearTime(calendar: Calendar) {
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
}

private fun normalizeMoodForStats(rawMood: String?): String {
    val mood = rawMood?.trim()?.lowercase(Locale.US).orEmpty()
    if (mood.isBlank()) return "Neutral"

    return when {
        mood.contains("happy") || mood.contains("joy") || mood.contains("excited") || mood.contains("good") -> "Happy"
        mood.contains("satisfied") || mood.contains("content") || mood.contains("peaceful") || mood.contains("calm") || mood.contains("relaxed") -> "Satisfied"
        mood.contains("sad") || mood.contains("down") || mood.contains("depress") -> "Sad"
        mood.contains("angry") || mood.contains("mad") || mood.contains("frustrat") -> "Angry"
        mood.contains("neutral") || mood.contains("ok") || mood.contains("normal") -> "Neutral"
        else -> "Neutral"
    }
}

private fun moodToScore(mood: String): Int {
    return when (mood) {
        "Happy" -> 5
        "Satisfied" -> 4
        "Neutral" -> 3
        "Sad" -> 2
        "Angry" -> 1
        else -> 3
    }
}
