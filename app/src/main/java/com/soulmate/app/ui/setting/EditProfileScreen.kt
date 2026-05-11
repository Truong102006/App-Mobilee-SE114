import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soulmate.app.domain.model.User

@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    // Tạo các state tạm thời để user chỉnh sửa trước khi Save
    var name by remember { mutableStateOf(user.anonymousName) }
    var phone by remember { mutableStateOf(user.phoneNumber) }
    var gender by remember { mutableStateOf(user.gender) }

    var socialLinks by remember { mutableStateOf(user.socialMedias.joinToString(", ")) }

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
                // Header
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

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Email (Read-only)
                    OutlinedTextField(
                        value = user.email,
                        onValueChange = {},
                        label = { Text("Email") },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Gender (Tạm dùng TextField, có thể nâng cấp thành Dropdown sau)
                    OutlinedTextField(
                        value = gender,
                        onValueChange = { gender = it },
                        label = { Text("Gender (Male/Female/Secret)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Social Medias
                    OutlinedTextField(
                        value = socialLinks,
                        onValueChange = { socialLinks = it },
                        label = { Text("Social Links") },
                        placeholder = { Text("e.g. fb.com/a, insta.com/b") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }

                // Save Button
                Button(
                    onClick = {
                        // Cắt chuỗi mxh thành List<String>
                        val parsedSocials = socialLinks.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        // Tạo object User mới với data đã sửa
                        val updatedUser = user.copy(
                            anonymousName = name,
                            phoneNumber = phone,
                            gender = gender,
                            socialMedias = parsedSocials,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(updatedUser)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}