package com.skyinit.pomodorotimer.util;

import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 应用级共享线程池，避免各处 {@code Executors.newSingleThreadExecutor()} 泄漏线程。
 */
public final class AppExecutors {

    public interface ErrorHandler {
        void onError(Throwable throwable);
    }

    private static final String TAG = "AppExecutors";
    private static volatile AppExecutors instance;

    private final ExecutorService diskIo;

    private AppExecutors() {
        diskIo = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "pomodoro-disk-io");
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((t, e) ->
                    AppLog.e(TAG, "Uncaught exception on " + t.getName(), e));
            return thread;
        });
    }

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    public void diskIo(Runnable runnable) {
        diskIo(runnable, null);
    }

    public void diskIo(Runnable runnable, @Nullable ErrorHandler errorHandler) {
        diskIo.execute(() -> runWithBoundary("diskIo", runnable, errorHandler));
    }

    private void runWithBoundary(String executorName, Runnable runnable,
                                 @Nullable ErrorHandler errorHandler) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            AppLog.e(TAG, "Unhandled exception in " + executorName, throwable);
            if (errorHandler != null) {
                try {
                    errorHandler.onError(throwable);
                } catch (Throwable handlerError) {
                    AppLog.e(TAG, "Error handler failed in " + executorName, handlerError);
                }
            }
        }
    }

    public ExecutorService diskIoExecutor() {
        return diskIo;
    }

    public static void resetForTest() {
        synchronized (AppExecutors.class) {
            if (instance != null) {
                instance.diskIo.shutdownNow();
                instance = null;
            }
        }
    }
}
