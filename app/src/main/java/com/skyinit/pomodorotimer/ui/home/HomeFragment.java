package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.MainActivity;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.repository.TaskRepository;
import com.skyinit.pomodorotimer.data.repository.TodoCleanupManager;
import com.skyinit.pomodorotimer.service.TimerService;
import com.skyinit.pomodorotimer.service.TimerServiceLauncher;
import com.skyinit.pomodorotimer.R;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements
        PauseReasonDialog.PauseReasonListener,
        HomeTodoController.Host,
        HomeTimerUiHelper.Host {

    private TaskRepository taskRepository;
    private ActivityResultLauncher<Intent> taskEditLauncher;

    private TextView dateText;
    private TextView timerText;
    private Button controlButton;
    private Button resetButton;

    private HomeViewModel viewModel;
    private HomeTodoController todoController;
    private HomeTimerUiHelper timerUiHelper;
    private TimerService timerService;
    private final TimerService.TimerListener timerListener = new TimerService.TimerListener() {
        @Override
        public void onTimerTick(long millisUntilFinished) {
            // UI 由 TimerStateRepository → ViewModel → LiveData 驱动
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

    private TodoItem swipedItem;
    private int swipedPosition = -1;
    private boolean isDeleteDialogShowing;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App app = (App) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(HomeViewModel.class);
        taskRepository = app.getContainer().getTaskRepository();

        taskEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        todoController.refreshTodoList();
                    }
                });

        TodoCleanupManager cleanupManager = new TodoCleanupManager(requireContext());
        todoController = new HomeTodoController(
                this,
                cleanupManager,
                app.getContainer().getTodoFilterManager(),
                app.getContainer().getTodoRepository(),
                app.getContainer().getTaskRepository()
        );
        timerUiHelper = new HomeTimerUiHelper(this);

        if (app.getContainer().getSettingsManager().isAutoDeleteEnabled()) {
            cleanupManager.performCleanup();
        }
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
            todoController.setPortraitVisibility(View.GONE);
        } else {
            todoController.bindPortraitViews(view);
        }

        setupButtonListeners();
        observeViewModel();
        return view;
    }

    private void bindCommonViews(View view) {
        timerText = view.findViewById(R.id.timer_text);
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

    private void setupButtonListeners() {
        controlButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TimerActivity.class);
            boolean start = timerService == null || (!timerService.isRunning() && !timerService.isPaused());
            intent.putExtra("start", start);
            startActivity(intent);
        });

        resetButton.setOnClickListener(v -> {
            if (timerService != null && (timerService.isRunning() || timerService.isPaused())) {
                Toast.makeText(requireContext(), R.string.home_toast_reset_while_running, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.resetStudyTimeToDefault();
            if (timerService != null) {
                timerService.resetTimer();
            }
            Toast.makeText(requireContext(), R.string.reset_to_default_minutes, Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        viewModel.getTimerDisplayText().observe(getViewLifecycleOwner(), text -> {
            if (timerText != null && text != null) {
                timerText.setText(text);
            }
        });

        viewModel.getControlButtonState().observe(getViewLifecycleOwner(), state -> {
            if (controlButton == null || state == null) {
                return;
            }
            if (state == HomeViewModel.CONTROL_PAUSE) {
                controlButton.setText(R.string.pause_button);
            } else if (state == HomeViewModel.CONTROL_RESUME) {
                controlButton.setText(R.string.resume_button);
            } else {
                controlButton.setText(R.string.start_button);
            }
        });
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
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateThemeColors();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            todoController.setPortraitVisibility(View.GONE);
        } else {
            todoController.setPortraitVisibility(View.VISIBLE);
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
        todoController.refreshThemeColors();
    }

    @Override
    public void onReasonSelected(String reason) {
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
    public LifecycleOwner getLifecycleOwner() {
        return getViewLifecycleOwner();
    }

    @Override
    public void onTodosChanged(List<TodoItem> todos) {
        viewModel.setTodos(todos);
    }

    @Override
    public void onRequestTaskEdit(TodoItem item) {
        if (item.isCollection()) {
            taskEditLauncher.launch(CollectionTodoEditActivity.editIntent(requireContext(), item));
        } else {
            taskEditLauncher.launch(SimpleTodoEditActivity.editIntent(requireContext(), item));
        }
    }

    @Override
    public void onRequestTaskTimer(TodoItem item) {
        if (item.isCollection()) {
            SubTaskTimerPickerDialog.show(
                    requireContext(),
                    item,
                    taskRepository,
                    (collection, subTask) -> timerUiHelper.showSubTaskTimePickerDialog(collection, subTask));
        } else {
            timerUiHelper.showTaskTimePickerDialog(item);
        }
    }

    @Override
    public void onRequestDeleteConfirm(TodoItem item, int swipedPositionArg) {
        isDeleteDialogShowing = true;
        swipedItem = item;
        swipedPosition = swipedPositionArg;
        final TodoItem pendingItem = item;
        final int pendingPosition = swipedPositionArg;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.common_dialog_hint_title)
                .setMessage(R.string.home_dialog_delete_message)
                .setPositiveButton(R.string.common_label_delete, (dialog, which) -> {
                    todoController.deleteTodo(pendingItem);
                    swipedItem = null;
                    swipedPosition = -1;
                    isDeleteDialogShowing = false;
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    if (pendingItem != null && pendingPosition >= 0) {
                        todoController.restoreSwipedItem(pendingItem, pendingPosition);
                    }
                    swipedItem = null;
                    swipedPosition = -1;
                    isDeleteDialogShowing = false;
                })
                .setOnDismissListener(dialog -> {
                    if (swipedItem != null && swipedPosition >= 0) {
                        todoController.restoreSwipedItem(swipedItem, swipedPosition);
                        swipedItem = null;
                        swipedPosition = -1;
                    }
                    isDeleteDialogShowing = false;
                })
                .show();
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
        // ViewModel 已在 helper 内更新
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timerService != null) {
            timerService.removeListener(timerListener);
        }
    }
}
