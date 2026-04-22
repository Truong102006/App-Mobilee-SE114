package com.soulmate.app.ui.home.components

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soulmate.app.R
import com.soulmate.app.ui.journal.history.HistoryViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Model dữ liệu
data class RecordingNote(
    val id: Long = System.currentTimeMillis(),
    val dateTime: String,
    val text: String,
    val userName: String = "Dmanhz",
    val avatarRes: Int = R.drawable.ava1
)

@Composable
fun MoodCard(historyViewModel: HistoryViewModel? = null) {
    var showRecordingScreen by remember { mutableStateOf(false) }
    var recordingNotes by remember { mutableStateOf(listOf<RecordingNote>()) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showRecordingScreen = true
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(96.dp)
            .shadow(6.dp, shape = RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.surface, shape = RoundedCornerShape(16.dp))
            .border(2.dp, MaterialTheme.colors.primary, RoundedCornerShape(16.dp))
            .clickable { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "How are you feeling today?",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                text = "Tap to record mood",
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colors.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Icon(Icons.Default.Mic, contentDescription = "Mic", tint = MaterialTheme.colors.primary)
            }
        }
    }

    if (showRecordingScreen) {
        RecordingOverlay(
            onDismiss = { showRecordingScreen = false },
            onPost = { newText ->
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val newNote = RecordingNote(dateTime = sdf.format(Date()), text = newText)
                recordingNotes = listOf(newNote) + recordingNotes
            },
            onDelete = { note ->
                recordingNotes = recordingNotes.filter { it.id != note.id }
            },
            onSave = { note ->
                historyViewModel?.addNote(note)
                recordingNotes = recordingNotes.filter { it.id != note.id }
            },
            history = recordingNotes
        )
    }
}

@Composable
fun RecordingOverlay(
    onDismiss: () -> Unit,
    onPost: (String) -> Unit,
    onDelete: (RecordingNote) -> Unit,
    onSave: (RecordingNote) -> Unit,
    history: List<RecordingNote>
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var transcribedText by remember { mutableStateOf("") }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!data.isNullOrEmpty()) transcribedText = data[0]
                isRecording = false
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val data = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!data.isNullOrEmpty()) transcribedText = data[0]
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isRecording = false }
            override fun onError(error: Int) { isRecording = false }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Ghi âm nhật ký",
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    backgroundColor = MaterialTheme.colors.surface,
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colors.onSurface)
                        }
                    },
                    actions = { Box(Modifier.size(48.dp)) },
                    elevation = 0.dp
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colors.background)
                    .padding(16.dp)
            ) {

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(history, key = { it.id }) { item ->
                        SwipeableDiaryItem(
                            item = item,
                            onDelete = { onDelete(item) },
                            onSave = { onSave(item) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = transcribedText,
                    onValueChange = { transcribedText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    placeholder = { Text("Đang lắng nghe...", color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)) },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = MaterialTheme.colors.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        textColor = MaterialTheme.colors.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Default.ArrowBackIos, contentDescription = null, tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    }

                    Button(
                        onClick = {
                            if (isRecording) {
                                speechRecognizer.stopListening()
                            } else {
                                transcribedText = ""
                                speechRecognizer.startListening(recognizerIntent)
                                isRecording = true
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = if (isRecording) Color.Red else MaterialTheme.colors.primary)
                    ) {
                        Icon(imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    IconButton(
                        onClick = {
                            if (transcribedText.isNotEmpty()) {
                                onPost(transcribedText)
                                transcribedText = ""
                                isRecording = false
                                speechRecognizer.stopListening()
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd).size(56.dp),
                        enabled = transcribedText.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null,
                            tint = if (transcribedText.isNotEmpty()) MaterialTheme.colors.primary else Color.Gray, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableDiaryItem(
    item: RecordingNote,
    onDelete: () -> Unit,
    onSave: () -> Unit
) {
    val density = LocalDensity.current
    // Quẹt khoảng 100dp để lộ đủ 2 nút (mỗi nút 40dp + khoảng cách)
    val swipeLimit = with(density) { 100.dp.toPx() }
    val swipeableState = rememberSwipeableState(initialValue = 0)
    val anchors = mapOf(0f to 0, -swipeLimit to 1)

    // Tự động đóng sau 3 giây nếu đang ở trạng thái mở
    if (swipeableState.currentValue == 1) {
        LaunchedEffect(item.id) {
            delay(3000)
            swipeableState.animateTo(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
    ) {
        // Lớp dưới: Chứa các nút bấm
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSave,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = Color(0xFF4CAF50))
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }

        // Lớp trên: Nội dung thẻ nhật ký, sẽ bị kéo đi
        Box(
            modifier = Modifier
                .offset { IntOffset(swipeableState.offset.value.toInt(), 0) }
                .fillMaxWidth()
        ) {
            DiaryPostItem(item)
        }
    }
}

@Composable
fun DiaryPostItem(item: RecordingNote) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Image(
                painter = painterResource(id = item.avatarRes),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = item.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colors.onSurface)
                    Text(text = item.dateTime, fontSize = 11.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = item.text, fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f))
            }
        }
    }
}