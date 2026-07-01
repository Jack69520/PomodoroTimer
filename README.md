# 番茄计时器

**语言：** [简体中文](README.zh.md) · [繁體中文](README.zh-Hant.md) · [English](README.en.md)

一款完全离线运行的 Android 番茄钟应用，帮助管理专注时间、待办任务与学习习惯。计时、待办与统计等数据均保存在设备本地，不会上传或联网同步。

## 功能特性

### 番茄钟计时

- 默认学习 25 分钟、休息 5 分钟，学习时长可在首页长按计时数字或「设置」中调整（1 分钟～3 小时）
- 支持短休息与可选的长休息（每 N 个番茄钟后 10～15 分钟长休息）
- 学习阶段可暂停并记录原因；可配置单次最大暂停次数（1～5 次）
- 暂停超过 5 分钟将判定本轮失败；有效学习满 5 分钟方可记入统计
- 休息结束后可选择开始新一轮或结束，也可开启自动开始下一轮
- 计时以后台前台服务持续运行，通知栏显示进度
- 支持自定义提示音、振动与精确闹钟提醒

### 待办与任务

- **普通待办**与**待办集**（含子任务）两种类型
- 分类（工作、学习、生活等）、优先级、标签、截止日期、预估番茄数
- 置顶（最多 3 个）、筛选、左滑删除、重复创建（每日 / 每周 / 每月）
- 为任务或子任务一键启动计时，自动累计番茄进度

### 统计与日历

- 今日 / 本周 / 本月专注次数与时长图表
- 最近 7 天趋势、本月时段分布、分类占比饼图（可钻取明细）
- 暂停原因分布统计
- 月历视图按日浏览专注记录，支持记录详情与计时心得

### 专注辅助

- **应用屏蔽**：学习计时期间自动拦截娱乐、社交、购物等分心应用（需授权使用情况访问、悬浮窗等权限）
- **专注期间勿扰**：可选在学习计时开始后自动开启系统勿扰模式
- 首页横屏精简计时界面，适合沉浸式专注

### 账户与个性化

- 首次启动自动创建本地档案，无需登录即可使用
- 可选升级为注册账户（密码保护、多档案切换），密码经加密存储
- 自定义昵称、签名与头像
- 多套主题色（标准色、中国色、国风渐变、莫兰迪）

### 其他

- 桌面快捷方式：快速开始 25 分钟专注、查看统计、启用屏蔽模式
- 内置常见问题与开发实验室（应用 / 设备信息、运行日志）

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 11 |
| 最低 SDK | Android 9（API 28） |
| 目标 SDK | API 36 |
| 架构 | ViewModel + LiveData + Repository + Room |
| 本地数据库 | Room |
| 导航 | AndroidX Navigation |
| 后台任务 | WorkManager |
| 图表 | MPAndroidChart |
| UI | Material Components |

## 环境要求

- **Android Studio**（推荐最新稳定版）
- **JDK 11** 或更高版本
- **Android SDK**，含 API 36 构建工具

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

生成的调试 APK 位于 `app/build/outputs/apk/debug/`。

### 发布版签名

发布构建需要在本机根目录创建 `keystore.properties` 并配置签名信息（该文件已在 `.gitignore` 中排除，请勿提交）。若未配置，Release 构建将回退使用调试签名。

```properties
storeFile=your-release-key.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

### 运行单元测试

```bash
./gradlew test
```

## 项目结构

```
app/src/main/java/com/skyinit/pomodorotimer/
├── data/           # Room 实体、DAO、Repository
├── service/        # 计时前台服务、应用屏蔽服务
├── ui/             # Activity、Fragment、ViewModel
│   ├── home/       # 主页、计时、待办编辑
│   ├── statistics/ # 统计图表
│   ├── calendar/   # 日历与记录详情
│   ├── profile/    # 设置、关于、应用屏蔽
│   ├── account/    # 登录、注册、账户管理
│   └── consent/    # 首次启动隐私同意
├── util/           # 工具类
└── worker/         # 重复待办调度
```

## 隐私与数据

- **完全离线**：应用不联网，不会上传或共享任何用户数据
- **本地存储**：计时记录、待办、统计与账户信息均保存在设备本地 Room 数据库中
- **权限按需**：仅在用户授权时使用通知、相机、勿扰、应用使用情况等系统能力
- 完整说明见应用内「我的 → 关于」中的隐私政策与用户服务协议

## 第三方开源组件

本应用使用了 AndroidX、Material Components、MPAndroidChart 等开源库。完整列表与许可信息见应用内「我的 → 关于 → 第三方开源组件」，或参阅 [`OpenSourceLicensesActivity`](app/src/main/java/com/skyinit/pomodorotimer/ui/profile/OpenSourceLicensesActivity.java)。

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 开源协议。

## 参与贡献

欢迎通过 Issue 反馈问题或提交 Pull Request。提交前请确保：

- 代码风格与现有项目保持一致
- 单元测试通过（`./gradlew test`）
- 不提交密钥、`local.properties`、`keystore.properties` 等敏感文件
