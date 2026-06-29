package com.skyinit.pomodorotimer.worker;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * 注册重复任务调度：应用启动时立即执行一次，并每 6 小时周期性检查。
 */
public final class RecurringTaskScheduler {

    private RecurringTaskScheduler() {
    }

    public static void schedule(Context context) {
        Context appContext = context.getApplicationContext();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                RecurringTaskWorker.class,
                6,
                TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                RecurringTaskWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request);
        WorkManager.getInstance(appContext)
                .enqueue(new androidx.work.OneTimeWorkRequest.Builder(RecurringTaskWorker.class).build());
    }
}
