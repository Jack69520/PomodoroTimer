package com.skyinit.pomodorotimer.domain.todo;

import com.skyinit.pomodorotimer.data.entity.TodoItem;

/**
 * 完成 / 取消完成状态机（纯函数，就地修改传入实体，由调用方落库）。
 */
public final class TodoCompletionPolicy {

    /** 完成结果：是否仍留在未完成列表（重复滚动） */
    public static final class Result {
        public final boolean rolledToNextOccurrence;
        public final boolean markedCompleted;

        public Result(boolean rolledToNextOccurrence, boolean markedCompleted) {
            this.rolledToNextOccurrence = rolledToNextOccurrence;
            this.markedCompleted = markedCompleted;
        }
    }

    private TodoCompletionPolicy() {
    }

    /**
     * 勾选完成：重复任务推进下一期并保持未完成；一次性任务标记完成。
     */
    public static Result complete(TodoItem todo, long nowMillis, long startOfToday) {
        if (todo == null) {
            return new Result(false, false);
        }
        if (RecurrenceType.isRecurring(todo.recurrenceType) && todo.isSimple()) {
            // 滚动到下一期：清空完成态与本期番茄进度，保留预估与其它字段
            long next = RecurrencePolicy.nextDueAfterCompletion(
                    todo.dueDate, todo.recurrenceType, startOfToday);
            todo.dueDate = next;
            todo.completed = false;
            todo.completedTime = 0L;
            todo.completedPomodoros = 0;
            return new Result(true, false);
        }
        todo.completed = true;
        todo.completedTime = nowMillis;
        return new Result(false, true);
    }

    /** 取消完成：回到未完成态。 */
    public static void markIncomplete(TodoItem todo) {
        if (todo == null) {
            return;
        }
        todo.completed = false;
        todo.completedTime = 0L;
    }
}
