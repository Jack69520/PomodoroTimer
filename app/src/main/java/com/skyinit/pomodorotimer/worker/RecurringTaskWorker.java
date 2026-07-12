package com.skyinit.pomodorotimer.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.RecurringTaskManager;
import com.skyinit.pomodorotimer.util.AppLog;

/**
 * 后台检查并生成到期的重复待办。
 */
public class RecurringTaskWorker extends Worker {

    private static final String TAG = "RecurringTaskWorker";

    public static final String UNIQUE_WORK_NAME = "recurring_task_processor";

    public RecurringTaskWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context appContext = getApplicationContext();
        try {
            AccountManager.getInstance(appContext).ensureDefaultProfileBlocking();
            new RecurringTaskManager(appContext).processDueTasksSync();
            return Result.success();
        } catch (Exception e) {
            AppLog.e(TAG, "Failed to process due recurring tasks", e);
            return Result.retry();
        }
    }
}
