# Cách Chạy App

File này hướng dẫn cách chạy app trên:

- Máy ảo Android trên laptop
- Điện thoại Android thật qua USB

## 1. Chuẩn bị lần đầu trên một máy

### Bước 1: Clone repo và mở PowerShell tại thư mục gốc project

Ví dụ thư mục hiện tại là:

```powershell
App-Mobilee-SE114
```

### Bước 2: Cấu hình backend local

Nếu backend chưa có file `.env`, tạo nó từ file mẫu:

```powershell
backend-spring\.env.example
```

Sau đó điền các biến môi trường/secrets cần thiết cho backend local.

### Bước 3: Cài tool tự động cho máy

Chạy lệnh này một lần:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-dev-machine.ps1 -EnsureAvd
```

Script này sẽ tự:

- kiểm tra Java
- cài JDK nếu thiếu
- cài Android SDK command-line tools nếu thiếu
- cài `adb`, emulator, SDK API 34
- tạo hoặc dùng lại máy ảo `Pixel_5`
- cập nhật `local.properties`

## 2. Chạy app trên máy ảo Android

Từ thư mục gốc project, chạy:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1
```

Script này sẽ tự:

- chuẩn bị máy nếu còn thiếu tool
- mở emulator hoặc dùng emulator đang chạy
- bật backend local ở nền
- build app
- cài app vào máy ảo
- mở app

Nếu có nhiều AVD:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1 -AvdName Pixel_5
```

## 3. Chạy app trên điện thoại Android thật

### Chuẩn bị trên điện thoại

1. Bật `Developer options`
2. Bật `USB debugging`
3. Cắm cáp USB vào laptop
4. Bấm `Allow` khi điện thoại hỏi quyền debug

### Chạy app

Từ thư mục gốc project, chạy:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1
```

Script này sẽ tự:

- chuẩn bị máy nếu còn thiếu tool
- bật backend local ở nền
- chạy `adb reverse`
- build app
- cài app lên điện thoại
- mở app

Nếu có nhiều điện thoại:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1 -Serial <device-serial>
```

## 4. Dừng backend và dọn local dev

Khi chạy xong, dùng:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-local-dev.ps1
```

## 5. Lệnh dùng hằng ngày

### Máy ảo

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1
```

### Điện thoại

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1
```

### Dừng local dev

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-local-dev.ps1
```

## 6. Ghi chú

- Cách này dùng được trên máy của bạn hoặc máy khác, miễn là máy đó đã clone repo và có cấu hình backend local cần thiết.
- Backend sẽ được script tự bật, nên bình thường bạn không cần tự chạy backend bằng tay nữa.
- Nếu máy đã setup sẵn và bạn muốn bỏ qua bước kiểm tra tool, có thể thêm `-SkipMachineSetup`.

Ví dụ:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1 -SkipMachineSetup
```
