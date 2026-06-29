package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;

import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 待办集的新增/编辑页。
 */
public class CollectionTodoEditActivity extends BaseTaskEditActivity {

    private EditText subtaskInput;
    private EditText subtaskPomodoroInput;
    private Button addSubtaskButton;
    private RecyclerView subtaskRecyclerView;
    private SubTaskEditAdapter subTaskEditAdapter;

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
        subtaskPomodoroInput = findViewById(R.id.subtask_pomodoro_input);
        addSubtaskButton = findViewById(R.id.btn_add_subtask);
        subtaskRecyclerView = findViewById(R.id.subtask_list);

        subTaskEditAdapter = new SubTaskEditAdapter(new SubTaskEditAdapter.Listener() {
            @Override
            public void onSubTaskToggle(SubTask subTask, boolean completed) {
                viewModel.toggleSubtask(subTask, completed);
            }

            @Override
            public void onSubTaskDelete(SubTask subTask) {
                viewModel.deleteSubtask(subTask);
            }

            @Override
            public void onSubTaskPomodorosChanged(SubTask subTask, int estimatedPomodoros) {
                viewModel.updateSubtaskPomodoros(subTask, estimatedPomodoros);
            }
        });
        subtaskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        subtaskRecyclerView.setAdapter(subTaskEditAdapter);

        addSubtaskButton.setOnClickListener(v -> addSubtaskFromInput());

        viewModel.getSubtasks().observe(this, subtasks -> subTaskEditAdapter.setSubTasks(subtasks));
    }

    @Override
    protected void applySpecificTaskToUi(TodoItem task) {
        // 子任务列表由 LiveData 驱动
    }

    @Override
    protected void collectSpecificFields(TodoItem task) {
        viewModel.setRecurringOptions(false, 0);
    }

    private void addSubtaskFromInput() {
        String title = subtaskInput.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.task_subtask_hint, Toast.LENGTH_SHORT).show();
            return;
        }
        int est = 1;
        try {
            est = Integer.parseInt(subtaskPomodoroInput.getText().toString().trim());
        } catch (NumberFormatException ignored) {
        }
        viewModel.addSubtask(title, est);
        subtaskInput.setText("");
        subtaskPomodoroInput.setText("1");
    }
}
