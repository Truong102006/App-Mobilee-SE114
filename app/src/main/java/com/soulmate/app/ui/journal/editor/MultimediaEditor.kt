package com.soulmate.app.ui.journal.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

import com.soulmate.app.ui.theme.TextPrimary
import com.soulmate.app.ui.theme.BackgroundMain
import com.soulmate.app.ui.theme.PrimaryGreen
import com.soulmate.app.ui.theme.PrimaryGreenLight

import androidx.compose.ui.tooling.preview.Preview
import com.soulmate.app.ui.theme.SoulMateTheme

data class DiaryDraft(
    val title: String,
    val contentHtml: String,
    val images: List<Uri>,
    val mood: Mood
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MultimediaEditor(
    onSaveClick: (DiaryDraft) -> Unit = {}
) {
    var title by remember { mutableStateOf("") }

    val richTextState = rememberRichTextState()

    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        selectedImages = selectedImages + uris
    }

    var zoomedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    var selectedMood by remember { mutableStateOf(Mood.Neutral) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundMain,
        topBar = {
            TopAppBar(
                title = { Text("Diary", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundMain
                ),
                actions = {
                    IconButton(
                        onClick = {
                            val draft = DiaryDraft(
                                title = title,
                                contentHtml = richTextState.toHtml(),
                                images = selectedImages,
                                mood = selectedMood
                            )
                            onSaveClick(draft)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save diary",
                            tint = PrimaryGreen
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
            MoodSelector(
                selectedMood = selectedMood,
                onMoodChange = { selectedMood = it }
            )

            HorizontalDivider(color = PrimaryGreenLight.copy(alpha = 0.2f), thickness = 1.dp)

            DiaryTitleField(
                title = title,
                onTitleChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(color = PrimaryGreenLight.copy(alpha = 0.2f), thickness = 1.dp)

            RichTextToolbar(state = richTextState)

            HorizontalDivider(color = PrimaryGreenLight.copy(alpha = 0.2f), thickness = 1.dp)

            DiaryContentField(
                state = richTextState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImages) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { zoomedImageUri = uri },
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                HorizontalDivider(color = PrimaryGreenLight.copy(alpha = 0.2f), thickness = 1.dp)
            }

            EditorBottomToolbar(
                onAddImageClick = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onRecordAudioClick = {
                    // record feature
                }
            )
        }
    }

    zoomedImageUri?.let { uri ->
        ImageZoomDialog(
            uri = uri,
            onDismiss = { zoomedImageUri = null }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MultimediaEditorPreview() {
    SoulMateTheme {
        MultimediaEditor()
    }
}