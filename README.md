# SoulMate

![Android](https://img.shields.io/badge/Android-API%2034-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02-4285F4)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20Storage-FFCA28?logo=firebase&logoColor=black)

SoulMate là đồ án Android về nhật ký cảm xúc, phân tích mood bằng AI, cộng đồng chia sẻ, chat cá nhân, thú cưng đồng hành và gói premium. Runtime chính của dự án hiện đi theo mô hình `Android app + Spring Boot backend + Firebase`.

## Quick Start

1. Chuẩn bị `app/google-services.json`.
2. Tạo `backend-spring/.env` từ `backend-spring/.env.example`.
3. Đặt Firebase Admin service account vào `backend-spring/secrets/service-account.json`.
4. Chạy backend ở cổng `8080`.
5. Mở project bằng Android Studio và chạy app trên emulator hoặc điện thoại Android.

## Kiến trúc repo

| Đường dẫn | Vai trò |
| --- | --- |
| `app/` | Ứng dụng Android viết bằng Kotlin + Jetpack Compose |
| `backend-spring/` | Backend Spring Boot xử lý AI mood, Cloudinary, diary, chat, payment |
| `functions/` | Firebase Functions cũ, hiện không phải runtime chính |

## Stack chính

| Khối | Công nghệ |
| --- | --- |
| Android | Kotlin `1.9.24`, Jetpack Compose, Hilt, Retrofit, Room, DataStore |
| Backend | Spring Boot `3.5.0`, Maven Wrapper, Firebase Admin SDK |
| Dữ liệu | Firebase Auth, Firestore, Firebase Storage |
| Tích hợp | Gemini, Cloudinary, OneSignal, SePay |
| Build | Android Gradle Plugin `8.2.2`, Java `17` |

## Yêu cầu môi trường

- Windows 10 hoặc Windows 11.
- Android Studio đủ mới để làm việc với Android Gradle Plugin `8.2.2`.
- JDK `17` trở lên.
- Android SDK packages:
  - `platform-tools`
  - `platforms;android-34`
  - `build-tools;34.0.0`
  - `emulator`
  - `system-images;android-34;google_apis;x86_64` nếu chạy emulator
- Một Android Emulator hoặc điện thoại Android thật đã bật `USB debugging`.
- Internet để truy cập Firebase, Cloudinary, Gemini, OneSignal và SePay khi test đầy đủ tính năng.

Ghi chú:

- Android app mặc định gọi backend local qua:
  - emulator: `http://10.0.2.2:8080/`
  - thiết bị thật: `http://localhost:8080/` sau khi chạy `adb reverse`

## File cấu hình bắt buộc

| File | Bắt buộc | Mục đích |
| --- | --- | --- |
| `app/google-services.json` | Có | Cấu hình Firebase cho app Android |
| `backend-spring/.env` | Có | Cấu hình runtime backend local |
| `backend-spring/secrets/service-account.json` | Có | Firebase Admin credentials cho backend |
| `.firebaserc` | Không bắt buộc để chạy app | Chỉ cần khi làm việc với Firebase CLI |

Quy tắc quan trọng:

- `app/google-services.json`, `backend-spring/.env` và `backend-spring/secrets/service-account.json` phải cùng một Firebase project.
- `FIREBASE_PROJECT_ID` và `FIREBASE_STORAGE_BUCKET` trong `.env` phải khớp với project của `google-services.json`.
- Đường dẫn `GOOGLE_APPLICATION_CREDENTIALS` nên giữ là `secrets/service-account.json`.

## Setup backend

Tất cả lệnh dưới đây nên chạy từ thư mục gốc repo, trừ khi có ghi khác.

### 1. Tạo file `.env`

```powershell
Copy-Item "backend-spring\.env.example" "backend-spring\.env"
```

Điền các biến tối thiểu sau:

```env
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
GOOGLE_APPLICATION_CREDENTIALS=secrets/service-account.json
FIREBASE_PROJECT_ID=your-firebase-project-id
FIREBASE_STORAGE_BUCKET=your-firebase-project-id.firebasestorage.app
BACKEND_SECURITY_REQUIRE_APP_CHECK=false
```

`BACKEND_SECURITY_REQUIRE_APP_CHECK=false` được khuyến nghị cho local vì app hiện chưa gửi `X-Firebase-AppCheck` ở mọi luồng.

Các biến cần thêm nếu muốn test đầy đủ tính năng:

- `GEMINI_API_KEY`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`
- `ONESIGNAL_ENABLED`, `ONESIGNAL_APP_ID`, `ONESIGNAL_API_KEY`
- `SEPAY_*`
- `PREMIUM_*`

### 2. Đặt Firebase service account

Tạo thư mục nếu chưa có:

```powershell
New-Item -ItemType Directory -Force "backend-spring\secrets"
```

Sau đó đặt file JSON tại:

```text
backend-spring/secrets/service-account.json
```

### 3. Chạy backend

```powershell
cd .\backend-spring
.\mvnw.cmd spring-boot:run
```

Backend mặc định chạy ở `http://localhost:8080`.

## Setup Android app

### 1. Mở project

1. Mở thư mục gốc repo bằng Android Studio.
2. Chờ Gradle sync hoàn tất.
3. Kiểm tra `local.properties`; Android Studio thường tự tạo khi nhận diện SDK.

### 2. Kiểm tra build target

Project Android hiện dùng:

- `compileSdk 34`
- `targetSdk 34`
- `minSdk 24`
- Java/Kotlin target `17`

### 3. Kiểm tra Firebase config

File Firebase phải nằm đúng vị trí:

```text
app/google-services.json
```

Nếu file này thiếu, Gradle vẫn có thể sync nhưng các tính năng Firebase sẽ không hoạt động đúng.

### 4. Chọn thiết bị chạy

#### Emulator

- Khuyến nghị emulator API 34, Google APIs, `x86_64`.
- App sẽ tự dùng `http://10.0.2.2:8080/` để gọi backend local.

#### Điện thoại Android thật

1. Bật `Developer options` và `USB debugging`.
2. Kết nối điện thoại bằng USB.
3. Chạy:

```powershell
adb reverse tcp:8080 tcp:8080
```

Khi đó app sẽ dùng `http://localhost:8080/`.

### 5. Chạy app

1. Đảm bảo backend đã chạy trước.
2. Trong Android Studio, chọn cấu hình `app`.
3. Chọn emulator hoặc điện thoại thật.
4. Bấm `Run`.

## Override backend URL

Nếu cần test qua Wi-Fi/LAN hoặc backend khác, có thể override URL khi cài app:

```powershell
.\gradlew installDebug -PBACKEND_BASE_URL=http://192.168.1.10:8080/
```

Hoặc override riêng cho emulator và thiết bị thật:

```powershell
.\gradlew installDebug -PBACKEND_BASE_URL_DEVICE=http://localhost:8080/ -PBACKEND_BASE_URL_EMULATOR=http://10.0.2.2:8080/
```

## Tính năng nào cần cấu hình thêm

| Tính năng | Cần gì để hoạt động đầy đủ |
| --- | --- |
| Đăng nhập và dữ liệu cơ bản | Firebase Auth + Firestore + service account |
| Upload ảnh diary, chat, community | `CLOUDINARY_*` |
| Phân tích mood bằng AI | `GEMINI_API_KEY` |
| Push notification | `ONESIGNAL_*` |
| Premium payment | `SEPAY_*` và `PREMIUM_*` |

## Troubleshooting

- `401` hoặc `403` từ backend: kiểm tra `BACKEND_SECURITY_REQUIRE_APP_CHECK`, Firebase project, ID token và service account.
- App không gọi được backend trên điện thoại thật: chạy lại `adb reverse tcp:8080 tcp:8080`.
- App không gọi được backend trên emulator: xác nhận backend đang nghe ở cổng `8080` và app dùng `10.0.2.2`.
- Build lỗi vì Java: kiểm tra `java -version`, cần `17+`.
- Firebase không hoạt động: kiểm tra lại `app/google-services.json` và `backend-spring/secrets/service-account.json` có cùng project hay không.

## Thành phần không bắt buộc để chạy runtime chính

- `functions/` vẫn còn trong repo nhưng hiện không phải backend chính của app Android.
- Bạn không cần setup `functions/` để chạy luồng local chuẩn của dự án này.

## Đóng góp

- Không commit secrets như `.env`, `service-account.json` hoặc file cấu hình Firebase riêng của nhóm.
- Nếu chỉnh runtime local, ưu tiên cập nhật README hoặc `docs/` để người sau dựng môi trường nhanh hơn.
