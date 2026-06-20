package com.soulmate.app.ui.components

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GalleryImageSaverState(
    val isSaving: Boolean,
    private val onSaveImages: (List<String>) -> Unit
) {
    fun saveImage(imageUrl: String) {
        onSaveImages(listOf(imageUrl))
    }

    fun saveImages(imageUrls: List<String>) {
        onSaveImages(imageUrls)
    }
}

private data class PendingSaveRequest(
    val imageUrls: List<String>
)

private data class SaveResult(
    val savedCount: Int,
    val totalCount: Int
)

@Composable
fun rememberGalleryImageSaver(
    albumName: String = "SoulMate"
): GalleryImageSaverState {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var pendingRequest by remember { mutableStateOf<PendingSaveRequest?>(null) }

    fun startSave(request: PendingSaveRequest) {
        if (isSaving) {
            Toast.makeText(context, "Đang lưu ảnh, vui lòng đợi một chút.", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isSaving = true
            val result = saveImagesToGallery(appContext, request.imageUrls, albumName)
            isSaving = false
            Toast.makeText(context, result.toUserMessage(), Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val request = pendingRequest
        pendingRequest = null
        if (request == null) {
            return@rememberLauncherForActivityResult
        }

        if (granted) {
            startSave(request)
        } else {
            Toast.makeText(
                context,
                "Cần cấp quyền bộ nhớ để lưu ảnh vào thư viện.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val onSaveImages: (List<String>) -> Unit = onSave@{ imageUrls ->
        val distinctUrls = imageUrls
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()

        if (distinctUrls.isEmpty()) {
            Toast.makeText(context, "Không có ảnh để lưu.", Toast.LENGTH_SHORT).show()
            return@onSave
        }

        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingRequest = PendingSaveRequest(distinctUrls)
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return@onSave
        }

        startSave(PendingSaveRequest(distinctUrls))
    }

    return GalleryImageSaverState(
        isSaving = isSaving,
        onSaveImages = onSaveImages
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavableImageDialog(
    imageUrl: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSaveClick: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Preview Image",
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { onSaveClick(imageUrl) }
                    ),
                contentScale = ContentScale.Fit
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                OverlayIconButton(
                    onClick = onDismiss
                ) {
                    androidx.compose.material.Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color.White
                    )
                }

                OverlayIconButton(
                    onClick = { onSaveClick(imageUrl) },
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        androidx.compose.material.Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Lưu ảnh",
                            tint = Color.White
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    text = "Nhấn giữ ảnh hoặc bấm tải xuống để lưu vào thư viện",
                    style = MaterialTheme.typography.caption,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun OverlayIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.35f)
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled
        ) {
            content()
        }
    }
}

private suspend fun saveImagesToGallery(
    context: Context,
    imageUrls: List<String>,
    albumName: String
): SaveResult = withContext(Dispatchers.IO) {
    var savedCount = 0

    imageUrls.forEachIndexed { index, imageUrl ->
        runCatching {
            saveSingleImageToGallery(
                context = context,
                imageUrl = imageUrl,
                albumName = albumName,
                index = index
            )
        }.onSuccess {
            savedCount++
        }
    }

    SaveResult(
        savedCount = savedCount,
        totalCount = imageUrls.size
    )
}

private fun saveSingleImageToGallery(
    context: Context,
    imageUrl: String,
    albumName: String,
    index: Int
) {
    val extension = resolveExtension(imageUrl)
    val mimeType = resolveMimeType(extension)
    val displayName = buildDisplayName(extension, index)
    val inputStream = openImageInputStream(context, imageUrl)
        ?: error("Cannot open input stream for image")

    inputStream.use { stream ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(
                context = context,
                inputStream = stream,
                mimeType = mimeType,
                displayName = displayName,
                albumName = albumName
            )
        } else {
            saveWithPublicDirectory(
                context = context,
                inputStream = stream,
                mimeType = mimeType,
                displayName = displayName,
                albumName = albumName
            )
        }
    }
}

private fun openImageInputStream(
    context: Context,
    imageUrl: String
): InputStream? {
    val uri = Uri.parse(imageUrl)
    val scheme = uri.scheme?.lowercase(Locale.ROOT)

    return when (scheme) {
        "content", "file" -> context.contentResolver.openInputStream(uri)
        "http", "https" -> URL(imageUrl).openStream()
        null -> File(imageUrl).takeIf(File::exists)?.inputStream()
        else -> null
    }
}

private fun saveWithMediaStore(
    context: Context,
    inputStream: InputStream,
    mimeType: String,
    displayName: String,
    albumName: String
) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            "${Environment.DIRECTORY_PICTURES}/$albumName"
        )
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("Cannot create MediaStore entry")

    runCatching {
        resolver.openOutputStream(imageUri)?.use { outputStream ->
            inputStream.copyTo(outputStream)
        } ?: error("Cannot open output stream")
    }.onFailure { throwable ->
        resolver.delete(imageUri, null, null)
        throw throwable
    }

    values.clear()
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    resolver.update(imageUri, values, null, null)
}

private fun saveWithPublicDirectory(
    context: Context,
    inputStream: InputStream,
    mimeType: String,
    displayName: String,
    albumName: String
) {
    val picturesDirectory = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES
    )
    val appDirectory = File(picturesDirectory, albumName)
    if (!appDirectory.exists()) {
        appDirectory.mkdirs()
    }

    val imageFile = File(appDirectory, displayName)
    FileOutputStream(imageFile).use { outputStream ->
        inputStream.copyTo(outputStream)
    }

    MediaScannerConnection.scanFile(
        context,
        arrayOf(imageFile.absolutePath),
        arrayOf(mimeType),
        null
    )
}

private fun resolveExtension(imageUrl: String): String {
    val cleanUrl = imageUrl.substringBefore('?').substringBefore('#')
    val fromUrl = MimeTypeMap.getFileExtensionFromUrl(cleanUrl)
        ?.lowercase(Locale.ROOT)
        ?.takeIf { it in SUPPORTED_EXTENSIONS }

    return fromUrl ?: "jpg"
}

private fun resolveMimeType(extension: String): String {
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        ?: "image/jpeg"
}

private fun buildDisplayName(extension: String, index: Int): String {
    val timestamp = SimpleDateFormat(
        "yyyyMMdd_HHmmss_SSS",
        Locale.US
    ).format(Date())
    return "SoulMate_${timestamp}_${index + 1}.$extension"
}

private fun SaveResult.toUserMessage(): String {
    return when {
        savedCount == 0 -> "Không thể lưu ảnh. Vui lòng thử lại."
        savedCount == totalCount && savedCount == 1 -> "Đã lưu ảnh vào thư viện."
        savedCount == totalCount -> "Đã lưu $savedCount ảnh vào thư viện."
        else -> "Đã lưu $savedCount/$totalCount ảnh vào thư viện."
    }
}

private val SUPPORTED_EXTENSIONS = setOf(
    "jpg",
    "jpeg",
    "png",
    "webp",
    "gif"
)
