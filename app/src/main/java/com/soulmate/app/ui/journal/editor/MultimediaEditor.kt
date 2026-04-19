package com.soulmate.app.ui.journal.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState

import com.soulmate.app.ui.theme.BackgroundMain
import com.soulmate.app.ui.theme.PrimaryGreenLight

import androidx.compose.ui.tooling.preview.Preview
import com.soulmate.app.ui.theme.SoulMateTheme

@Composable
fun MultimediaEditor() {
    var title by remember { mutableStateOf("") }

    val richTextState = rememberRichTextState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundMain
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

            RichTextToolbar(
                isBold = richTextState.currentSpanStyle.fontWeight == FontWeight.Bold,
                isItalic = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic,
                isUnderline = richTextState.currentSpanStyle.textDecoration == TextDecoration.Underline,
                isStrikeout = richTextState.currentSpanStyle.textDecoration == TextDecoration.LineThrough,

                onToggleBold = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                onToggleItalic = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                onToggleUnderline = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
                onToggleStrikeout = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) }
            )

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