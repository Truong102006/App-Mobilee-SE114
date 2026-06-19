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
import com.soulmate.app.ui.components.Screen
import com.soulmate.app.ui.login.AuthViewModel
import java.util.Locale
import androidx.core.net.toUri
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun SettingScreen(
    themeViewModel: ThemeViewModel,
    authViewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    navController: NavController,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    val currentUser by authViewModel.currentUser
    val notificationEnabled by settingsViewModel.notificationEnabled.collectAsState()
    var showEditProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.error.collectLatest { errorMsg ->
            android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

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

        ProfileSection(user = currentUser)

        Spacer(modifier = Modifier.height(24.dp))

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
        SettingItem(
            icon = Icons.Default.Lock,
            title = "Privacy & Security",
            onClick = { showPrivacyDialog = true }
        )
        SettingItem(icon = Icons.Default.Language, title = "Language", subtitle = "Vietnamese")

        Spacer(modifier = Modifier.height(24.dp))

        SettingSectionTitle("Premium")
        SettingItem(
            icon = Icons.Default.Star,
            title = if (currentUser?.isPremiumActive() == true) "Renew Premium" else "Upgrade Premium",
            subtitle = if (currentUser?.isPremiumActive() == true && currentUser?.premiumUntil != null) {
                "Active until ${formatPremiumDate(currentUser!!.premiumUntil!!)}"
            } else {
                "Pay with SePay to upgrade your account"
            },
            onClick = {
                navController.navigate(Screen.Premium.route)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingSectionTitle("Support")
        SettingItem(
            icon = Icons.Default.Info,
            title = "About SoulMate",
            onClick = { showAboutDialog = true }
        )
        SettingItem(
            icon = Icons.Default.Help,
            title = "Help Center",
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:".toUri()
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("support@soulmate.com")) // TODO: Thay email
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback/Support for SoulMate App")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "No email app found!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
        if (currentUser?.role == "admin") {
            SettingItem(
                icon = Icons.Default.SettingsSystemDaydream,
                title = "Admin Control Panel",
                onClick = {
                    navController.navigate("admin_dashboard")
                }
            )
        }

        // dialogs
        if (showPrivacyDialog) {
            PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
        }
        if (showFAQDialog) {
            FAQDialog(onDismiss = { showFAQDialog = false })
        }
        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }

        Spacer(modifier = Modifier.height(32.dp))

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

    val authLoading by authViewModel.isLoading

    if (showEditProfileDialog && currentUser != null) {
        EditProfileDialog(
            user = currentUser!!,
            isLoading = authLoading,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updatedUser, imageUri ->
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
            if (user?.isPremiumActive() == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Premium until ${formatPremiumDate(user.premiumUntil ?: 0L)}",
                        color = Color(0xFFEF6C00),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (user?.isSocialBanned == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "🚫 Tài khoản bị hạn chế cộng đồng",
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun buildAvatarInitial(name: String): String {
    val firstLetter = name.trim().firstOrNull { it.isLetterOrDigit() } ?: 'U'
    return firstLetter.toString().uppercase(Locale.getDefault())
}

private fun formatPremiumDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
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

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Chính sách bảo mật",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colors.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text(
                    "Chào mừng bạn đến với SoulMate. Quyền riêng tư của bạn là ưu tiên hàng đầu của chúng tôi.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1. Thu thập thông tin",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    "Chúng tôi thu thập nhật ký viết tay và dữ liệu cảm xúc tự nhập để cung cấp phân tích tâm trạng bằng AI của Gemini. Các dữ liệu này được bảo mật hoàn toàn.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "2. Bảo mật dữ liệu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    "Mọi thông tin cá nhân và nhật ký của bạn đều được mã hóa và lưu trữ an toàn trên máy chủ đám mây Firebase Cloud Firestore của Google.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "3. Quyền của người dùng",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    "Bạn có quyền xem thông tin cá nhân, sửa đổi, cập nhật hoặc xóa hoàn toàn tài khoản và toàn bộ lịch sử nhật ký của mình bất kỳ lúc nào.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đã hiểu", color = MaterialTheme.colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Về SoulMate",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colors.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "SoulMate App",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    "Phiên bản v1.0.0",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "SoulMate là một ứng dụng di động được xây dựng nhằm hỗ trợ theo dõi tâm trạng, viết ký sự và chăm sóc thú cưng ảo đồng hành, giúp bạn kết nối sâu sắc hơn với nội tâm.",
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Phát triển bởi Nhóm 3 - FIS",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = MaterialTheme.colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun FAQDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Trung tâm trợ giúp & FAQ",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colors.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                FAQItem(
                    question = "SoulMate hoạt động như thế nào?",
                    answer = "SoulMate giúp bạn ghi lại những rung động cảm xúc qua nhật ký và phân tích chúng bằng trí tuệ nhân tạo Gemini AI."
                )
                FAQItem(
                    question = "Làm thế nào để tăng cấp cho thú cưng?",
                    answer = "Viết nhật ký hàng ngày để nhận điểm kinh nghiệm (XP) cho người bạn đồng hành ảo của mình."
                )
                FAQItem(
                    question = "Dữ liệu của tôi có được bảo mật không?",
                    answer = "Tất cả nhật ký và phân tích cảm xúc chỉ hiển thị duy nhất với bạn và được bảo mật tuyệt đối bởi Firebase."
                )
                FAQItem(
                    question = "Ứng dụng có cần kết nối mạng không?",
                    answer = "Có, ứng dụng yêu cầu kết nối mạng internet để đồng bộ dữ liệu đám mây và chạy phân tích AI."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = MaterialTheme.colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun FAQItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.05f))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = question,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.Gray
            )
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = answer,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}
