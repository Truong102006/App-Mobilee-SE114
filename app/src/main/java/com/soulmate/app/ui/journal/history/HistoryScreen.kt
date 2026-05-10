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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
                .sortedByDescending { it.dateTime }
        }
    }

    val groupedNotes = remember(filteredNotes) {
        filteredNotes.groupBy { it.dateTime.split(" ").firstOrNull() ?: "" }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_journal),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(48.dp))

            val headerColor = Color(0xFF004BA0)
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Chào buổi sáng!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = headerColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "🌸", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Hôm nay là một ngày tuyệt vời để\nghi lại những khoảnh khắc đáng nhớ",
                    fontSize = 16.sp,
                    color = headerColor.copy(alpha = 0.7f),
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)),
                color = Color.White,
                elevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        modifier = Modifier
                            .wrapContentWidth()
                            .clickable { showMonthPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE3F2FD)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF1E88E5),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tháng $selectedMonth, $selectedYear",
                                color = Color(0xFF1E88E5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        groupedNotes.forEach { (date, notesInDate) ->
                            item {
                                Surface(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F8FE)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = Color(0xFF42A5F5),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = date,
                                            color = Color(0xFF42A5F5),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            itemsIndexed(notesInDate) { index, note ->
                                Timeline(
                                    item = note,
                                    isFirstItem = index == 0,
                                    isLastItem = index == notesInDate.lastIndex,
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
            text = { Text("Bạn có muốn xóa nhật kí này không?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteNote(noteToDelete!!); noteToDelete = null }) {
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
            onConfirm = { m, y -> selectedMonth = m; selectedYear = y; showMonthPicker = false }
        )
    }
}