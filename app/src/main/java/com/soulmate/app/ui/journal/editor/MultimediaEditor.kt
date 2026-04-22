package com.soulmate.app.ui.journal.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.soulmate.app.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
import java.text.SimpleDateFormat
import java.util.*

// GIỮ NGUYÊN DATA CLASS GỐC
data class DiaryDraft(
    val title: String,
    val contentHtml: String,
    val images: List<Uri>,
    val mood: Mood
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultimediaEditor(
    onSaveClick: (DiaryDraft) -> Unit = {}
) {
    // GIỮ NGUYÊN CÁC LOGIC STATE GỐC
    var title by remember { mutableStateOf("") }
    val richTextState = rememberRichTextState()
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var zoomedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMood by remember { mutableStateOf(Mood.Neutral) }

    // Logic lấy thời gian hiện tại
    val currentDateTime = remember {
        val sdf = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN"))
        sdf.format(Date())
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        selectedImages = selectedImages + uris
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Nhật Ký",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    Surface(
                        onClick = {
                            val draft = DiaryDraft(
                                title = title,
                                contentHtml = richTextState.toHtml(),
                                images = selectedImages,
                                mood = selectedMood
                            )
                            onSaveClick(draft)
                        },
                        modifier = Modifier.padding(end = 12.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save diary",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp).size(24.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Section 1: Mood Selector
            Box(modifier = Modifier.padding(vertical = 8.dp)) {
                MoodSelector(
                    selectedMood = selectedMood,
                    onMoodChange = { selectedMood = it }
                )
            }

            // Body chính với VIỀN XANH LÁ và SHADOW
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    // CHUYỂN VỀ: Viền xanh PrimaryGreen cho border ngoài
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp)
                ) {
                    // Hiển thị Thứ, ngày, tháng, năm trước Title
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Text(
                            text = currentDateTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary, // Chữ ngày tháng màu xanh lá
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        DiaryTitleField(
                            title = title,
                            onTitleChange = { title = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Toolbar Rich Editor với VIỀN XANH LÁ nhạt
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            // CHUYỂN VỀ: Viền xanh cho thanh thuộc tính
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        RichTextToolbar(state = richTextState)
                    }

                    // Content Area
                    DiaryContentField(
                        state = richTextState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )

                    // Image List
                    if (selectedImages.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(selectedImages) { uri ->
                                Box {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(85.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { zoomedImageUri = uri },
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(18.dp)
                                            .clickable { selectedImages = selectedImages.filter { it != uri } },
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.4f)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.padding(2.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Toolbar
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    EditorBottomToolbar(
                        onAddImageClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRecordAudioClick = { /* record feature */ }
                    )
                }
            }
        }
    }

    zoomedImageUri?.let { uri ->
        ImageZoomDialog(
            uri = uri,
            onDismiss = { zoomedImageUri = null }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MultimediaEditorPreview() {
    SoulMateTheme {
        MultimediaEditor()
    }
}
