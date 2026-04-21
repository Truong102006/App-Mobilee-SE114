package com.soulmate.app.ui.recording

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soulmate.app.data.model.RecordedNote
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordingScreen() {
    // Sử dụng state ở cấp cao nhất của hàm
    var currentState by remember { mutableStateOf("MAIN") }
    val notesList = remember { mutableStateListOf<RecordedNote>() }

    // Chuyển đổi màn hình dựa trên currentState
    when (currentState) {
        "MAIN" -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Trình ghi âm", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        Log.d("RecordingApp", "Bấm nút chuyển màn hình")
                        currentState = "DETAIL"
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFED8413))
                ) {
                    Icon(Icons.Default.Mic, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bắt đầu ghi âm", color = Color.White)
                }
            }
        }
        "DETAIL" -> {
            RecordingDetailView(
                notesList = notesList,
                onBack = { currentState = "MAIN" }
            )
        }
    }
}

@Composable
fun RecordingDetailView(
    notesList: MutableList<RecordedNote>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
        }
    }

    BackHandler { onBack() }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onResults(results: Bundle?) {
                val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = data?.get(0) ?: ""
                if (text.isNotEmpty()) {
                    val currentTime = SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault()).format(Date())
                    notesList.add(0, RecordedNote(text = text, dateTime = currentTime))
                }
                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
                Log.e("SpeechError", "Lỗi nhận diện: $error")
            }
            override fun onEndOfSpeech() { isListening = false }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) speechRecognizer.startListening(recognizerIntent)
        else Toast.makeText(context, "Vui lòng cấp quyền ghi âm", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đang ghi âm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                backgroundColor = Color.White,
                elevation = 1.dp
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Danh sách ở trên
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notesList) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = Color(0xFFF5F5F5),
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(note.dateTime, fontSize = 10.sp, color = Color.Gray)
                            Text(note.text, fontSize = 16.sp)
                        }
                    }
                }
            }

            // Nút to ở cuối
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isListening) {
                    Text("Đang nghe...", color = Color.Red, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (isListening) speechRecognizer.stopListening()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isListening) Color.Red else Color(0xFFED8413)
                    )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = Color.White
                    )
                }
                Text(if (isListening) "Dừng" else "Bắt đầu nói", modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}