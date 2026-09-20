# 番茄计时器

**语言：** [简体中文](README.zh.md) · [繁體中文](README.zh-Hant.md) · [English](README.en.md)

番茄计时器是一款面向学习与工作场景的 Android 番茄钟应用：按专注—休息节奏计时，把任务写进待办，用统计与日历回看投入，并在专注时用应用屏蔽、勿扰、锁屏全屏减少干扰。

数据只保存在本机，运行时不联网、不同步、不上传。仓库提供 Java/MVVM 源码，便于阅读、构建与二次开发。

| 项 | 值 |
|----|----|
| 包名 | `com.skyinit.pomodorotimer` |
| 最低 / 目标 SDK | Android 9（API 28） / API 36 |
| 许可证 | [Apache License 2.0](LICENSE) |

## 功能特性

### 番茄钟计时

- 默认学习 25 分钟、休息 5 分钟；学习时长可在首页长按计时数字或「我的 → 设置 → 番茄钟与待办」中调整（1 分钟～3 小时），休息 1～30 分钟
- 常用时长快捷选择：45 / 60 / 90 / 120 / 150 / 180 分钟；默认学习时长 ≥ 1 小时时以 `HH:MM:SS` 显示
- 短休息与可选长休息（每 N=2～8 个番茄后进入 10～15 分钟长休息，默认关闭）
- 学习阶段可暂停并记录原因（临时有事、被打断、灵感来了等）；最大暂停次数 1～5（默认 2）；单次暂停超过 5 分钟判定本轮失败
- 学习满约 5 分钟才建议保存为完成记录；休息结束后可选手动续轮或开启「休息结束后自动开始下一轮」
- **`TimerService` 前台服务**（`specialUse` / `pomodoro_focus_timer`）持续计时，通知栏显示进度；精确闹钟兜底到点与暂停超时；开机按会话快照重挂 Alarm
- 进程被杀后可根据本地快照恢复或结算（会话恢复）
- 提示音：系统默认 / 静音 / 自定义铃声；到点可振动
- 专属计时页支持横屏精简界面（仅保留计时与控制）

### 锁屏全屏计时

- 设置项「锁屏全屏显示计时」：专注与休息期间在锁屏上全屏展示计时（仅展示，长按解锁）
- 亮屏且处于锁屏时尝试将计时页拉回；灭屏不主动强唤醒
- 需通知权限；建议授予「全屏通知」以便离开计时页后可靠拉回（部分机型可能仅显示锁屏通知倒计时）

### 待办与任务

- **普通待办**与**待办集**（含子任务）两种类型
- 分类：工作、学习、生活、运动、娱乐、其他；优先级、标签、截止日期、预估番茄数
- 置顶最多 3 个；按优先级 / 截止日期 / 分类筛选；左滑删除
- 普通待办支持重复：每天 / 每周 / 每月（完成时推进到下一期，由域策略处理，非 WorkManager）
- 首页分组：置顶 / 过期 / 今天 / 即将 / 无日期 / 已完成
- 可一键为任务或子任务启动计时，自动累计番茄进度；待办集可显示进度并选择「下一步」子任务
- 可选「自动删除已完成任务」（完成超过 3 天后清理）

### 统计与日历

- 今日 / 本周 / 本月专注次数与时长；近 7 日趋势、本月时段分布、分类饼图（可钻取）
- 连续专注、周同比、本月洞察、干扰诊断（暂停原因分布）
- 月历按日浏览记录；记录详情支持**计时心得**（最多 200 字）；可查看拦截应用记录
- 「我的」页展示当前档案「累计完成的番茄钟」

### 专注辅助：应用屏蔽与勿扰

- **应用屏蔽**：基于使用情况访问 + 悬浮窗遮罩拦截分心应用（**不使用**无障碍服务或设备管理员）
  - 「我的」页可独立开启屏蔽模式；也可在设置中开启「番茄计时期间自动屏蔽应用」（仅学习进行中生效，暂停与休息不屏蔽）
  - 管理页支持搜索、分类、全部 / 已屏蔽 / 已放行、扫描已安装应用；规则来自本地 JSON 策略引擎
  - 单次屏蔽服务最长约 5 小时后自动停止
- **专注期间勿扰**：学习计时开始后可选开启系统勿扰（需通知策略访问权限）；开启前的系统勿扰状态会持久化落盘，计时结束（含进程被杀后到点结算）仍能可靠恢复；冷启动时对无会话的孤儿状态做条件恢复兜底

### 账户与个性化

- 启动流：隐私同意 → 首次注册（可跳过）→ 功能介绍 → 主界面（主页 / 统计 / 日历 / 我的）
- **游客**：可浏览界面并调节默认展示时长，**不能**开始计时、管理待办、查看真实统计/日历或开启应用屏蔽
- **注册账户**：本机 12 位账户 ID、密码登录、改密、凭 ID+昵称找回；多档案切换，数据按账户隔离
- 密码以 **PBKDF2-HMAC-SHA256** 哈希存储；可自定义昵称、签名与头像（相机 / 相册）
- 主题皮肤：标准色、中国色、国风渐变、莫兰迪；支持系统夜间资源

### 设置与其他

- 设置枢纽：账户 / 账户与安全 / 主题色 / 提示音 / 番茄钟与待办 / **系统权限**
- 桌面 **App Shortcuts**（长按图标，非桌面小部件）：专注 25 分钟、查看统计、屏蔽模式
- 内置常见问题（可搜索）与**开发实验室**（版本、设备、存储/内存、运行日志）
- 关于页：隐私政策、用户服务协议、第三方开源组件许可

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 11 |
| 构建 | Android Gradle Plugin 9.1.1 · Gradle 9.3.1 |
| 最低 / 目标 SDK | API 28 / 36 |
| 架构 | **MVVM**（UI → ViewModel → Repository → Room / SharedPreferences）；domain 纯策略；`AppContainer` 手工依赖注入 |
| UI | AppCompat · Material Components · ConstraintLayout · Navigation |
| 状态 | LiveData / ViewModel；部分页面采用轻量 **MVI**（`Intent` / `UiState` / `Effect` + `dispatch`） |
| 本地数据库 | Room 2.6.1（schema 导出至 `app/schemas/`） |
| 图表 | MPAndroidChart |
| 后台 | 双 `specialUse` 前台服务 + 精确 Alarm + BootReceiver（**无** WorkManager） |
| 测试 | JUnit · Robolectric · Architecture Components Testing · Room Testing |

Release 构建默认开启 minify 与 shrinkResources。

## 环境要求

- **Android Studio**（推荐最新稳定版）
- **JDK 11** 或更高版本
- **Android SDK**，含 compileSdk / targetSdk 36
- 无需后端、API Key 或网络配置

## 构建与运行

### 克隆仓库

```bash
git clone https://github.com/Jack69520/PomodoroTimer.git
cd PomodoroTimer
```

### 使用 Android Studio

1. 打开 Android Studio，选择 **Open**，选中项目根目录
2. 等待 Gradle 同步完成
3. 连接设备或启动模拟器，点击 **Run**

首次同步时，Android Studio 会根据 `local.properties` 中的 SDK 路径拉取依赖；该文件由本机自动生成，不会提交到仓库。

### 命令行构建

**Windows：**

```bat
gradlew.bat assembleDebug
```

**macOS / Linux：**

```bash
./gradlew assembleDebug
```

调试 APK 输出至 `app/build/outputs/apk/debug/`。

### 发布版签名

将 [`keystore.properties.example`](keystore.properties.example) 复制为项目根目录的 `keystore.properties` 并填写真实值（已在 `.gitignore` 中排除，请勿提交）。未配置时 Release 构建回退使用调试签名。

```properties
storeFile=release_key_for_PomodoroTimer.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

### 测试

**Windows：**

```bat
gradlew.bat test
```

**macOS / Linux：**

```bash
./gradlew test
```

单元测试覆盖计时策略与会话恢复、锁屏展示、应用屏蔽、账户隔离、待办域逻辑、密码哈希、Room migrations 等（主要为 JUnit + Robolectric）。仪器测试目前仅做包名校验。

## 项目结构

```
PomodoroTimer/
├── app/                          # 唯一应用模块
│   ├── schemas/                  # Room schema 导出
│   └── src/main/
│       ├── assets/               # 法律文档、屏蔽/分类/身份规则 JSON
│       ├── java/com/skyinit/pomodorotimer/
│       │   ├── App.java / AppContainer.java / MainActivity.java / …
│       │   ├── data/             # entity、dao、database、repository、model
│       │   ├── domain/           # timer、todo、blocking、appidentity、account
│       │   ├── security/         # PasswordHasher（PBKDF2）
│       │   ├── service/          # TimerService、AppBlockingService、Alarm/Boot Receivers
│       │   ├── ui/               # home、statistics、calendar、profile、settings、
│       │   │                     # account、auth、consent、onboarding、bootstrap、theme、…
│       │   └── util/             # 锁屏、屏蔽遮罩、勿扰、权限、快捷方式等
│       └── res/                  # layout（含 layout-land）、navigation、values(-night)、xml
├── gradle/libs.versions.toml     # Version Catalog
├── keystore.properties.example
├── LICENSE
├── README*.md
└── …
```

## 主要权限说明

| 权限 | 用途 |
|------|------|
| `POST_NOTIFICATIONS` | 计时 / 屏蔽相关通知 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | 计时与屏蔽前台服务 |
| `SCHEDULE_EXACT_ALARM` | 到点与暂停超时兜底 |
| `RECEIVE_BOOT_COMPLETED` | 开机重挂 Alarm |
| `USE_FULL_SCREEN_INTENT` | 锁屏全屏拉回 |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 降低后台被杀概率 |
| `ACCESS_NOTIFICATION_POLICY` | 专注勿扰 |
| `PACKAGE_USAGE_STATS` | 检测前台应用（屏蔽） |
| `SYSTEM_ALERT_WINDOW` | 屏蔽遮罩 |
| `QUERY_ALL_PACKAGES` | 扫描已安装应用 |
| `VIBRATE` | 提醒振动 |
| `CAMERA` / `READ_MEDIA_IMAGES` | 头像 |
| `READ_MEDIA_AUDIO`（及旧版存储读权限） | 自定义铃声 |

应用在 Manifest 中主动移除了 `ACCESS_NETWORK_STATE`，自身不声明联网权限。可在「我的 → 设置 → 系统权限」集中查看与引导授权。

## 隐私与数据

- **完全离线**：运行时不向开发者或第三方服务器上传或共享用户数据
- **本地存储**：计时、待办、统计与账户等业务数据在 Room；主题、铃声等设备级偏好在 SharedPreferences；各注册账户数据相互隔离
- **系统备份**：若设备开启自动备份 / 云备份，部分本地数据可能由系统备份至厂商或 Google 备份服务，详见应用内隐私政策
- **权限按需**：通知、相机、勿扰、使用情况访问等仅在用户授权后使用
- 完整说明见应用内「我的 → 关于」中的隐私政策与用户服务协议

## 第三方开源组件

本应用使用 AndroidX、Material Components、MPAndroidChart 等开源库。完整列表与许可见应用内「我的 → 关于 → 第三方开源组件」，或参阅 [`OpenSourceLicensesActivity`](app/src/main/java/com/skyinit/pomodorotimer/ui/profile/OpenSourceLicensesActivity.java)。

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 开源协议。

## 参与贡献

欢迎通过 Issue 反馈问题或提交 Pull Request。提交前请确保：

- 代码风格与现有项目保持一致
- 单元测试通过（`./gradlew test` / `gradlew.bat test`）
- 不提交密钥、`local.properties`、`keystore.properties` 等敏感文件
