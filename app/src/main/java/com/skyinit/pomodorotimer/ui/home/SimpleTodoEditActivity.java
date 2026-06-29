package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.TodoItem;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

/**
 * 普通待办的新增/编辑页。
 */
public class SimpleTodoEditActivity extends BaseTaskEditActivity {

    private EditText estimatedPomodorosInput;
    private Switch recurringSwitch;
    private Spinner recurrenceSpinner;

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
        estimatedPomodorosInput = findViewById(R.id.edit_estimated_pomodoros);
        recurringSwitch = findViewById(R.id.recurring_switch);
        recurrenceSpinner = findViewById(R.id.recurrence_spinner);

        String[] recurrenceOptions = {
                getString(R.string.recurrence_daily),
                getString(R.string.recurrence_weekly),
                getString(R.string.recurrence_monthly)
        };
        ArrayAdapter<String> recurrenceAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, recurrenceOptions);
        recurrenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recurrenceSpinner.setAdapter(recurrenceAdapter);

        recurringSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                recurrenceSpinner.setVisibility(isChecked ? View.VISIBLE : View.GONE));
    }

    @Override
    protected void applySpecificTaskToUi(TodoItem task) {
        estimatedPomodorosInput.setText(String.valueOf(Math.max(1, task.estimatedPomodoros)));
    }

    @Override
    protected void collectSpecificFields(TodoItem task) {
        try {
            task.estimatedPomodoros = Integer.parseInt(
                    estimatedPomodorosInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            task.estimatedPomodoros = 1;
        }
        viewModel.setRecurringOptions(
                recurringSwitch.isChecked(),
                recurrenceSpinner.getSelectedItemPosition());
    }
}
