package com.soulmate.app.ui.setting

import EditProfileDialog
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.soulmate.app.domain.model.User
import com.soulmate.app.ui.login.AuthViewModel
import java.util.Locale
import androidx.core.net.toUri

@Composable
fun SettingScreen(
    themeViewModel: ThemeViewModel,
    authViewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    val currentUser by authViewModel.currentUser
    val notificationEnabled by settingsViewModel.notificationEnabled.collectAsState()
    var showEditProfileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // --- SECTION: PROFILE ---
        ProfileSection(user = currentUser)

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION: GENERAL ---
        SettingSectionTitle("General")
        SettingItem(
            icon = Icons.Default.Brightness4,
            title = "Dark Mode",
            trailing = {
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { isChecked -> themeViewModel.toggleDarkMode(isChecked) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary,
                        checkedTrackColor = MaterialTheme.colors.primaryVariant
                    )
                )
            }
        )
        SettingItem(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            trailing = {
                Switch(
                    checked = notificationEnabled,
                    onCheckedChange = { isChecked -> settingsViewModel.toggleNotification(isChecked) },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION: ACCOUNT & SECURITY ---
        SettingSectionTitle("Account")
        SettingItem(
            icon = Icons.Default.Person,
            title = "Edit Profile",
            onClick = {
                if (currentUser != null) {
                    showEditProfileDialog = true
                }
            }
        )
        SettingItem(icon = Icons.Default.Lock, title = "Privacy & Security")
        SettingItem(icon = Icons.Default.Language, title = "Language", subtitle = "Vietnamese")

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION: SUPPORT ---
        SettingSectionTitle("Support")
        SettingItem(
            icon = Icons.Default.Info,
            title = "About SoulMate",
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://github.com/Truong102006/App-Mobilee-SE114".toUri()
                )
                context.startActivity(intent)
            }
        )
        SettingItem(
            icon = Icons.Default.Help,
            title = "Help Center",
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:".toUri()
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("support@soulmate.com")) // TODO: Thay email thật của nhóm
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback/Support for SoulMate App")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "No email app found!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- LOGOUT BUTTON ---
        Button(
            onClick = {
                authViewModel.logout()
                onLogout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF4444))
        ) {
            Text(text = "Log Out", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // Hiển thị Dialog Edit Profile
    val authLoading by authViewModel.isLoading // Lấy state loading

    if (showEditProfileDialog && currentUser != null) {
        EditProfileDialog(
            user = currentUser!!,
            isLoading = authLoading,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updatedUser, imageUri ->
                // Gọi hàm update mới
                authViewModel.updateProfileWithImage(updatedUser, imageUri)

                showEditProfileDialog = false
            }
        )
    }
}

@Composable
fun ProfileSection(user: User?) {
    val displayName = user?.anonymousName?.takeIf { it.isNotBlank() } ?: "SoulMate User"
    val avatarUrl = user?.avatarUrl?.takeIf { it.isNotBlank() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(65.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(65.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buildAvatarInitial(displayName),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                text = user?.email ?: "user@example.com",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

private fun buildAvatarInitial(name: String): String {
    val firstLetter = name.trim().firstOrNull { it.isLetterOrDigit() } ?: 'U'
    return firstLetter.toString().uppercase(Locale.getDefault())
}

@Composable
fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colors.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {},
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = MaterialTheme.colors.onSurface
            )
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}
