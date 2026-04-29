package com.soulmate.app.ui.journal.history

import MonthYearPickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.soulmate.app.ui.home.components.RecordingNote
import com.soulmate.app.ui.journal.editor.Mood
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val notes = viewModel.historyNotes
    var noteToEdit by remember { mutableStateOf<RecordingNote?>(null) }
    var noteToDelete by remember { mutableStateOf<RecordingNote?>(null) }

    val calendar = Calendar.getInstance()
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val filteredNotes = remember(notes, selectedMonth, selectedYear) {
        val monthStr = selectedMonth.toString().padStart(2, '0')
        val targetPattern = "/$monthStr/$selectedYear"

        notes.filter { it.dateTime.contains(targetPattern) }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colors.surface)) {
                Spacer(modifier = Modifier.height(30.dp))
                TopAppBar(
                    title = { Text("Lịch sử nhật ký", fontWeight = FontWeight.Bold) },
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.primary,
                    elevation = 0.dp
                )

                MonthSelector(
                    currentMonth = selectedMonth,
                    currentYear = selectedYear,
                    onPreviousMonth = {
                        if (selectedMonth == 1) {
                            selectedMonth = 12
                            selectedYear--
                        } else {
                            selectedMonth--
                        }
                    },
                    onNextMonth = {
                        if (selectedMonth == 12) {
                            selectedMonth = 1
                            selectedYear++
                        } else {
                            selectedMonth++
                        }
                    },
                    onTextClick = { showMonthPicker = true }
                )

                Spacer(modifier = Modifier.height(8.dp).fillMaxWidth().background(MaterialTheme.colors.background))
            }
        },
        backgroundColor = MaterialTheme.colors.background
    ) { padding ->
        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Không có nhật ký nào trong tháng $selectedMonth/$selectedYear", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                itemsIndexed(filteredNotes, key = { _, note -> note.id }) { index, note ->
                    val isLast = index == filteredNotes.lastIndex
                    Timeline(
                        item = note,
                        isLastItem = isLast,
                        onDelete = { noteToDelete = note },
                        onEdit = { noteToEdit = note }
                    )
                }
            }
        }
    }

    if (noteToEdit != null) {
        EditNoteDialog(
            note = noteToEdit!!,
            onDismiss = { noteToEdit = null },
            onConfirm = { newHtml ->
                viewModel.updateNote(noteToEdit!!.id, newHtml)
                noteToEdit = null
            }
        )
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
