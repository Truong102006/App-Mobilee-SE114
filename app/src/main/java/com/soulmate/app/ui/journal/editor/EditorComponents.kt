package com.soulmate.app.ui.journal.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults

import com.soulmate.app.ui.theme.TextPrimary
import com.soulmate.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryTitleField(
    title: String,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = "Title of your today...",
                color = TextSecondary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        textStyle = TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        ),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = TextPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryContentField(
    state: RichTextState,
    modifier: Modifier = Modifier
) {
    RichTextEditor(
        state = state,
        modifier = modifier,
        placeholder = {
            Text(
                text = "How is your day going on? Tell me about it...",
                color = TextSecondary,
                fontSize = 16.sp
            )
        },
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = TextPrimary,
            lineHeight = 24.sp
        ),
        colors = RichTextEditorDefaults.richTextEditorColors(
            containerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = TextPrimary
        )
    )
}