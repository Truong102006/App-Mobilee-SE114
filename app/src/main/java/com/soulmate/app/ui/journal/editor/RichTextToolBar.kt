package com.soulmate.app.ui.journal.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState

import com.soulmate.app.ui.theme.PrimaryGreen
import com.soulmate.app.ui.theme.PrimaryGreenLight
import com.soulmate.app.ui.theme.TextPrimary
import com.soulmate.app.ui.theme.TextSecondary

@Composable
fun RichTextToolbar(
    state: RichTextState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FormatActionButton(
            label = "B",
            isActive = state.currentSpanStyle.fontWeight == FontWeight.Bold,
            onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
            textStyle = TextStyle(fontWeight = FontWeight.Bold)
        )
        FormatActionButton(
            label = "I",
            isActive = state.currentSpanStyle.fontStyle == FontStyle.Italic,
            onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
            textStyle = TextStyle(fontStyle = FontStyle.Italic)
        )
        FormatActionButton(
            label = "U",
            isActive = state.currentSpanStyle.textDecoration == TextDecoration.Underline,
            onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
            textStyle = TextStyle(textDecoration = TextDecoration.Underline)
        )

        DividerVertical()

        val fontSize = state.currentSpanStyle.fontSize
        var currentSize = 16
        if (fontSize.isSp) currentSize = fontSize.value.toInt()

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            SizeButton("-") {
                val nextSize = (currentSize - 1).coerceAtLeast(8)
                state.toggleSpanStyle(SpanStyle(fontSize = nextSize.sp))
            }

            Text(
                text = currentSize.toString(),
                modifier = Modifier.padding(horizontal = 10.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )

            SizeButton("+") {
                val nextSize = (currentSize + 1).coerceAtMost(99)
                state.toggleSpanStyle(SpanStyle(fontSize = nextSize.sp))
            }
        }

        DividerVertical()

        IconButton(onClick = { state.toggleUnorderedList() }) {
            Icon(Icons.Default.FormatListBulleted, "Bulleted", tint = if (state.isUnorderedList) PrimaryGreen else TextSecondary)
        }
        IconButton(onClick = { state.toggleOrderedList() }) {
            Icon(Icons.Default.FormatListNumbered, "Numbered", tint = if (state.isOrderedList) PrimaryGreen else TextSecondary)
        }

        DividerVertical()

        val colors = listOf(Color(0xFFf0f6fc), PrimaryGreen, Color(0xFFE53935), Color(0xFF1E88E5))
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { state.toggleSpanStyle(SpanStyle(color = color)) }
            )
        }
    }
}

@Composable
private fun SizeButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(PrimaryGreenLight.copy(alpha = 0.1f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = PrimaryGreen, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DividerVertical() {
    VerticalDivider(
        modifier = Modifier.height(24.dp),
        color = PrimaryGreenLight.copy(alpha = 0.3f)
    )
}

@Composable
private fun FormatActionButton(
    label: String, isActive: Boolean, onClick: () -> Unit, textStyle: TextStyle
) {
    val bgColor = if (isActive) PrimaryGreenLight.copy(alpha = 0.2f) else Color.Transparent
    val txtColor = if (isActive) PrimaryGreen else TextSecondary

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = textStyle, color = txtColor)
    }
}