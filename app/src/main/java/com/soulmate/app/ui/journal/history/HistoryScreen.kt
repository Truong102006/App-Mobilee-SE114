package com.soulmate.app.ui.journal.history

import MonthYearPickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soulmate.app.R
import com.soulmate.app.ui.home.components.RecordingNote
import java.util.Calendar

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToEdit: (String) -> Unit
) {
    val notes = viewModel.historyNotes
    var noteToDelete by remember { mutableStateOf<RecordingNote?>(null) }

    val calendar = Calendar.getInstance()
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val filteredNotes by remember(selectedMonth, selectedYear, notes) {
        derivedStateOf {
            val monthStr = selectedMonth.toString().padStart(2, '0')
            val targetPattern = "/$monthStr/$selectedYear"
            notes.filter { it.dateTime.contains(targetPattern) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Background Image
        Image(
            painter = painterResource(id = R.drawable.img_3),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(40.dp))

            // 2. Greeting Header
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "Chào buổi sáng! 🌸",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A5BAF)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Hôm nay là một ngày tuyệt vời để ghi lại những khoảnh khắc đáng nhớ",
                    fontSize = 16.sp,
                    color = Color(0xFF1A5BAF).copy(alpha = 0.8f),
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Main White Container
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                color = Color.White
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Date Selector (Pill design)
                    Surface(
                        modifier = Modifier
                            .wrapContentWidth()
                            .clickable { showMonthPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F1FF)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF5B9DFF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tháng $selectedMonth, $selectedYear",
                                color = Color(0xFF5B9DFF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Timeline List
                    if (filteredNotes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Không có nhật ký nào", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            itemsIndexed(filteredNotes, key = { _, note -> note.id }) { index, note ->
                                Timeline(
                                    item = note,
                                    isLastItem = index == filteredNotes.lastIndex,
                                    onDelete = { noteToDelete = note },
                                    onEdit = { onNavigateToEdit(note.diaryId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Xác nhận xóa", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa mục nhật ký này không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(noteToDelete!!)
                        noteToDelete = null
                    }
                ) {
                    Text("Xóa", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Hủy", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentMonth = selectedMonth,
            currentYear = selectedYear,
            onDismiss = { showMonthPicker = false },
            onConfirm = { newMonth, newYear ->
                selectedMonth = newMonth
                selectedYear = newYear
                showMonthPicker = false
            }
        )
    }
}
