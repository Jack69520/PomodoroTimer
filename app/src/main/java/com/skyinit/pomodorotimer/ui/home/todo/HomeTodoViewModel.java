package com.skyinit.pomodorotimer.ui.home.todo;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.model.TodoCollectionSummary;
import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.data.repository.TodoWorkspaceRepository;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.domain.todo.DueDateTime;
import com.skyinit.pomodorotimer.domain.todo.TodoFilterQuery;
import com.skyinit.pomodorotimer.domain.todo.TodoGrouping;
import com.skyinit.pomodorotimer.domain.todo.TodoSectionType;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 首页待办 ViewModel：筛选分组 + 待办集展开子任务。
 */
public class HomeTodoViewModel extends AndroidViewModel {

    private final TodoWorkspaceRepository workspace;
    private final UserSessionRepository userSessionRepository;
    private final SettingsManager settingsManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String categoryAllLabel;

    private final MutableLiveData<HomeTodoUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<HomeTodoEffect> effects = new SingleLiveEvent<>();

    private final AtomicInteger reduceGeneration = new AtomicInteger(0);
    private final AtomicInteger mutateGeneration = new AtomicInteger(0);
    private final AtomicInteger expandGeneration = new AtomicInteger(0);

    @Nullable
    private LiveData<List<TodoItem>> todosSource;
    @Nullable
    private Observer<List<TodoItem>> todosObserver;
    @Nullable
    private LiveData<List<SubTask>> expandedSubtasksSource;
    @Nullable
    private Observer<List<SubTask>> expandedSubtasksObserver;
    private final Observer<Integer> sessionVersionObserver = unused -> onStart();

    @NonNull
    private List<TodoItem> rawTodos = new ArrayList<>();
    @NonNull
    private List<SubTask> expandedSubtasks = new ArrayList<>();

    @Nullable
    private TodoItem pendingDeleteTodo;
    @Nullable
    private SubTask pendingDeleteSubtask;

    public HomeTodoViewModel(@NonNull Application application,
                             @NonNull TodoWorkspaceRepository workspace,
                             @NonNull UserSessionRepository userSessionRepository) {
        super(application);
        this.workspace = workspace;
        this.userSessionRepository = userSessionRepository;
        this.settingsManager = new SettingsManager(application);
        this.categoryAllLabel = application.getString(R.string.common_filter_all);
        uiState.setValue(HomeTodoUiState.initial(DueDateTime.startOfToday(), categoryAllLabel));
        userSessionRepository.getSessionVersion().observeForever(sessionVersionObserver);
    }

    @NonNull
    public LiveData<HomeTodoUiState> getUiState() {
        return uiState;
    }

    @NonNull
    public LiveData<HomeTodoEffect> getEffects() {
        return effects;
    }

    @MainThread
    public void dispatch(@NonNull HomeTodoIntent intent) {
        switch (intent.type) {
            case START:
                onStart();
                break;
            case REFRESH_DAY_BOUNDARY:
                onRefreshDayBoundary();
                break;
            case TOGGLE_FILTER_PANEL:
                updateState(current -> current.copyWith(
                        null, !current.filterExpanded, null, null, null, false,
                        null, null, null, null, null, null, null, null));
                break;
            case SET_PRIORITY_FILTER:
                if (intent.priorityFilter != null) {
                    updateState(current -> current.copyWith(
                            null, null, intent.priorityFilter, null, null, false,
                            null, null, null, null, null, null, null, null));
                    reduceRows();
                }
                break;
            case SET_DUE_DATE_FILTER:
                if (intent.dueDateFilter != null) {
                    updateState(current -> current.copyWith(
                            null, null, null, intent.dueDateFilter, null, false,
                            null, null, null, null, null, null, null, null));
                    reduceRows();
                }
                break;
            case SET_CATEGORY_FILTER:
                updateState(current -> current.copyWith(
                        null, null, null, null, intent.categoryFilter, true,
                        null, null, null, null, null, null, null, null));
                reduceRows();
                break;
            case CLEAR_FILTERS:
                updateState(current -> current.copyWith(
                        null, null,
                        TodoFilterQuery.PriorityFilter.ALL,
                        TodoFilterQuery.DueDateFilter.ALL,
                        categoryAllLabel, true,
                        null, null, null, null, null, null, null, null));
                reduceRows();
                break;
            case TOGGLE_COMPLETE:
                toggleComplete(intent.todoId);
                break;
            case TOGGLE_PIN:
                togglePin(intent.todoId);
                break;
            case DELETE:
                requestDeleteById(intent.todoId);
                break;
            case CONFIRM_DELETE:
                confirmDelete();
                break;
            case CANCEL_DELETE:
                cancelDelete();
                break;
            case REQUEST_RESCHEDULE:
                effects.setValue(HomeTodoEffect.showRescheduleSheet(intent.todoId));
                break;
            case RESCHEDULE_TO:
                reschedule(intent.todoId, intent.dueMillis);
                break;
            case RESCHEDULE_TO_TODAY:
                reschedule(intent.todoId, DueDateTime.startOfToday());
                break;
            case TOGGLE_COMPLETED_SECTION:
                updateState(current -> current.copyWith(
                        null, null, null, null, null, false,
                        !current.completedExpanded, null, null, null, null, null, null, null));
                reduceRows();
                break;
            case OPEN_EDIT:
                if (intent.todo != null) {
                    effects.setValue(HomeTodoEffect.navigateEdit(intent.todo.id, intent.todo.taskType));
                }
                break;
            case START_TIMER:
                if (intent.todo != null) {
                    if (intent.todo.isCollection()) {
                        startNextSubtask(intent.todo.id);
                    } else {
                        effects.setValue(HomeTodoEffect.startTimer(intent.todo));
                    }
                }
                break;
            case SWIPE_DELETE:
                if (intent.todo != null) {
                    pendingDeleteTodo = intent.todo;
                    pendingDeleteSubtask = null;
                    updateState(current -> current.copyWith(
                            null, null, null, null, null, false,
                            null, null, null, null, null,
                            intent.todo.id, -1, null));
                    effects.setValue(HomeTodoEffect.confirmDelete(intent.todo));
                }
                break;
            case TOGGLE_COLLECTION_EXPAND:
                toggleCollectionExpand(intent.todoId);
                break;
            case TOGGLE_SUBTASK_COMPLETE:
                toggleSubtaskComplete(intent.subtaskId, intent.parentId, intent.checked);
                break;
            case START_NEXT_SUBTASK:
                startNextSubtask(intent.todoId);
                break;
            case START_SUBTASK_TIMER:
                if (intent.todo != null && intent.subTask != null) {
                    effects.setValue(HomeTodoEffect.startSubtaskTimer(intent.todo, intent.subTask));
                }
                break;
            case REQUEST_DELETE_SUBTASK:
            case SWIPE_DELETE_SUBTASK:
                if (intent.subTask != null) {
                    pendingDeleteSubtask = intent.subTask;
                    pendingDeleteTodo = null;
                    effects.setValue(HomeTodoEffect.confirmDeleteSubtask(intent.subTask));
                }
                break;
            case CONFIRM_DELETE_SUBTASK:
                confirmDeleteSubtask();
                break;
            case CANCEL_DELETE_SUBTASK:
                pendingDeleteSubtask = null;
                reduceRows();
                break;
            default:
                break;
        }
    }

    private void onStart() {
        workspace.performCleanup();
        if (todosSource != null && todosObserver != null) {
            todosSource.removeObserver(todosObserver);
        }
        todosSource = workspace.observeAllTodos();
        todosObserver = todos -> {
            rawTodos = todos != null ? new ArrayList<>(todos) : new ArrayList<>();
            reduceRows();
            loadSummariesAsync(rawTodos);
        };
        todosSource.observeForever(todosObserver);
        reduceRows();
    }

    private void onRefreshDayBoundary() {
        long today = DueDateTime.startOfToday();
        updateState(current -> current.copyWith(
                null, null, null, null, null, false,
                null, null, null, today, null, null, null, null));
        reduceRows();
    }

    private void toggleCollectionExpand(int collectionId) {
        HomeTodoUiState current = requireState();
        if (current.expandedCollectionId == collectionId) {
            clearExpandedObservation();
            updateState(s -> s.copyWith(
                    null, null, null, null, null, false,
                    null, null, null, null, null, null, null, null, 0));
            expandedSubtasks = new ArrayList<>();
            reduceRows();
            return;
        }
        TodoItem item = findRaw(collectionId);
        if (item == null || !item.isCollection()) {
            return;
        }
        clearExpandedObservation();
        updateState(s -> s.copyWith(
                null, null, null, null, null, false,
                null, null, null, null, null, null, null, null, collectionId));
        expandedSubtasks = new ArrayList<>();
        final int gen = expandGeneration.incrementAndGet();
        expandedSubtasksSource = workspace.observeSubtasks(collectionId);
        expandedSubtasksObserver = list -> {
            if (gen != expandGeneration.get()) {
                return;
            }
            if (requireState().expandedCollectionId != collectionId) {
                return;
            }
            expandedSubtasks = list != null ? new ArrayList<>(list) : new ArrayList<>();
            reduceRows();
        };
        expandedSubtasksSource.observeForever(expandedSubtasksObserver);
        reduceRows();
    }

    private void clearExpandedObservation() {
        expandGeneration.incrementAndGet();
        if (expandedSubtasksSource != null && expandedSubtasksObserver != null) {
            expandedSubtasksSource.removeObserver(expandedSubtasksObserver);
        }
        expandedSubtasksSource = null;
        expandedSubtasksObserver = null;
    }

    private void reduceRows() {
        final int gen = reduceGeneration.incrementAndGet();
        HomeTodoUiState current = requireState();
        long startOfToday = current.startOfToday > 0
                ? current.startOfToday
                : DueDateTime.startOfToday();

        List<TodoItem> filtered = TodoFilterQuery.apply(
                rawTodos,
                current.priorityFilter,
                current.dueDateFilter,
                current.categoryFilter,
                categoryAllLabel,
                startOfToday,
                true);

        // 展开的集合若已不在筛选结果中，清空展开
        int expandedId = current.expandedCollectionId;
        if (expandedId > 0 && !containsTodoId(filtered, expandedId)) {
            clearExpandedObservation();
            expandedSubtasks = new ArrayList<>();
            expandedId = 0;
            current = current.copyWith(
                    null, null, null, null, null, false,
                    null, null, null, null, null, null, null, null, 0);
            uiState.setValue(current);
        }

        List<TodoGrouping.Section> sections = TodoGrouping.group(filtered, startOfToday);
        List<HomeTodoUiState.DisplayRow> rows = new ArrayList<>();
        Map<Integer, TodoCollectionSummary> summaries = current.collectionSummaries;
        boolean showProgressDetails = settingsManager.isCollectionProgressDetailsEnabled();

        for (TodoGrouping.Section section : sections) {
            boolean isCompleted = section.type == TodoSectionType.COMPLETED;
            boolean sectionExpanded = !isCompleted || current.completedExpanded;
            rows.add(new HomeTodoUiState.HeaderRow(
                    section.type,
                    section.items.size(),
                    isCompleted,
                    sectionExpanded));
            if (!sectionExpanded) {
                continue;
            }
            for (TodoItem item : section.items) {
                if (item.isCollection()) {
                    TodoCollectionSummary summary = summaries.get(item.id);
                    boolean collectionExpanded = expandedId == item.id;
                    SubTask nextIncomplete = collectionExpanded
                            ? firstIncomplete(expandedSubtasks) : null;
                    String nextTitle = null;
                    if (showProgressDetails) {
                        nextTitle = collectionExpanded
                                ? (nextIncomplete != null ? nextIncomplete.title : null)
                                : pendingNextTitles.get(item.id);
                    }
                    rows.add(new HomeTodoUiState.CollectionHeaderRow(
                            item, section.type, summary, collectionExpanded, nextTitle,
                            showProgressDetails));
                    if (collectionExpanded) {
                        List<SubTask> subs = sortedSubtasks(expandedSubtasks);
                        for (SubTask sub : subs) {
                            rows.add(new HomeTodoUiState.SubTaskRow(sub, item));
                        }
                    }
                } else {
                    rows.add(new HomeTodoUiState.SimpleItemRow(item, section.type));
                }
            }
        }

        boolean empty = filtered.isEmpty();
        if (gen != reduceGeneration.get()) {
            return;
        }
        final int finalExpandedId = expandedId;
        uiState.setValue(current.copyWith(
                rows, null, null, null, null, false,
                null, false, empty, startOfToday, false, null, null, null, finalExpandedId));
    }

    @Nullable
    private static SubTask firstIncomplete(@Nullable List<SubTask> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        SubTask best = null;
        for (SubTask sub : list) {
            if (sub != null && !sub.completed) {
                if (best == null || sub.order < best.order) {
                    best = sub;
                }
            }
        }
        return best;
    }

    @NonNull
    private static List<SubTask> sortedSubtasks(@NonNull List<SubTask> source) {
        List<SubTask> copy = new ArrayList<>(source);
        Collections.sort(copy, Comparator.comparingInt(a -> a.order));
        return copy;
    }

    private static boolean containsTodoId(@NonNull List<TodoItem> list, int id) {
        for (TodoItem item : list) {
            if (item != null && item.id == id) {
                return true;
            }
        }
        return false;
    }

    private void loadSummariesAsync(@NonNull List<TodoItem> source) {
        final int gen = reduceGeneration.get();
        workspace.runOnDisk(() -> {
            Map<Integer, TodoCollectionSummary> map =
                    workspace.loadCollectionSummariesSync(source);
            Map<Integer, String> nextTitles = new java.util.HashMap<>();
            if (settingsManager.isCollectionProgressDetailsEnabled()) {
                for (TodoItem item : source) {
                    if (item != null && item.isCollection()) {
                        SubTask next = workspace.findNextIncompleteSubtaskSync(item.id);
                        if (next != null && next.title != null) {
                            nextTitles.put(item.id, next.title);
                        }
                    }
                }
            }
            mainHandler.post(() -> {
                if (gen != reduceGeneration.get()) {
                    return;
                }
                pendingNextTitles = nextTitles;
                updateState(current -> current.copyWith(
                        null, null, null, null, null, false,
                        null, null, null, null, null, null, null, map));
                reduceRows();
            });
        });
    }

    /** 折叠态下一步标题缓存（parentId → title）。 */
    @NonNull
    private Map<Integer, String> pendingNextTitles = new java.util.HashMap<>();

    private void toggleComplete(int todoId) {
        TodoItem existing = findRaw(todoId);
        if (existing == null) {
            return;
        }
        // A1：集合不可直接勾选完成
        if (existing.isCollection()) {
            return;
        }
        if (requireState().mutating) {
            return;
        }
        final boolean target = !existing.completed;
        final int gen = mutateGeneration.incrementAndGet();
        updateState(current -> current.copyWith(
                null, null, null, null, null, false,
                null, null, null, null, true, null, null, null));
        workspace.runOnDisk(() -> {
            boolean rolled = workspace.setCompletedSync(todoId, target);
            mainHandler.post(() -> {
                if (gen != mutateGeneration.get()) {
                    return;
                }
                updateState(current -> current.copyWith(
                        null, null, null, null, null, false,
                        null, null, null, null, false, null, null, null));
                if (rolled) {
                    effects.setValue(HomeTodoEffect.toastRes(R.string.todo_toast_recurrence_advanced));
                }
            });
        }, () -> mainHandler.post(() -> updateState(current -> current.copyWith(
                null, null, null, null, null, false,
                null, null, null, null, false, null, null, null))));
    }

    private void toggleSubtaskComplete(int subtaskId, int parentId, boolean checked) {
        if (requireState().mutating) {
            return;
        }
        final int gen = mutateGeneration.incrementAndGet();
        updateState(current -> current.copyWith(
                null, null, null, null, null, false,
                null, null, null, null, true, null, null, null));
        workspace.runOnDisk(() -> {
            SubTask fresh = workspace.getSubTaskByIdSync(subtaskId);
            if (fresh != null) {
                workspace.toggleSubTask(fresh, checked);
            }
            mainHandler.post(() -> {
                if (gen != mutateGeneration.get()) {
                    return;
                }
                updateState(current -> current.copyWith(
                        null, null, null, null, null, false,
                        null, null, null, null, false, null, null, null));
                // 摘要会随 LiveData 刷新；主动刷新 summaries
                loadSummariesAsync(rawTodos);
            });
        }, () -> mainHandler.post(() -> updateState(current -> current.copyWith(
                null, null, null, null, null, false,
                null, null, null, null, false, null, null, null))));
    }

    private void startNextSubtask(int collectionId) {
        TodoItem parent = findRaw(collectionId);
        if (parent == null || !parent.isCollection()) {
            return;
        }
        workspace.runOnDisk(() -> {
            SubTask next = workspace.findNextIncompleteSubtaskSync(collectionId);
            mainHandler.post(() -> {
                if (next == null) {
                    TodoCollectionSummary summary = requireState().collectionSummaries.get(collectionId);
                    if (summary == null || summary.totalSubtasks <= 0) {
                        effects.setValue(HomeTodoEffect.toastRes(R.string.todo_toast_collection_no_subtasks));
                    } else {
                        effects.setValue(HomeTodoEffect.toastRes(R.string.todo_toast_collection_all_done));
                    }
                    return;
                }
                effects.setValue(HomeTodoEffect.startSubtaskTimer(parent, next));
            });
        });
    }

    private void togglePin(int todoId) {
        TodoItem existing = findRaw(todoId);
        if (existing == null) {
            return;
        }
        final boolean wasPinned = existing.isPinned;
        final int gen = mutateGeneration.incrementAndGet();
        workspace.runOnDisk(() -> {
            boolean ok = workspace.togglePinSync(todoId);
            mainHandler.post(() -> {
                if (gen != mutateGeneration.get()) {
                    return;
                }
                if (!ok) {
                    effects.setValue(HomeTodoEffect.toastRes(R.string.task_toast_pin_limit));
                } else if (wasPinned) {
                    effects.setValue(HomeTodoEffect.toastRes(R.string.task_toast_unpinned));
                } else {
                    effects.setValue(HomeTodoEffect.toastRes(R.string.task_toast_pinned));
                }
            });
        });
    }

    private void requestDeleteById(int todoId) {
        TodoItem item = findRaw(todoId);
        if (item == null) {
            return;
        }
        pendingDeleteTodo = item;
        pendingDeleteSubtask = null;
        updateState(current -> current.copyWith(
                null, null, null, null, null, false,
                null, null, null, null, null, todoId, -1, null));
        effects.setValue(HomeTodoEffect.confirmDelete(item));
    }

    private void confirmDelete() {
        TodoItem item = pendingDeleteTodo;
        if (item == null) {
            HomeTodoUiState state = requireState();
            if (state.pendingDeleteTodoId > 0) {
                item = findRaw(state.pendingDeleteTodoId);
            }
        }
        if (item == null) {
            clearPendingDelete();
            return;
        }
        final TodoItem toDelete = item;
        if (requireState().expandedCollectionId == toDelete.id) {
            clearExpandedObservation();
            updateState(s -> s.copyWith(
                    null, null, null, null, null, false,
                    null, null, null, null, null, null, null, null, 0));
            expandedSubtasks = new ArrayList<>();
        }
        clearPendingDelete();
        workspace.runOnDisk(() -> workspace.deleteTaskWithSubtasks(toDelete));
    }

    private void confirmDeleteSubtask() {
        SubTask sub = pendingDeleteSubtask;
        pendingDeleteSubtask = null;
        if (sub == null) {
            reduceRows();
            return;
        }
        final int subId = sub.id;
        workspace.runOnDisk(() -> {
            workspace.deleteSubTaskByIdSync(subId);
            mainHandler.post(() -> loadSummariesAsync(rawTodos));
        });
    }

    private void cancelDelete() {
        clearPendingDelete();
        reduceRows();
    }

    private void clearPendingDelete() {
        pendingDeleteTodo = null;
        updateState(current -> current.copyWith(
                null, null, null, null, null, false,
                null, null, null, null, null, 0, -1, null));
    }

    private void reschedule(int todoId, long dueMillis) {
        final long normalized = DueDateTime.startOfDay(dueMillis);
        workspace.runOnDisk(() -> workspace.rescheduleSync(todoId, normalized), null);
        effects.setValue(HomeTodoEffect.toastRes(R.string.todo_toast_rescheduled));
    }

    @Nullable
    private TodoItem findRaw(int todoId) {
        for (TodoItem item : rawTodos) {
            if (item != null && item.id == todoId) {
                return item;
            }
        }
        return null;
    }

    @NonNull
    private HomeTodoUiState requireState() {
        HomeTodoUiState state = uiState.getValue();
        if (state == null) {
            state = HomeTodoUiState.initial(DueDateTime.startOfToday(), categoryAllLabel);
            uiState.setValue(state);
        }
        return state;
    }

    private interface StateTransform {
        HomeTodoUiState apply(HomeTodoUiState current);
    }

    private void updateState(StateTransform transform) {
        uiState.setValue(transform.apply(requireState()));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userSessionRepository.getSessionVersion().removeObserver(sessionVersionObserver);
        if (todosSource != null && todosObserver != null) {
            todosSource.removeObserver(todosObserver);
        }
        clearExpandedObservation();
        todosSource = null;
        todosObserver = null;
    }
}
