package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.domain.todo.RecurrenceType;

import android.content.Context;
import android.content.Intent;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.chip.ChipGroup;

/**
 * 普通待办编辑：重复 Chip + 番茄步进器。
 */
public class SimpleTodoEditActivity extends BaseTaskEditActivity {

    private TextView estimatedPomodorosView;
    private ImageButton pomodoroMinus;
    private ImageButton pomodoroPlus;
    @Nullable
    private ChipGroup recurrenceChipGroup;
    private int estimatedPomodoros = 1;

    public static Intent createIntent(Context context, int taskId) {
        return buildIntent(context, SimpleTodoEditActivity.class, taskId);
    }

    public static Intent editIntent(Context context, TodoItem task) {
        return createIntent(context, task.id);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_simple_todo_edit;
    }

    @Override
    protected int getFixedTaskType() {
        return TodoItem.TYPE_SIMPLE;
    }

    @Override
    protected int getCreateTitleRes() {
        return R.string.task_create_simple;
    }

    @Override
    protected int getEditTitleRes() {
        return R.string.task_edit_simple;
    }

    @Override
    protected void bindSpecificViews() {
        estimatedPomodorosView = findViewById(R.id.edit_estimated_pomodoros);
        pomodoroMinus = findViewById(R.id.btn_pomodoro_minus);
        pomodoroPlus = findViewById(R.id.btn_pomodoro_plus);
        recurrenceChipGroup = findViewById(R.id.recurrence_chip_group);

        pomodoroMinus.setOnClickListener(v -> setEstimatedPomodoros(estimatedPomodoros - 1));
        pomodoroPlus.setOnClickListener(v -> setEstimatedPomodoros(estimatedPomodoros + 1));
        setEstimatedPomodoros(1);
    }

    private void setEstimatedPomodoros(int value) {
        estimatedPomodoros = Math.max(1, Math.min(99, value));
        if (estimatedPomodorosView != null) {
            estimatedPomodorosView.setText(String.valueOf(estimatedPomodoros));
        }
        if (pomodoroMinus != null) {
            pomodoroMinus.setEnabled(estimatedPomodoros > 1);
        }
        if (pomodoroPlus != null) {
            pomodoroPlus.setEnabled(estimatedPomodoros < 99);
        }
    }

    @Override
    protected void applySpecificTaskToUi(TodoItem task) {
        setEstimatedPomodoros(Math.max(1, task.estimatedPomodoros));
        int type = RecurrenceType.isValid(task.recurrenceType)
                ? task.recurrenceType
                : RecurrenceType.NONE;
        setRecurrenceSelection(type);
    }

    @Override
    protected void collectSpecificFields(TodoItem task) {
        task.estimatedPomodoros = estimatedPomodoros;
        task.recurrenceType = getSelectedRecurrence();
    }

    private int getSelectedRecurrence() {
        if (recurrenceChipGroup == null) {
            return RecurrenceType.NONE;
        }
        int checked = recurrenceChipGroup.getCheckedChipId();
        if (checked == R.id.chip_recurrence_daily) {
            return RecurrenceType.DAILY;
        }
        if (checked == R.id.chip_recurrence_weekly) {
            return RecurrenceType.WEEKLY;
        }
        if (checked == R.id.chip_recurrence_monthly) {
            return RecurrenceType.MONTHLY;
        }
        return RecurrenceType.NONE;
    }

    private void setRecurrenceSelection(int recurrenceType) {
        if (recurrenceChipGroup == null) {
            return;
        }
        int chipId = R.id.chip_recurrence_none;
        switch (recurrenceType) {
            case RecurrenceType.DAILY:
                chipId = R.id.chip_recurrence_daily;
                break;
            case RecurrenceType.WEEKLY:
                chipId = R.id.chip_recurrence_weekly;
                break;
            case RecurrenceType.MONTHLY:
                chipId = R.id.chip_recurrence_monthly;
                break;
            default:
                break;
        }
        recurrenceChipGroup.check(chipId);
    }
}
