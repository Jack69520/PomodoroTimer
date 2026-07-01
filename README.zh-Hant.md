# 番茄計時器

**語言：** [简体中文](README.zh.md) · [繁體中文](README.zh-Hant.md) · [English](README.en.md)

一款完全離線運行的 Android 番茄鐘應用，協助管理專注時間、待辦任務與學習習慣。計時、待辦與統計等資料均保存在裝置本機，不會上傳或連網同步。

## 功能特性

### 番茄鐘計時

- 預設學習 25 分鐘、休息 5 分鐘，學習時長可在首頁長按計時數字或「設定」中調整（1 分鐘～3 小時）
- 支援短休息與可選的長休息（每 N 個番茄鐘後 10～15 分鐘長休息）
- 學習階段可暫停並記錄原因；可設定單次最大暫停次數（1～5 次）
- 暫停超過 5 分鐘將判定本輪失敗；有效學習滿 5 分鐘方可記入統計
- 休息結束後可選擇開始新一輪或結束，也可開啟自動開始新一輪
- 計時以後台前台服務持續運行，通知欄顯示進度
- 支援自訂提示音、振動與精確鬧鐘提醒

### 待辦與任務

- **普通待辦**與**待辦集**（含子任務）兩種類型
- 分類（工作、學習、生活等）、優先級、標籤、截止日期、預估番茄數
- 置頂（最多 3 個）、篩選、左滑刪除、重複建立（每日 / 每週 / 每月）
- 為任務或子任務一鍵啟動計時，自動累計番茄進度

### 統計與日曆

- 今日 / 本週 / 本月專注次數與時長圖表
- 最近 7 天趨勢、本月時段分佈、分類佔比圓餅圖（可鑽取明細）
- 暫停原因分佈統計
- 月曆視圖按日瀏覽專注記錄，支援記錄詳情與計時心得

### 專注輔助

- **應用屏蔽**：學習計時期間自動攔截娛樂、社交、購物等分心應用（需授權使用情況存取、懸浮窗等權限）
- **專注期間勿擾**：可選在學習計時開始後自動開啟系統勿擾模式
- 首頁橫屏精簡計時介面，適合沉浸式專注

### 帳戶與個人化

- 首次啟動自動建立本機檔案，無需登入即可使用
- 可選升級為註冊帳戶（密碼保護、多檔案切換），密碼經加密儲存
- 自訂暱稱、簽名與頭像
- 多套主題色（標準色、中國色、國風漸層、莫蘭迪）

### 其他

- 桌面捷徑：快速開始 25 分鐘專注、查看統計、啟用屏蔽模式
- 內建常見問題與開發實驗室（應用 / 裝置資訊、運行日誌）

## 技術棧

| 類別 | 技術 |
|------|------|
| 語言 | Java 11 |
| 最低 SDK | Android 9（API 28） |
| 目標 SDK | API 36 |
| 架構 | ViewModel + LiveData + Repository + Room |
| 本機資料庫 | Room |
| 導航 | AndroidX Navigation |
| 背景任務 | WorkManager |
| 圖表 | MPAndroidChart |
| UI | Material Components |

## 環境要求

- **Android Studio**（建議最新穩定版）
- **JDK 11** 或更高版本
- **Android SDK**，含 API 36 建置工具

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

產生的偵錯 APK 位於 `app/build/outputs/apk/debug/`。

### 發布版簽名

發布建置需要在本機根目錄建立 `keystore.properties` 並設定簽名資訊（該檔案已在 `.gitignore` 中排除，請勿提交）。若未設定，Release 建置將回退使用偵錯簽名。

```properties
storeFile=your-release-key.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

### 執行單元測試

```bash
./gradlew test
```

## 專案結構

```
app/src/main/java/com/skyinit/pomodorotimer/
├── data/           # Room 實體、DAO、Repository
├── service/        # 計時前台服務、應用屏蔽服務
├── ui/             # Activity、Fragment、ViewModel
│   ├── home/       # 首頁、計時、待辦編輯
│   ├── statistics/ # 統計圖表
│   ├── calendar/   # 日曆與記錄詳情
│   ├── profile/    # 設定、關於、應用屏蔽
│   ├── account/    # 登入、註冊、帳戶管理
│   └── consent/    # 首次啟動隱私同意
├── util/           # 工具類
└── worker/         # 重複待辦排程
```

## 隱私與資料

- **完全離線**：應用不連網，不會上傳或共享任何使用者資料
- **本機儲存**：計時記錄、待辦、統計與帳戶資訊均保存在裝置本機 Room 資料庫中
- **權限按需**：僅在使用者授權時使用通知、相機、勿擾、應用使用情況等系統能力
- 完整說明見應用內「我的 → 關於」中的隱私政策與使用者服務協議

## 第三方開源元件

本應用使用了 AndroidX、Material Components、MPAndroidChart 等開源庫。完整列表與許可資訊見應用內「我的 → 關於 → 第三方開源元件」，或參閱 [`OpenSourceLicensesActivity`](app/src/main/java/com/skyinit/pomodorotimer/ui/profile/OpenSourceLicensesActivity.java)。

## 許可證

本專案採用 [Apache License 2.0](LICENSE) 開源協議。

## 參與貢獻

歡迎透過 Issue 回饋問題或提交 Pull Request。提交前請確保：

- 程式碼風格與現有專案保持一致
- 單元測試通過（`./gradlew test`）
- 不提交金鑰、`local.properties`、`keystore.properties` 等敏感檔案
