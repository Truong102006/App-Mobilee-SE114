package com.soulmate.app.ui.journal.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soulmate.app.ui.theme.BackgroundMain

@Composable
fun MultimediaEditor() {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundMain
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            DiaryTitleField(
                title = title,
                onTitleChange = { newTitle -> title = newTitle },
                modifier = Modifier.fillMaxWidth()
            )

            DiaryContentField(
                content = content,
                onContentChange = { newContent -> content = newContent },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // more feat: album -> insert images, voice, save button, bottom navigator
        }
    }
}