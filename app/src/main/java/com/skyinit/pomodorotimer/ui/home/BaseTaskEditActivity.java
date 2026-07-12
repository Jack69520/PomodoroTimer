package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.util.CategoryDefaults;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 任务编辑页基类：公共字段与 ViewModel 逻辑，子类使用独立布局。
 */
public abstract class BaseTaskEditActivity extends BaseActivity {

    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String RESULT_TASK_SAVED = "result_task_saved";

    protected abstract int getLayoutRes();

    protected abstract int getFixedTaskType();

    protected abstract int getCreateTitleRes();

    protected abstract int getEditTitleRes();

    /** 绑定类型专属控件（番茄数/重复任务 或 子任务列表等） */
    protected abstract void bindSpecificViews();

    /** 将类型专属字段回填到 UI */
    protected abstract void applySpecificTaskToUi(TodoItem task);

    /** 从 UI 收集类型专属字段到 task，并设置 ViewModel 附加选项 */
    protected abstract void collectSpecificFields(TodoItem task);

    protected TaskEditViewModel viewModel;
    protected TodoItem currentTask;

    protected EditText titleInput;
    protected EditText descriptionInput;
    protected EditText tagsInput;
    protected Spinner categorySpinner;
    protected Spinner prioritySpinner;
    protected EditText dueDateInput;
    protected Button clearDueDateButton;

    protected Calendar selectedDueDate;
    protected SimpleDateFormat dateFormat;
    protected final List<String> categories = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutRes());

        int taskId = getIntent().getIntExtra(EXTRA_TASK_ID, -1);
        setupActionBar(taskId);
        initCategories();
        bindCommonViews();
        setupSpinners();
        setupViewModel(taskId);
        bindSpecificViews();
        observeViewModel();
    }

    private void setupActionBar(int taskId) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(taskId > 0 ? getEditTitleRes() : getCreateTitleRes());
        }
    }

    private void initCategories() {
        categories.clear();
        for (String category : CategoryDefaults.getTaskCategories(this)) {
            categories.add(category);
        }
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    private void bindCommonViews() {
        titleInput = findViewById(R.id.edit_title);
        descriptionInput = findViewById(R.id.edit_description);
        tagsInput = findViewById(R.id.edit_tags);
        categorySpinner = findViewById(R.id.edit_category);
        prioritySpinner = findViewById(R.id.edit_priority);
        dueDateInput = findViewById(R.id.edit_due_date);
        clearDueDateButton = findViewById(R.id.btn_clear_due_date);

        dueDateInput.setOnClickListener(v -> showDatePicker());
        clearDueDateButton.setOnClickListener(v -> clearDueDate());
    }

    private void setupSpinners() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        String[] priorities = getResources().getStringArray(R.array.task_priorities);
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
    }

    private void setupViewModel(int taskId) {
        AppContainer container = ((App) getApplication()).getContainer();
        int taskType = getFixedTaskType();

        viewModel = new ViewModelProvider(this, container.getViewModelFactory()
                .createTaskEditFactory(taskId, taskType))
                .get(TaskEditViewModel.class);
    }

    private void observeViewModel() {
        viewModel.getTask().observe(this, task -> {
            if (task == null) {
                return;
            }
            currentTask = task;
            applyTaskToUi(task);
        });

        viewModel.getSaveResult().observe(this, result -> {
            if (result == null) {
                return;
            }
            if (result.success) {
                Toast.makeText(this, R.string.task_saved, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK, new Intent().putExtra(RESULT_TASK_SAVED, true));
                finish();
            } else if (result.message != null) {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyTaskToUi(TodoItem task) {
        if (titleInput.getText().length() == 0 && task.title != null) {
            titleInput.setText(task.title);
        }
        if (task.description != null) {
            descriptionInput.setText(task.description);
        }
        if (task.tags != null) {
            tagsInput.setText(task.tags);
        }
        if (task.category != null) {
            int categoryIndex = categories.indexOf(task.category);
            if (categoryIndex >= 0) {
                categorySpinner.setSelection(categoryIndex);
            }
        }
        prioritySpinner.setSelection(task.priority);

        if (task.dueDate > 0) {
            selectedDueDate = Calendar.getInstance();
            selectedDueDate.setTimeInMillis(task.dueDate);
            dueDateInput.setText(dateFormat.format(selectedDueDate.getTime()));
        }

        applySpecificTaskToUi(task);
    }

    protected void saveTask() {
        if (currentTask == null) {
            currentTask = new TodoItem("");
            currentTask.taskType = getFixedTaskType();
        }
        currentTask.title = titleInput.getText().toString().trim();
        currentTask.description = descriptionInput.getText().toString().trim();
        currentTask.tags = tagsInput.getText().toString().trim();
        currentTask.category = (String) categorySpinner.getSelectedItem();
        currentTask.priority = prioritySpinner.getSelectedItemPosition();

        if (selectedDueDate != null) {
            currentTask.dueDate = selectedDueDate.getTimeInMillis();
        } else {
            currentTask.dueDate = 0;
        }

        collectSpecificFields(currentTask);
        viewModel.save(currentTask);
    }

    private void showDatePicker() {
        Calendar calendar = selectedDueDate != null ? selectedDueDate : Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDueDate = Calendar.getInstance();
                    selectedDueDate.set(selectedYear, selectedMonth, selectedDay);
                    dueDateInput.setText(dateFormat.format(selectedDueDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void clearDueDate() {
        selectedDueDate = null;
        dueDateInput.setText("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.task_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_save) {
            saveTask();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    protected static Intent buildIntent(android.content.Context context,
                                          Class<?> activityClass,
                                          int taskId) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        return intent;
    }
}
