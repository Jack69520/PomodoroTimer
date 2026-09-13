package com.skyinit.pomodorotimer;

import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.data.repository.AppInitializationRepository;
import com.skyinit.pomodorotimer.data.repository.PrivacyConsentRepository;
import com.skyinit.pomodorotimer.service.TimerServiceLauncher;
import com.skyinit.pomodorotimer.domain.appidentity.AppIdentityRulesLoader;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyRulesLoader;
import com.skyinit.pomodorotimer.util.AppCategory;
import com.skyinit.pomodorotimer.util.AppCategoryRulesLoader;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.CategoryDefaults;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 应用全局入口。完全离线运行，所有数据存于本地 Room 数据库与 SharedPreferences。
 * <p>
 * 启动流程：先检查隐私同意 → 同意后<strong>异步</strong>初始化数据库、账户、依赖容器与后台任务；
 * 若存在未恢复的番茄钟快照，则尝试拉起 {@link com.skyinit.pomodorotimer.service.TimerService}。
 */
public class App extends Application {
    private static final String TAG = "App";
    private static App instance;
    private static AppDatabase database;

    private final AppInitializationRepository initializationRepository = new AppInitializationRepository();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Runnable> pendingInitCallbacks = new ArrayList<>();

    private volatile boolean userDataInitialized;
    private volatile boolean initializationInProgress;

    @Override
    public void onCreate() {
        super.onCreate();
        DebugConfig.init(this);
        instance = this;
        installCrashHandler();

        if (PrivacyConsentRepository.getInstance(this).hasAccepted() && shouldAutoInitializeAfterConsent()) {
            initializeAfterConsent(null);
        }
    }

    /**
     * 子类可关闭自动异步初始化（Robolectric 测试在 {@link #onCreate()} 中同步引导）。
     */
    protected boolean shouldAutoInitializeAfterConsent() {
        return true;
    }

    /**
     * 测试专用：在 diskIo 线程同步完成用户数据引导，避免主线程 Room 访问与 Handler 死锁。
     */
    public synchronized void initializeAfterConsentSyncForTest() {
        if (userDataInitialized) {
            initializationRepository.markReady();
            return;
        }
        if (isRobolectricEnvironment()) {
            performInitializationOnDisk();
        } else {
            java.util.concurrent.ExecutorService bootstrapExecutor =
                    Executors.newSingleThreadExecutor(r -> {
                        Thread thread = new Thread(r, "pomodoro-test-bootstrap");
                        thread.setDaemon(true);
                        return thread;
                    });
            try {
                Future<?> future = bootstrapExecutor.submit(this::performInitializationOnDisk);
                future.get(15, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new IllegalStateException("Test bootstrap timed out", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Test bootstrap interrupted", e);
            } catch (Exception e) {
                throw new IllegalStateException("Test bootstrap failed", e);
            } finally {
                bootstrapExecutor.shutdownNow();
            }
        }
        startMainThreadWatchdog();
        userDataInitialized = true;
        initializationRepository.markReady();
    }

    public AppInitializationRepository getInitializationRepository() {
        return initializationRepository;
    }

    /**
     * 用户同意隐私政策后初始化业务数据与后台任务（异步，不阻塞主线程）。
     * 可安全重复调用（内部幂等）。
     *
     * @param onComplete 初始化成功后在主线程回调；可为 null
     */
    public synchronized void initializeAfterConsent(@Nullable Runnable onComplete) {
        if (userDataInitialized) {
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
            return;
        }
        if (onComplete != null) {
            pendingInitCallbacks.add(onComplete);
        }
        if (initializationInProgress) {
            return;
        }
        initializationInProgress = true;
        initializationRepository.markLoading();

        AppExecutors.getInstance().diskIo(() -> {
            try {
                performInitializationOnDisk();
                mainHandler.post(this::onInitializationSucceeded);
            } catch (Throwable throwable) {
                AppLog.e(TAG, "User data initialization failed", throwable);
                mainHandler.post(() -> onInitializationFailed(throwable));
            }
        }, throwable -> mainHandler.post(() -> onInitializationFailed(throwable)));
    }

    private void performInitializationOnDisk() {
        database = AppDatabase.getDatabase(this);
        AccountManager.getInstance(this).restoreSessionOnDisk();
        AppContainer.init(this);
        CategoryDefaults.init(this);
        AppCategory.init(this);
        AppIdentityRulesLoader.init(this);
        AppCategoryRulesLoader.init(this);
        BlockingPolicyRulesLoader.init(this);
        AppContainer.getInstance(this).getUserSessionRepository().syncFromAccountManager();
        AppContainer.getInstance(this).getUserPomodoroSettingsRepository().warmCacheOnDisk();

    }

    private synchronized void onInitializationSucceeded() {
        if (!userDataInitialized) {
            startMainThreadWatchdog();
            userDataInitialized = true;
            initializationRepository.markReady();
        }
        initializationInProgress = false;
        dispatchPendingCallbacks();
        restoreTimerServiceIfNeeded();
    }

    private void restoreTimerServiceIfNeeded() {
        if (isRobolectricEnvironment() || !ActiveSessionStore.hasActiveSession(this)) {
            return;
        }
        mainHandler.post(() -> {
            if (!TimerServiceLauncher.isAppInForeground(this)) {
                return;
            }
            try {
                TimerServiceLauncher.ensureRunning(this);
            } catch (Exception e) {
                AppLog.w(TAG, "Failed to restore timer service on startup", e);
            }
        });
    }

    private synchronized void onInitializationFailed(Throwable throwable) {
        initializationInProgress = false;
        String message = throwable.getMessage() != null
                ? throwable.getMessage()
                : throwable.getClass().getSimpleName();
        initializationRepository.markFailed(message);
        pendingInitCallbacks.clear();
    }

    private synchronized void dispatchPendingCallbacks() {
        if (pendingInitCallbacks.isEmpty()) {
            return;
        }
        List<Runnable> callbacks = new ArrayList<>(pendingInitCallbacks);
        pendingInitCallbacks.clear();
        for (Runnable callback : callbacks) {
            mainHandler.post(callback);
        }
    }

    public boolean isUserDataInitialized() {
        return userDataInitialized;
    }

    /**
     * 测试专用：阻塞等待异步初始化完成。
     */
    public boolean awaitUserDataInitializedForTest(long timeoutMs) throws InterruptedException {
        if (userDataInitialized) {
            return true;
        }
        initializeAfterConsent(null);
        CountDownLatch latch = new CountDownLatch(1);
        Runnable waiter = latch::countDown;
        synchronized (this) {
            pendingInitCallbacks.add(waiter);
        }
        boolean completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        return completed && userDataInitialized;
    }

    private void installCrashHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            AppLog.e("GlobalException", "Crash: " + ex.getMessage(), ex);
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, ex);
            } else if (isRobolectricEnvironment()) {
                throw new RuntimeException(
                        "Unhandled exception in thread " + thread.getName(), ex);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });
    }

    private static boolean isRobolectricEnvironment() {
        try {
            Class.forName("org.robolectric.RuntimeEnvironment");
            return true;
        } catch (ClassNotFoundException e) {
            AppLog.d(TAG, "Robolectric not on classpath");
            return false;
        }
    }

    /** 周期性检测主线程是否卡顿，便于排查 ANR 前兆。 */
    private void startMainThreadWatchdog() {
        final long watchdogInterval = 4000L;
        mainHandler.postDelayed(new Runnable() {
            long lastTick = System.currentTimeMillis();

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long delta = now - lastTick;
                if (delta > watchdogInterval * 2) {
                    AppLog.w("AppWatchdog", "Main thread stall detected: " + delta + "ms");
                }
                lastTick = now;
                mainHandler.postDelayed(this, watchdogInterval);
            }
        }, watchdogInterval);
    }

    public static AppDatabase getDatabase() {
        return database != null ? database : (instance != null ? AppDatabase.getDatabase(instance) : null);
    }

    public AppContainer getContainer() {
        return AppContainer.getInstance(this);
    }

    /** 测试专用：重置引导状态，便于在 {@link AppDatabase#resetForTest()} 后重新初始化。 */
    public static synchronized void resetUserDataStateForTest() {
        if (instance != null) {
            instance.userDataInitialized = false;
            instance.initializationInProgress = false;
            synchronized (instance.pendingInitCallbacks) {
                instance.pendingInitCallbacks.clear();
            }
        }
        database = null;
    }
}
