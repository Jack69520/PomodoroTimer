package com.skyinit.pomodorotimer.ui.home;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.MainActivity;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.domain.todo.DueDateTime;
import com.skyinit.pomodorotimer.domain.todo.TodoFilterQuery;
import com.skyinit.pomodorotimer.service.TimerService;
import com.skyinit.pomodorotimer.service.TimerServiceLauncher;
import com.skyinit.pomodorotimer.ui.home.todo.HomeTodoEffect;
import com.skyinit.pomodorotimer.ui.home.todo.HomeTodoIntent;
import com.skyinit.pomodorotimer.ui.home.todo.HomeTodoUiState;
import com.skyinit.pomodorotimer.ui.home.todo.HomeTodoViewModel;
import com.skyinit.pomodorotimer.ui.home.todo.TodoListAdapter;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 首页 Fragment：计时舞台 + 待办工作台（时间分组）。
 * <p>
 * 待办交互全部经 {@link HomeTodoViewModel#dispatch(HomeTodoIntent)}；
 * 计时仍由 {@link HomeViewModel} + {@link HomeTimerUiHelper} 负责。
 */
public class HomeFragment extends Fragment implements
        PauseReasonDialog.PauseReasonListener,
        HomeTimerUiHelper.Host {

    private ActivityResultLauncher<Intent> taskEditLauncher;

    private TextView dateText;
    private TextView timerText;
    private TextView timerPhaseText;
    private Button controlButton;
    private Button resetButton;

    private HomeViewModel viewModel;
    private HomeTodoViewModel todoViewModel;
    private HomeTimerUiHelper timerUiHelper;
    private TimerService timerService;

    private RecyclerView todoRecyclerView;
    private View filterPanelRoot;
    private LinearLayout filterHeader;
    private LinearLayout filterContent;
    private ImageView filterArrow;
    private Spinner priorityFilterSpinner;
    private Spinner dueDateFilterSpinner;
    private Spinner categoryFilterSpinner;
    private View clearFilterButton;
    private TextView todoEmptyView;
    private TodoListAdapter todoListAdapter;
    private boolean filterSpinnersReady;

    private final TimerService.TimerListener timerListener = new TimerService.TimerListener() {
        @Override
        public void onTimerTick(long millisUntilFinished) {
        }

        @Override
        public void onTimerFinish() {
        }

        @Override
        public void onTimerReset() {
        }

        @Override
        public void onTimerStateChanged() {
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App app = (App) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(HomeViewModel.class);
        todoViewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(HomeTodoViewModel.class);
        timerUiHelper = new HomeTimerUiHelper(this);

        taskEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 列表由 Room LiveData 自动刷新，无需手动 reload
                });

        todoViewModel.dispatch(HomeTodoIntent.start());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        int orientation = getResources().getConfiguration().orientation;
        int layoutRes = orientation == Configuration.ORIENTATION_LANDSCAPE
                ? R.layout.fragment_home_landscape
                : R.layout.fragment_home;

        View view = inflater.inflate(layoutRes, container, false);
        bindCommonViews(view);

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            dateText = view.findViewById(R.id.date_text);
            updateDateDisplay();
            setTodoPortraitVisibility(View.GONE);
        } else {
            bindTodoWorkbench(view);
        }

        setupButtonListeners();
        observeTimerViewModel();
        observeTodoViewModel();
        return view;
    }

    private void bindCommonViews(View view) {
        timerText = view.findViewById(R.id.timer_text);
        timerPhaseText = view.findViewById(R.id.timer_phase_text);
        controlButton = view.findViewById(R.id.control_button);
        resetButton = view.findViewById(R.id.reset_button);

        timerText.setOnLongClickListener(v -> {
            if (timerService == null || timerService.isRunning() || timerService.isPaused()) {
                return false;
            }
            timerUiHelper.showDefaultTimePickerDialog();
            return true;
        });
    }

    /** 绑定竖屏待办工作台：筛选 + 分组列表 + 左滑删除。 */
    private void bindTodoWorkbench(View view) {
        todoRecyclerView = view.findViewById(R.id.todo_list);
        filterPanelRoot = view.findViewById(R.id.todo_filter_panel);
        filterHeader = view.findViewById(R.id.filter_header);
        filterContent = view.findViewById(R.id.filter_content);
        filterArrow = view.findViewById(R.id.filter_arrow);
        priorityFilterSpinner = view.findViewById(R.id.priority_filter_spinner);
        dueDateFilterSpinner = view.findViewById(R.id.due_date_filter_spinner);
        categoryFilterSpinner = view.findViewById(R.id.category_filter_spinner);
        clearFilterButton = view.findViewById(R.id.btn_clear_filter);
        todoEmptyView = view.findViewById(R.id.todo_empty_view);

        if (filterHeader != null) {
            filterHeader.setOnClickListener(v ->
                    todoViewModel.dispatch(HomeTodoIntent.toggleFilterPanel()));
        }
        if (clearFilterButton != null) {
            clearFilterButton.setOnClickListener(v ->
                    todoViewModel.dispatch(HomeTodoIntent.clearFilters()));
        }

        setupFilterSpinners();
        setupTodoRecycler();
    }

    private void setupTodoRecycler() {
        if (todoRecyclerView == null) {
            return;
        }
        todoListAdapter = new TodoListAdapter(new TodoListAdapter.Listener() {
            @Override
            public void onToggleComplete(@NonNull TodoItem todo, boolean checked) {
                todoViewModel.dispatch(HomeTodoIntent.toggleComplete(todo.id));
            }

            @Override
            public void onOpenEdit(@NonNull TodoItem todo) {
                todoViewModel.dispatch(HomeTodoIntent.openEdit(todo));
            }

            @Override
            public void onTogglePin(@NonNull TodoItem todo) {
                todoViewModel.dispatch(HomeTodoIntent.togglePin(todo.id));
            }

            @Override
            public void onStartTimer(@NonNull TodoItem todo) {
                todoViewModel.dispatch(HomeTodoIntent.startTimer(todo));
            }

            @Override
            public void onDueDateClick(@NonNull TodoItem todo) {
                todoViewModel.dispatch(HomeTodoIntent.requestReschedule(todo.id));
            }

            @Override
            public void onRescheduleToToday(@NonNull TodoItem todo) {
                todoViewModel.dispatch(HomeTodoIntent.rescheduleToToday(todo.id));
            }

            @Override
            public void onToggleCompletedSection() {
                todoViewModel.dispatch(HomeTodoIntent.toggleCompletedSection());
            }

            @Override
            public void onToggleCollectionExpand(@NonNull TodoItem collection) {
                todoViewModel.dispatch(HomeTodoIntent.toggleCollectionExpand(collection.id));
            }

            @Override
            public void onStartNextSubtask(@NonNull TodoItem collection) {
                todoViewModel.dispatch(HomeTodoIntent.startNextSubtask(collection.id));
            }

            @Override
            public void onToggleSubtaskComplete(@NonNull SubTask subTask, boolean checked) {
                todoViewModel.dispatch(HomeTodoIntent.toggleSubtaskComplete(
                        subTask.id, subTask.parentTaskId, checked));
            }

            @Override
            public void onStartSubtaskTimer(@NonNull TodoItem parent, @NonNull SubTask subTask) {
                todoViewModel.dispatch(HomeTodoIntent.startSubtaskTimer(parent, subTask));
            }
        });
        todoRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        todoRecyclerView.setAdapter(todoListAdapter);
        todoRecyclerView.setClipToPadding(false);
        todoRecyclerView.setClipChildren(false);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new TodoSwipeCallback(position -> {
            if (todoListAdapter == null) {
                return;
            }
            TodoListAdapter.SwipeTarget target = todoListAdapter.getSwipeTarget(position);
            if (target == null) {
                todoListAdapter.notifyItemChanged(position);
                return;
            }
            if (target.kind == TodoListAdapter.SwipeTarget.Kind.SUBTASK && target.subTask != null) {
                todoViewModel.dispatch(HomeTodoIntent.swipeDeleteSubtask(target.subTask));
            } else if (target.todo != null) {
                todoViewModel.dispatch(HomeTodoIntent.swipeDelete(target.todo));
            }
            todoListAdapter.notifyItemChanged(position);
        }));
        touchHelper.attachToRecyclerView(todoRecyclerView);
    }

    private void setupFilterSpinners() {
        if (priorityFilterSpinner == null || dueDateFilterSpinner == null || categoryFilterSpinner == null) {
            return;
        }
        filterSpinnersReady = false;
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.filter_priority_options));
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priorityFilterSpinner.setAdapter(priorityAdapter);

        ArrayAdapter<String> dueAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.filter_due_date_options));
        dueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dueDateFilterSpinner.setAdapter(dueAdapter);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.filter_category_options));
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(categoryAdapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!filterSpinnersReady) {
                    return;
                }
                if (parent == priorityFilterSpinner) {
                    TodoFilterQuery.PriorityFilter[] values = TodoFilterQuery.PriorityFilter.values();
                    if (position >= 0 && position < values.length) {
                        todoViewModel.dispatch(HomeTodoIntent.setPriorityFilter(values[position]));
                    }
                } else if (parent == dueDateFilterSpinner) {
                    TodoFilterQuery.DueDateFilter[] values = TodoFilterQuery.DueDateFilter.values();
                    if (position >= 0 && position < values.length) {
                        todoViewModel.dispatch(HomeTodoIntent.setDueDateFilter(values[position]));
                    }
                } else if (parent == categoryFilterSpinner) {
                    Object item = parent.getItemAtPosition(position);
                    todoViewModel.dispatch(HomeTodoIntent.setCategoryFilter(
                            item != null ? item.toString() : null));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        priorityFilterSpinner.setOnItemSelectedListener(listener);
        dueDateFilterSpinner.setOnItemSelectedListener(listener);
        categoryFilterSpinner.setOnItemSelectedListener(listener);
        filterSpinnersReady = true;
    }

    private void observeTodoViewModel() {
        todoViewModel.getUiState().observe(getViewLifecycleOwner(), this::renderTodoState);
        todoViewModel.getEffects().observe(getViewLifecycleOwner(), this::handleTodoEffect);
    }

    private void renderTodoState(@Nullable HomeTodoUiState state) {
        if (state == null) {
            return;
        }
        if (filterContent != null) {
            filterContent.setVisibility(state.filterExpanded ? View.VISIBLE : View.GONE);
        }
        if (filterArrow != null) {
            filterArrow.setRotation(state.filterExpanded ? 270f : 90f);
        }
        syncSpinnerSelection(priorityFilterSpinner, state.priorityFilter.ordinal());
        syncSpinnerSelection(dueDateFilterSpinner, state.dueDateFilter.ordinal());
        if (categoryFilterSpinner != null && state.categoryFilter != null) {
            android.widget.SpinnerAdapter adapter = categoryFilterSpinner.getAdapter();
            if (adapter instanceof ArrayAdapter) {
                @SuppressWarnings("unchecked")
                ArrayAdapter<String> stringAdapter = (ArrayAdapter<String>) adapter;
                int idx = stringAdapter.getPosition(state.categoryFilter);
                if (idx >= 0) {
                    syncSpinnerSelection(categoryFilterSpinner, idx);
                }
            }
        }
        if (todoListAdapter != null) {
            todoListAdapter.submit(state.rows, state.startOfToday);
        }
        if (todoEmptyView != null) {
            boolean showEmpty = state.empty && !state.loading;
            todoEmptyView.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
            if (todoRecyclerView != null) {
                todoRecyclerView.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
            }
        }
    }

    private void syncSpinnerSelection(@Nullable Spinner spinner, int position) {
        if (spinner == null || position < 0) {
            return;
        }
        if (spinner.getSelectedItemPosition() != position) {
            boolean wasReady = filterSpinnersReady;
            filterSpinnersReady = false;
            spinner.setSelection(position, false);
            filterSpinnersReady = wasReady;
        }
    }

    private void handleTodoEffect(@Nullable HomeTodoEffect effect) {
        if (effect == null) {
            return;
        }
        switch (effect.type) {
            case NAVIGATE_EDIT:
                if (effect.taskType == TodoItem.TYPE_COLLECTION) {
                    taskEditLauncher.launch(
                            CollectionTodoEditActivity.createIntent(requireContext(), effect.taskId));
                } else {
                    taskEditLauncher.launch(
                            SimpleTodoEditActivity.createIntent(requireContext(), effect.taskId));
                }
                break;
            case CONFIRM_DELETE:
                showDeleteConfirm(effect.todo);
                break;
            case CONFIRM_DELETE_SUBTASK:
                showDeleteSubtaskConfirm(effect.subTask);
                break;
            case SHOW_RESCHEDULE_SHEET:
                showRescheduleSheet(effect.todoId);
                break;
            case TOAST_RES:
                if (effect.resId != 0) {
                    Toast.makeText(requireContext(), effect.resId, Toast.LENGTH_SHORT).show();
                }
                break;
            case TOAST_TEXT:
                if (effect.text != null) {
                    Toast.makeText(requireContext(), effect.text, Toast.LENGTH_SHORT).show();
                }
                break;
            case START_TIMER:
                if (effect.todo != null) {
                    timerUiHelper.showTaskTimePickerDialog(effect.todo);
                }
                break;
            case START_SUBTASK_TIMER:
                if (effect.todo != null && effect.subTask != null) {
                    timerUiHelper.showSubTaskTimePickerDialog(effect.todo, effect.subTask);
                }
                break;
            default:
                break;
        }
    }

    private void showDeleteConfirm(@Nullable TodoItem todo) {
        if (todo == null) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.common_dialog_hint_title)
                .setMessage(R.string.home_dialog_delete_message)
                .setPositiveButton(R.string.common_label_delete, (d, w) ->
                        todoViewModel.dispatch(HomeTodoIntent.confirmDelete()))
                .setNegativeButton(R.string.cancel, (d, w) ->
                        todoViewModel.dispatch(HomeTodoIntent.cancelDelete()))
                .setOnCancelListener(d ->
                        todoViewModel.dispatch(HomeTodoIntent.cancelDelete()))
                .show();
    }

    private void showDeleteSubtaskConfirm(@Nullable SubTask subTask) {
        if (subTask == null) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.common_dialog_hint_title)
                .setMessage(R.string.todo_dialog_delete_subtask)
                .setPositiveButton(R.string.common_label_delete, (d, w) ->
                        todoViewModel.dispatch(HomeTodoIntent.confirmDeleteSubtask()))
                .setNegativeButton(R.string.cancel, (d, w) ->
                        todoViewModel.dispatch(HomeTodoIntent.cancelDeleteSubtask()))
                .setOnCancelListener(d ->
                        todoViewModel.dispatch(HomeTodoIntent.cancelDeleteSubtask()))
                .show();
    }

    /** 改期：今天 / 明天 / 选日期。 */
    private void showRescheduleSheet(int todoId) {
        CharSequence[] items = new CharSequence[]{
                getString(R.string.todo_reschedule_today),
                getString(R.string.todo_reschedule_tomorrow),
                getString(R.string.todo_reschedule_pick_date)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.todo_reschedule_title)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        todoViewModel.dispatch(HomeTodoIntent.rescheduleToToday(todoId));
                    } else if (which == 1) {
                        todoViewModel.dispatch(HomeTodoIntent.rescheduleTo(
                                todoId, DueDateTime.startOfDayOffset(1)));
                    } else {
                        showDatePickerForReschedule(todoId);
                    }
                })
                .show();
    }

    private void showDatePickerForReschedule(int todoId) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    long due = DueDateTime.startOfDay(calendar.getTimeInMillis());
                    todoViewModel.dispatch(HomeTodoIntent.rescheduleTo(todoId, due));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        // 允许改到今天及未来；过去日期无意义（过期应改期到今天起）
        dialog.getDatePicker().setMinDate(DueDateTime.startOfToday());
        dialog.show();
    }

    private void setTodoPortraitVisibility(int visibility) {
        if (todoRecyclerView != null) {
            todoRecyclerView.setVisibility(visibility);
        }
        if (filterPanelRoot != null) {
            filterPanelRoot.setVisibility(visibility);
        } else if (filterHeader != null) {
            filterHeader.setVisibility(visibility);
        }
    }

    private void setupButtonListeners() {
        controlButton.setOnClickListener(v -> {
            if (!viewModel.isLoggedIn()) {
                com.skyinit.pomodorotimer.ui.auth.AuthGate.show(requireActivity());
                return;
            }
            Intent intent = new Intent(requireContext(), TimerActivity.class);
            boolean start = timerService == null || (!timerService.isRunning() && !timerService.isPaused());
            intent.putExtra("start", start);
            startActivity(intent);
        });

        resetButton.setOnClickListener(v -> {
            if (timerService != null && (timerService.isRunning() || timerService.isPaused())) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.timer_confirm_reset_title)
                        .setMessage(R.string.timer_confirm_reset_message)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.stop_button, (d, w) -> {
                            timerService.resetTimer();
                            Toast.makeText(requireContext(), R.string.timer_toast_timer_ended, Toast.LENGTH_SHORT).show();
                        })
                        .show();
                return;
            }
            viewModel.resetStudyTimeToDefault();
            if (timerService != null) {
                timerService.resetTimer();
            }
            Toast.makeText(requireContext(), R.string.reset_to_default_minutes, Toast.LENGTH_SHORT).show();
        });
    }

    private void observeTimerViewModel() {
        viewModel.getTimerDisplayText().observe(getViewLifecycleOwner(), text -> {
            if (timerText != null && text != null) {
                timerText.setText(text);
            }
        });

        viewModel.getControlButtonState().observe(getViewLifecycleOwner(), state -> {
            if (controlButton == null || state == null) {
                return;
            }
            applyControlButtonAppearance(state);
        });

        viewModel.getTimerState().observe(getViewLifecycleOwner(), state -> {
            if (timerPhaseText == null || state == null) {
                return;
            }
            if (state.paused) {
                timerPhaseText.setText(R.string.home_phase_paused);
            } else if (state.running && state.isBreakSession()) {
                timerPhaseText.setText(R.string.home_phase_break);
            } else if (state.running) {
                timerPhaseText.setText(R.string.home_phase_study);
            } else {
                timerPhaseText.setText(R.string.home_phase_idle);
            }
        });
    }

    private void applyControlButtonAppearance(int state) {
        if (!(controlButton instanceof MaterialButton)) {
            if (state == HomeViewModel.CONTROL_PAUSE) {
                controlButton.setText(R.string.home_control_open);
            } else if (state == HomeViewModel.CONTROL_RESUME) {
                controlButton.setText(R.string.home_control_resume);
            } else {
                controlButton.setText(R.string.home_control_start);
            }
            return;
        }
        MaterialButton button = (MaterialButton) controlButton;
        if (state == HomeViewModel.CONTROL_PAUSE) {
            button.setText(R.string.home_control_open);
            button.setIconResource(R.drawable.ic_pause);
        } else if (state == HomeViewModel.CONTROL_RESUME) {
            button.setText(R.string.home_control_resume);
            button.setIconResource(R.drawable.ic_resume);
        } else {
            button.setText(R.string.home_control_start);
            button.setIconResource(R.drawable.ic_resume);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            TimerService service = ((MainActivity) getActivity()).getTimerService();
            if (service != null) {
                setTimerService(service);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDateDisplay();
        refreshTimerService();
        viewModel.refreshStudyTimeFromSettings();
        // 跨日回来时重算分组边界
        todoViewModel.dispatch(HomeTodoIntent.refreshDayBoundary());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateThemeColors();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setTodoPortraitVisibility(View.GONE);
        } else {
            setTodoPortraitVisibility(View.VISIBLE);
        }
        if (timerService != null) {
            viewModel.syncFromService(timerService);
        }
    }

    public void setTimerService(TimerService service) {
        if (timerService != null) {
            timerService.removeListener(timerListener);
        }
        this.timerService = service;
        if (timerService == null) {
            return;
        }
        timerService.addListener(timerListener);
        viewModel.syncFromService(timerService);
    }

    private void refreshTimerService() {
        if (getActivity() instanceof MainActivity) {
            TimerService service = ((MainActivity) getActivity()).getTimerService();
            if (service != null) {
                setTimerService(service);
            }
        }
    }

    private void updateDateDisplay() {
        if (dateText != null) {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            dateText.setText(date);
        }
    }

    private void updateThemeColors() {
        if (getView() == null) {
            return;
        }
        if (timerText != null) {
            timerText.setTextColor(getResources().getColor(R.color.text_primary));
        }
        if (dateText != null) {
            dateText.setTextColor(getResources().getColor(R.color.text_primary));
        }
        if (todoListAdapter != null) {
            todoListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onReasonSelected(String reason) {
        if (timerService != null && !timerService.canPause()) {
            Toast.makeText(requireContext(), R.string.timer_toast_max_pause_reached, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(requireContext(), TimerService.class);
        intent.setAction(TimerService.ACTION_PAUSE_WITH_REASON);
        intent.putExtra("pause_reason", reason);
        TimerServiceLauncher.deliverAction(requireContext(), intent);
    }

    @Override
    public void resumeTimer() {
        TimerServiceLauncher.deliverAction(requireContext(), TimerService.ACTION_RESUME);
    }

    @Override
    public Fragment getFragment() {
        return this;
    }

    @Override
    public HomeViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public TimerService getTimerService() {
        return timerService;
    }

    @Override
    public void onDefaultStudyTimeChanged(long millis) {
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timerService != null) {
            timerService.removeListener(timerListener);
        }
    }
}
