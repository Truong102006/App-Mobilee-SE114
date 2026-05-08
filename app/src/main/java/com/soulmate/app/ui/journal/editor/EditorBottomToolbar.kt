package com.soulmate.app.ui.journal.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.soulmate.app.ui.theme.PrimaryGreen

@Composable
fun EditorBottomToolbar(
    richTextState: RichTextState,
    onPhotoClick: () -> Unit,
    onMoodClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPhotoClick) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "Thêm ảnh",
                tint = PrimaryGreen
            )
        }

        IconButton(onClick = onMoodClick) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Phân tích tâm trạng",
                tint = PrimaryGreen
            )
        }
    }
}