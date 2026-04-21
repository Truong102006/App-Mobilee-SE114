package com.soulmate.app.ui.setting

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soulmate.app.R

@Composable
fun SettingScreen() {
    // Trạng thái Dark Mode (Lưu ý: Để áp dụng toàn app cần đẩy biến này lên cấp cao hơn hoặc dùng ViewModel)
    var isDarkMode by remember { mutableStateOf(false) }
    var notificationEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else Color.Black,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // --- SECTION: PROFILE (Đã sửa dùng ảnh ava1) ---
        ProfileSection(isDarkMode)

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION: GENERAL ---
        SettingSectionTitle("General", isDarkMode)
        SettingItem(
            icon = Icons.Default.Brightness4,
            title = "Dark Mode",
            isDarkMode = isDarkMode,
            trailing = {
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { isDarkMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6200EE),
                        checkedTrackColor = Color(0xFFBB86FC)
                    )
                )
            }
        )
        SettingItem(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            isDarkMode = isDarkMode,
            trailing = {
                Switch(
                    checked = notificationEnabled,
                    onCheckedChange = { notificationEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6200EE))
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION: ACCOUNT & SECURITY ---
        SettingSectionTitle("Account", isDarkMode)
        SettingItem(icon = Icons.Default.Person, title = "Edit Profile", isDarkMode = isDarkMode)
        SettingItem(icon = Icons.Default.Lock, title = "Privacy & Security", isDarkMode = isDarkMode)
        SettingItem(icon = Icons.Default.Language, title = "Language", subtitle = "Vietnamese", isDarkMode = isDarkMode)

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION: SUPPORT ---
        SettingSectionTitle("Support", isDarkMode)
        SettingItem(icon = Icons.Default.Info, title = "About SoulMate", isDarkMode = isDarkMode)
        SettingItem(icon = Icons.Default.Help, title = "Help Center", isDarkMode = isDarkMode)

        Spacer(modifier = Modifier.height(32.dp))

        // --- LOGOUT BUTTON ---
        Button(
            onClick = { /* Xử lý đăng xuất */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF4444))
        ) {
            Text(text = "Log Out", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp)) // Padding dưới cùng để tránh bị che bởi Bottom Bar
    }
}

@Composable
fun ProfileSection(isDarkMode: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDarkMode) Color(0xFF1E1E1E) else Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- THAY THẾ BOX TEXT THÀNH IMAGE ---
        Image(
            painter = painterResource(id = R.drawable.ava1), // Đảm bảo file tên là ava1 trong drawable
            contentDescription = "Avatar",
            modifier = Modifier
                .size(65.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = "Dmanhz",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = if (isDarkMode) Color.White else Color.Black
            )
            Text(
                text = "dmanhz@gmail.com",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SettingSectionTitle(title: String, isDarkMode: Boolean) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isDarkMode) Color(0xFFBB86FC) else Color(0xFF6200EE),
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    isDarkMode: Boolean,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkMode) Color(0xFF1E1E1E) else Color.White)
            .clickable { /* Xử lý khi click vào item */ }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDarkMode) Color.LightGray else Color.DarkGray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = if (isDarkMode) Color.White else Color.Black
            )
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                // Nếu bị lỗi đỏ ở ChevronRight, hãy dùng KeyboardArrowRight
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}