package com.soulmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

import com.soulmate.app.ui.theme.SoulMateTheme
import com.soulmate.app.ui.journal.editor.MultimediaEditor

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MultimediaEditor();
        }
    }
}
