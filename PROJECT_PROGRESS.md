# Project Progress Update

Ngay cap nhat: 2026-06-21

Pham vi danh gia:
- Danh gia dua tren ma nguon hien co trong repo `d:\App-Mobilee-SE114`.
- Da xac nhan build local va xac nhan chay thanh cong cac unit test/integration test.
- Cap nhat dua tren nhat ky commit gan nhat ben dev branch (den ngay 2026-06-21).

## 1. Tom tat nhanh

Du an SoulMate da dat duoc buoc tien lon va hoan thien dang ke ca ve phan Android UI (FE) lan Spring Boot Backend (BE). Cac tinh nang cot loi duoc nang cap len dang ke phuc vu yeu cau do an:
- **He thong Pet (Level & Animation)**: Da hoan thanh ca ve mat UI/giao dien tuong tac va Firebase sync backend.
- **Cong thanh toan SePay**: Cho phep nang cap vip/premium tu dong.
- **Admin Dashboard & Moderation Flow**: Giao dien dashboard cap quyen admin, an bai viet vi pham, block social interaction doi voi user bi ban.
- **Realtime (OneSignal & Online Presence)**: Nhan thong bao day tren thiet bi cho cuoc tro chuyen, tin nhan va hien thi dong thoi trang thai online/offline trong chat va community.
- **Nang cap hinh anh**: Xem anh thu phong toan man hinh (zoom viewer) va luu/tai anh tu nhat ky/community ve album thiet bi.
- **Co ke hoach chi tiet cho cac tinh nang moi (rw.md)**: Ke hoach trien khai phan con thieu ve chat (reactions, reply, edit tin nhan), user info page, privacy (doi pass, xoa account).

Uoc luong tien do hien tai:
- Muc hoan thien theo huong demo/bao cao mon hoc: khoang 90-95%.
- Muc san sang de chay nhu san pham hoan chinh: khoang 75-85%.

## 2. Tien do theo hang muc

| Hang muc | Tien do uoc luong | Trang thai hien tai |
|---|---:|---|
| Nen tang du an Android | 95% | Cau truc Compose + Hilt + Navigation, ho tro Online Presence realtime, push notification OneSignal. |
| Home + Music player | 90% | Mini player, full detail player. Asset nhac local day du. |
| Diary editor | 90% | Chon mood, rich content, luu tru, greeting theo thoi gian thuc tren thiet bi, download image ve gallery. |
| Diary backend query | 95% | Ho tro endpoint lay theo user_id, sua loi bug cache, khong bi 429 hay 500 nua. |
| Mood statistics | 90% | Hoan thien UseCase thong ke mood tag, noi vao UI bieu do phan tich trong can doi du lieu. |
| History diary | 90% | Doc ghi dong bo tu Firestore, sua triet de loi cache phan du lieu lich su note cuc bo. |
| Voice/recording | 75% | Co speech-to-text, pipeline audio nhat ky dang trong giai doan tinh chinh. |
| Setting & Profile | 85% | Giao dien profile, doi mat khau quen, thay doi ngon ngu, navigation sang Admin. |
| Pet UI & Backend | 95% | Hoan tat UI Pet Screen, Animation level up, phep tinh kinh nghiem va repository backend dong bo. |
| Admin & Moderation | 95% | AdminDashboard UI, logic an bai viet phan anh, khoa user khoi social runtime. |
| SePay Integration | 90% | He thong nang cap tai khoan vip tu dong qua webhook SePay backend. |
| Kiem thu / build / release readiness | 85% | Gradle build on dinh, day du file setup-dev, viet cac manual unit test & web integration test. |

## 3. Nhung phan da lam duoc

### 3.1. Nen tang ung dung da hinh thanh
- App co `MainActivity`, `SoulMateApplication`, Hilt annotation va bottom navigation hoat dong.
- Co 4 man chinh dang duoc dua vao navigation: `Home`, `Diary`, `History`, `Setting`.
- Cau truc source da chia thanh `data`, `domain`, `di`, `ui`, `utils`, phu hop voi huong clean architecture.

### 3.2. Home va music player la phan hoan thien nhat
- Co danh sach nhac local voi asset tuong doi day du.
- Co phat/tam dung/chuyen bai/quay lai.
- Co mini player va man chi tiet player fullscreen.

### 3.3. Diary flow da co khung end-to-end
- Co man soan diary voi rich text editor, mood selector.
- Ho tro cap nhat loi chao (greeting) bat dong bo theo thoi gian cua he thong.
- Co goi Gemini de phan tich cam xuc tu noi dung text.
- Co luu diary len Firestore theo collection `diaries`.

### 3.4. Thong ke mood tag va bieu do
- UseCase backend thong ke dem so luong post theo moodtag, sap xep theo tan suat giam dan.
- Ho tro bieu do truc quan cho phan FE StatsScreen.

### 3.5. System Admin & Moderation Dashboard
- UI Admin Dashboard de quan ly bao cao cua user, an bai viet vi pham.
- Cho phap Admin bat/tat trang thai cam tuong tac (isSocialBanned) tren collection backend. Cấm user bi ban viet post hoac chat tren community.

### 3.6. He thong Pet dong bo hoa
- UI PetScreen hoan thien tuong tac nuoi pet, animation khi len tuong thich.
- Dong bo du lieu exp (kinh nghiem), thong so thoi gian va streak dang nhap nhat ky ve cho Firebase backend thiet lap chung.

### 3.7. He thong thong bao OneSignal & Realtime Presence
- Cau hinh service gui thoi gian thuc cac hoat dong thong bao khi duoc user khac thich hoac binh luan qua OneSignal backend.
- Theo doi presence trang thai online/offline cua user tai man hinh community va chat screen.

### 3.8. Cong thanh toan SePay Upgrade VIP
- Backend kiem tra giao dich tu webhook SePay, tu dong tinh toan va gia han thoi gian dung Premium cho User.

### 3.9. Ho tro lam viec hinh anh nang cao
- Zoom viewer xem thu hinh anh fullscreen cuc ky muot ma.
- Download va ghi truc tiep hinh anh ve thiet bi qua MediaStore danh cho cac image trong nhat ky nhat va bai dang community.

## 4. Nhat ky trien khai gan day tung buoc

- **Buoc 10. setup backend build & chay local (2026-06-15)**: Sua doi loi SDK va build config Firebase admin, test khoi chay local on dinh qua custom script powershell.
- **Buoc 11. Xay dung tinh nang Admin & cam social (2026-06-16)**: Them truong cam tuong tac social cho nguoi dung, ve dashboard cho phep xem danh sach bai posts vi pham va resolve reports.
- **Buoc 12. Ket noi backend Pet system (2026-06-19)**: Tich hop tinh toan diem streak, phep tinh level up va sync thong so pet tren ca thiet bi di dong va Firestore collection.
- **Buoc 13. He thong premium SePay & webhook processing (2026-06-20)**: Them API endpoint callback webhook, parser tu dong xac thuc ma thanh toan tuong ung va update field premium status/role cho Account.
- **Buoc 14. Push notification & user presence (2026-06-20)**: Tich hop sdk OneSignal voi backend trigger notification event; real-time checking presence trigger de cap nhat view online.
- **Buoc 15. Hinh anh zoom/download & code audit (2026-06-20)**: Fix logic dong bo community, code profile editor cap nhat layout va full images view.
- **Buoc 16. Ke hoach bo sung tinh nang backend chat/user tu rw.md (2026-06-21)**: Len ke hoach chi tiet cho 5 nhom tinh nang (emoji chat, block user, reset pass, xoa nick) va lap sanity checking integration tests.

## 5. Cac blocker va rui ro chinh

- **Firebase Config cho live app**: Voi runtime production can thiet lap file services va rule phan quyen de chat, data dong bo dung vung bao mat thiet ke.
- **Su kien thanh toan**: can kiem test callback phia IPN thoi gian thuc cua gateway trong nhieu ca thanh toan khac nhau.

## 6. De xuat uu tien tiep theo

1. **Trien khai cac APIs va DTO cho Chat chat/user**: Them truong `replyTo`, `reactions` va build api `editMessage` theo file `implementation-plan`.
2. **Bo sung API Block, Profile va Social Links** trong `UserController` moi.
3. **Thuc hien chay integration mock test suites** bang `mvnw test` de dat chat luong verify 100% cho phan logic moi.
