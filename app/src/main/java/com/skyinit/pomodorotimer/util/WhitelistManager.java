package com.skyinit.pomodorotimer.util;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import android.content.Context;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WhitelistManager {
    
    // 仅包含"不可屏蔽"的系统关键服务（强制白名单，用户不可移除）
    private static final Set<String> CRITICAL_APPS = new HashSet<>(Arrays.asList(
        // 核心系统界面与设置
        "com.android.systemui", //系统用户界面
        "com.android.settings", //设置
        "com.google.android.gsf", //Google 服务框架
        "com.android.vending",//Google Play Service
        
        // 通话与电话服务（底层服务，非拨号器UI）
        "com.android.phone", //拨号服务
        
        // 权限、网络与连接基础
        "com.android.permissioncontroller", //权限控制器
        "com.android.networkstack", //网络管理器
        "com.android.captiveportallogin", //强制门户登录
        "com.android.wifi.resources", //系统WLAN资源
        "com.android.cellbroadcastreceiver",
        "com.android.ethernet",
        "com.android.networkstack.tethering", //Tethering
        "com.android.networkstack.permissionconfig",
        
        // 基础硬件连接
        "com.android.bluetooth", //蓝牙
        "com.android.nfc" //NFC 服务
    ));

    // 默认白名单（可由用户移除以强制屏蔽）
    private static final Set<String> DEFAULT_WHITELIST = new HashSet<>(Arrays.asList(
        // 时钟与数字健康
        "com.android.deskclock",
        "com.android.clock",
        "com.google.android.deskclock",
        "com.android.wellbeing",
        "com.google.android.apps.wellbeing",
        
        // 华为/荣耀服务与运动健康
        "com.huawei.hwid", //华为HMS Core
        "com.huawei.hms",
        "com.huawei.android.hwouc",
        "com.huawei.hihealth",
        "com.huawei.health", //华为运动健康
        "com.hihonor.android.clone",
        "com.hihonor.health", //荣耀运动健康
        
	    //金融安全
	    "com.unionpay.tsmservice", // 银联可信服务
	
        // 桌面、账号、文件管理等基础服务
        "com.hihonor.android.launcher", //荣耀桌面
        "com.hihonor.android.hwbase",
        "com.hihonor.filemanager", //文件管理
        "com.hihonor.id", //荣耀账号服务
        "com.hihonor.iconnect",
        "com.hihonor.printassistant", //荣耀打印助手
        "com.hihonor.android.instantshare", //荣耀分享
        "com.huawei.android.thememanager", // 华为主题
        "com.hihonor.calendar", //荣耀日历
        "com.hihonor.parentcontrol", //荣耀健康使用手机
        "com.hihonor.magicvoice",
        "com.hihonor.nearby",
        "com.hihonor.search", //荣耀搜索
        "com.baidu.input_hihonor", //百度输入法荣耀版
        "com.hihonor.aod",
        "com.hihonor.crossdeviceserviceserviceshare",
        "com.hihonor.handoff",
        "com.hihonor.magazine",
        "com.hihonor.pcassistant",
        "com.hihonor.awareness", //荣耀情景感知
        "com.hihonor.controlcenter", //荣耀信任环控制中心
        "com.hihonor.koBackup",
        "com.hihonor.smartiot",
        "com.hihonor.virtualinput",
        "com.hihonor.trustcircle", //荣耀信任环
        
        // 相机、信息、相册
        "com.android.camera2",
        "com.android.camera",
        "com.huawei.camera",
        "com.hihonor.camera",
        "com.android.mms",
        "com.android.messaging",
        "com.huawei.mms",
        "com.hihonor.mms",
        "com.android.gallery3d",
        "com.android.gallery",
        "com.huawei.photos",
        "com.hihonor.photos",
        "com.google.android.apps.photos",
        "com.hihonor.lens", //荣耀YOYO看见（智慧视觉）

        // 拨号器UI
        "com.android.dialer",
        "com.google.android.dialer",
        "com.huawei.dialer",
        "com.hihonor.dialer",
        "com.miui.dialer",
        "com.xiaomi.dialer",
        "com.vivo.dialer",
        "com.oppo.dialer",
        "com.oneplus.dialer",
        "com.samsung.dialer",
        "com.sony.dialer",
        "com.lge.dialer",
        
        // 通讯录/联系人
        "com.android.contacts",
        "com.google.android.contacts",
        "com.huawei.contacts",
        "com.hihonor.contacts",
        "com.miui.contacts",
        "com.xiaomi.contacts",
        "com.vivo.contacts",
        "com.oppo.contacts",
        "com.oneplus.contacts",
        "com.samsung.contacts",
        "com.sony.contacts",
        "com.lge.contacts",
        
        // 小米系统服务
        "com.miui.securitycenter",
        "com.xiaomi.finddevice",
        "com.miui.compass",
        "com.miui.daemon", // MIUI后台守护程序
        "com.miui.powerkeeper", // 小米电池管理
        "com.miui.miservice", // MIUI服务
        "com.miui.tsmclient", // 支持NFC支付和安全管理
        "com.miui.securitycore", // MIUI的安全核心服务
        "com.miui.vpnsdkmanager", // VPN SDK管理
        "android.miui.home.launcher.res", // MIUI的启动器资源，管理主屏幕和应用抽屉
        "com.miui.securityinputmethod", // MIUI安全输入法
        "com.sohu.inputmethod.sogou.xiaomi", // 搜狗输入法小米版
        "org.mipay.android.manager", // 小米支付管理器
        "com.android.provision", // 配置服务，支持设备的初始设置和配置
        "com.android.printspooler", // 打印后台服务，管理设备的打印功能
        "com.android.quicksearchbox", // 快速搜索框，提供设备上的快速搜索功能
        "com.android.storagemanager", // 存储管理器
        "com.miui.system.overlay", // MIUI系统的覆盖层，定制系统的用户界面和功能
        "com.miui.wmsvc", // MIUI无线管理服务，管理无线连接和设备间通信
        "com.qualcomm.qti.biometrics.fingerprint.service", // 高通的生物识别指纹服务，管理指纹识别功能
        "com.android.cellbroadcastreceiver", // 紧急广播接收器，接收政府或运营商的紧急广播信息
        "com.android.uwb.resources", // 超宽带（UWB）资源服务，管理超宽带通信功能
        "com.qualcomm.qti.powersavemode", // 高通省电模式服务，管理设备的省电功能
        "com.android.wifi.resources.xiaomi", // 小米的Wi-Fi资源管理
        "com.miui.touchassistant", // MIUI的触摸助手，提供快捷操作功能
        "com.xiaomi.account", // 小米账户管理，支持账户的登录和同步
        "com.lbe.security.miui", // 小米安全中心
        "com.miui.core", // 小米核心服务
        "com.xiaomi.gnss.polaris", // 小米的GNSS（全球导航卫星系统）服务
        "com.miui.privacycomputing", // MIUI隐私计算服务，保护用户隐私和数据
        "com.miui.screenshot", // MIUI截屏服务，支持设备的截屏功能
        "com.xiaomi.aon", // 小米Always-on-Display服务，管理屏幕常亮显示功能
        "com.miui.securitycenter", // MIUI安全中心，提供系统的安全保护功能
        "com.miui.system", // MIUI系统核心，提供系统的基本功能和服务
        "com.miui.yellowpage", // MIUI黄页服务，提供电话本和企业信息
        "com.xiaomi.bluetooth", // 小米蓝牙服务，管理设备的蓝牙功能
        "com.miui.accessibility", // MIUI的无障碍服务，支持残障用户的设备使用
        "com.miui.aod", // MIUI的息屏显示功能，显示锁屏时钟和通知
        "com.miui.home", // MIUI桌面应用，管理设备的主屏幕和应用布局
        "com.xiaomi.mi_connect_service", // 小米连接服务，管理小米设备间的连接
        "com.sohu.inputmethod.sogou.xiaomi", // 搜狗输入法小米版，提供文本输入功能
        "com.xiaomi.aiasst.service", // 小米人工智能助手服务，提供AI助手功能
        "com.xiaomi.cameratools", // 小米相机工具，提供相机的高级功能和设置
        "com.miui.miinput", // MIUI输入法，提供文本输入功能
        "com.miui.fileexplorer", // 小米文件管理

        //vivo 基础服务
        "com.bbk.launcher",
        "com.vivo.iotserver", //vivo IOT引擎
        "com.vivo.nightpearl",
        "com.bbk.account", // vivo账户
        "com.baidu.input_bbk.service", // 百度输入法vivo版
        "com.vivo.daemonService", // vivo服务
        "com.vivo.sdkplugin", // vivo服务安全插件
        "com.vivo.smartshot", // vivo超级截屏
        "com.iqoo.powersaving", // 电池
        "com.vivo.pem", // vivo电量守护
        "com.vivo.livewallpaper.coffeetime", // vivo非凡睿智
        "com.android.tethersettings", // 个人热点
        "com.vivo.upslide", // vivo控制中心
        "com.bbk.calendar", // vivo日历
        "com.vivo.widget.calendar", // vivo日历挂件
        "com.vivo.compass", // vivo指南针
        "com.vivo.fingerprint", // vivo指纹与密码
        "com.vivo.abe", // vivo智慧引擎
        "com.vivo.abeui", // vivo智慧引擎
        "com.vivo.motionrecognition", // vivo智能体感

        //OPPO设备（Color OS）
        "com.oppo.launcher",
        "com.coloros.launcher",
        "com.heytap.htms", // （欢太）移动服务
        "com.baidu.input_oppo", // 百度输入法OPPO版
        "com.coloros.securitykeyboard", // OPPO安全键盘
        "com.coloros.securepay", // OPPO支付保护
        "com.coloros.ocrservice", // OPPO识屏服务
        "com.coloros.childrenspace", // OPPO儿童空间
        "com.coloros.translate.engine", // OPPO翻译引擎
        "com.heytap.speechassist", // OPPO语音助手
        "com.oplus.aod", // OPPO息屏显示
        "com.coloros.sharescreen", // OPPO共享与协助
        "com.coloros.oshare", // OPPO互传
        "com.android.dlna.service", // OPPO手机投屏服务
        "com.coloros.translate.engine", // OPPO翻译引擎

        //Google Pixel手机 桌面
        "com.google.android.apps.nexuslauncher",

        // -------------------- 小米 / MIUI --------------------
        "com.miui.home",
        "com.miui.securitycenter",
        "com.miui.powerkeeper",
        "com.lbe.security.miui",
        "com.miui.systemAdSolution",

        // -------------------- OPPO / ColorOS / 一加 --------------------
        "com.oplus.launcher",
        "com.oneplus.launcher",
        "net.oneplus.launcher",
        "com.oneplus.security",

        // -------------------- vivo --------------------
        "com.bbk.launcher2",
        "com.vivo.launcher",

        // -------------------- 三星 --------------------
        "com.sec.android.app.launcher",
        "com.samsung.android.app.telephonyui",
        "com.samsung.android.incallui",

        // -------------------- 红魔 / nubia --------------------
        "cn.nubia.launcher",
        "cn.nubia.redmagic.app",
        "cn.nubia.neogamecenter",
        "cn.nubia.security",
        "cn.nubia.gamelauncher",

        // 反诈应用
        "com.hicorenational.antifraud",
        "gov.picc.antifraud",
        
        // 安装器与商店
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.vending"
    ));
    
    // 注意：移除“特殊应用”标签机制。所有非系统关键应用默认不白名单，统一纳入屏蔽管理，由用户自行调整。
    
    // 浏览器应用（用户可选择是否屏蔽）
    private static final Set<String> BROWSER_APPS = new HashSet<>(Arrays.asList(
        "com.android.browser",
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.google.android.apps.chrome",
        "com.microsoft.emmx",
        "com.tencent.mtt",
        "com.UCMobile",
        "com.quark.browser",
        "com.huawei.browser",
        "com.hihonor.browser",
        "com.hihonor.baidu.browser",
        "com.mi.globalbrowser",
        "com.vivo.browser",
        "com.oppo.browser",
        "com.heytap.browser",
        "com.sec.android.app.sbrowser"
    ));

    // 应用商店（用于识别与扫描，默认可管理）
    private static final Set<String> STORE_APPS = new HashSet<>(Arrays.asList(
        "com.android.vending", // Google Play
        "com.huawei.appmarket", // 华为应用商店
        "com.xiaomi.market", // 小米应用商店
        "com.hihonor.appmarket", // 荣耀应用商店
        "com.vivo.appstore",
        "com.bbk.appstore",
        "com.heytap.market",
        "com.oppo.market",
        "com.sec.android.app.samsungapps",
        "com.samsung.android.app.galaxyfinder"
    ));

    // 邮箱应用（用于识别与扫描；分类在 AppScanner 中已有）
    private static final Set<String> EMAIL_APPS = new HashSet<>(Arrays.asList(
        "com.netease.mail",
        "com.qihoo.mail",
        "com.163.mail",
        "com.google.android.gm",
        "com.tencent.android.qqmail"
    ));

    // 支付类（用于识别与扫描）
    private static final Set<String> PAYMENT_APPS = new HashSet<>(Arrays.asList(
        "com.eg.android.AlipayGphone",
        "com.tencent.mm"
    ));

    // 办公通讯类（用于识别与扫描）
    private static final Set<String> WORK_COMM_APPS = new HashSet<>(Arrays.asList(
        "com.tencent.wework",
        "com.tencent.meeting"
    ));

    // Google 应用（用于识别与扫描）
    private static final Set<String> GOOGLE_APPS = new HashSet<>(Arrays.asList(
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.google.android.apps.maps",
        "com.google.android.apps.photos",
        "com.google.android.apps.chromecast.app",
        "com.google.android.apps.searchlite",
        "com.google.android.apps.meetings",
        "com.google.android.apps.walletnfcrel",
        "com.google.android.apps.docs",
        "com.google.android.apps.sheets",
        "com.google.android.apps.slides",
        "com.google.android.apps.dynamite",
        "com.google.android.apps.subscriptions.red"
    ));

    // 国际常见应用（用于识别与扫描）
    private static final Set<String> INTERNATIONAL_APPS = new HashSet<>(Arrays.asList(
        "com.ss.android.ugc.aweme", // TikTok
        "com.facebook.katana",
        "com.instagram.android",
        "org.telegram.messenger",
        "com.valvesoftware.android.steam.community",
        "com.microsoft.xboxapp"
    ));

    // 品牌系统类（可管理）的常见系统应用集合（默认屏蔽）
    // 厂商分区：请将不同品牌的应用集中在对应分区，便于维护
    private static final Set<String> BRAND_SYSTEM_APPS = new HashSet<>(Arrays.asList(
        // -------------------- 小米 / MIUI --------------------
        "com.miui.securitycenter",
        "com.miui.video",
        "com.miui.player",
        "com.miui.music",
        "com.miui.compass",
        "com.miui.thememanager",
        "com.miui.gallery",
        "com.miui.weather2",
        "com.miui.calculator",
        "com.xiaomi.market",
        "com.mi.globalbrowser",
        "com.miui.notes",
        "com.miui.fm",
        "com.xiaomi.mipicks",
        "com.miui.mediaviewer", // MIUI 媒体查看器
        "com.miui.carlink", // MIUI 车载连接服务
        "com.xiaomi.xmsf", // 小米推送服务框架
        "com.miui.voiceassist", // MIUI 语音助手
        "com.miui.micloudsync", // MIUI 云同步服务
        "com.miuix.editor", // MIUI 编辑器
        "com.xiaomi.joyose", // 小米娱乐/游戏相关服务
        "com.miui.translation.kingsoft", // 金山词霸 MIUI 定制翻译
        "com.miui.personalassistant", // MIUI 个人助理
        "com.miui.guardprovider", // MIUI 安全卫士
        "com.miui.cloudbackup", // MIUI 云备份
        "com.miui.freeform", // MIUI 自由窗口模式
        "com.xiaomi.finddevice", // 小米查找设备
        "com.miui.miwallpaper", // MIUI 壁纸管理
        "com.xiaomi.barrage", // 小米弹幕服务
        "com.android.internal.display.cutout.emulation.waterfall", // 瀑布屏仿真
        "com.xiaomi.mirror", // 小米镜像服务
        "com.xiaomi.migameservice", // 小米游戏服务
        "com.xiaomi.gamecenter.sdk.service", // 小米游戏中心 SDK服务
        "com.miui.cloudservice", // MIUI 云服务
        "com.xiaomi.misettings", // 小米设置服务
        "com.miui.translation.xmcloud", // 小米云翻译服务
        "com.miui.usercenter", // 我的小米
        "com.xiaomi.shop", // 小米商城
        "com.miui.manual", // 小米玩机技巧
        "com.xiaomi.smarthome", // 米家
        "com.miui.player", // 小米音乐
        "com.miui.video", // 小米视频
        "com.duokan.phone.remotecontroller", // 小米阅读（按清单要求，原为遥控器）
        "com.mipay.wallet", // 小米钱包
        "com.xiaomi.gamecenter", // 小米游戏中心


        // -------------------- Vivo --------------------
        "com.vivo.browser",
        "com.vivo.gallery",
        "com.vivo.video",
        "com.vivo.music",
        "com.vivo.appstore",
        "com.vivo.theme",
        "com.vivo.filemanager",
        "com.vivo.email",
        "com.vivo.hiborad",
        "com.vivo.magazine",
        "com.vivo.numbermark",
        "com.vivo.hybrid",
        "com.vlife.vivo.wallpaper", // vivo 动态壁纸
        "com.vivo.space", // vivo 官网
        "com.chaozh.iReader", // 掌阅 iReader（预装）
        "com.ibimuyu.lockscreen", // ibimuyu 锁屏
        "com.vivo.setupwizard", // vivo 开机向导
        "com.vivo.doubletimezoneclock", // i 挂件（轻.净）双时钟
        "com.iqoo.secure", // i 管家
        "com.vivo.appfilter", // 管家服务插件
        "com.android.bbkmusic", // i 音乐
        "com.vivo.dream.music", // i 音乐（轻简约）
        "com.bbk.theme", // i 主题
        "com.bbk.VoiceAssistant", // Vivoice 语音助手
        "com.vivo.bspservice", // VivoTestService
        "com.bbk.theme.resources", // VivoWallpaperRes
        "com.vivo.msgpush", // vivo 消息推送
        "com.bbk.scene.tech", // X 空间
        "com.vivo.dream.note", // vivo 便签（轻简约）
        "com.bbk.SuperPowerSave", // vivo 超级省电
        "com.vivo.smartmultiwindow", // 分屏多任务
        "com.vivo.collage", // vivo 拼图
        "com.vivo.livewallpaper.silk", // 轻舞飞扬 动态壁纸
        "com.vivo.livewallpaper.coralsea", // vivo 珊瑚海 动态壁纸
        "com.vivo.devicereg", // vivo设备管理
        "com.vivo.dream.clock", // vivo 时钟（轻简约）
        "com.vivo.Tips", // vivo使用技巧
        "com.vivo.findphone", // vivo手机寻回
        "com.vivo.weather", // vivo天气
        "com.vivo.dream.weather", // vivo天气（轻简约）
        "com.vivo.weather.provider", // vivo天气存储
        "com.bbk.updater", // vivo系统升级
        "com.bbk.photoframewidget", // vivo相框
        "com.vivo.minscreen", // vivo小屏
        "com.vivo.vivokaraoke", // vivo 移动KTV
        "com.vivo.doubleinstance", // vivo 应用分身
        "com.bbk.cloud", // vivo 云服务

        // -------------------- OPPO / ColorOS / HeyTap --------------------
        "com.oppo.browser",
        "com.coloros.gallery3d",
        "com.coloros.video",
        "com.coloros.music",
        "com.heytap.market",
        "com.coloros.themestore",
        "com.coloros.filemanager",
        "com.coloros.email",
        "com.coloros.healthservice", // OPPO健康服务
        "com.heytap.music", // OPPO 音乐
        "com.heytap.browser", // OPPO 浏览器
        "com.finshell.wallet", // OPPO 钱包
        "com.oppo.usercenter", // 我的OPPO
        "com.coloros.calendar", // OPPO 日历
        "com.heytap.themestore", // OPPO 主题
        "com.opos.ads", // OPPO 系统广告服务
        "com.coloros.karaoke", // OPPO k歌返听
        "com.coloros.logkit", // OPPO 反馈工具箱
        "com.heytap.pictorial", // OPPO 乐划锁屏
        "com.coloros.smartlock", // OPPO 智能解锁
        "com.coloros.gamespace", // OPPO 游戏空间
        "com.coloros.smartsidebar", // OPPO 智能边栏
        "com.coloros.focusmode", // OPPO 专注模式
        "com.coloros.smartdrive", // OPPO 智能驾驶
        "com.coloros.directui", // OPPO Breeno 识屏
        "com.coloros.systemclone", // OPPO 系统分身
        "com.heytap.yoli", // OPPO 音乐（Yoli）
        "com.iflytek.speechsuite", // OPPO 讯飞语音引擎
        "com.heytap.speechassist", // OPPO 语音助手
        "com.coloros.ocrscanner", // OPPO Breeno 识屏
        "com.heytap.quicksearchbox", // OPPO 全局搜索
        "com.nearme.instant.platform", // OPPO 快应用平台
        "com.coloros.accessibilityassistant", // OPPO 语音字幕/无障碍助手
        "com.nearme.statistics.rom", // OPPO 用户体验改进计划
        "com.coloros.feedback", // OPPO 反馈服务
        "com.android.bips", // OPPO 默认打印服务
        "com.heytap.cast", // OPPO 手机投屏
        "com.coloros.encryption", // OPPO 私密保险箱
        "com.heytap.mydevices", // OPPO 我的设备
        "com.coloros.scenemode", // OPPO 情景模式
        "com.coloros.assistantscreen", // OPPO Breeno 速览
        "com.coloros.activation", // OPPO 电子保卡
        "com.coloros.operationManual", // OPPO 使用说明
        "com.coloros.floatassistant", // OPPO 悬浮球
        "com.coloros.oppomultiapp", // OPPO 应用分身
        "com.heytap.cloud", // 欢太云
        "com.coloros.findmyphone", // OPPO 查找手机

        // -------------------- OnePlus（与OPPO同源部分包名） --------------------
        "com.oneplus.gallery",
        "com.oneplus.music",
        "com.heytap.market",

        // -------------------- Samsung --------------------
        "com.sec.android.app.sbrowser",
        "com.sec.android.gallery3d",
        "com.samsung.android.videolist",
        "com.samsung.android.themestore",
        "com.samsung.android.themecenter",
        "com.samsung.android.calendar",
        "com.samsung.android.calendarwidget",

        // -------------------- 红魔 / nubia --------------------
        "cn.nubia.launcher",
        "cn.nubia.redmagic.app",
        "cn.nubia.neogamecenter",
        "cn.nubia.security",
        "cn.nubia.gamelauncher",
        "cn.nubia.gallery",
        "cn.nubia.music",
        "cn.nubia.browser",
        "cn.nubia.theme",
        "cn.nubia.filemanager",
        "cn.nubia.video"
    ));
    
    /**
     * 检查应用是否应该默认加入白名单。
     * 默认白名单只代表专注期间默认放行；除系统关键应用外，用户仍可手动改为屏蔽。
     */
    public static boolean shouldBeWhitelisted(String packageName) {
        return DEFAULT_WHITELIST.contains(packageName) ||
               CRITICAL_APPS.contains(packageName) ||
               isHealthApp(packageName) ||
               isDialerApp(packageName) ||
               isContactsApp(packageName) ||
               isCameraMessageGalleryApp(packageName);
    }

    /**
     * 判断系统分区应用是否值得纳入屏蔽管理列表。
     * 科学分类原则：只展示用户可感知、可消耗注意力或可被主动打开的应用；底层服务保持隐藏。
     */
    public static boolean shouldIncludeSystemApp(String packageName) {
        return isSystemCriticalApp(packageName) ||
               shouldBeWhitelisted(packageName) ||
               isBrowserApp(packageName) ||
               isStoreApp(packageName) ||
               isEmailAppPackage(packageName) ||
               isPaymentApp(packageName) ||
               isWorkCommApp(packageName) ||
               isGoogleApp(packageName) ||
               isInternationalApp(packageName) ||
               isHonorHuaweiSystemApp(packageName) ||
               isBrandSystemApp(packageName);
    }

    /**
     * 统一默认屏蔽策略，避免扫描入库与运行期补录产生不一致。
     */
    public static boolean shouldBlockByDefault(String packageName) {
        return !shouldBeWhitelisted(packageName) && !isSystemCriticalApp(packageName);
    }
    
    /**
     * 检查应用是否为系统关键应用
     */
    public static boolean isSystemCriticalApp(String packageName) {
        return CRITICAL_APPS.contains(packageName);
    }
    
    // 已取消“特殊应用”概念
    public static boolean isSpecialApp(String packageName) {
        return false;
    }
    
    /**
     * 检查应用是否为浏览器应用
     */
    public static boolean isBrowserApp(String packageName) {
        return BROWSER_APPS.contains(packageName) || 
               packageName.contains("browser") ||
               packageName.contains("chrome");
    }

    public static boolean isStoreApp(String packageName) {
        return STORE_APPS.contains(packageName) || packageName.contains("market") || packageName.contains("store");
    }

    public static boolean isEmailAppPackage(String packageName) {
        return EMAIL_APPS.contains(packageName) || packageName.contains("mail");
    }

    public static boolean isPaymentApp(String packageName) {
        return PAYMENT_APPS.contains(packageName) || packageName.contains("pay");
    }

    public static boolean isWorkCommApp(String packageName) {
        return WORK_COMM_APPS.contains(packageName);
    }

    public static boolean isGoogleApp(String packageName) {
        return GOOGLE_APPS.contains(packageName) || packageName.startsWith("com.google.android.apps.") || packageName.startsWith("com.google.android.");
    }

    public static boolean isInternationalApp(String packageName) {
        return INTERNATIONAL_APPS.contains(packageName);
    }

    /**
     * 其他品牌系统应用（小米/Vivo/OPPO/OnePlus/Samsung 等）
     * 列表中的为常见可管理系统应用，默认作为可屏蔽对象纳入管理
     */
    public static boolean isBrandSystemApp(String packageName) {
        if (BRAND_SYSTEM_APPS.contains(packageName)) return true;

        if (packageName.startsWith("com.miui.") || packageName.startsWith("com.xiaomi.")) {
            return matchesAnyPackageKeyword(packageName,
                    "theme", "gallery", "music", "video", "browser", "market", "notes",
                    "file", "email", "game", "wallet", "weather", "reader", "shop",
                    "personalassistant", "voiceassist", "smarthome");
        }
        if (packageName.startsWith("com.vivo.") || packageName.startsWith("com.bbk.")) {
            return matchesAnyPackageKeyword(packageName,
                    "browser", "gallery", "music", "video", "appstore", "theme", "file",
                    "email", "game", "wallet", "weather", "reader", "voice", "assistant",
                    "space", "notes", "karaoke");
        }
        if (packageName.startsWith("com.oppo.") || packageName.startsWith("com.coloros.") || packageName.startsWith("com.heytap.") || packageName.startsWith("com.oplus.")) {
            return matchesAnyPackageKeyword(packageName,
                    "browser", "gallery", "music", "video", "market", "theme", "file",
                    "email", "health", "game", "wallet", "weather", "reader", "speech",
                    "assistant", "pictorial", "quicksearch", "karaoke", "cast");
        }
        if (packageName.startsWith("com.oneplus.")) {
            return matchesAnyPackageKeyword(packageName,
                    "gallery", "music", "video", "theme", "weather", "file", "notes", "game");
        }
        if (packageName.startsWith("com.samsung.") || packageName.startsWith("com.sec.android.")) {
            return matchesAnyPackageKeyword(packageName,
                    "browser", "gallery", "video", "theme", "music", "game", "weather", "wallet", "notes");
        }
        if (packageName.startsWith("cn.nubia.") || packageName.startsWith("com.redmagic.")) {
            return matchesAnyPackageKeyword(packageName,
                    "launcher", "gallery", "music", "browser", "theme", "game", "file", "security", "video");
        }
        return false;
    }

    private static boolean matchesAnyPackageKeyword(String packageName, String... keywords) {
        for (String keyword : keywords) {
            if (packageName.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查应用是否为运动健康应用（默认白名单）
     */
    public static boolean isHealthApp(String packageName) {
        return packageName.equals("com.huawei.health") || // 华为运动健康
               packageName.equals("com.hihonor.health") || // 荣耀运动健康
               packageName.equals("com.hihonor.android.honorhealth") || // 荣耀运动健康旧包名
               packageName.equals("com.xiaomi.hm.health") || // 小米运动健康
               packageName.equals("com.mi.health") || // 小米运动健康新包名
               packageName.equals("com.vivo.health") || // Vivo运动健康
               packageName.equals("com.oppo.health") || // OPPO运动健康
               packageName.equals("com.heytap.health") || // 欢太健康
               packageName.equals("com.samsung.health") || // 三星运动健康
               packageName.equals("com.samsung.shealth"); // 三星健康
    }
    
    /**
     * 检查应用是否为其他运动健康应用（默认屏蔽）
     */
    public static boolean isOtherHealthApp(String packageName) {
        return packageName.contains("health") || packageName.contains("fitness") ||
               packageName.contains("sport") || packageName.contains("exercise") ||
               packageName.contains("yoga") || packageName.contains("medical") ||
               packageName.contains("wellness") || packageName.contains("workout") ||
               packageName.contains("step") || packageName.contains("run") ||
               packageName.contains("walk") || packageName.contains("gym");
    }
    
    /**
     * 检查应用是否为荣耀/华为系统应用（默认屏蔽）
     */
    public static boolean isHonorHuaweiSystemApp(String packageName) {
        return packageName.equals("com.hihonor.android.hwintelligence") || // 荣耀智能检测
               packageName.equals("com.hihonor.filemanager") || // 荣耀文件管理
               packageName.equals("com.hihonor.android.hncloud") || // 荣耀云空间
               packageName.equals("com.huawei.hidisk") || // 华为云空间
               packageName.equals("com.huawei.hicloud") || // 华为云空间服务
               packageName.equals("com.huawei.gamebox") || // 华为游戏中心
               packageName.equals("com.hihonor.android.gamecenter") || // 荣耀游戏中心
               packageName.equals("com.hihonor.quickengine") || // 荣耀快应用引擎
               packageName.equals("com.huawei.music") || // 华为音乐
               packageName.equals("com.huawei.himovie") || // 华为视频
               packageName.equals("com.hihonor.hnoffice") || // 荣耀文档
               packageName.equals("com.hihonor.hnofficelauncher") || // 荣耀文档处理服务
               packageName.equals("com.hihonor.android.pushagent") || // 荣耀推送服务
               packageName.equals("com.huawei.education") || // 华为教育中心
               packageName.equals("com.huawei.hwread.dz") || // 华为阅读
               packageName.equals("com.huawei.android.thememanager") || // 华为主题
               packageName.equals("com.hihonor.android.carlink") || // 荣耀车联
               packageName.equals("com.huawei.lives") || // 华为生活服务
               packageName.equals("com.huawei.wallet") || // 华为钱包
               packageName.equals("com.hihonor.wallet") || // 荣耀钱包
               packageName.equals("com.huawei.fastapp") || // 华为快应用中心
               packageName.equals("com.hihonor.assistant") || // 荣耀YOYO建议
               packageName.equals("com.hihonor.brain") || // 荣耀智慧决策
               packageName.equals("com.hihonor.collectcenter") || // 荣耀YOYO记忆
               packageName.equals("com.hihonor.voiceengine") || // 荣耀语音服务
               packageName.equals("com.hihonor.hiboard") || // 荣耀负一屏
               packageName.equals("com.hihonor.notepad") || // 荣耀笔记
               packageName.equals("com.hihonor.systemmanager") || // 荣耀系统管家
               packageName.equals("com.hihonor.hndockbar") || // 荣耀智慧多窗
               packageName.equals("com.hihonor.devicefinder") || // 荣耀查找设备
               packageName.equals("com.hihonor.findmydevice") || // 荣耀查找设备服务
               packageName.equals("com.hihonor.hncloud") || // 荣耀云空间（别名）
               packageName.equals("com.hihonor.servicecenter") || // 荣耀快服务
               packageName.equals("com.hihonor.auto") || // 荣耀车联
               packageName.equals("com.hihonor.reteamobile.roaming") || // 荣耀环球行
               packageName.equals("com.hihonor.systemappsupdater") || // 荣耀系统更新器
               packageName.equals("com.hihonor.wallpapereditor") || // 荣耀壁纸编辑
               packageName.equals("com.hihonor.android.remotecontroller") || // 荣耀智能遥控
               packageName.equals("com.hihonor.android.totemweather") || // 荣耀天气
               packageName.equals("com.hihonor.videoeditor") || // 荣耀视频剪辑
               packageName.equals("com.hihonor.youku.video") || // 荣耀视频
               packageName.equals("com.hihonor.cloudmusic") || // 荣耀音乐
               packageName.equals("com.hihonor.dz.reader") || // 荣耀阅读
               packageName.equals("com.huawei.mycenter") || // 华为会员中心
               packageName.equals("com.huawei.education") || // 华为教育中心（重复保留）
               packageName.equals("com.huawei.hwread.dz") || // 华为阅读
               packageName.equals("com.huawei.lives") || // 华为生活服务
               packageName.equals("com.huawei.allianceapp") || // 华为开发者联盟
               packageName.equals("com.huawei.maps.app") || // 花瓣地图
               packageName.equals("com.mapp") || // 华为云
               packageName.equals("com.huawei.hiskytone") || // 华为天际通
               packageName.equals("com.huawei.skytone") || // 华为天际通（别名）
               packageName.equals("com.microsoft.appmanager") || // 微软连接至Windows
               packageName.equals("com.huawei.appmarket") || // 华为应用商店
               packageName.equals("com.hihonor.appmarket") || // 荣耀应用商店
               packageName.equals("cn.honor.qinxuan") || // 荣耀亲选
               packageName.equals("com.hihonor.phoneservice") || // 我的荣耀
               packageName.equals("com.hihonor.vmall") || // 荣耀商城
               packageName.equals("com.vmall.client") || // 华为商城
               packageName.equals("com.hihonor.sceneservice") || // 荣耀智慧空间基础服务
               packageName.equals("com.hihonor.magichome") || // 荣耀智慧空间
               packageName.equals("com.huawei.smartthome") || // 华为智慧生活
               packageName.equals("com.hihonor.youku.video") || // 荣耀视频
               packageName.equals("com.hihonor.cloudmusic") || // 荣耀音乐
               packageName.equals("com.hihonor.dz.reader") || // 荣耀阅读
               packageName.equals("com.hihonor.gamecenter") || // 荣耀游戏中心（别名）
               packageName.equals("com.hihonor.gameassistant") || // 荣耀游戏助手
               packageName.equals("com.hihonor.magicavatar"); // 荣耀魔法形象（修正包名）
    }
    
    /**
     * 检查应用是否为相机、信息、相册应用（默认白名单）
     */
    public static boolean isCameraMessageGalleryApp(String packageName) {
        return packageName.equals("com.android.camera2") ||
               packageName.equals("com.android.camera") ||
               packageName.equals("com.huawei.camera") ||
               packageName.equals("com.hihonor.camera") ||
               packageName.equals("com.android.mms") ||
               packageName.equals("com.android.messaging") ||
               packageName.equals("com.huawei.mms") ||
               packageName.equals("com.hihonor.mms") ||
               packageName.equals("com.android.gallery3d") ||
               packageName.equals("com.android.gallery") ||
               packageName.equals("com.huawei.photos") ||
               packageName.equals("com.hihonor.photos") ||
               packageName.equals("com.google.android.apps.photos");
    }
    
    /**
     * 检查应用是否为拨号应用（默认白名单）
     */
    public static boolean isDialerApp(String packageName) {
        return packageName.equals("com.android.dialer") ||
               packageName.equals("com.google.android.dialer") ||
               packageName.equals("com.huawei.dialer") ||
               packageName.equals("com.hihonor.dialer") ||
               packageName.equals("com.miui.dialer") ||
               packageName.equals("com.xiaomi.dialer") ||
               packageName.equals("com.vivo.dialer") ||
               packageName.equals("com.oppo.dialer") ||
               packageName.equals("com.oneplus.dialer") ||
               packageName.equals("com.samsung.dialer") ||
               packageName.equals("com.sony.dialer") ||
               packageName.equals("com.lge.dialer") ||
               packageName.contains("dialer");
    }
    
    /**
     * 检查应用是否为通讯录/联系人应用（默认白名单）
     */
    public static boolean isContactsApp(String packageName) {
        return packageName.equals("com.android.contacts") ||
               packageName.equals("com.google.android.contacts") ||
               packageName.equals("com.huawei.contacts") ||
               packageName.equals("com.hihonor.contacts") ||
               packageName.equals("com.miui.contacts") ||
               packageName.equals("com.xiaomi.contacts") ||
               packageName.equals("com.vivo.contacts") ||
               packageName.equals("com.oppo.contacts") ||
               packageName.equals("com.oneplus.contacts") ||
               packageName.equals("com.samsung.contacts") ||
               packageName.equals("com.sony.contacts") ||
               packageName.equals("com.lge.contacts") ||
               packageName.contains("contacts");
    }
    
    /**
     * 根据包名判断应用类型（本地化描述）
     */
    public static String getAppTypeDescription(Context context, String packageName) {
        if (isSystemCriticalApp(packageName)) {
            return context.getString(R.string.app_type_system_critical);
        } else if (isHealthApp(packageName) || isOtherHealthApp(packageName)) {
            return context.getString(R.string.app_category_health);
        } else if (isDialerApp(packageName)) {
            return context.getString(R.string.app_type_dialer);
        } else if (isContactsApp(packageName)) {
            return context.getString(R.string.app_type_contacts);
        } else if (isCameraMessageGalleryApp(packageName)) {
            return context.getString(R.string.app_type_camera_messaging);
        } else if (isHonorHuaweiSystemApp(packageName)) {
            return context.getString(R.string.app_category_system);
        } else if (isBrandSystemApp(packageName)) {
            return context.getString(R.string.app_category_system);
        } else if (isBrowserApp(packageName)) {
            return context.getString(R.string.app_type_browser);
        } else if ((packageName.startsWith("com.hihonor.") || packageName.startsWith("cn.honor."))
                && !packageName.equals("com.hihonor.android.launcher")) {
            return context.getString(R.string.app_category_system);
        } else if (packageName.startsWith("com.huawei.")) {
            return context.getString(R.string.app_category_system);
        } else if (packageName.startsWith("com.miui.") || packageName.startsWith("com.xiaomi.")) {
            return context.getString(R.string.app_category_system);
        } else if (packageName.startsWith("com.vivo.")) {
            return context.getString(R.string.app_category_system);
        } else if (packageName.startsWith("com.oppo.")
                || packageName.startsWith("com.coloros.")
                || packageName.startsWith("com.heytap.")) {
            return context.getString(R.string.app_category_system);
        } else if (packageName.startsWith("com.samsung.")) {
            return context.getString(R.string.app_category_system);
        } else if (packageName.startsWith("com.oneplus.")) {
            return context.getString(R.string.app_category_system);
        } else {
            return context.getString(R.string.blocking_app_type_normal);
        }
    }
    
    /**
     * 检查包名是否包含在已知的无法屏蔽应用列表中
     */
    public static boolean isUnblockableApp(String packageName) {
        // 仅系统关键应用不可屏蔽
        return isSystemCriticalApp(packageName);
    }
    
    /**
     * 应用白名单配置到BlockedApp
     */
    public static void applyWhitelistConfig(List<BlockedApp> apps) {
        for (BlockedApp app : apps) {
            if (shouldBeWhitelisted(app.packageName)) {
                app.isWhitelisted = true;
                // 白名单优先：加入白名单时一律取消屏蔽（包含系统关键应用）
                app.isEnabled = false;
            }
        }
    }
}
