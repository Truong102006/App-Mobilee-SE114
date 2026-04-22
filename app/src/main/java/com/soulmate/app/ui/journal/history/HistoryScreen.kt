package com.soulmate.app.ui.journal.history

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.soulmate.app.ui.home.components.RecordingNote
import kotlinx.coroutines.delay

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val notes = viewModel.historyNotes
    var noteToEdit by remember { mutableStateOf<RecordingNote?>(null) }
    var noteToDelete by remember { mutableStateOf<RecordingNote?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử nhật ký", fontWeight = FontWeight.Bold) },
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.primary,
                elevation = 0.dp
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { padding ->
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Chưa có lịch sử nào", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    SwipeableHistoryItem(
                        item = note,
                        onDelete = { noteToDelete = note },
                        onEdit = { noteToEdit = note }
                    )
                }
            }
        }
    }

    // Dialog chỉnh sửa
    if (noteToEdit != null) {
        EditNoteDialog(
            note = noteToEdit!!,
            onDismiss = { noteToEdit = null },
            onConfirm = { newText ->
                viewModel.updateNote(noteToEdit!!.id, newText)
                noteToEdit = null
            }
        )
    }

    // Dialog xác nhận xóa
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
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableHistoryItem(
    item: RecordingNote,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val density = LocalDensity.current
    val swipeLimit = with(density) { 100.dp.toPx() }
    val swipeableState = rememberSwipeableState(initialValue = 0)
    val anchors = mapOf(0f to 0, -swipeLimit to 1)

    if (swipeableState.currentValue == 1) {
        LaunchedEffect(item.id) {
            delay(3000)
            swipeableState.animateTo(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
    ) {
        // Nút bấm ở dưới
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colors.primary)
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }

        // Nội dung thẻ ở trên
        Box(
            modifier = Modifier
                .offset { IntOffset(swipeableState.offset.value.toInt(), 0) }
                .fillMaxWidth()
        ) {
            HistoryItem(item)
        }
    }
}

@Composable
fun HistoryItem(item: RecordingNote) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Image(
                painter = painterResource(id = item.avatarRes),
                contentDescription = null,
                modifier = Modifier
                    .size(45.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = item.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colors.onSurface)
                    Text(text = item.dateTime, fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = item.text, fontSize = 15.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun EditNoteDialog(
    note: RecordingNote,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(note.text) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colors.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Chỉnh sửa nhật ký", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colors.primary,
                        textColor = MaterialTheme.colors.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Hủy", color = Color.Gray)
                    }
                    Button(
                        onClick = { onConfirm(text) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                    ) {
                        Text("Lưu", color = Color.White)
                    }
                }
            }
        }
    }
}
