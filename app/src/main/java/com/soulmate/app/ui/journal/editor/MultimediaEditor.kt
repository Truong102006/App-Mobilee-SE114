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

import com.soulmate.app.ui.theme.TextPrimary
import com.soulmate.app.ui.theme.BackgroundMain
import com.soulmate.app.ui.theme.PrimaryGreen
import com.soulmate.app.ui.theme.PrimaryGreenLight

import androidx.compose.ui.tooling.preview.Preview
import com.soulmate.app.ui.theme.SoulMateTheme

data class DiaryDraft(
    val title: String,
    val contentHtml: String
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MultimediaEditor(
    onSaveClick: (DiaryDraft) -> Unit = {}
) {
    var title by remember { mutableStateOf("") }

    val richTextState = rememberRichTextState()

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
                                contentHtml = richTextState.toHtml()
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
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MultimediaEditorPreview() {
    SoulMateTheme {
        MultimediaEditor()
    }
}