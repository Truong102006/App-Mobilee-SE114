package com.soulmate.app.ui.journal.editor

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.soulmate.app.ui.journal.history.HistoryViewModel
import com.soulmate.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDiaryScreen(
    diaryId: String,
    viewModel: DiaryViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var title by remember { mutableStateOf("") }
    val richTextState = rememberRichTextState()
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var zoomedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSaveSuccess by remember { mutableStateOf(false) }

    // Khởi tạo dữ liệu từ diaryId
    LaunchedEffect(diaryId) {
        viewModel.setDiaryId(diaryId)
        val existingNote = historyViewModel.getNoteById(diaryId)
        if (existingNote != null) {
            val fullHtml = existingNote.text
            if (fullHtml.startsWith("<h3>")) {
                val titleEndIndex = fullHtml.indexOf("</h3>")
                if (titleEndIndex != -1) {
                    title = fullHtml.substring(4, titleEndIndex)
                    richTextState.setHtml(fullHtml.substring(titleEndIndex + 5))
                } else {
                    richTextState.setHtml(fullHtml)
                }
            } else {
                richTextState.setHtml(fullHtml)
            }
            viewModel.onMoodSelected(existingNote.moodTag)
            selectedImages = existingNote.imageUrls.map { it.toUri() }
        }
    }

    // Đồng bộ danh sách ảnh với ViewModel
    LaunchedEffect(selectedImages) {
        viewModel.onImagesChanged(selectedImages.map { it.toString() })
    }

    val selectedMood = remember(uiState.selectedMood) {
        Mood.entries.find { it.label == uiState.selectedMood } ?: Mood.Neutral
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            val contentHtml = richTextState.toHtml()
            val combinedHtml = if (title.isNotBlank()) "<h3>$title</h3>$contentHtml" else contentHtml
            
            historyViewModel.updateNote(
                diaryId = diaryId,
                newHtml = combinedHtml,
                newImages = selectedImages.map { it.toString() },
                newMood = selectedMood.label
            )
            showSaveSuccess = true
            viewModel.onSaveCompleteHandled()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onErrorHandled()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        selectedImages = selectedImages + uris
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Sửa Nhật Ký",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    actions = {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp))
                        } else {
                            Surface(
                                onClick = {
                                    val contentHtml = richTextState.toHtml()
                                    val combinedHtml = if (title.isNotBlank()) "<h3>$title</h3>$contentHtml" else contentHtml
                                    viewModel.onTextChanged(combinedHtml)
                                    viewModel.saveDiary()
                                },
                                modifier = Modifier.padding(end = 12.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Icon(Icons.Default.Check, "Update", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp).size(24.dp))
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                MoodSelector(selectedMood = selectedMood, onMoodChange = { viewModel.onMoodSelected(it.label) })

                Surface(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp).border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            DiaryTitleField(title = title, onTitleChange = { title = it }, modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            RichTextToolbar(state = richTextState)
                        }
                        DiaryContentField(state = richTextState, modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp, vertical = 12.dp))
                        
                        if (selectedImages.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(selectedImages) { uri ->
                                    Box {
                                        AsyncImage(model = uri, contentDescription = null, modifier = Modifier.size(85.dp).clip(RoundedCornerShape(12.dp)).clickable { zoomedImageUri = uri }, contentScale = ContentScale.Crop)
                                        Surface(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).clickable { selectedImages = selectedImages.filter { it != uri } }, shape = CircleShape, color = Color.Black.copy(alpha = 0.4f)) {
                                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.padding(2.dp))
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        EditorBottomToolbar(
                            richTextState = richTextState,
                            onPhotoClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onMoodClick = { viewModel.analyzeMoodFromText(richTextState.toHtml()) }
                        )
                    }
                }
            }
        }
        if (zoomedImageUri != null) FullScreenImageOverlay(uri = zoomedImageUri!!, onDismiss = { zoomedImageUri = null })
        if (showSaveSuccess) SaveSuccessOverlay(onAnimationFinish = { showSaveSuccess = false; onBackClick() })
    }
}
