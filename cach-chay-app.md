# Cách chạy app

Tài liệu này hướng dẫn chạy app Android của dự án trên Windows bằng PowerShell, theo cả 2 cách:

- chạy trên máy ảo Android Emulator của laptop;
- chạy trên điện thoại Android thật qua cáp USB.

Mục tiêu là để người mới clone repo cũng có thể làm theo từng bước, biết phải gõ lệnh gì, gõ ở đâu, cần chuẩn bị file gì, và script nào của repo sẽ tự cài công cụ nếu máy còn thiếu.

## 1. Dự án này gồm những phần nào

Repo này có 3 phần chính:

- `app/`: ứng dụng Android.
- `backend-spring/`: backend Spring Boot chạy local ở cổng `8080`.
- `scripts/`: các script PowerShell để setup máy, chạy backend, build app, cài app vào máy ảo hoặc điện thoại, rồi tự mở app.

Khi chạy app local, thông thường ta sẽ chạy cả app Android và backend Spring Boot.

## 2. Cần gõ lệnh ở đâu

Mọi lệnh trong tài liệu này đều nên chạy trong PowerShell, tại thư mục gốc của project:

```powershell
cd "C:\Users\ASUS\Documents\KÌ 4-UIT\App-Mobilee-SE114"
```

Nếu bạn chưa mở PowerShell:

1. Mở Start Menu.
2. Gõ `PowerShell`.
3. Mở `Windows PowerShell` hoặc `PowerShell`.
4. Dán lệnh `cd` ở trên.

Mẹo: luôn dùng dấu ngoặc kép `"` quanh đường dẫn vì tên thư mục có khoảng trắng.

## 3. Máy cần có gì trước khi chạy

### 3.1. Những thứ script có thể tự lo

Repo này đã có script bootstrap. Nếu máy chưa có công cụ cần thiết, script sẽ tự cài hoặc tự cấu hình một phần. Cụ thể:

- Nếu chưa có Java đủ phiên bản, script sẽ tự cài JDK.
- Nếu chưa có Android SDK command-line tools, script sẽ tự tải từ Google.
- Nếu chưa có Android SDK packages cần cho project, script sẽ tự cài.
- Nếu chạy emulator, script có thể tự tạo hoặc dùng lại máy ảo `Pixel_5`.
- Script sẽ tự cập nhật `local.properties`, `ANDROID_SDK_ROOT`, `ANDROID_HOME`, và thêm đường dẫn cần thiết vào `PATH`.
- Backend không bắt buộc phải có Maven cài global, vì repo dùng `backend-spring\mvnw.cmd`.

Nói ngắn gọn:

- thiếu `JDK`: script có thể tự cài;
- thiếu Android SDK tools: script có thể tự cài;
- thiếu Android emulator package: script có thể tự cài;
- thiếu Maven global: không sao;
- thiếu Android Studio: không bắt buộc để chạy bằng script.

### 3.2. Những thứ script không thể tự bịa ra cho bạn

Bạn vẫn phải tự chuẩn bị các file cấu hình bí mật hoặc file gắn với project Firebase riêng của nhóm.

Các file quan trọng:

1. `app/google-services.json`
2. `backend-spring/.env`
3. `backend-spring/secrets/service-account.json`

Chi tiết từng file ở phần dưới.

### 3.3. Máy nên có thêm gì

- Kết nối Internet để script tải JDK hoặc Android SDK nếu máy chưa có.
- `winget` hoạt động bình thường.
- Nếu `winget` không có, cần cài `App Installer` từ Microsoft Store trước.
- Nếu chạy emulator, máy nên bật virtualization trong BIOS/UEFI và có đủ RAM.

## 4. Cần chuẩn bị file gì, để ở đâu

### 4.1. File `app/google-services.json`

Vị trí:

```text
app/google-services.json
```

Ý nghĩa:

- Đây là file cấu hình Firebase cho app Android.

Hiện trạng:

- Trong repo hiện tại đã có file này sẵn.

Nếu một ngày clone repo mới mà file này bị thiếu:

- lấy đúng file `google-services.json` của Firebase project;
- chép vào thư mục `app/`.

### 4.2. File `backend-spring/.env`

Vị trí:

```text
backend-spring/.env
```

Nếu chưa có file này, tạo bằng cách copy từ:

```text
backend-spring/.env.example
```

Ví dụ trong PowerShell:

```powershell
Copy-Item "backend-spring\.env.example" "backend-spring\.env"
```

Sau đó mở file `backend-spring/.env` và điền các giá trị thật.

Các biến quan trọng:

- `SPRING_PROFILES_ACTIVE=dev`
- `SERVER_PORT=8080`
- `GOOGLE_APPLICATION_CREDENTIALS=...`
- `FIREBASE_PROJECT_ID=...`
- `FIREBASE_STORAGE_BUCKET=...`
- `BACKEND_SECURITY_REQUIRE_APP_CHECK=...`
- `BACKEND_SECURITY_FIREBASE_PROJECT_NUMBER=...`
- `GEMINI_API_KEY=...`
- `GEMINI_MODEL=gemini-1.5-flash`
- `CLOUDINARY_CLOUD_NAME=...`
- `CLOUDINARY_API_KEY=...`
- `CLOUDINARY_API_SECRET=...`
- `CLOUDINARY_UPLOAD_FOLDER=soulmate_uploads`

Lưu ý:

- Nếu thiếu các biến Firebase cơ bản hoặc service account, backend sẽ không chạy đúng.
- Nếu để trống `GEMINI_API_KEY` hoặc `CLOUDINARY_*`, backend có thể vẫn khởi động được nhưng các tính năng AI hoặc upload ảnh sẽ lỗi khi dùng tới.

### 4.3. File `backend-spring/secrets/service-account.json`

Vị trí:

```text
backend-spring/secrets/service-account.json
```

Đây là file service account của Firebase Admin SDK.

Bạn cần:

1. Lấy file JSON service account từ Firebase hoặc Google Cloud của project.
2. Tạo thư mục `backend-spring/secrets/` nếu chưa có.
3. Chép file vào đúng tên:

```text
backend-spring/secrets/service-account.json
```

Ngoài ra, trong `backend-spring/.env` cần trỏ `GOOGLE_APPLICATION_CREDENTIALS` tới file này. Ví dụ:

```env
GOOGLE_APPLICATION_CREDENTIALS=C:/Users/ASUS/Documents/KÌ 4-UIT/App-Mobilee-SE114/backend-spring/secrets/service-account.json
```

## 5. Cách chạy nhanh nhất trên máy mới hoặc máy cũ

### 5.1. Chạy trên máy ảo Android Emulator

Mở PowerShell tại thư mục gốc project, rồi chạy:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1
```

Script này sẽ tự làm lần lượt:

1. setup máy nếu cần;
2. tạo hoặc dùng lại AVD `Pixel_5`;
3. mở emulator;
4. khởi động backend Spring Boot ở `http://localhost:8080/`;
5. build app debug;
6. cài APK vào emulator;
7. mở app.

Nếu máy đã có đủ công cụ rồi, script sẽ kiểm tra và bỏ qua phần không cần cài lại.

Nếu máy có nhiều AVD, có thể chỉ rõ tên:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1 -AvdName Pixel_5
```

### 5.2. Chạy trên điện thoại Android thật qua USB

Điều kiện:

- điện thoại đã bật `Developer options`;
- đã bật `USB debugging`;
- đã cắm cáp USB;
- đã bấm `Allow USB debugging` khi máy hỏi;
- máy tính nhìn thấy thiết bị qua `adb`.

Lệnh chạy:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1
```

Script này sẽ tự làm:

1. setup máy nếu cần;
2. nhận diện điện thoại;
3. đợi điện thoại boot xong;
4. chạy backend local ở cổng `8080`;
5. cấu hình `adb reverse` để điện thoại gọi được backend ở `localhost:8080`;
6. build app debug;
7. cài APK vào điện thoại;
8. mở app.

Nếu có nhiều thiết bị Android đang cắm:

```powershell
adb devices
```

Lấy serial của máy cần dùng, rồi chạy:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1 -Serial <serial-thiet-bi>
```

## 6. Nên chạy lệnh nào trước trên máy mới hoàn toàn

Nếu muốn chuẩn bị máy trước rồi mới chạy app, dùng:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-dev-machine.ps1 -EnsureAvd
```

Lệnh này phù hợp khi:

- máy mới tinh;
- muốn để script cài tool trước;
- muốn chắc rằng emulator `Pixel_5` đã sẵn sàng.

Script setup này sẽ:

- kiểm tra Java;
- tự cài JDK nếu chưa có Java 17 trở lên;
- tìm hoặc tạo Android SDK root;
- tải Android command-line tools nếu còn thiếu;
- cài các Android packages cần cho project:
  - `platform-tools`
  - `platforms;android-34`
  - `build-tools;34.0.0`
  - `emulator`
  - `system-images;android-34;google_apis;x86_64`
- chấp nhận Android SDK licenses;
- tạo hoặc dùng lại AVD `Pixel_5`;
- cập nhật `local.properties`.

Sau khi setup xong, bạn chỉ cần chạy tiếp một trong hai lệnh:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1
```

hoặc:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1
```

## 7. App đang gọi backend local như thế nào

Repo đã cấu hình sẵn theo kiểu:

- emulator Android gọi backend ở `http://10.0.2.2:8080/`;
- điện thoại thật gọi backend ở `http://localhost:8080/` thông qua `adb reverse`.

Vì vậy:

- chạy emulator thì không cần tự cấu hình `adb reverse`;
- chạy điện thoại thật thì script `run-phone.ps1` sẽ tự cấu hình `adb reverse`.

## 8. App Check local hoạt động ra sao

Khi chạy local bằng script:

- App Check bị tắt mặc định ở backend local để app hiện tại có thể làm việc được.

Nếu bạn muốn bật App Check để test kỹ hơn:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1 -RequireAppCheck
```

Hoặc với emulator:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1 -RequireAppCheck
```

Nếu không có nhu cầu test App Check, cứ để mặc định là dễ nhất.

## 9. Cách dừng backend và dọn local dev

Khi muốn dừng backend local do script đã mở nền, chạy:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-local-dev.ps1
```

Script này sẽ:

- dừng backend do repo đang quản lý;
- gỡ `adb reverse` cho điện thoại thật nếu có.

## 10. Xem log ở đâu

Các script local dev ghi log tại:

```text
.agent/local-dev/
```

Các file log thường gặp:

- `.agent/local-dev/backend.stdout.log`
- `.agent/local-dev/backend.stderr.log`
- `.agent/local-dev/gradle.stdout.log`
- `.agent/local-dev/gradle.stderr.log`
- `.agent/local-dev/emulator.stdout.log`
- `.agent/local-dev/emulator.stderr.log`

Nếu app không chạy được, nên xem các file này trước.

## 11. Cách chạy tay nếu không muốn dùng script tổng

Phần này dùng khi bạn muốn tự chạy từng bước để debug.

### 11.1. Chạy backend bằng tay

Mở PowerShell ở thư mục gốc project, rồi chạy:

```powershell
cd ".\backend-spring"
.\mvnw.cmd spring-boot:run
```

Nếu đã có backend chạy ở cổng `8080`, app có thể dùng backend đó luôn.

### 11.2. Build app bằng tay

Từ thư mục gốc project, cách an toàn hơn là dùng helper của repo để nó tự chọn JDK phù hợp:

```powershell
powershell -ExecutionPolicy Bypass -Command ". .\scripts\dev-common.ps1; Build-DebugApk"
```

Nếu máy của bạn đã cấu hình Gradle và JDK ổn sẵn, có thể dùng thêm lệnh Gradle trực tiếp:

```powershell
.\gradlew.bat :app:assembleDebug
```

APK debug sau khi build xong nằm ở:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 11.3. Cài app lên điện thoại hoặc emulator bằng tay

Xem danh sách thiết bị:

```powershell
adb devices
```

Cài APK:

```powershell
adb install -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

Nếu có nhiều thiết bị:

```powershell
adb -s <serial-thiet-bi> install -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

### 11.4. Trường hợp dùng điện thoại thật và backend local

Cần cấu hình `adb reverse` để app trên điện thoại gọi được backend trên laptop:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-android-local-backend.ps1
```

Hoặc thủ công bằng `adb`:

```powershell
adb reverse tcp:8080 tcp:8080
```

## 12. Cách chạy khi đã biết máy đủ tool rồi

Nếu chắc chắn máy đã setup xong hết và chỉ muốn chạy nhanh hơn, có thể bỏ qua bước kiểm tra bootstrap:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1 -SkipMachineSetup
```

Hoặc:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1 -SkipMachineSetup
```

## 13. Các tuỳ chọn hữu ích

### 13.1. Dùng file `.env` khác

Nếu bạn có file env riêng:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1 -EnvFile "C:\duong-dan-toi-file\.env"
```

Hoặc:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1 -EnvFile "C:\duong-dan-toi-file\.env"
```

### 13.2. Bắt script khởi động lại backend

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1 -RestartBackend
```

### 13.3. Không build lại APK

Chỉ dùng khi chắc chắn APK debug mới nhất đã có sẵn:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1 -SkipBuild
```

## 14. Những lỗi thường gặp

### 14.1. Thiếu `winget`

Triệu chứng:

- script setup báo không tìm thấy `winget`.

Cách xử lý:

- cài `App Installer` từ Microsoft Store;
- mở PowerShell mới;
- chạy lại script setup.

### 14.2. Không có `backend-spring/.env`

Triệu chứng:

- backend không đọc được biến môi trường;
- backend khởi động lỗi;
- một số API không hoạt động.

Cách xử lý:

```powershell
Copy-Item "backend-spring\.env.example" "backend-spring\.env"
```

Sau đó chỉnh giá trị thật trong file `.env`.

### 14.3. Không có `service-account.json`

Triệu chứng:

- backend lỗi liên quan Firebase Admin;
- backend không truy cập được Firebase.

Cách xử lý:

- đặt file tại `backend-spring/secrets/service-account.json`;
- kiểm tra biến `GOOGLE_APPLICATION_CREDENTIALS` trong `backend-spring/.env`.

### 14.4. Điện thoại không hiện trong `adb devices`

Cách xử lý:

1. Kiểm tra cáp USB.
2. Bật `USB debugging`.
3. Rút ra cắm lại.
4. Bấm `Allow` trên điện thoại.
5. Chạy lại:

```powershell
adb devices
```

### 14.5. Emulator không lên

Cách xử lý:

1. Chạy lại setup:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-dev-machine.ps1 -EnsureAvd
```

2. Kiểm tra danh sách AVD:

```powershell
emulator -list-avds
```

3. Thử chạy lại:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1
```

### 14.6. Build app lỗi vì tool Android hoặc Java

Cách xử lý an toàn nhất:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-dev-machine.ps1 -EnsureAvd
```

Sau đó chạy lại script run.

Nếu vẫn muốn build tay, ưu tiên lệnh helper này thay vì gọi `.\gradlew.bat` trực tiếp:

```powershell
powershell -ExecutionPolicy Bypass -Command ". .\scripts\dev-common.ps1; Build-DebugApk"
```

## 15. Quy trình khuyến nghị ngắn gọn

### Trường hợp 1: muốn chạy trên emulator

1. Mở PowerShell.
2. Vào thư mục project:

```powershell
cd "C:\Users\ASUS\Documents\KÌ 4-UIT\App-Mobilee-SE114"
```

3. Đảm bảo có đủ 3 file:
   - `app/google-services.json`
   - `backend-spring/.env`
   - `backend-spring/secrets/service-account.json`
4. Chạy:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1
```

### Trường hợp 2: muốn chạy trên điện thoại thật

1. Mở PowerShell.
2. Vào thư mục project:

```powershell
cd "C:\Users\ASUS\Documents\KÌ 4-UIT\App-Mobilee-SE114"
```

3. Đảm bảo có đủ 3 file:
   - `app/google-services.json`
   - `backend-spring/.env`
   - `backend-spring/secrets/service-account.json`
4. Cắm điện thoại, bật `USB debugging`.
5. Kiểm tra:

```powershell
adb devices
```

6. Chạy:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1
```

## 16. Một câu trả lời ngắn cho câu hỏi “máy mới có tự cài tool không?”

Có, nhưng không phải mọi thứ.

Script của repo có thể tự:

- cài JDK nếu thiếu;
- cài Android command-line tools nếu thiếu;
- cài Android SDK packages nếu thiếu;
- tạo AVD nếu cần;
- cấu hình SDK path cho project.

Script của repo không tự:

- tạo file `backend-spring/.env` với giá trị thật;
- tự sinh `backend-spring/secrets/service-account.json`;
- tự biết secret Firebase, Gemini, Cloudinary của project;
- tự bật USB debugging trên điện thoại;
- tự cài Android Studio trừ khi bạn gọi setup với tuỳ chọn riêng để cài.

## 17. Lệnh quan trọng nhất

Nếu chỉ cần nhớ 3 lệnh:

```powershell
cd "C:\Users\ASUS\Documents\KÌ 4-UIT\App-Mobilee-SE114"
```

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1
```

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1
```
