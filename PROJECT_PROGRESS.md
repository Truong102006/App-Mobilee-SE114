# Project Progress Update

Ngay cap nhat: 2026-04-25

Pham vi danh gia:
- Danh gia dua tren ma nguon hien co trong repo `d:\App-Mobilee-SE114`.
- Da xac nhan build local bang terminal tren may hien tai.
- Da xac nhan unit test moi chay thanh cong.

## 1. Tom tat nhanh

Du an hien da co mot MVP Android app kha ro ve mat giao dien, navigation va demo luong chinh. Cac phan manh nhat hien tai la:
- khung app Compose + Hilt + navigation,
- man Home va music player local,
- man Diary editor,
- truy van diary theo `user_id` tren Firestore,
- logic thong ke mood o tang domain,
- build local bang `gradlew` va tao duoc APK debug.

So voi lan danh gia truoc, du an da tien them o 3 diem quan trong:
- repo da chay build duoc tu terminal,
- da co backend logic thong ke mood cho FE,
- da co unit test co ban cho phan thong ke.

Uoc luong tien do hien tai:
- Muc hoan thien theo huong demo/bao cao mon hoc: khoang 65-75%.
- Muc san sang de chay on dinh nhu mot san pham hoan chinh: khoang 35-45%.

## 2. Tien do theo hang muc

| Hang muc | Tien do uoc luong | Trang thai hien tai |
|---|---:|---|
| Nen tang du an Android | 85% | Da co cau truc app chuan voi `Compose`, `Hilt`, `Navigation`, `Firebase`, `Media3`. |
| Home + Music player | 85% | Co giao dien home, danh sach nhac, player mini/full screen, phat nhac local kha day du. |
| Diary editor | 65% | Co editor, chon mood, chon anh, goi Gemini phan tich mood, luu diary qua use case/repository. |
| Diary backend query | 70% | Da co ham lay diary theo `user_id`, sap xep moi nhat truoc o `DiaryRepositoryImpl`. |
| Mood statistics backend | 40% | Da co `GetMoodStatisticsUseCase` va model tra ve cho FE, nhung chua noi vao UI chart. |
| History diary | 45% | Co UI xem/chinh sua/xoa, nhung du lieu van la state cuc bo, chua doc tu Firestore. |
| Voice/recording | 60% | Co speech-to-text demo va xin quyen micro, nhung chua thanh audio diary hoan chinh. |
| Setting | 40% | Co UI va toggle dark mode, nhung chua co persistence, profile/account van la du lieu cung. |
| Pet / Social / Stats UI | 10% | File da tao nhung phan lon van chua co logic thuc te. |
| Kiem thu / build / release readiness | 55% | Da co `gradlew`, da verify `assembleDebug`, da co unit test co ban, nhung Firebase config van chua day du. |

## 3. Nhung phan da lam duoc

### 3.1. Nen tang ung dung da hinh thanh
- App co `MainActivity`, `SoulMateApplication`, Hilt annotation va bottom navigation hoat dong.
- Co 4 man chinh dang duoc dua vao navigation: `Home`, `Diary`, `History`, `Setting`.
- Cau truc source da chia thanh `data`, `domain`, `di`, `ui`, `utils`, phu hop voi huong clean architecture.

File tham chieu chinh:
- `app/src/main/java/com/soulmate/app/MainActivity.kt`
- `app/src/main/java/com/soulmate/app/SoulMateApplication.kt`
- `app/src/main/java/com/soulmate/app/ui/NavRoute.kt`

### 3.2. Home va music player la phan hoan thien nhat
- Co danh sach nhac local voi asset tuong doi day du.
- Co phat/tam dung/chuyen bai/quay lai.
- Co mini player va man chi tiet player fullscreen.

File tham chieu chinh:
- `app/src/main/java/com/soulmate/app/ui/home/HomeScreen.kt`
- `app/src/main/java/com/soulmate/app/ui/home/MusicViewModel.kt`

### 3.3. Diary flow da co khung end-to-end dau tien
- Co man soan diary voi rich text editor.
- Co mood selector.
- Co chon nhieu anh tu thu vien.
- Co goi Gemini de phan tich cam xuc tu noi dung text.
- Co `SaveDiaryUseCase`, `AnalyzeMoodUseCase`, `DiaryRepositoryImpl`, `AIRepositoryImpl`.
- Co luu diary len Firestore theo collection `diaries`.

File tham chieu chinh:
- `app/src/main/java/com/soulmate/app/ui/journal/editor/MultimediaEditor.kt`
- `app/src/main/java/com/soulmate/app/ui/journal/editor/DiaryViewModel.kt`
- `app/src/main/java/com/soulmate/app/domain/usecase/SaveDiaryUseCase.kt`
- `app/src/main/java/com/soulmate/app/domain/usecase/AnalyzeMoodUseCase.kt`
- `app/src/main/java/com/soulmate/app/data/repository/DiaryRepositoryImpl.kt`
- `app/src/main/java/com/soulmate/app/data/repository/AIRepositoryImpl.kt`

### 3.4. Da co backend query va thong ke mood co ban
- Ham lay diary theo `user_id` va sap xep moi nhat truoc da co san trong repository.
- Da them `MoodStatistic` de chuan hoa du lieu tra ve cho FE.
- Da them `GetMoodStatisticsUseCase` de gom nhom theo mood, dem so luong va sap xep giam dan theo so bai.
- Logic nay da xu ly ca truong hop mood rong bang nhan `Unknown`.

File tham chieu chinh:
- `app/src/main/java/com/soulmate/app/data/repository/DiaryRepositoryImpl.kt`
- `app/src/main/java/com/soulmate/app/domain/model/MoodStatistic.kt`
- `app/src/main/java/com/soulmate/app/domain/usecase/GetMoodStatisticsUseCase.kt`

### 3.5. Build local da chay duoc bang terminal
- Da tao `gradlew`, `gradlew.bat` va `gradle/wrapper` cho repo.
- Da tao `local.properties` tren may hien tai de tro den Android SDK.
- Da dieu chinh `app/build.gradle` de local build van chay duoc khi chua co `google-services.json`.
- Da build thanh cong bang lenh `.\gradlew.bat assembleDebug --no-daemon`.
- Da tao duoc APK debug tai `app/build/outputs/apk/debug/app-debug.apk`.

File tham chieu chinh:
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`
- `app/build.gradle`

### 3.6. Da co unit test dau tien cho tang domain
- Da them test cho `GetMoodStatisticsUseCase`.
- Da verify duoc grouping khong phan biet hoa thuong, sorting theo count giam dan, va xu ly user id rong.
- Da chay thanh cong `.\gradlew.bat testDebugUnitTest --no-daemon`.

File tham chieu chinh:
- `app/src/test/java/com/soulmate/app/domain/usecase/GetMoodStatisticsUseCaseTest.kt`

## 4. Nhat ky trien khai gan day tung buoc

### Buoc 1. Khao sat blocker build tren may hien tai
Da kiem tra cau truc repo, lenh `gradle`, `adb`, emulator va file Firebase.

Giai thich:
- Muc tieu la xac dinh vi sao project chua the build/chay tu terminal.
- Ket qua phat hien repo chua co `gradlew`, may chua nhan `gradle` global, va thieu `app/google-services.json`.

### Buoc 2. Tao cau hinh local de repo nhan Android SDK
Da tao `local.properties` voi `sdk.dir` tro den SDK thuc te tren may.

Giai thich:
- Android Gradle Plugin can biet chinh xac duong dan SDK de build.
- Neu thieu file nay thi terminal build rat de fail ngay tu buoc dau.

### Buoc 3. Tao Gradle Wrapper cho repo
Da sinh va dua vao repo bo file `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`.

Giai thich:
- Wrapper giup project build duoc ngay ca khi may khong co `gradle` global.
- Day la buoc quan trong nhat de chuan hoa cach run project giua cac may.

### Buoc 4. Chot version Gradle tuong thich voi project
Da chuyen wrapper ve `Gradle 8.2.1`.

Giai thich:
- Build bang `Gradle 9.3.1` bi loi tuong thich voi `AGP 8.2.2` va `Kotlin 1.9.24`.
- `Gradle 8.2.1` phu hop hon voi stack hien tai nen build di tiep duoc.

### Buoc 5. Dieu chinh local build de khong bi chan boi Firebase config
Da sua `app/build.gradle` de chi apply `com.google.gms.google-services` khi file `google-services.json` ton tai.

Giai thich:
- Trong giai doan local run, repo hien khong co file Firebase config.
- Neu van apply plugin bat buoc thi build se bi chan ngay.
- Cach sua nay giup app van build local, nhung khong co nghia la Firebase da san sang cho production.

### Buoc 6. Verify build va cai app tu terminal
Da chay thanh cong `assembleDebug`, tao duoc APK debug, da install APK len emulator va gui lenh mo app.

Giai thich:
- Buoc nay xac nhan repo khong chi "co code build" ma da build duoc tren may that.
- Day la bang chung quan trong de cap nhat progress theo huong thuc thi, khong chi danh gia tren mat code.

### Buoc 7. Hoan thien backend logic thong ke mood
Da them `MoodStatistic` va implement `GetMoodStatisticsUseCase`.

Giai thich:
- Phan query diary theo `user_id` da co san, nen phan con thieu la business logic de dem so bai theo tung mood.
- Use case moi se gom nhom theo `moodTag`, dem so luong, chuyen thanh danh sach tra ve cho FE, va sap xep theo count giam dan.

### Buoc 8. Them unit test cho phan thong ke
Da them `GetMoodStatisticsUseCaseTest`.

Giai thich:
- Muc tieu la tranh viec logic thong ke viet xong nhung chua duoc kiem chung.
- Test nay giup xac nhan cac truong hop can ban da dung truoc khi noi vao UI chart.

### Buoc 9. Verify lai sau khi sua code
Da chay thanh cong:
- `.\gradlew.bat testDebugUnitTest --no-daemon`
- `.\gradlew.bat assembleDebug --no-daemon`

Giai thich:
- Day la buoc dong vong lap ky thuat.
- Sau moi thay doi quan trong, can verify lai de chac chan project van o trang thai build duoc.

## 5. Nhung phan dang do hoac moi o muc demo

### 5.1. Diary chua luu day du nhung gi UI dang cho nhap
- `MultimediaEditor` co title, anh va luong "record", nhung `DiaryViewModel.saveDiary()` hien chi luu `text`, `moodTag`, `timestamp`.
- `title` hien chua di vao model/domain.
- `selectedImages` chua duoc map sang `imageUrls`.
- `audioUrl` co trong model nhung chua duoc ghi nhan tu UI.

Anh huong:
- Giao dien nhin day du hon du lieu thuc te dang luu.

### 5.2. History chua noi voi repository/Firestore
- `HistoryViewModel` dang giu du lieu bang `mutableStateListOf`.
- Chua goi `IDiaryRepository.getDiaries(userId)` de tai lich su that tu backend.
- Man history hien phan anh note local duoc day tu `MoodCard`, khong phai diary da dong bo.

### 5.3. Recording moi la speech-to-text demo
- Co quyen micro va nhan dien giong noi.
- Nhung chua co pipeline ghi am audio file thuc su.
- `AudioRecorder.kt` hien van trong.
- Nut record trong `MultimediaEditor` thuc te dang dung de goi Gemini phan tich text, chua phai luu audio.

### 5.4. Settings moi la giao dien
- Dark mode chi doi state trong memory, chua luu lai sau khi mo app lai.
- Notifications chi la state cuc bo.
- Profile, edit profile, privacy, help, logout chua co xu ly that.

### 5.5. Stats UI va cac module mo rong van chua noi hoan chinh
- `StatsScreen`, `PetScreen`, `CommunityScreen`, `PostDetailScreen`, `HomeViewModel` gan nhu chua co logic UI/VM hoan chinh.
- Da co backend thong ke mood, nhung chua duoc dua vao `StatsScreen` de ve chart.
- `CalculatePetXPUseCase`, `IPetRepository`, `PetRepositoryImpl`, `DiaryDao`, `PetDao` van chua co trien khai thuc te.

## 6. Cac blocker va rui ro chinh

### 6.1. Firebase moi o muc local build workaround
- Repo hien van chua co `app/google-services.json`.
- App build duoc la nho bo qua plugin Google Services khi file nay khong ton tai.
- Dieu nay phu hop cho local run, nhung chua phai cau hinh hoan chinh de dung Firebase day du.

### 6.2. Mot so phan quan trong van hard-code
- Gemini API key dang hard-code truc tiep trong `NetworkModule`.
- `userId` dang gan cung la `"current_user_id"` thay vi lay tu Firebase Auth hoac user session that.
- Thong tin profile o Settings cung dang la du lieu cung.

### 6.3. Room va local database van chua day du
- Room da them dependency nhung chua thay `@Database`.
- `DiaryDao` va `PetDao` hien chua co ham truy van thuc te.
- Neu muon ho tro offline, phan nay van con thieu kha nhieu.

### 6.4. Co dau hieu bug logic o flow save note
- Trong `MoodCard`, callback save sau animation dang goi `onSave(history[0])`, tuc la luon lay phan tu dau danh sach thay vi item duoc swipe/save.
- Dieu nay co the lam luu nham note khi danh sach co nhieu muc.

### 6.5. Test da co nhung do phu con thap
- Hien moi co test cho `GetMoodStatisticsUseCase`.
- Chua co test cho diary save flow, history flow, Firebase integration, hoac UI behavior.

## 7. De xuat uu tien tiep theo

1. Noi `StatsScreen` voi `GetMoodStatisticsUseCase`.
   Muc tieu la bien backend mood statistics thanh man hinh bieu do thuc su.

2. Noi `HistoryViewModel` voi Firestore.
   Muc tieu la hien thi diary that thay vi note cuc bo.

3. Chot lai luong diary that.
   Luu day du `title`, `imageUrls`, `audioUrl`, `moodTag`, `timestamp`, `userId`.

4. Bo hard-code nhay cam.
   Dua Gemini API key ra `local.properties` hoac secret management va noi `userId` voi Auth that.

5. Them `google-services.json` dung moi truong.
   Muc tieu la chuyen tu local build workaround sang cau hinh Firebase day du.

6. Mo rong test.
   Uu tien test cho use case diary, history, va mot smoke test cho app launch.

## 8. Ket luan ngan

Hien trang tot nhat de mo ta du an la:

"Da co MVP Android app voi UI va luong chinh kha ro; da build duoc bang terminal; da co backend query diary va thong ke mood co ban; tuy nhien phan Firebase day du, history dong bo, stats UI, local database va mot so tinh nang mo rong van chua hoan thien."
