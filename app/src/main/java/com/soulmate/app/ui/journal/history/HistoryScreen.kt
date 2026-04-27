package com.soulmate.app.ui.journal.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.soulmate.app.ui.home.components.RecordingNote
import com.soulmate.app.ui.journal.editor.Mood
import kotlinx.coroutines.delay

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val notes = viewModel.historyNotes
    var noteToEdit by remember { mutableStateOf<RecordingNote?>(null) }
    var noteToDelete by remember { mutableStateOf<RecordingNote?>(null) }

    Scaffold(
        topBar = {
            // Thêm margin top 15.dp cho phần TopAppBar
            Column(modifier = Modifier.background(MaterialTheme.colors.surface)) {
                Spacer(modifier = Modifier.height(35.dp))
                TopAppBar(
                    title = { Text("Lịch sử nhật ký", fontWeight = FontWeight.Bold) },
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.primary,
                    elevation = 0.dp
                )
            }
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
            onConfirm = { newHtml ->
                viewModel.updateNote(noteToEdit!!.id, newHtml)
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

    // Tự động đóng thanh chức năng sau 3 giây
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
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val richTextState = rememberRichTextState()

    // Đồng bộ HTML từ Diary sang RichTextState trong History
    LaunchedEffect(item.text, isDark) {
        val processedHtml = if (isDark) {
            // Tự động chuyển màu chữ đen sang trắng trong chế độ Dark Mode để dễ đọc
            val blackPattern = "(?i)color\\s*:\\s*(?:rgb\\(0,\\s*0,\\s*0\\)|rgba\\(0,\\s*0,\\s*0,\\s*1(?:\\.0)?\\)|#000(?:000)?|black)".toRegex()
            item.text.replace(blackPattern, "color: #FFFFFF")
        } else {
            item.text
        }
        richTextState.setHtml(processedHtml)
    }

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
            }.build()
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = item.avatarRes),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = item.userName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                        Text(text = item.dateTime, fontSize = 11.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                    }
                }

                // Hiển thị mood icon nếu có
                item.moodTag?.let { tag ->
                    Mood.entries.find { it.label == tag }?.let { mood ->
                        AsyncImage(
                            model = mood.iconRes,
                            contentDescription = mood.label,
                            imageLoader = imageLoader,
                            modifier = Modifier.size(35.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hiển thị văn bản với đầy đủ định dạng (màu sắc, kiểu chữ) từ Diary
            RichText(
                state = richTextState,
                modifier = Modifier.fillMaxWidth()
            )

            // Hiển thị ảnh kèm theo nếu có
            if (item.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(item.imageUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditNoteDialog(
    note: RecordingNote,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val richTextState = rememberRichTextState()
    
    // Khởi tạo nội dung RichText từ mã HTML đã lưu
    LaunchedEffect(note.text) {
        richTextState.setHtml(note.text)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colors.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Chỉnh sửa nhật ký", 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Sử dụng RichTextEditor thay vì TextField để hiển thị định dạng thật
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp)
                        .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
                ) {
                    RichTextEditor(
                        state = richTextState,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = MaterialTheme.colors.onSurface
                        ),
                        colors = RichTextEditorDefaults.richTextEditorColors(
                            containerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Hủy", color = Color.Gray)
                    }
                    Button(
                        onClick = { onConfirm(richTextState.toHtml()) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Lưu", color = Color.White)
                    }
                }
            }
        }
    }
}
