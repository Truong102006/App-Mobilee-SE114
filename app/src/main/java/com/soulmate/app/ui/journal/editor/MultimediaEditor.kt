package com.soulmate.app.ui.journal.editor

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.soulmate.app.ui.journal.history.HistoryViewModel
import com.soulmate.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultimediaEditor(
    diaryId: String? = null,
    prefilledContent: String? = null,
    viewModel: DiaryViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel? = null,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var title by remember { mutableStateOf("") }
    val richTextState = rememberRichTextState()
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var zoomedImageUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()

    // Update ViewModel whenever selected image list changes
    LaunchedEffect(selectedImages) {
        viewModel.onImagesChanged(selectedImages.map { it.toString() })
    }

    LaunchedEffect(diaryId, prefilledContent) {
        if (diaryId != null && historyViewModel != null) {
            val existingNote = historyViewModel.getNoteById(diaryId)
            if (existingNote != null) {
                val fullHtml = existingNote.text
                if (fullHtml.startsWith("<h3>")) {
                    val titleEndIndex = fullHtml.indexOf("</h3>")
                    if (titleEndIndex != -1) {
                        title = fullHtml.substring(4, titleEndIndex)
                        val content = fullHtml.substring(titleEndIndex + 5)
                        richTextState.setHtml(content)
                    } else {
                        richTextState.setHtml(fullHtml)
                    }
                } else {
                    richTextState.setHtml(fullHtml)
                }

                viewModel.onMoodSelected(existingNote.moodTag)
                selectedImages = existingNote.imageUrls.map { it.toUri() }
            }
        } else if (prefilledContent != null) {
            richTextState.setHtml(prefilledContent)
        }
    }
    
    var showSaveSuccess by remember { mutableStateOf(false) }

    val selectedMood = remember(uiState.selectedMood) {
        Mood.entries.find { it.label == uiState.selectedMood } ?: Mood.Neutral
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            showSaveSuccess = true
            viewModel.onSaveCompleteHandled()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onErrorHandled()
        }
    }

    val currentDateTime = remember {
        val sdf = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN"))
        sdf.format(Date())
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        scope.launch {
            val copiedUris = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    runCatching { copyImageToInternalStorage(context, uri) }
                        .getOrElse { uri }
                }
            }
            selectedImages = selectedImages + copiedUris
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Nh\u1EADt K\u00FD",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(end = 16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Surface(
                                onClick = {
                                    val contentHtml = richTextState.toHtml()
                                    val combinedHtml = if (title.isNotBlank()) "<h3>$title</h3>$contentHtml" else contentHtml
                                    
                                    viewModel.onTextChanged(combinedHtml)
                                    viewModel.saveDiary(diaryId)
                                },
                                modifier = Modifier.padding(end = 12.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save diary",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp).size(24.dp)
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(modifier = Modifier.padding(vertical = 8.dp)) {
                    MoodSelector(
                        selectedMood = selectedMood,
                        onMoodChange = { viewModel.onMoodSelected(it.label) }
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Text(
                                text = currentDateTime,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            DiaryTitleField(
                                title = title,
                                onTitleChange = { title = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            RichTextToolbar(state = richTextState)
                        }

                        DiaryContentField(
                            state = richTextState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        )

                        if (selectedImages.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(selectedImages) { uri ->
                                    Box {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(85.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { zoomedImageUri = uri },
                                            contentScale = ContentScale.Crop
                                        )
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(18.dp)
                                                .clickable { selectedImages = selectedImages.filter { it != uri } },
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.4f)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.padding(2.dp))
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        EditorBottomToolbar(
                            richTextState = richTextState,
                            onPhotoClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onMoodClick = {
                                viewModel.analyzeMoodFromText(richTextState.toHtml())
                            }
                        )
                    }
                }
            }
        }

        if (zoomedImageUri != null) {
            FullScreenImageOverlay(
                uri = zoomedImageUri!!,
                onDismiss = { zoomedImageUri = null }
            )
        }

        if (showSaveSuccess) {
            SaveSuccessOverlay(
                onAnimationFinish = {
                    showSaveSuccess = false
                    onBackClick()
                }
            )
        }
    }
}

private fun copyImageToInternalStorage(context: Context, sourceUri: Uri): Uri {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(sourceUri)
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.ifBlank { null } ?: "jpg"

    val imageDir = File(context.filesDir, "diary_images")
    if (!imageDir.exists()) {
        imageDir.mkdirs()
    }

    val outputFile = File(imageDir, "diary_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension")
    resolver.openInputStream(sourceUri)?.use { input ->
        FileOutputStream(outputFile).use { output ->
            input.copyTo(output)
        }
    } ?: return sourceUri

    return Uri.fromFile(outputFile)
}

@Composable
fun FullScreenImageOverlay(uri: Uri, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun SaveSuccessOverlay(onAnimationFinish: () -> Unit) {
    var startTickAnimation by remember { mutableStateOf(false) }
    val sweepAngle = animateFloatAsState(
        targetValue = if (startTickAnimation) 360f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = ""
    )
    
    val tickScale = animateFloatAsState(
        targetValue = if (sweepAngle.value >= 360f) 1.5f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = ""
    )

    LaunchedEffect(Unit) {
        startTickAnimation = true
        delay(2000)
        onAnimationFinish()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.size(160.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                    Canvas(modifier = Modifier.size(60.dp)) {
                        drawArc(
                            color = Color(0xFFE0E0E0),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = Color(0xFF4CAF50),
                            startAngle = -90f,
                            sweepAngle = sweepAngle.value,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    if (sweepAngle.value >= 360f) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier
                                .size(40.dp)
                                .scale(tickScale.value)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "\u0110\u00E3 l\u01B0u",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                )
            }
        }
    }
}
