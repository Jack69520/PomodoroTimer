package com.skyinit.pomodorotimer.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.skyinit.pomodorotimer.data.repository.RecurringTaskManager;

/**
 * 后台检查并生成到期的重复待办。
 */
public class RecurringTaskWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "recurring_task_processor";

    public RecurringTaskWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            new RecurringTaskManager(getApplicationContext()).processDueTasks();
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
