package com.skyinit.pomodorotimer.data.model;

/**
 * 待办集在列表中的汇总信息（子任务完成数与番茄进度）。
 */
public class TodoCollectionSummary {
    public final int parentTaskId;
    public final int completedSubtasks;
    public final int totalSubtasks;
    public final int completedPomodoros;
    public final int estimatedPomodoros;

    public TodoCollectionSummary(int parentTaskId,
                                 int completedSubtasks,
                                 int totalSubtasks,
                                 int completedPomodoros,
                                 int estimatedPomodoros) {
        this.parentTaskId = parentTaskId;
        this.completedSubtasks = completedSubtasks;
        this.totalSubtasks = totalSubtasks;
        this.completedPomodoros = completedPomodoros;
        this.estimatedPomodoros = estimatedPomodoros;
    }

    public static TodoCollectionSummary empty(int parentTaskId) {
        return new TodoCollectionSummary(parentTaskId, 0, 0, 0, 0);
    }
}
