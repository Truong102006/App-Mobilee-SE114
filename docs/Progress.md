# SoulMate - Báo Cáo Tiến Độ Tổng Thể

> **Ngày cập nhật:** 2026-05-14  
> **Phương pháp:** Deep scan toàn bộ source code (Android, Backend Spring, Firebase Functions, Firestore Rules)

---

## 1. Tổng Quan Dự Án

SoulMate là ứng dụng chăm sóc sức khỏe tinh thần (mental health) trên Android, cho phép người dùng viết nhật ký, phân tích cảm xúc bằng AI, nghe nhạc thư giãn, tham gia cộng đồng chia sẻ và nhắn tin trực tiếp.

### Kiến trúc hệ thống

```
┌──────────────────┐    REST/JSON     ┌────────────────────┐     Admin SDK     ┌──────────────┐
│  Android App     │ ──────────────►  │  Spring Boot       │ ──────────────►   │  Firestore   │
│  (Jetpack        │  Bearer Token    │  Backend Gateway   │                   │  (NoSQL DB)  │
│   Compose + Hilt)│                  │  (Java 21)         │                   └──────────────┘
└────────┬─────────┘                  └────────┬───────────┘
         │                                     │
         │ Firebase Auth                       │ Gemini API, Cloudinary
         │ Google Sign-In                      │ (server-side secrets)
         ▼                                     ▼
┌──────────────────┐                  ┌────────────────────┐
│  Firebase Auth   │                  │  Cloud Functions   │
│  (ID Token)      │                  │  (TypeScript,      │
│                  │                  │   Node 20)         │
└──────────────────┘                  └────────────────────┘
```

**Dự án có 3 thành phần triển khai:**
1. **Android App** (`app/`) — Frontend chính
2. **Spring Boot Backend** (`backend-spring/`) — API Gateway bảo mật
3. **Firebase Cloud Functions** (`functions/`) — Serverless backend (phiên bản thay thế/bổ sung)

---

## 2. Công Nghệ Sử Dụng

### 2.1. Android Frontend (`app/`)

| Hạng mục | Công nghệ | Phiên bản |
|---|---|---|
| **Ngôn ngữ** | Kotlin | 1.9.24 |
| **UI Framework** | Jetpack Compose | BOM 2024.02.00 |
| **Design System** | Material 2 + Material 3 (dùng song song) | — |
| **DI Framework** | Dagger Hilt | 2.50 |
| **Navigation** | Navigation Compose | 2.7.7 |
| **Networking** | Retrofit 2 + Gson Converter | 2.9.0 |
| **Image Loading** | Coil Compose + Coil GIF + Glide Compose | 2.4.0 / 4.16.0 |
| **Media Player** | AndroidX Media3 (ExoPlayer) | 1.2.0 |
| **Rich Text** | RichEditor Compose | 1.0.0-rc08 |
| **Charts** | Vico (Compose) | 1.15.0 |
| **Auth** | Firebase Auth + Google Sign-In | BOM 32.8.0 / 21.1.0 |
| **Database (Remote)** | Firebase Firestore | BOM 32.8.0 |
| **Database (Local)** | Room (dependency có nhưng chưa triển khai) | 2.6.1 |
| **Storage** | Firebase Storage (dependency có) + Cloudinary (upload thực tế) | — |
| **Preferences** | DataStore Preferences | 1.0.0 |
| **Build System** | Gradle + AGP | 8.2.1 / 8.2.2 |
| **Min SDK / Target SDK** | 24 / 34 | — |
| **Java Target** | JVM 17 | — |

### 2.2. Spring Boot Backend (`backend-spring/`)

| Hạng mục | Công nghệ | Phiên bản |
|---|---|---|
| **Framework** | Spring Boot | 3.5.0 |
| **Ngôn ngữ** | Java | 21 |
| **Build Tool** | Maven | — |
| **Firebase Admin** | firebase-admin SDK | 9.8.0 |
| **JWT Verification** | Nimbus JOSE JWT | 10.6 |
| **Monitoring** | Spring Boot Actuator | — |
| **Validation** | Spring Boot Starter Validation | — |

**Backend cung cấp 9 endpoint bảo mật:**
- `GET /api/secure/ping` — Health check
- `POST /api/secure/ai/predict-mood` — Phân tích mood qua Gemini
- `POST /api/secure/cloudinary/sign-upload` — Ký URL upload Cloudinary
- `POST /api/secure/diaries/save` — Lưu nhật ký
- `GET /api/secure/diaries/me` — Lấy danh sách nhật ký
- `DELETE /api/secure/diaries/{diaryId}` — Xóa nhật ký
- `POST /api/secure/chats/send` — Gửi tin nhắn
- `GET /api/secure/chats/conversation/{otherUserId}` — Lấy cuộc trò chuyện
- `DELETE /api/secure/chats/conversation/{otherUserId}` — Xóa cuộc trò chuyện
- `GET /api/secure/chats/inbox` — Lấy danh sách hộp thư

### 2.3. Firebase Cloud Functions (`functions/`)

| Hạng mục | Công nghệ | Phiên bản |
|---|---|---|
| **Runtime** | Node.js | 20 |
| **Ngôn ngữ** | TypeScript | 5.8.3 |
| **Firebase Admin** | firebase-admin | 13.9.0 |
| **Firebase Functions** | firebase-functions (v2) | 7.2.5 |
| **Region** | asia-southeast1 | — |

**Cloud Functions cung cấp 8 callable functions:**
- `pingSecure` — Health check
- `predictMoodSecure` — AI mood prediction (Gemini 1.5 Flash)
- `getCloudinarySignedUpload` — Signed upload URL
- `saveDiarySecure` — CRUD nhật ký
- `listMyDiariesSecure` — Liệt kê nhật ký
- `deleteDiarySecure` — Xóa nhật ký
- `sendChatMessageSecure` — Gửi tin nhắn
- `listConversationSecure` / `deleteConversationSecure` — Quản lý chat

### 2.4. Firebase / Firestore

| Hạng mục | Chi tiết |
|---|---|
| **Project ID** | `soulmate-app-777bc` |
| **Firestore Rules** | Deny-all cho client (tất cả access qua Admin SDK) |
| **Firestore Indexes** | 2 composite indexes: `diaries(user_id↑, timestamp↓)`, `chats(conversationId↑, timestamp↑)` |
| **Collections** | `diaries`, `chats`, `users`, `community_posts` |

### 2.5. Dịch vụ bên thứ 3

| Dịch vụ | Mục đích |
|---|---|
| **Google Gemini AI** (1.5 Flash) | Phân tích cảm xúc từ text nhật ký |
| **Cloudinary** | Upload & lưu trữ hình ảnh (signed upload) |
| **Firebase Auth** | Xác thực (Email/Password + Google Sign-In) |

---

## 3. Tiến Độ Theo Module

### Bảng tổng hợp

| Module | Tiến độ | Trạng thái |
|---|---:|---|
| **Authentication (Login/Register/Google)** | 90% | ✅ Hoàn thiện cao — Email/Password + Google Sign-In hoạt động, profile lưu Firestore |
| **Home + Music Player** | 85% | ✅ Giao diện đầy đủ, player local hoạt động, mini player + fullscreen |
| **Diary Editor** | 80% | ✅ Rich text editor, mood selector, chọn ảnh, phân tích AI, lưu qua backend |
| **Diary History** | 80% | ✅ Đã nối Firestore qua backend, load/xóa/sửa/xem chi tiết hoạt động |
| **Stats / Mood Statistics** | 70% | ⚠️ ViewModel load dữ liệu từ backend, UI chart đã có, nhưng StatsScreen có thể chưa nối hoàn chỉnh |
| **Community / Social** | 75% | ✅ CRUD posts + comments + likes qua Firestore trực tiếp (realtime listener) |
| **Chat / Messaging** | 80% | ✅ Chat 1-1 qua backend, gửi text + ảnh, polling, inbox, xóa conversation |
| **Settings** | 55% | ⚠️ Dark mode persist qua DataStore, notification toggle có, nhưng profile edit chưa hoàn thiện |
| **Edit Profile** | 50% | ⚠️ UI có, upload avatar qua Cloudinary có, nhưng `updateUserProfile()` = `TODO()` |
| **Recording / Voice** | 25% | ❌ `AudioRecorder` rỗng, `RecordingScreen` có UI nhưng chưa có logic ghi âm thật |
| **Pet** | 5% | ❌ `PetScreen` rỗng, `PetViewModel` rỗng, `CalculatePetXPUseCase` rỗng |
| **Room / Offline DB** | 5% | ❌ Entity + DAO đã khai báo nhưng rỗng, chưa có `@Database`, chưa có truy vấn |
| **Backend Spring Boot** | 90% | ✅ 10 endpoint, security filter, Firebase Auth + App Check verify, Gemini proxy, Cloudinary signed upload |
| **Cloud Functions** | 90% | ✅ 8 callable functions, full validation, App Check enforcement |
| **Testing** | 15% | ❌ Chỉ có 1 file test: `GetMoodStatisticsUseCaseTest.kt` |

---

## 4. Phân Tích Chi Tiết Từng Module

### 4.1. ✅ Authentication — 90%

**Đã hoàn thành:**
- Login bằng Email/Password (`AuthRepositoryImpl.login()`)
- Đăng ký tài khoản mới (`AuthRepositoryImpl.register()`) — tự tạo profile trên Firestore
- Google Sign-In (`AuthRepositoryImpl.signInWithGoogle()`) — tạo/cập nhật profile
- Logout (`AuthRepositoryImpl.logout()`)
- `getCurrentUser()` trả về `User` từ `FirebaseAuth.currentUser`
- `getUserProfile(uid)` lấy profile từ Firestore
- UI: `LoginScreen.kt` (12KB), `RegisterScreen.kt` (13KB) — giao diện đầy đủ

**Còn thiếu:**
- ❌ `updateUserProfile()` đang là `TODO("Not yet implemented")` — **crash nếu gọi**
- ⚠️ Chưa có forgot password flow
- ⚠️ Chưa có email verification

**File liên quan:**
- `ui/login/AuthViewModel.kt`, `LoginScreen.kt`, `RegisterScreen.kt`
- `data/repository/AuthRepositoryImpl.kt`
- `domain/repository/IAuthRepository.kt`

### 4.2. ✅ Home + Music Player — 85%

**Đã hoàn thành:**
- `HomeScreen.kt` (14KB) — giao diện home, hiển thị danh sách nhạc, mood cards
- `MusicViewModel.kt` (5KB) — quản lý phát nhạc qua Media3 ExoPlayer
- `BottomMusicPlayer.kt`, `MusicPlayerDetailScreen.kt` — mini player + fullscreen
- `MoodCard.kt` (21KB) — card hiển thị mood, tương tác swipe
- `HeaderSection.kt` — header với avatar, chat bubble
- `SongItem.kt` — hiển thị bài hát
- Phát/tạm dừng/chuyển bài/quay lại hoạt động

**Còn thiếu:**
- ❌ `HomeViewModel.kt` rỗng (chỉ có class khai báo, 4 dòng)
- ⚠️ Nhạc chỉ phát từ asset local, chưa có streaming

### 4.3. ✅ Diary Editor — 80%

**Đã hoàn thành:**
- `MultimediaEditor.kt` (19KB) — editor với rich text (RichEditor Compose)
- `DiaryViewModel.kt` — quản lý state, gọi `AnalyzeMoodUseCase` + `SaveDiaryUseCase`
- `MoodSelector.kt` — chọn mood
- `RichTextToolBar.kt` — toolbar formatting
- `EditorBottomToolbar.kt`, `EditorComponents.kt`
- `Mood.kt` — enum mood
- Phân tích mood bằng AI (Gemini) qua backend
- Lưu diary qua backend REST API
- Hỗ trợ chọn nhiều ảnh, upload qua Cloudinary
- Hỗ trợ edit diary đã tồn tại (truyền `diaryId` qua navigation)

**Còn thiếu:**
- ⚠️ `title` chưa được lưu trong `DiaryUiState` (diary tạo với title rỗng)
- ⚠️ `audioUrl` chưa được xử lý trong editor flow

### 4.4. ✅ Diary History — 80%

**Đã hoàn thành (THAY ĐỔI LỚN so với lần đánh giá trước):**
- `HistoryViewModel.kt` — **ĐÃ NỐI FIRESTORE** qua `diaryRepository.getDiaries()` (không còn dữ liệu cứng)
- `HistoryScreen.kt` (9KB) — danh sách nhật ký theo timeline
- `DiaryDetailScreen.kt` (8KB) — xem chi tiết nhật ký
- `EditNoteDialog.kt` — dialog chỉnh sửa
- `Timeline.kt` (10KB) — hiển thị timeline
- `MonthSelector.kt`, `MonthYearPickerDialog.kt` — chọn tháng/năm
- Delete diary qua backend (`diaryRepository.deleteDiary()`)
- Update note qua `updateNote()`
- Navigate to edit (`onNavigateToEdit`) và detail (`onNavigateToDetail`)
- Chia sẻ diary lên Community từ DiaryDetail

**Còn thiếu:**
- ⚠️ `patchDiary()` ở backend chưa có endpoint riêng (đang dùng full save)
- ⚠️ Search/filter diary chưa có

### 4.5. ⚠️ Stats / Mood Statistics — 70%

**Đã hoàn thành:**
- `StatsViewModel.kt` — load diaries từ `IDiaryRepository`, truyền xuống `StatsUiState`
- `StatsScreen.kt` (15KB) — UI hiển thị thống kê
- `StatsComponents.kt` (7KB) — các component chart/thống kê
- `GetMoodStatisticsUseCase.kt` — grouping, counting, sorting mood
- `MoodStatistic.kt` — domain model

**Còn thiếu:**
- ⚠️ Cần verify `StatsScreen` có sử dụng `GetMoodStatisticsUseCase` hay tính toán trực tiếp từ diaries
- ⚠️ Chưa rõ chart (Vico) đã render đúng dữ liệu thực hay chưa

### 4.6. ✅ Community / Social — 75%

**Đã hoàn thành:**
- `CommunityViewModel.kt` — CRUD posts + comments, toggle like
- `CommunityRepositoryImpl.kt` (10KB) — **trực tiếp dùng Firestore client** (không qua backend!)
- `CommunityScreen.kt` — danh sách bài đăng
- `CommunityCard.kt` (21KB) — card bài đăng phức tạp
- `CommentSection.kt` (13KB) — bình luận + reply
- `CommunityPost.kt` — data model
- Realtime listener via `callbackFlow` + `addSnapshotListener`
- Toggle like với Firestore transaction
- Nested comments (parent_id, reply_to_user_name)
- Upload ảnh community qua Cloudinary

**Còn thiếu:**
- ⚠️ **Truy cập Firestore trực tiếp từ client** — vi phạm nguyên tắc bảo mật (Firestore rules deny-all, nhưng community_posts chưa có rule riêng → có thể bị block)
- ⚠️ Chưa có content moderation (AI)
- ⚠️ Chưa có report/block user

### 4.7. ✅ Chat / Messaging — 80%

**Đã hoàn thành:**
- `ChatViewModel.kt` (6KB) — load messages, send text/image, reply, delete
- `ChatRepositoryImpl.kt` (5KB) — giao tiếp qua backend REST API
- `ChatListScreen.kt` (17KB) — danh sách cuộc trò chuyện (inbox)
- `ChatDetailScreen.kt` (30KB) — chat chi tiết, gửi text + ảnh
- Gửi ảnh qua Cloudinary upload
- Reply to message
- Delete conversation
- Polling interval: 2 giây

**Còn thiếu:**
- ⚠️ `markAsRead()` — `TODO` (chưa có backend endpoint)
- ⚠️ `deleteMessage()` — `TODO` (chưa có backend endpoint)
- ⚠️ Polling thay vì realtime (WebSocket/Firestore listener) → tốn bandwidth
- ⚠️ Chưa có push notification cho tin nhắn mới
- ⚠️ Inbox endpoint (`GET /api/secure/chats/inbox`) cần verify đã implement đầy đủ ở backend

### 4.8. ⚠️ Settings — 55%

**Đã hoàn thành:**
- `SettingScreen.kt` (10KB) — giao diện settings
- `SettingsViewModel.kt` — notification toggle
- `ThemeViewModel.kt` — dark mode toggle
- `ISettingsRepository` + `SettingsRepositoryImpl` — **DataStore Preferences persist** (dark mode + notification)
- `EditProfileScreen.kt` (16KB) — UI chỉnh sửa profile
- Logout hoạt động (Firebase + Google Sign-In)

**Còn thiếu:**
- ❌ `AuthRepositoryImpl.updateUserProfile()` = `TODO()` — **update profile sẽ crash**
- ⚠️ Privacy, Help, About chưa có nội dung thật
- ⚠️ Reminder time chưa persist

### 4.9. ❌ Recording / Voice — 25%

**Đã hoàn thành:**
- `RecordingScreen.kt` (8KB) — UI hiển thị speech-to-text
- Xin quyền microphone

**Còn thiếu:**
- ❌ `AudioRecorder.kt` — **hoàn toàn rỗng** (chỉ có class declaration)
- ❌ Chưa có pipeline ghi âm audio file
- ❌ Chưa có lưu audio vào diary
- ❌ `DateFormatter.kt` — **rỗng**

### 4.10. ❌ Pet — 5%

**Trạng thái: Placeholder, chưa có gì**

- `PetScreen.kt` — rỗng (8 dòng, `@Composable fun PetScreen() {}`)
- `PetViewModel.kt` — rỗng (10 dòng, chỉ có annotation)
- `CalculatePetXPUseCase.kt` — rỗng (5 dòng)
- `IPetRepository.kt` — rỗng (interface không method)
- `PetRepositoryImpl.kt` — rỗng
- `SoulPet.kt` — domain model tồn tại nhưng chưa dùng

### 4.11. ❌ Room / Local Database — 5%

**Trạng thái: Khai báo nhưng chưa triển khai**

- `DiaryEntity.kt` — chỉ có `@PrimaryKey id`, không có field nào khác
- `PetEntity.kt` — chỉ có `@PrimaryKey id`
- `DiaryDao.kt` — `@Dao interface` rỗng
- `PetDao.kt` — `@Dao interface` rỗng
- ❌ **Không có `@Database` class** — Room chưa được khởi tạo
- ❌ Không hỗ trợ offline

---

## 5. Đánh Giá Backend

### 5.1. Spring Boot Backend — 90%

**Cấu trúc (38 files Java):**

| Package | Files | Chức năng |
|---|---|---|
| `config/` | 4 | Firebase Admin init, properties, HTTP client |
| `controller/` | 5 | AI, Chat, Cloudinary, Diary, Security endpoints |
| `dto/` | 15 | Request/Response DTOs cho tất cả endpoints |
| `exception/` | 3 | Global exception handler, API error response |
| `security/` | 6 | Firebase JWT filter, App Check verifier, Auth context |
| `service/` | 4 | ChatService, CloudinaryService, DiaryService, GeminiService |

**Điểm mạnh:**
- ✅ Security filter xác minh Firebase ID Token + App Check
- ✅ Tất cả secrets (Gemini API Key, Cloudinary) ở server-side
- ✅ Ownership verification (chỉ user tạo mới được sửa/xóa diary)
- ✅ Global exception handling
- ✅ Actuator monitoring

**Còn thiếu:**
- ⚠️ Chưa có rate limiting
- ⚠️ Chưa có CORS configuration
- ⚠️ Chưa có unit test cho backend
- ⚠️ `markAsRead` và `deleteMessage` chưa implement

### 5.2. Cloud Functions — 90%

**Điểm mạnh:**
- ✅ Tất cả functions enforce App Check + Auth
- ✅ Input validation chặt (maxLength, type check, required fields)
- ✅ Secrets quản lý qua `defineSecret()` (Firebase Secret Manager)
- ✅ Gemini API key không expose ra client

**Lưu ý:**
- ⚠️ Cloud Functions và Spring Boot backend hiện **song song tồn tại** — Android app đang gọi Spring Boot backend, Cloud Functions là phiên bản serverless thay thế
- ⚠️ Cần quyết định dùng 1 trong 2 (hoặc phân chia rõ ràng)

---

## 6. Các Vấn Đề Bảo Mật & Rủi Ro

### 🔴 Critical

| # | Vấn đề | Vị trí | Ảnh hưởng |
|---|---|---|---|
| 1 | **`.env` chứa Cloudinary API Secret commit vào repo** | `.env` line 16 | Credential leak — bất kỳ ai clone repo đều có key |
| 2 | **`service-account.json` commit vào repo** | `backend-spring/service-account.json` | Firebase Admin credential leak — full DB access |
| 3 | **`google-services (1).json` trong `app/`** | `app/google-services (1).json` | Firebase project config exposed |
| 4 | **`updateUserProfile()` = `TODO()`** | `AuthRepositoryImpl.kt:144` | Runtime crash khi user cập nhật profile |

### 🟡 Warning

| # | Vấn đề | Vị trí |
|---|---|---|
| 5 | Community module truy cập Firestore trực tiếp (bypass backend security) | `CommunityRepositoryImpl.kt` |
| 6 | Chat dùng polling 2s thay vì realtime → tốn battery & bandwidth | `ChatRepositoryImpl.kt` |
| 7 | Cloudinary upload dùng OkHttp trực tiếp thay vì qua Retrofit instance | `CloudinaryHelper.kt` |
| 8 | Material 2 + Material 3 dùng song song → inconsistent UI | `app/build.gradle` |
| 9 | Navigation Compose khai báo 2 lần (2.7.7 và 2.7.6) | `app/build.gradle` line 95 + 129 |

---

## 7. Tổng Số File Source Code

| Thành phần | Số file | Tổng dung lượng |
|---|---|---|
| **Android (Kotlin)** | ~75 files | ~350 KB |
| **Backend Spring (Java)** | 38 files | ~50 KB |
| **Cloud Functions (TypeScript)** | 1 file | 16 KB |
| **Config / Rules / Indexes** | ~10 files | ~5 KB |
| **Test** | 1 file | 2.4 KB |

---

## 8. Tổng Kết: Hoàn Thiện vs Thiếu Sót

### ✅ Đã hoàn thiện tốt (>75%)

1. **Authentication** — Login/Register/Google Sign-In hoạt động đầy đủ
2. **Diary CRUD end-to-end** — Editor → AI mood → Save → History → Edit → Delete
3. **Backend Security** — Spring Boot gateway với Firebase Auth + App Check verification
4. **Chat 1-1** — Gửi text/ảnh, inbox, xóa conversation
5. **Community Social** — CRUD posts + comments + likes (realtime)
6. **Music Player** — Phát nhạc local với mini/fullscreen player
7. **Image Upload** — Cloudinary signed upload qua backend

### ⚠️ Cần hoàn thiện thêm (40-75%)

1. **Stats UI** — ViewModel có data nhưng cần verify chart render đúng
2. **Settings** — Dark mode persist OK, nhưng profile update crash (TODO)
3. **Recording** — UI có nhưng AudioRecorder rỗng

### ❌ Chưa triển khai (<25%)

1. **Pet Module** — Toàn bộ rỗng (Screen, ViewModel, UseCase, Repository)
2. **Room / Offline** — Entity + DAO rỗng, không có @Database
3. **Testing** — Chỉ 1 unit test duy nhất
4. **Push Notifications** — Chưa có
5. **Forgot Password / Email Verification** — Chưa có

### 📊 Ước lượng tổng thể

| Tiêu chí | Mức độ |
|---|---|
| **Hoàn thiện cho demo/báo cáo môn học** | **~75-80%** |
| **Sẵn sàng production** | **~45-50%** |

---

## 9. Đề Xuất Ưu Tiên

### Ưu tiên 1: Sửa lỗi critical (1-2 ngày)
1. ❌ Implement `updateUserProfile()` (đang TODO → crash)
2. ❌ Xóa `service-account.json` và `.env` khỏi git, thêm vào `.gitignore`
3. ❌ Xóa Cloudinary credentials khỏi `.env` đã commit

### Ưu tiên 2: Hoàn thiện module có sẵn (3-5 ngày)
4. ⚠️ Verify StatsScreen nối đúng data
5. ⚠️ Implement `markAsRead()` + `deleteMessage()` ở backend
6. ⚠️ Chuyển Community module qua backend (hiện bypass security)

### Ưu tiên 3: Module mới (tùy yêu cầu)
7. Pet Module — thiết kế + implement
8. AudioRecorder — implement ghi âm thật
9. Room offline database — implement cache diary

### Ưu tiên 4: Chất lượng (nên làm)
10. Thêm unit test cho repositories + ViewModels
11. Dọn dependency trùng (Navigation Compose, Material 2/3)
12. Thêm ProGuard rules cho release build
