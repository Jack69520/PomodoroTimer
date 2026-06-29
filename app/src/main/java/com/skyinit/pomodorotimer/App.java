package com.skyinit.pomodorotimer;

import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.data.repository.PrivacyConsentRepository;
import com.skyinit.pomodorotimer.service.TimerServiceLauncher;
import com.skyinit.pomodorotimer.util.AppCategory;
import com.skyinit.pomodorotimer.util.AppCategoryRulesLoader;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.CategoryDefaults;
import com.skyinit.pomodorotimer.worker.RecurringTaskScheduler;

import android.app.Application;

public class App extends Application {
    private static App instance;
    private static AppDatabase database;

    private volatile boolean userDataInitialized;

    @Override
    public void onCreate() {
        super.onCreate();
        DebugConfig.init(this);
        instance = this;
        installCrashHandler();

        if (PrivacyConsentRepository.getInstance(this).hasAccepted()) {
            initializeAfterConsent();
        }
    }

    /**
     * 用户同意隐私政策后初始化业务数据与后台任务。
     * 可安全重复调用（内部幂等）。
     */
    public synchronized void initializeAfterConsent() {
        if (userDataInitialized) {
            return;
        }
        database = AppDatabase.getDatabase(this);
        AccountManager.getInstance(this).ensureDefaultProfile();
        AppContainer.init(this);
        CategoryDefaults.init(this);
        AppCategory.init(this);
        AppCategoryRulesLoader.init(this);
        AppContainer.getInstance(this).getUserSessionRepository().syncFromAccountManager();
        AppContainer.getInstance(this).getUserPomodoroSettingsRepository().warmCache();

        if (ActiveSessionStore.hasActiveSession(this)) {
            try {
                TimerServiceLauncher.ensureRunning(this);
            } catch (Exception e) {
                AppLog.w("App", "Failed to restore timer service on startup", e);
            }
        }

        RecurringTaskScheduler.schedule(this);
        startMainThreadWatchdog();

        userDataInitialized = true;
    }

    public boolean isUserDataInitialized() {
        return userDataInitialized;
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
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private void startMainThreadWatchdog() {
        final android.os.Handler mainHandler = new android.os.Handler(getMainLooper());
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
}
