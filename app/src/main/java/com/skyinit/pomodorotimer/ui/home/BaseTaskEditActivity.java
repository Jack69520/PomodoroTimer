package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.domain.todo.DueDateTime;
import com.skyinit.pomodorotimer.ui.SubpageActivity;
import com.skyinit.pomodorotimer.ui.home.todoedit.TaskEditEffect;
import com.skyinit.pomodorotimer.ui.home.todoedit.TaskEditIntent;
import com.skyinit.pomodorotimer.ui.home.todoedit.TaskEditUiState;
import com.skyinit.pomodorotimer.ui.home.todoedit.TaskEditViewModel;
import com.skyinit.pomodorotimer.util.CategoryDefaults;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 任务编辑页基类：公共字段 + due/priority Chip；观察 UiState/Effect。
 */
public abstract class BaseTaskEditActivity extends SubpageActivity {

    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String RESULT_TASK_SAVED = "result_task_saved";

    protected abstract int getLayoutRes();

    protected abstract int getFixedTaskType();

    protected abstract int getCreateTitleRes();

    protected abstract int getEditTitleRes();

    protected abstract void bindSpecificViews();

    protected abstract void applySpecificTaskToUi(TodoItem task);

    protected abstract void collectSpecificFields(TodoItem task);

    protected void onUiStateExtra(@NonNull TaskEditUiState state) {
    }

    protected TaskEditViewModel viewModel;
    protected TodoItem currentTask;
    @Nullable
    protected MenuItem saveMenuItem;

    protected EditText titleInput;
    protected EditText descriptionInput;
    protected EditText tagsInput;
    protected Spinner categorySpinner;

    @Nullable
    protected ChipGroup priorityChipGroup;
    @Nullable
    protected ChipGroup dueChipGroup;
    @Nullable
    protected Chip chipDueNone;
    @Nullable
    protected Chip chipDueToday;
    @Nullable
    protected Chip chipDueTomorrow;
    @Nullable
    protected Chip chipDuePick;

    protected Calendar selectedDueDate;
    protected SimpleDateFormat dateFormat;
    protected final List<String> categories = new ArrayList<>();
    private boolean formBoundOnce;
    /** 程序回填 Chip 时忽略监听，避免递归弹窗 */
    private boolean syncingDueChips;
    private boolean syncingPriorityChips;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutRes());

        int taskId = getIntent().getIntExtra(EXTRA_TASK_ID, -1);
        int titleRes = taskId > 0 ? getEditTitleRes() : getCreateTitleRes();
        setupSubpageEdgeToEdge(titleRes);
        initCategories();
        bindCommonViews();
        setupCategorySpinner();
        setupPriorityChips();
        setupDueChips();
        setupViewModel(taskId);
        bindSpecificViews();
        observeViewModel();
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
        priorityChipGroup = findViewById(R.id.priority_chip_group);
        dueChipGroup = findViewById(R.id.due_chip_group);
        chipDueNone = findViewById(R.id.chip_due_none);
        chipDueToday = findViewById(R.id.chip_due_today);
        chipDueTomorrow = findViewById(R.id.chip_due_tomorrow);
        chipDuePick = findViewById(R.id.chip_due_pick);
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
    }

    private void setupPriorityChips() {
        if (priorityChipGroup == null) {
            return;
        }
        priorityChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (syncingPriorityChips || checkedIds.isEmpty()) {
                return;
            }
        });
    }

    private void setupDueChips() {
        if (dueChipGroup == null) {
            return;
        }
        dueChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (syncingDueChips || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chip_due_none) {
                selectedDueDate = null;
                resetPickChipLabel();
            } else if (id == R.id.chip_due_today) {
                selectedDueDate = calendarAtOffset(0);
                resetPickChipLabel();
            } else if (id == R.id.chip_due_tomorrow) {
                selectedDueDate = calendarAtOffset(1);
                resetPickChipLabel();
            } else if (id == R.id.chip_due_pick) {
                // 选中「选日期」时弹出；若用户取消则回落到当前状态
                showDatePicker(/* fromChip */ true);
            }
        });
    }

    private void setupViewModel(int taskId) {
        AppContainer container = ((App) getApplication()).getContainer();
        int taskType = getFixedTaskType();
        viewModel = new ViewModelProvider(this, container.getViewModelFactory()
                .createTaskEditFactory(taskId, taskType))
                .get(TaskEditViewModel.class);
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(this, state -> {
            if (state == null) {
                return;
            }
            setFormEnabled(!state.loading && !state.saving);
            if (saveMenuItem != null) {
                saveMenuItem.setEnabled(!state.loading && !state.saving);
            }
            if (state.task != null && !formBoundOnce && !state.loading) {
                currentTask = state.task;
                applyTaskToUi(state.task);
                formBoundOnce = true;
            } else if (state.task != null) {
                currentTask = state.task;
            }
            onUiStateExtra(state);
        });

        viewModel.getEffects().observe(this, effect -> {
            if (effect == null) {
                return;
            }
            switch (effect.type) {
                case TOAST_RES:
                    if (effect.resId != 0) {
                        Toast.makeText(this, effect.resId, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case TOAST_TEXT:
                case SAVE_FAILED:
                    if (effect.text != null) {
                        Toast.makeText(this, effect.text, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case SAVE_SUCCESS:
                    Toast.makeText(this, R.string.task_saved, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK, new Intent().putExtra(RESULT_TASK_SAVED, true));
                    finish();
                    break;
                default:
                    break;
            }
        });
    }

    private void setFormEnabled(boolean enabled) {
        if (titleInput != null) {
            titleInput.setEnabled(enabled);
        }
        if (descriptionInput != null) {
            descriptionInput.setEnabled(enabled);
        }
        if (tagsInput != null) {
            tagsInput.setEnabled(enabled);
        }
        if (categorySpinner != null) {
            categorySpinner.setEnabled(enabled);
        }
        setChipGroupEnabled(priorityChipGroup, enabled);
        setChipGroupEnabled(dueChipGroup, enabled);
    }

    private static void setChipGroupEnabled(@Nullable ChipGroup group, boolean enabled) {
        if (group == null) {
            return;
        }
        group.setEnabled(enabled);
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(enabled);
        }
    }

    private void applyTaskToUi(TodoItem task) {
        if (task.title != null) {
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
        setPrioritySelection(Math.max(0, Math.min(task.priority, 3)));

        if (task.dueDate > 0) {
            selectedDueDate = Calendar.getInstance();
            selectedDueDate.setTimeInMillis(DueDateTime.startOfDay(task.dueDate));
        } else {
            selectedDueDate = null;
        }
        syncDueChipsFromSelected();

        applySpecificTaskToUi(task);
    }

    protected void saveTask() {
        TaskEditUiState state = viewModel.getUiState().getValue();
        if (state != null && (state.saving || state.loading)) {
            return;
        }
        if (currentTask == null) {
            currentTask = new TodoItem("");
            currentTask.taskType = getFixedTaskType();
        }
        currentTask.title = titleInput.getText().toString().trim();
        currentTask.description = descriptionInput.getText().toString().trim();
        currentTask.tags = tagsInput.getText().toString().trim();
        currentTask.category = (String) categorySpinner.getSelectedItem();
        currentTask.priority = getSelectedPriority();

        if (selectedDueDate != null) {
            currentTask.dueDate = DueDateTime.startOfDay(selectedDueDate.getTimeInMillis());
        } else {
            currentTask.dueDate = 0;
        }

        collectSpecificFields(currentTask);
        viewModel.dispatch(TaskEditIntent.save(currentTask));
    }

    private int getSelectedPriority() {
        if (priorityChipGroup == null) {
            return 0;
        }
        int checked = priorityChipGroup.getCheckedChipId();
        if (checked == R.id.chip_priority_medium) {
            return 1;
        }
        if (checked == R.id.chip_priority_high) {
            return 2;
        }
        if (checked == R.id.chip_priority_urgent) {
            return 3;
        }
        return 0;
    }

    private void setPrioritySelection(int priority) {
        if (priorityChipGroup == null) {
            return;
        }
        syncingPriorityChips = true;
        int chipId = R.id.chip_priority_low;
        switch (priority) {
            case 1:
                chipId = R.id.chip_priority_medium;
                break;
            case 2:
                chipId = R.id.chip_priority_high;
                break;
            case 3:
                chipId = R.id.chip_priority_urgent;
                break;
            default:
                break;
        }
        priorityChipGroup.check(chipId);
        syncingPriorityChips = false;
    }

    private void syncDueChipsFromSelected() {
        if (dueChipGroup == null) {
            return;
        }
        syncingDueChips = true;
        if (selectedDueDate == null) {
            dueChipGroup.check(R.id.chip_due_none);
            resetPickChipLabel();
        } else {
            long due = DueDateTime.startOfDay(selectedDueDate.getTimeInMillis());
            long today = DueDateTime.startOfToday();
            long tomorrow = today + TimeUnit.DAYS.toMillis(1);
            if (due == today) {
                dueChipGroup.check(R.id.chip_due_today);
                resetPickChipLabel();
            } else if (due == tomorrow) {
                dueChipGroup.check(R.id.chip_due_tomorrow);
                resetPickChipLabel();
            } else {
                dueChipGroup.check(R.id.chip_due_pick);
                if (chipDuePick != null) {
                    chipDuePick.setText(dateFormat.format(selectedDueDate.getTime()));
                }
            }
        }
        syncingDueChips = false;
    }

    private void resetPickChipLabel() {
        if (chipDuePick != null) {
            chipDuePick.setText(R.string.task_due_pick);
        }
    }

    private Calendar calendarAtOffset(int dayOffset) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(DueDateTime.startOfDayOffset(dayOffset));
        return calendar;
    }

    /**
     * @param fromChip 由「选日期」Chip 触发；若用户取消对话框则恢复到变更前选中态
     */
    private void showDatePicker(boolean fromChip) {
        final Calendar previous = selectedDueDate != null
                ? (Calendar) selectedDueDate.clone()
                : null;
        Calendar calendar = selectedDueDate != null ? selectedDueDate : Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDueDate = Calendar.getInstance();
                    selectedDueDate.set(Calendar.YEAR, selectedYear);
                    selectedDueDate.set(Calendar.MONTH, selectedMonth);
                    selectedDueDate.set(Calendar.DAY_OF_MONTH, selectedDay);
                    selectedDueDate.set(Calendar.HOUR_OF_DAY, 0);
                    selectedDueDate.set(Calendar.MINUTE, 0);
                    selectedDueDate.set(Calendar.SECOND, 0);
                    selectedDueDate.set(Calendar.MILLISECOND, 0);
                    syncDueChipsFromSelected();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(DueDateTime.startOfToday());
        if (fromChip) {
            datePickerDialog.setOnCancelListener(dialog -> {
                selectedDueDate = previous;
                syncDueChipsFromSelected();
            });
        }
        datePickerDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.task_edit_menu, menu);
        saveMenuItem = menu.findItem(R.id.action_save);
        TaskEditUiState state = viewModel != null ? viewModel.getUiState().getValue() : null;
        if (saveMenuItem != null && state != null) {
            saveMenuItem.setEnabled(!state.loading && !state.saving);
        }
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
