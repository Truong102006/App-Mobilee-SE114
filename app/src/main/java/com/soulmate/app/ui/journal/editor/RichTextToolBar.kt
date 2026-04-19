package com.soulmate.app.ui.journal.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.soulmate.app.ui.theme.PrimaryGreen
import com.soulmate.app.ui.theme.PrimaryGreenLight
import com.soulmate.app.ui.theme.TextSecondary

@Composable
fun RichTextToolbar(
    modifier: Modifier = Modifier,

    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    isStrikeout: Boolean,

    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onToggleStrikeout: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FormatActionButton(
            label = "B",
            isActive = isBold,
            onClick = onToggleBold,
            textStyle = TextStyle(fontWeight = FontWeight.Bold)
        )

        FormatActionButton(
            label = "I",
            isActive = isItalic,
            onClick = onToggleItalic,
            textStyle = TextStyle(fontStyle = FontStyle.Italic)
        )

        FormatActionButton(
            label = "U",
            isActive = isUnderline,
            onClick = onToggleUnderline,
            textStyle = TextStyle(textDecoration = TextDecoration.Underline)
        )

        FormatActionButton(
            label = "S",
            isActive = isStrikeout,
            onClick = onToggleStrikeout,
            textStyle = TextStyle(textDecoration = TextDecoration.LineThrough)
        )
    }
}

@Composable
private fun FormatActionButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    textStyle: TextStyle
) {
    val backgroundColor = if (isActive) PrimaryGreenLight.copy(alpha = 0.2f) else Color.Transparent
    val contentColor = if (isActive) PrimaryGreen else TextSecondary

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp)) // Bo góc cho nút
            .background(backgroundColor)
    ) {
        Text(
            text = label,
            style = textStyle,
            color = contentColor,
            fontSize = 18.sp
        )
    }
}