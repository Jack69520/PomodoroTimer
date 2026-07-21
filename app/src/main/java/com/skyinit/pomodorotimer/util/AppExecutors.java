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

    /** 标记当前是否正在执行 diskIo 任务，避免同线程再 enqueue 造成读写乱序。 */
    private static final ThreadLocal<Boolean> DISK_IO_DEPTH = ThreadLocal.withInitial(() -> Boolean.FALSE);

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

    /** 是否正在 diskIo 工作线程的任务上下文中（含嵌套调用）。 */
    public boolean isDiskIoThread() {
        return Boolean.TRUE.equals(DISK_IO_DEPTH.get());
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
        boolean enteredDiskIo = "diskIo".equals(executorName);
        if (enteredDiskIo) {
            DISK_IO_DEPTH.set(Boolean.TRUE);
        }
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
        } finally {
            if (enteredDiskIo) {
                DISK_IO_DEPTH.set(Boolean.FALSE);
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
