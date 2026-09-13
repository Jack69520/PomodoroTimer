package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.domain.todo.RecurrenceType;
import com.skyinit.pomodorotimer.ui.home.todoedit.TaskEditIntent;
import com.skyinit.pomodorotimer.ui.home.todoedit.TaskEditUiState;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 待办集编辑：子任务列表 + 添加区步进器。
 */
public class CollectionTodoEditActivity extends BaseTaskEditActivity {

    private EditText subtaskInput;
    private TextView subtaskPomodoroView;
    private ImageButton subtaskPomodoroMinus;
    private ImageButton subtaskPomodoroPlus;
    private ImageButton addSubtaskButton;
    private RecyclerView subtaskRecyclerView;
    private TextView subtaskEmptyView;
    private SubTaskEditAdapter subTaskEditAdapter;
    private int subtaskEstimatedPomodoros = 1;

    public static Intent createIntent(Context context, int taskId) {
        return buildIntent(context, CollectionTodoEditActivity.class, taskId);
    }

    public static Intent editIntent(Context context, TodoItem task) {
        return createIntent(context, task.id);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_collection_todo_edit;
    }

    @Override
    protected int getFixedTaskType() {
        return TodoItem.TYPE_COLLECTION;
    }

    @Override
    protected int getCreateTitleRes() {
        return R.string.task_create_collection;
    }

    @Override
    protected int getEditTitleRes() {
        return R.string.task_edit_collection;
    }

    @Override
    protected void bindSpecificViews() {
        subtaskInput = findViewById(R.id.subtask_input);
        subtaskPomodoroView = findViewById(R.id.subtask_pomodoro_input);
        subtaskPomodoroMinus = findViewById(R.id.btn_subtask_pomodoro_minus);
        subtaskPomodoroPlus = findViewById(R.id.btn_subtask_pomodoro_plus);
        addSubtaskButton = findViewById(R.id.btn_add_subtask);
        subtaskRecyclerView = findViewById(R.id.subtask_list);
        subtaskEmptyView = findViewById(R.id.subtask_empty_view);

        subTaskEditAdapter = new SubTaskEditAdapter(new SubTaskEditAdapter.Listener() {
            @Override
            public void onSubTaskToggle(SubTask subTask, boolean completed) {
                viewModel.dispatch(TaskEditIntent.toggleSubtask(subTask, completed));
            }

            @Override
            public void onSubTaskDelete(SubTask subTask) {
                viewModel.dispatch(TaskEditIntent.deleteSubtask(subTask));
            }

            @Override
            public void onSubTaskPomodorosChanged(SubTask subTask, int estimatedPomodoros) {
                viewModel.dispatch(TaskEditIntent.updateSubtaskPomodoros(subTask, estimatedPomodoros));
            }
        });
        subtaskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        subtaskRecyclerView.setAdapter(subTaskEditAdapter);

        subtaskPomodoroMinus.setOnClickListener(v ->
                setSubtaskEstimatedPomodoros(subtaskEstimatedPomodoros - 1));
        subtaskPomodoroPlus.setOnClickListener(v ->
                setSubtaskEstimatedPomodoros(subtaskEstimatedPomodoros + 1));
        setSubtaskEstimatedPomodoros(1);

        addSubtaskButton.setOnClickListener(v -> addSubtaskFromInput());
        subtaskInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addSubtaskFromInput();
                return true;
            }
            return false;
        });
    }

    private void setSubtaskEstimatedPomodoros(int value) {
        subtaskEstimatedPomodoros = Math.max(1, Math.min(99, value));
        if (subtaskPomodoroView != null) {
            subtaskPomodoroView.setText(String.valueOf(subtaskEstimatedPomodoros));
        }
        if (subtaskPomodoroMinus != null) {
            subtaskPomodoroMinus.setEnabled(subtaskEstimatedPomodoros > 1);
        }
        if (subtaskPomodoroPlus != null) {
            subtaskPomodoroPlus.setEnabled(subtaskEstimatedPomodoros < 99);
        }
    }

    @Override
    protected void onUiStateExtra(@NonNull TaskEditUiState state) {
        if (subTaskEditAdapter != null) {
            subTaskEditAdapter.setSubTasks(state.subtasks);
        }
        boolean empty = state.subtasks == null || state.subtasks.isEmpty();
        if (subtaskEmptyView != null) {
            subtaskEmptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        if (subtaskRecyclerView != null) {
            subtaskRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
        boolean enabled = !state.loading && !state.saving;
        if (addSubtaskButton != null) {
            addSubtaskButton.setEnabled(enabled);
        }
        if (subtaskInput != null) {
            subtaskInput.setEnabled(enabled);
        }
        if (subtaskPomodoroMinus != null) {
            subtaskPomodoroMinus.setEnabled(enabled && subtaskEstimatedPomodoros > 1);
        }
        if (subtaskPomodoroPlus != null) {
            subtaskPomodoroPlus.setEnabled(enabled && subtaskEstimatedPomodoros < 99);
        }
    }

    @Override
    protected void applySpecificTaskToUi(TodoItem task) {
        // 子任务由 onUiStateExtra 刷新
    }

    @Override
    protected void collectSpecificFields(TodoItem task) {
        task.recurrenceType = RecurrenceType.NONE;
    }

    private void addSubtaskFromInput() {
        String title = subtaskInput.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.task_hint_subtask_short, Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.dispatch(TaskEditIntent.addSubtask(title, subtaskEstimatedPomodoros));
        subtaskInput.setText("");
        setSubtaskEstimatedPomodoros(1);
    }
}
