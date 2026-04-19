package com.soulmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

import com.soulmate.app.ui.theme.SoulMateTheme
import com.soulmate.app.ui.journal.editor.MultimediaEditor

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current

            MultimediaEditor(
                onSaveClick = { draftData ->
                    val message = "Title: ${draftData.title}\nContent: ${draftData.contentHtml}"

                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
