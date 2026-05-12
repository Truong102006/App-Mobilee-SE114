import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.soulmate.app.domain.model.User
import com.soulmate.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun EditProfileDialog(
    user: User,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (User, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(user.anonymousName) }
    var phone by remember { mutableStateOf(user.phoneNumber) }

    // 1. Biến cho Gender Dropdown
    var gender by remember { mutableStateOf(user.gender) }
    var expandedGender by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female", "Other", "Secret")

    // 2. Biến cho Social Links List
    var socialLinks by remember { mutableStateOf(user.socialMedias) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkInput by remember { mutableStateOf("") }
    var editLinkIndex by remember { mutableStateOf<Int?>(null) } // null = Add new, Int = Edit

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Edit Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- NỘI DUNG FORM ---
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. AVATAR SECTION (Chỉ UI, logic up ảnh làm sau)
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clickable(enabled = !isLoading) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        val imageToShow = selectedImageUri ?: user.avatarUrl

                        if (imageToShow != null && imageToShow.toString().isNotBlank()) {
                            AsyncImage(
                                model = imageToShow,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colors.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.anonymousName.firstOrNull()?.toString()?.uppercase(Locale.getDefault()) ?: "U",
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // Icon Camera nhỏ đè lên
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colors.surface)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Edit Avatar",
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email (Read-only)
                    OutlinedTextField(
                        value = user.email,
                        onValueChange = {},
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. GENDER DROPDOWN
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            label = { Text("Gender") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedGender = !expandedGender }) {
                                    Icon(Icons.Default.ArrowDropDown, "Drop down")
                                }
                            }
                        )

                        Spacer(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { expandedGender = true }
                        )
                        DropdownMenu(
                            expanded = expandedGender,
                            onDismissRequest = { expandedGender = false }
                        ) {
                            genderOptions.forEach { option ->
                                DropdownMenuItem(onClick = {
                                    gender = option
                                    expandedGender = false
                                }) {
                                    Text(text = option)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. SOCIAL LINKS LIST
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Social Links", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = {
                                editLinkIndex = null
                                linkInput = ""
                                showLinkDialog = true
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Link")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hiển thị danh sách các link đã thêm
                    socialLinks.forEachIndexed { index, link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(MaterialTheme.colors.surface, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = link,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colors.onSurface,
                                maxLines = 1
                            )
                            IconButton(
                                onClick = {
                                    editLinkIndex = index
                                    linkInput = link
                                    showLinkDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colors.primary)
                            }
                            IconButton(
                                onClick = {
                                    // Xóa link
                                    val newList = socialLinks.toMutableList()
                                    newList.removeAt(index)
                                    socialLinks = newList
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- SAVE BUTTON ---
                Button(
                    onClick = {
                        val updatedUser = user.copy(
                            anonymousName = name,
                            phoneNumber = phone,
                            gender = gender,
                            socialMedias = socialLinks,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(updatedUser, selectedImageUri)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- POPUP NHẬP LINK SOCIAL ---
        if (showLinkDialog) {
            AlertDialog(
                onDismissRequest = { showLinkDialog = false },
                title = { Text(if (editLinkIndex == null) "Add Social Link" else "Edit Social Link") },
                text = {
                    OutlinedTextField(
                        value = linkInput,
                        onValueChange = { linkInput = it },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    IconButton(
                        onClick = {
                            if (linkInput.isNotBlank()) {
                                val newList = socialLinks.toMutableList()
                                if (editLinkIndex == null) {
                                    newList.add(linkInput.trim())
                                } else {
                                    newList[editLinkIndex!!] = linkInput.trim()
                                }
                                socialLinks = newList
                            }
                            showLinkDialog = false
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colors.primary)
                    }
                },
                dismissButton = {
                    IconButton(onClick = { showLinkDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Gray)
                    }
                }
            )
        }
    }
}