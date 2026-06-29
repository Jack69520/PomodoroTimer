package com.skyinit.pomodorotimer.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 应用级共享线程池，避免各处 {@code Executors.newSingleThreadExecutor()} 泄漏线程。
 */
public final class AppExecutors {

    private static volatile AppExecutors instance;

    private final ExecutorService diskIo;

    private AppExecutors() {
        diskIo = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "pomodoro-disk-io");
            thread.setDaemon(true);
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
        diskIo.execute(runnable);
    }

    public ExecutorService diskIoExecutor() {
        return diskIo;
    }
}
