import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soulmate.app.ui.social.CommunityCard
import com.soulmate.app.ui.social.CommunityPost

@Composable
fun CommunityScreen() {
    val feedPosts = remember { getMockCommunityPosts() }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colors.surface)) {
                Spacer(modifier = Modifier.height(30.dp))
                TopAppBar(
                    title = { Text("Feeds", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.primary,
                    elevation = 0.dp
                )
            }
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(feedPosts, key = { it.id }) { post ->
                CommunityCard(post = post)
            }
        }
    }
}

// ==========================================
// HÀM TẠO DỮ LIỆU ẢO (MOCK DATA)
// ==========================================
private fun getMockCommunityPosts(): List<CommunityPost> {
    return listOf(
        CommunityPost(
            id = "post_1",
            userName = "Marvin McKinney",
            userAvatarUrl = null,
            isVerified = true,
            mood = "Happy",
            timeAgo = "Today at 6:41",
            textContent = "<h3>10 Tips for Beginners in Stock Market Investing</h3><p>Start your journey today with these simple steps. Don't let the market scare you! 📈💰</p>",
            imageUrls = listOf(
                "https://dummyimage.com/600x400/4caf50/ffffff.png&text=Investing+101",
                "https://dummyimage.com/600x400/2196f3/ffffff.png&text=Stock+Market"
            ),
            likeCount = 2321,
            commentCount = 5321,
            viewCount = 8900
        ),
        CommunityPost(
            id = "post_2",
            userName = "Nguyễn Khánh",
            userAvatarUrl = null,
            isVerified = false,
            mood = "Peaceful",
            timeAgo = "Yesterday at 14:30",
            textContent = "<h3>Hoàn thành xong Demo</h3><p>Hôm nay thời tiết thật đẹp, mình đã hoàn thành xong đồ án môn học. Một ngày thật năng suất và ý nghĩa! Cảm giác code chạy mượt mà không lỗi (crash) thật là <b>tuyệt vời</b> ✨</p>",
            imageUrls = emptyList(),
            likeCount = 128,
            commentCount = 12,
            viewCount = 450
        ),
        CommunityPost(
            id = "post_3",
            userName = "Sarah Jenkins",
            userAvatarUrl = null,
            isVerified = true,
            mood = "Sad",
            timeAgo = "2 days ago",
            textContent = "<p>Sometimes things don't go as planned. Taking a step back to breathe and reflect today. Tomorrow is a new start. 🌧️</p>",
            imageUrls = listOf(
                "https://dummyimage.com/600x400/9e9e9e/ffffff.png&text=Rainy+Day"
            ),
            likeCount = 890,
            commentCount = 145,
            viewCount = 3200
        )
    )
}