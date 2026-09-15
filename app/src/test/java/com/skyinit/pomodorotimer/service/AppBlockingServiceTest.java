package com.skyinit.pomodorotimer.service;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.os.SystemClock;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.TestApp;
import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.util.AppBlockingServiceUtils;
import com.skyinit.pomodorotimer.util.AppExecutors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAppOpsManager;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link AppBlockingService} 单元 / 模块测试。
 * <p>
 * 覆盖双来源生命周期（独立屏蔽 vs 计时联动）以及 {@code isAppBlocked} 判定规则。
 * 使用统计权限通过 {@link ShadowAppOpsManager} 授予；内部标志通过反射读取，
 * 避免为测试污染生产 API。监控循环与悬浮窗依赖系统服务，本类不测端到端拦截 UI。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class AppBlockingServiceTest {

    private static final String TEST_PASSWORD = "Abcdef1";
    private static final long CALLBACK_TIMEOUT_MS = 10_000L;

    private Context context;
    private AppBlockingService service;
    private ServiceController<AppBlockingService> controller;

    @Before
    public void setUp() throws InterruptedException {
        context = org.robolectric.RuntimeEnvironment.getApplication();
        ((App) context).initializeAfterConsentSyncForTest();
        ActiveSessionStore.clear(context);
        ensureLoggedIn();
        grantUsageStatsPermission();

        controller = Robolectric.buildService(AppBlockingService.class);
        service = controller.create().get();
    }

    @After
    public void tearDown() {
        if (controller != null) {
            try {
                controller.destroy();
            } catch (Exception ignored) {
                // 部分路径已 stopSelf，destroy 可能重复，忽略即可
            }
        }
    }

    // ---------- 生命周期：双来源启停 ----------

    /**
     * 无 usage stats 权限时，start 不得进入监控（避免无权限空转前台服务）。
     */
    @Test
    public void start_withoutUsageStats_doesNotMonitor() throws Exception {
        // 收回权限后再测
        ShadowAppOpsManager shadowOps = Shadows.shadowOf(
                (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE));
        shadowOps.setMode(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(),
                context.getPackageName(), AppOpsManager.MODE_IGNORED);

        deliverStart(AppBlockingServiceUtils.SOURCE_STANDALONE, 0L);

        assertFalse(readBoolean("standaloneBlockingActive"));
        assertFalse(readBoolean("isMonitoring"));
    }

    /** 独立屏蔽启动后：standalone 标志与监控应为 true。 */
    @Test
    public void startStandalone_activatesMonitoring() throws Exception {
        deliverStart(AppBlockingServiceUtils.SOURCE_STANDALONE, 0L);

        assertTrue(readBoolean("standaloneBlockingActive"));
        assertFalse(readBoolean("timerBlockingActive"));
        assertTrue(readBoolean("isMonitoring"));
        assertEquals(AccountManager.getInstance(context).getCurrentUserId(),
                readString("blockingUserId"));
    }

    /** 计时联动启动：应写入 timer 来源与 sessionStartTime。 */
    @Test
    public void startTimer_setsSessionStartTime() throws Exception {
        long sessionStart = 1_700_123_456_789L;
        deliverStart(AppBlockingServiceUtils.SOURCE_TIMER, sessionStart);

        assertTrue(readBoolean("timerBlockingActive"));
        assertFalse(readBoolean("standaloneBlockingActive"));
        assertTrue(readBoolean("isMonitoring"));
        assertEquals(sessionStart, readLong("timerSessionStartTime"));
    }

    /**
     * 双来源同时开启时，只停独立来源：监控应继续（计时联动仍有效）。
     * 这是「番茄结束但用户仍开着独立屏蔽」的关键路径。
     */
    @Test
    public void stopStandalone_whileTimerActive_keepsMonitoring() throws Exception {
        deliverStart(AppBlockingServiceUtils.SOURCE_STANDALONE, 0L);
        deliverStart(AppBlockingServiceUtils.SOURCE_TIMER, 99L);

        deliverStop(AppBlockingServiceUtils.SOURCE_STANDALONE);

        assertFalse(readBoolean("standaloneBlockingActive"));
        assertTrue(readBoolean("timerBlockingActive"));
        assertTrue("计时联动仍在，监控不得停", readBoolean("isMonitoring"));
    }

    /**
     * 双来源都停后：监控结束，服务应 stopSelf。
     */
    @Test
    public void stopBothSources_stopsMonitoring() throws Exception {
        deliverStart(AppBlockingServiceUtils.SOURCE_STANDALONE, 0L);
        deliverStart(AppBlockingServiceUtils.SOURCE_TIMER, 50L);

        deliverStop(AppBlockingServiceUtils.SOURCE_STANDALONE);
        deliverStop(AppBlockingServiceUtils.SOURCE_TIMER);

        assertFalse(readBoolean("standaloneBlockingActive"));
        assertFalse(readBoolean("timerBlockingActive"));
        assertFalse(readBoolean("isMonitoring"));
    }

    /**
     * 仅停计时来源、独立仍开：监控继续，且 sessionStartTime 应清零。
     */
    @Test
    public void stopTimer_whileStandaloneActive_clearsSessionStart() throws Exception {
        deliverStart(AppBlockingServiceUtils.SOURCE_STANDALONE, 0L);
        deliverStart(AppBlockingServiceUtils.SOURCE_TIMER, 88L);

        deliverStop(AppBlockingServiceUtils.SOURCE_TIMER);

        assertTrue(readBoolean("standaloneBlockingActive"));
        assertFalse(readBoolean("timerBlockingActive"));
        assertEquals(0L, readLong("timerSessionStartTime"));
        assertTrue(readBoolean("isMonitoring"));
    }

    // ---------- isAppBlocked 判定 ----------

    /** 缓存未加载完成前一律放行，避免扫描期间误拦系统桌面等。 */
    @Test
    public void isAppBlocked_false_whenCacheNotLoaded() throws Exception {
        setField("cacheLoaded", new java.util.concurrent.atomic.AtomicBoolean(false));
        assertFalse(invokeIsAppBlocked("com.tencent.mm"));
    }

    /** 本应用自身永不拦截。 */
    @Test
    public void isAppBlocked_false_forOwnPackage() throws Exception {
        prepareLoadedCache();
        assertFalse(invokeIsAppBlocked(context.getPackageName()));
    }

    /** 策略标记为 CRITICAL 的包（如 systemui）永不拦截。 */
    @Test
    public void isAppBlocked_false_forCriticalSystemUi() throws Exception {
        prepareLoadedCache();
        assertFalse(invokeIsAppBlocked("com.android.systemui"));
    }

    /** 白名单优先于启用列表：即便也在 enabled 集合中也不拦。 */
    @Test
    public void isAppBlocked_false_whenWhitelisted() throws Exception {
        prepareLoadedCache();
        @SuppressWarnings("unchecked")
        Set<String> whitelist = (Set<String>) getField("whitelistedPackages");
        @SuppressWarnings("unchecked")
        Set<String> enabled = (Set<String>) getField("enabledBlockPackages");
        whitelist.add("com.example.chat");
        enabled.add("com.example.chat");

        assertFalse(invokeIsAppBlocked("com.example.chat"));
    }

    /** 启用列表中的第三方包应拦截。 */
    @Test
    public void isAppBlocked_true_whenInEnabledList() throws Exception {
        prepareLoadedCache();
        @SuppressWarnings("unchecked")
        Set<String> enabled = (Set<String>) getField("enabledBlockPackages");
        enabled.add("com.example.game");

        // 标记为非系统分区，避免走「系统分区不拦」分支
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentHashMap<String, Boolean> sysCache =
                (java.util.concurrent.ConcurrentHashMap<String, Boolean>) getField("systemPartitionCache");
        sysCache.put("com.example.game", false);

        assertTrue(invokeIsAppBlocked("com.example.game"));
    }

    /**
     * 缓存刷新：DB 中启用/白名单包应写入内存集合。
     * 异步经 {@link AppExecutors}，需短暂等待 + idle looper。
     */
    @Test
    public void refreshBlockCache_loadsEnabledAndWhitelist() throws Exception {
        deliverStart(AppBlockingServiceUtils.SOURCE_STANDALONE, 0L);
        String userId = AccountManager.getInstance(context).getCurrentUserId();

        BlockedApp blocked = new BlockedApp("com.example.blocked", "被拦应用", "娱乐");
        blocked.userId = userId;
        blocked.isEnabled = true;
        blocked.isWhitelisted = false;

        BlockedApp white = new BlockedApp("com.example.white", "白名单应用", "工具");
        white.userId = userId;
        white.isEnabled = false;
        white.isWhitelisted = true;

        com.skyinit.pomodorotimer.AppDatabase.getDatabase(context)
                .blockedAppDao()
                .insert(blocked);
        com.skyinit.pomodorotimer.AppDatabase.getDatabase(context)
                .blockedAppDao()
                .insert(white);

        // 强制再刷一次（首次 start 可能已刷过空表）
        Method refresh = AppBlockingService.class.getDeclaredMethod(
                "refreshBlockCacheAsync", boolean.class);
        refresh.setAccessible(true);
        refresh.invoke(service, true);

        waitUntilCacheContains("com.example.blocked", "com.example.white");

        @SuppressWarnings("unchecked")
        Set<String> enabled = (Set<String>) getField("enabledBlockPackages");
        @SuppressWarnings("unchecked")
        Set<String> whitelist = (Set<String>) getField("whitelistedPackages");
        assertTrue(enabled.contains("com.example.blocked"));
        assertTrue(whitelist.contains("com.example.white"));
        assertFalse(enabled.contains("com.example.white"));
    }

    // ---------- 辅助 ----------

    /** 标记缓存已就绪，供 isAppBlocked 判定用例使用。 */
    private void prepareLoadedCache() throws Exception {
        setField("cacheLoaded", new java.util.concurrent.atomic.AtomicBoolean(true));
        setField("blockingUserId", AccountManager.getInstance(context).getCurrentUserId());
    }

    private void waitUntilCacheContains(String enabledPkg, String whitePkg)
            throws InterruptedException, Exception {
        long deadline = SystemClock.elapsedRealtime() + 5_000L;
        while (SystemClock.elapsedRealtime() < deadline) {
            ShadowLooper.idleMainLooper();
            Thread.sleep(50);
            @SuppressWarnings("unchecked")
            Set<String> enabled = (Set<String>) getField("enabledBlockPackages");
            @SuppressWarnings("unchecked")
            Set<String> whitelist = (Set<String>) getField("whitelistedPackages");
            java.util.concurrent.atomic.AtomicBoolean loaded =
                    (java.util.concurrent.atomic.AtomicBoolean) getField("cacheLoaded");
            if (loaded.get()
                    && enabled.contains(enabledPkg)
                    && whitelist.contains(whitePkg)) {
                return;
            }
        }
        fail("Timed out waiting for block cache refresh");
    }

    private void grantUsageStatsPermission() {
        AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        ShadowAppOpsManager shadowOps = Shadows.shadowOf(ops);
        shadowOps.setMode(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(),
                context.getPackageName(), AppOpsManager.MODE_ALLOWED);
    }

    private void deliverStart(String source, long sessionStartTime) {
        Intent intent = new Intent(context, AppBlockingService.class);
        intent.putExtra(AppBlockingServiceUtils.EXTRA_ACTION,
                AppBlockingServiceUtils.ACTION_START_BLOCKING);
        intent.putExtra(AppBlockingServiceUtils.EXTRA_BLOCKING_SOURCE, source);
        if (sessionStartTime > 0L) {
            intent.putExtra(AppBlockingServiceUtils.EXTRA_SESSION_START_TIME, sessionStartTime);
        }
        service.onStartCommand(intent, 0, 1);
    }

    private void deliverStop(String source) {
        Intent intent = new Intent(context, AppBlockingService.class);
        intent.putExtra(AppBlockingServiceUtils.EXTRA_ACTION,
                AppBlockingServiceUtils.ACTION_STOP_BLOCKING);
        intent.putExtra(AppBlockingServiceUtils.EXTRA_BLOCKING_SOURCE, source);
        service.onStartCommand(intent, 0, 1);
    }

    private boolean invokeIsAppBlocked(String packageName) throws Exception {
        Method m = AppBlockingService.class.getDeclaredMethod("isAppBlocked", String.class);
        m.setAccessible(true);
        return (boolean) m.invoke(service, packageName);
    }

    private boolean readBoolean(String fieldName) throws Exception {
        return getField(fieldName) instanceof Boolean
                ? (Boolean) getField(fieldName)
                : false;
    }

    private long readLong(String fieldName) throws Exception {
        Object v = getField(fieldName);
        return ((Number) v).longValue();
    }

    private String readString(String fieldName) throws Exception {
        return (String) getField(fieldName);
    }

    private Object getField(String fieldName) throws Exception {
        Field f = AppBlockingService.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(service);
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field f = AppBlockingService.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(service, value);
    }

    /** 与 TimerServiceTest 一致：注册测试账号并保持已登录会话。 */
    private void ensureLoggedIn() throws InterruptedException {
        AccountManager accountManager = AccountManager.getInstance(context);
        if (accountManager.hasActiveSession()) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>();
        accountManager.register("BlockTester", TEST_PASSWORD, TEST_PASSWORD, null, null,
                new AccountManager.RegisterCallback() {
                    @Override
                    public void onSuccess(User user) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        error.set(message);
                        latch.countDown();
                    }
                });
        long deadline = SystemClock.elapsedRealtime() + CALLBACK_TIMEOUT_MS;
        while (latch.getCount() > 0 && SystemClock.elapsedRealtime() < deadline) {
            ShadowLooper.idleMainLooper();
            latch.await(50, TimeUnit.MILLISECONDS);
        }
        if (latch.getCount() > 0) {
            fail("Timed out registering test user");
        }
        if (error.get() != null) {
            fail("Register failed: " + error.get());
        }
        assertTrue(accountManager.hasActiveSession());
    }
}
