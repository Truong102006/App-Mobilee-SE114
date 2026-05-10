package com.soulmate.app.ui.journal.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.soulmate.app.ui.journal.editor.Mood

@Composable
fun DiaryDetailScreen(
    diaryId: String,
    viewModel: HistoryViewModel,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit
) {
    val note = viewModel.getNoteById(diaryId)
    val richTextState = rememberRichTextState()

    LaunchedEffect(note) {
        note?.let {
            richTextState.setHtml(it.text)
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                Spacer(modifier = Modifier.height(48.dp)) // Thêm margin top cho toàn bộ thanh tiêu đề và nút
                TopAppBar(
                    title = { Text("Chi tiết nhật ký", fontWeight = FontWeight.Bold) },
                    backgroundColor = Color.White,
                    elevation = 0.dp,
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onEditClick(diaryId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF4CAF50))
                        }
                    }
                )
            }
        },
        backgroundColor = Color.White
    ) { paddingValues ->
        if (note == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Không tìm thấy nhật ký")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp)) // Thêm khoảng cách phía trên nội dung

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mood Icon
                    Mood.entries.find { it.label == note.moodTag }?.let { mood ->
                        AsyncImage(
                            model = mood.iconRes,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFFAEB))
                                .padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = note.dateTime,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        if (note.moodTag != null) {
                            Text(
                                text = "Cảm thấy: ${note.moodTag}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E88E5)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                RichText(
                    state = richTextState,
                    modifier = Modifier.fillMaxWidth()
                )

                if (note.imageUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Hình ảnh",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    note.imageUrls.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
