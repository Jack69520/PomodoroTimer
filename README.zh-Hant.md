# 番茄計時器

**語言：** [简体中文](README.zh.md) · [繁體中文](README.zh-Hant.md) · [English](README.en.md)

番茄計時器是一款面向學習與工作場景的 Android 番茄鐘應用：依專注—休息節奏計時，把任務寫進待辦，用統計與日曆回看投入，並在專注時用應用屏蔽、勿擾、鎖屏全螢幕減少干擾。

資料只保存在本機，執行時不連網、不同步、不上傳。儲存庫提供 Java/MVVM 原始碼，便於閱讀、建置與二次開發。

| 項 | 值 |
|----|----|
| 套件名稱 | `com.skyinit.pomodorotimer` |
| 版本 | 1.0.0（versionCode 1） |
| 最低 / 目標 SDK | Android 9（API 28） / API 36 |
| 許可證 | [Apache License 2.0](LICENSE) |

## 功能特性

### 番茄鐘計時

- 預設學習 25 分鐘、休息 5 分鐘；學習時長可在首頁長按計時數字或「我的 → 設定 → 番茄鐘與待辦」中調整（1 分鐘～3 小時），休息 1～30 分鐘
- 常用時長快捷選擇：45 / 60 / 90 / 120 / 150 / 180 分鐘；預設學習時長 ≥ 1 小時時以 `HH:MM:SS` 顯示
- 短休息與可選長休息（每 N=2～8 個番茄後進入 10～15 分鐘長休息，預設關閉）
- 學習階段可暫停並記錄原因（臨時有事、被打斷、靈感來了等）；最大暫停次數 1～5（預設 2）；單次暫停超過 5 分鐘判定本輪失敗
- 學習滿約 5 分鐘才建議儲存為完成記錄；休息結束後可選手動續輪或開啟「休息結束後自動開始下一輪」
- **`TimerService` 前景服務**（`specialUse` / `pomodoro_focus_timer`）持續計時，通知列顯示進度；精確鬧鐘兜底到點與暫停逾時；開機依工作階段快照重新排程 Alarm
- 行程被終止後可依本機快照恢復或結算（工作階段恢復）
- 提示音：系統預設 / 靜音 / 自訂鈴聲；到點可震動
- 專屬計時頁支援橫向精簡介面（僅保留計時與控制）

### 鎖屏全螢幕計時

- 設定項「鎖屏全螢幕顯示計時」：專注與休息期間在鎖屏上全螢幕展示計時（僅展示，長按解鎖）
- 亮屏且處於鎖屏時嘗試將計時頁拉回；熄屏不主動強行喚醒
- 需通知權限；建議授予「全螢幕通知」以便離開計時頁後可靠拉回（部分機型可能僅顯示鎖屏通知倒數）

### 待辦與任務

- **普通待辦**與**待辦集**（含子任務）兩種類型
- 分類：工作、學習、生活、運動、娛樂、其他；優先級、標籤、截止日期、預估番茄數
- 置頂最多 3 個；按優先級 / 截止日期 / 分類篩選；左滑刪除
- 普通待辦支援重複：每天 / 每週 / 每月（完成時推進到下一期，由領域策略處理，非 WorkManager）
- 首頁分組：置頂 / 過期 / 今天 / 即將 / 無日期 / 已完成
- 可一鍵為任務或子任務啟動計時，自動累計番茄進度；待辦集可顯示進度並選擇「下一步」子任務
- 可選「自動刪除已完成任務」（完成超過 3 天後清理）

### 統計與日曆

- 今日 / 本週 / 本月專注次數與時長；近 7 日趨勢、本月時段分佈、分類圓餅圖（可鑽取）
- 連續專注、週同比、本月洞察、干擾診斷（暫停原因分佈）
- 月曆按日瀏覽記錄；記錄詳情支援**計時心得**（最多 200 字）；可查看攔截應用記錄
- 「我的」頁展示目前檔案「累計完成的番茄鐘」

### 專注輔助：應用屏蔽與勿擾

- **應用屏蔽**：基於使用情況存取 + 懸浮窗遮罩攔截分心應用（**不使用**無障礙服務或裝置管理員）
  - 「我的」頁可獨立開啟屏蔽模式；也可在設定中開啟「番茄計時期間自動屏蔽應用」（僅學習進行中生效，暫停與休息不屏蔽）
  - 管理頁支援搜尋、分類、全部 / 已屏蔽 / 已放行、掃描已安裝應用；規則來自本機 JSON 策略引擎
  - 單次屏蔽服務最長約 5 小時後自動停止
- **專注期間勿擾**：學習計時開始後可選開啟系統勿擾（需通知策略存取權限）

### 帳戶與個人化

- 啟動流程：隱私同意 → 首次註冊（可跳過）→ 功能介紹 → 主介面（首頁 / 統計 / 日曆 / 我的）
- **訪客**：可瀏覽介面並調節預設展示時長，**不能**開始計時、管理待辦、查看真實統計/日曆或開啟應用屏蔽
- **註冊帳戶**：本機 12 位帳戶 ID、密碼登入、改密、憑 ID+暱稱找回；多檔案切換，資料按帳戶隔離
- 密碼以 **PBKDF2-HMAC-SHA256** 雜湊儲存；可自訂暱稱、簽名與頭像（相機 / 相簿）
- 主題皮膚：標準色、中國色、國風漸層、莫蘭迪；支援系統夜間資源

### 設定與其他

- 設定樞紐：帳戶 / 帳戶與安全 / 主題色 / 提示音 / 番茄鐘與待辦 / **系統權限**
- 桌面 **App Shortcuts**（長按圖示，非桌面小工具）：專注 25 分鐘、查看統計、屏蔽模式
- 內建常見問題（可搜尋）與**開發實驗室**（版本、裝置、儲存/記憶體、執行日誌）
- 關於頁：隱私政策、使用者服務協議、第三方開源元件許可

## 技術棧

| 類別 | 技術 |
|------|------|
| 語言 | Java 11 |
| 建置 | Android Gradle Plugin 9.1.1 · Gradle 9.3.1 |
| 最低 / 目標 SDK | API 28 / 36 |
| 架構 | **MVVM**（UI → ViewModel → Repository → Room / SharedPreferences）；domain 純策略；`AppContainer` 手工依賴注入 |
| UI | AppCompat · Material Components · ConstraintLayout · Navigation |
| 狀態 | LiveData / ViewModel；部分頁面採用輕量 **MVI**（`Intent` / `UiState` / `Effect` + `dispatch`） |
| 本機資料庫 | Room 2.6.1（schema 匯出至 `app/schemas/`） |
| 圖表 | MPAndroidChart |
| 背景 | 雙 `specialUse` 前景服務 + 精確 Alarm + BootReceiver（**無** WorkManager） |
| 測試 | JUnit · Robolectric · Architecture Components Testing · Room Testing |

Release 建置預設開啟 minify 與 shrinkResources。

## 環境要求

- **Android Studio**（建議最新穩定版）
- **JDK 11** 或更高版本
- **Android SDK**，含 compileSdk / targetSdk 36
- 無需後端、API Key 或網路設定

## 建置與運行

### 複製儲存庫

```bash
git clone https://github.com/Jack69520/PomodoroTimer.git
cd PomodoroTimer
```

### 使用 Android Studio

1. 開啟 Android Studio，選擇 **Open**，選中專案根目錄
2. 等待 Gradle 同步完成
3. 連接裝置或啟動模擬器，點擊 **Run**

首次同步時，Android Studio 會根據 `local.properties` 中的 SDK 路徑拉取依賴；該檔案由本機自動產生，不會提交到儲存庫。

### 命令列建置

**Windows：**

```bat
gradlew.bat assembleDebug
```

**macOS / Linux：**

```bash
./gradlew assembleDebug
```

偵錯 APK 輸出至 `app/build/outputs/apk/debug/`。

### 發布版簽名

將 [`keystore.properties.example`](keystore.properties.example) 複製為專案根目錄的 `keystore.properties` 並填寫真實值（已在 `.gitignore` 中排除，請勿提交）。未設定時 Release 建置回退使用偵錯簽名。

```properties
storeFile=release_key_for_PomodoroTimer.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

### 測試

**Windows：**

```bat
gradlew.bat test
```

**macOS / Linux：**

```bash
./gradlew test
```

單元測試涵蓋計時策略與工作階段恢復、鎖屏展示、應用屏蔽、帳戶隔離、待辦領域邏輯、密碼雜湊、Room migrations 等（主要為 JUnit + Robolectric）。儀器測試目前僅做套件名稱校驗。

## 專案結構

```
PomodoroTimer/
├── app/                          # 唯一應用模組
│   ├── schemas/                  # Room schema 匯出
│   └── src/main/
│       ├── assets/               # 法律文件、屏蔽/分類/身分規則 JSON
│       ├── java/com/skyinit/pomodorotimer/
│       │   ├── App.java / AppContainer.java / MainActivity.java / …
│       │   ├── data/             # entity、dao、database、repository、model
│       │   ├── domain/           # timer、todo、blocking、appidentity、account
│       │   ├── security/         # PasswordHasher（PBKDF2）
│       │   ├── service/          # TimerService、AppBlockingService、Alarm/Boot Receivers
│       │   ├── ui/               # home、statistics、calendar、profile、settings、
│       │   │                     # account、auth、consent、onboarding、bootstrap、theme、…
│       │   └── util/             # 鎖屏、屏蔽遮罩、勿擾、權限、捷徑等
│       └── res/                  # layout（含 layout-land）、navigation、values(-night)、xml
├── gradle/libs.versions.toml     # Version Catalog
├── keystore.properties.example
├── LICENSE
├── README*.md
└── …
```

## 主要權限說明

| 權限 | 用途 |
|------|------|
| `POST_NOTIFICATIONS` | 計時 / 屏蔽相關通知 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | 計時與屏蔽前景服務 |
| `SCHEDULE_EXACT_ALARM` | 到點與暫停逾時兜底 |
| `RECEIVE_BOOT_COMPLETED` | 開機重新排程 Alarm |
| `USE_FULL_SCREEN_INTENT` | 鎖屏全螢幕拉回 |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 降低背景被終止機率 |
| `ACCESS_NOTIFICATION_POLICY` | 專注勿擾 |
| `PACKAGE_USAGE_STATS` | 偵測前景應用（屏蔽） |
| `SYSTEM_ALERT_WINDOW` | 屏蔽遮罩 |
| `QUERY_ALL_PACKAGES` | 掃描已安裝應用 |
| `VIBRATE` | 提醒震動 |
| `CAMERA` / `READ_MEDIA_IMAGES` | 頭像 |
| `READ_MEDIA_AUDIO`（及舊版儲存讀取權限） | 自訂鈴聲 |

應用在 Manifest 中主動移除了 `ACCESS_NETWORK_STATE`，自身不宣告連網權限。可在「我的 → 設定 → 系統權限」集中查看與引導授權。

## 隱私與資料

- **完全離線**：執行時不向開發者或第三方伺服器上傳或共享使用者資料
- **本機儲存**：計時、待辦、統計與帳戶等業務資料在 Room；主題、鈴聲等裝置級偏好在 SharedPreferences；各註冊帳戶資料相互隔離
- **系統備份**：若裝置開啟自動備份 / 雲端備份，部分本機資料可能由系統備份至廠商或 Google 備份服務，詳見應用內隱私政策
- **權限按需**：通知、相機、勿擾、使用情況存取等僅在使用者授權後使用
- 完整說明見應用內「我的 → 關於」中的隱私政策與使用者服務協議

## 第三方開源元件

本應用使用 AndroidX、Material Components、MPAndroidChart 等開源庫。完整列表與許可見應用內「我的 → 關於 → 第三方開源元件」，或參閱 [`OpenSourceLicensesActivity`](app/src/main/java/com/skyinit/pomodorotimer/ui/profile/OpenSourceLicensesActivity.java)。

## 許可證

本專案採用 [Apache License 2.0](LICENSE) 開源協議。

## 參與貢獻

歡迎透過 Issue 回饋問題或提交 Pull Request。提交前請確保：

- 程式碼風格與現有專案保持一致
- 單元測試通過（`./gradlew test` / `gradlew.bat test`）
- 不提交金鑰、`local.properties`、`keystore.properties` 等敏感檔案
