package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.data.repository.TodoRepository;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.TaskRepository;
import com.skyinit.pomodorotimer.data.repository.TodoCleanupManager;
import com.skyinit.pomodorotimer.data.repository.TodoFilterManager;
import com.skyinit.pomodorotimer.util.TodoSortUtils;
import com.skyinit.pomodorotimer.R;

import android.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skyinit.pomodorotimer.data.model.TodoCollectionSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 主页待办列表：CRUD、筛选、滑动删除。
 */
public class HomeTodoController {

    public interface Host {
        Fragment getFragment();

        LifecycleOwner getLifecycleOwner();

        void onTodosChanged(List<TodoItem> todos);

        void onRequestTaskEdit(TodoItem item);

        void onRequestTaskTimer(TodoItem item);

        void onRequestDeleteConfirm(TodoItem item, int swipedPosition);
    }

    private final Host host;
    private final TodoRepository todoRepository;
    private final TaskRepository taskRepository;
    private final TodoCleanupManager cleanupManager;
    private final TodoFilterManager filterManager;
    private final AccountManager accountManager;

    private TodoAdapter adapter;
    private RecyclerView todoRecyclerView;
    private LinearLayout filterHeader;
    private LinearLayout filterContent;
    private ImageView filterArrow;
    private Spinner priorityFilterSpinner;
    private Spinner dueDateFilterSpinner;
    private Spinner categoryFilterSpinner;
    private Button clearFilterButton;

    private boolean isFilterExpanded;
    private TodoFilterManager.PriorityFilter currentPriorityFilter = TodoFilterManager.PriorityFilter.ALL;
    private TodoFilterManager.DueDateFilter currentDueDateFilter = TodoFilterManager.DueDateFilter.ALL;
    private String currentCategoryFilter;

    public HomeTodoController(Host host, TodoCleanupManager cleanupManager,
                              TodoFilterManager filterManager,
                              TodoRepository todoRepository,
                              TaskRepository taskRepository) {
        this.host = host;
        this.cleanupManager = cleanupManager;
        this.filterManager = filterManager;
        this.todoRepository = todoRepository;
        this.taskRepository = taskRepository;
        this.accountManager = AccountManager.getInstance(host.getFragment().requireContext());
        if (todoRepository == null || taskRepository == null) {
            throw new IllegalArgumentException("todoRepository and taskRepository must not be null");
        }
    }

    public TodoAdapter getAdapter() {
        return adapter;
    }

    public void bindPortraitViews(View view) {
        todoRecyclerView = view.findViewById(R.id.todo_list);
        priorityFilterSpinner = view.findViewById(R.id.priority_filter_spinner);
        dueDateFilterSpinner = view.findViewById(R.id.due_date_filter_spinner);
        categoryFilterSpinner = view.findViewById(R.id.category_filter_spinner);
        clearFilterButton = view.findViewById(R.id.btn_clear_filter);
        filterHeader = view.findViewById(R.id.filter_header);
        filterContent = view.findViewById(R.id.filter_content);
        filterArrow = view.findViewById(R.id.filter_arrow);

        if (filterHeader != null) {
            filterHeader.setOnClickListener(v -> toggleFilterExpansion());
        }

        setupRecyclerView();
        setupFilters();
        loadTodos();
    }

    public void setPortraitVisibility(int visibility) {
        if (todoRecyclerView != null) {
            todoRecyclerView.setVisibility(visibility);
        }
        if (filterHeader != null) {
            filterHeader.setVisibility(visibility);
        }
    }

    public void updateTodo(TodoItem item) {
        todoRepository.updateTodo(item);
    }

    public void deleteTodo(TodoItem item) {
        taskRepository.runOnDisk(() -> {
            taskRepository.deleteTaskWithSubtasks(item);
            host.getFragment().requireActivity().runOnUiThread(this::loadTodos);
        });
    }

    private void applyTodosToAdapter(List<TodoItem> list) {
        if (adapter == null) {
            return;
        }
        adapter.setTodos(list);
        host.onTodosChanged(list);
        taskRepository.runOnDisk(() -> {
            Map<Integer, TodoCollectionSummary> summaries =
                    taskRepository.loadCollectionSummariesSync(list);
            host.getFragment().requireActivity().runOnUiThread(() -> {
                if (adapter != null) {
                    adapter.setCollectionSummaries(summaries);
                }
            });
        });
    }

    public void togglePinTodo(TodoItem item) {
        if (item.isPinned) {
            item.isPinned = false;
            item.pinnedTime = 0;
            updateTodo(item);
            Toast.makeText(host.getFragment().requireContext(), R.string.task_toast_unpinned, Toast.LENGTH_SHORT).show();
        } else if (adapter != null && TodoSortUtils.canPinMore(adapter.getTodoList(), 3)) {
            item.isPinned = true;
            item.pinnedTime = System.currentTimeMillis();
            updateTodo(item);
            Toast.makeText(host.getFragment().requireContext(), R.string.task_toast_pinned, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(host.getFragment().requireContext(), R.string.task_toast_pin_limit, Toast.LENGTH_SHORT).show();
        }
        refreshTodoList();
    }

    public void refreshTodoList() {
        if (adapter == null) {
            return;
        }
        List<TodoItem> todos = adapter.getTodoList();
        TodoSortUtils.sortTodosByPriority(todos);
        adapter.notifyDataSetChanged();
        host.onTodosChanged(todos);
    }

    public void restoreSwipedItem(TodoItem item, int position) {
        if (adapter != null && item != null && position >= 0) {
            adapter.restoreItem(item, position);
            if (todoRecyclerView != null) {
                todoRecyclerView.smoothScrollToPosition(Math.max(position, 0));
            }
        }
    }

    public void showDeleteConfirmation(TodoItem item) {
        host.onRequestDeleteConfirm(item, -1);
    }

    public void onCheckChanged(TodoItem item, boolean isChecked) {
        item.completed = isChecked;
        if (isChecked) {
            cleanupManager.markTodoAsCompleted(item);
        } else {
            cleanupManager.markTodoAsIncomplete(item);
        }
        updateTodo(item);
        refreshTodoList();
    }

    private void setupRecyclerView() {
        if (todoRecyclerView == null) {
            return;
        }

        todoRecyclerView.setLayoutManager(new LinearLayoutManager(host.getFragment().getContext()));
        adapter = new TodoAdapter(new ArrayList<>(), new TodoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(TodoItem item) {
            }

            @Override
            public void onItemSwiped(TodoItem item) {
                showDeleteConfirmation(item);
            }

            @Override
            public void onCheckChanged(TodoItem item, boolean isChecked) {
                HomeTodoController.this.onCheckChanged(item, isChecked);
            }

            @Override
            public void onItemEdit(TodoItem item) {
                host.onRequestTaskEdit(item);
            }

            @Override
            public void onItemPin(TodoItem item) {
                togglePinTodo(item);
            }

            @Override
            public void onItemStartTimer(TodoItem item) {
                host.onRequestTaskTimer(item);
            }
        });
        todoRecyclerView.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new TodoSwipeCallback(position -> {
                    TodoItem item = adapter.getTodoAt(position);
                    adapter.removeItem(position);
                    host.onRequestDeleteConfirm(item, position);
                }));
        itemTouchHelper.attachToRecyclerView(todoRecyclerView);
        todoRecyclerView.setClipToPadding(false);
        todoRecyclerView.setClipChildren(false);
    }

    private void loadTodos() {
        if (adapter == null) {
            return;
        }

        LiveData<List<TodoItem>> todosLiveData = todoRepository.observeAllTodos();
        todosLiveData.observe(host.getLifecycleOwner(), todos -> {
                    List<TodoItem> list = todos == null ? new ArrayList<>() : todos;
                    applyTodosToAdapter(list);
                });
    }

    private void setupFilters() {
        if (priorityFilterSpinner == null || dueDateFilterSpinner == null || categoryFilterSpinner == null) {
            return;
        }

        android.content.Context context = host.getFragment().requireContext();
        currentCategoryFilter = context.getString(R.string.common_filter_all);

        String[] priorityOptions = context.getResources().getStringArray(R.array.filter_priority_options);
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                priorityOptions);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priorityFilterSpinner.setAdapter(priorityAdapter);

        String[] dueDateOptions = context.getResources().getStringArray(R.array.filter_due_date_options);
        ArrayAdapter<String> dueDateAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                dueDateOptions);
        dueDateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dueDateFilterSpinner.setAdapter(dueDateAdapter);

        String[] categoryOptions = context.getResources().getStringArray(R.array.filter_category_options);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                categoryOptions);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(categoryAdapter);

        priorityFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPriorityFilter = TodoFilterManager.PriorityFilter.values()[position];
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        dueDateFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentDueDateFilter = TodoFilterManager.DueDateFilter.values()[position];
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategoryFilter = categoryOptions[position];
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (clearFilterButton != null) {
            clearFilterButton.setOnClickListener(v -> clearFilters());
        }
    }

    private void applyFilters() {
        if (adapter == null) {
            return;
        }

        LiveData<List<TodoItem>> todosLiveData = filterManager.getTodosByPriorityAndDueDate(
                false, currentPriorityFilter, currentDueDateFilter);
        if (todosLiveData == null) {
            return;
        }

        todosLiveData.observe(host.getLifecycleOwner(), todos -> {
            List<TodoItem> list = todos == null ? new ArrayList<>() : todos;
            String filterAll = host.getFragment().requireContext().getString(R.string.common_filter_all);
            if (!filterAll.equals(currentCategoryFilter)) {
                List<TodoItem> filtered = new ArrayList<>();
                for (TodoItem todo : list) {
                    if (currentCategoryFilter.equals(todo.category)) {
                        filtered.add(todo);
                    }
                }
                list = filtered;
            }
            applyTodosToAdapter(list);
        });
    }

    private void clearFilters() {
        if (priorityFilterSpinner != null) {
            priorityFilterSpinner.setSelection(0);
        }
        if (dueDateFilterSpinner != null) {
            dueDateFilterSpinner.setSelection(0);
        }
        if (categoryFilterSpinner != null) {
            categoryFilterSpinner.setSelection(0);
        }
        currentPriorityFilter = TodoFilterManager.PriorityFilter.ALL;
        currentDueDateFilter = TodoFilterManager.DueDateFilter.ALL;
        currentCategoryFilter = host.getFragment().requireContext().getString(R.string.common_filter_all);
        loadTodos();
    }

    private void toggleFilterExpansion() {
        if (filterContent == null || filterArrow == null) {
            return;
        }
        isFilterExpanded = !isFilterExpanded;
        filterContent.setVisibility(isFilterExpanded ? View.VISIBLE : View.GONE);
        filterArrow.setRotation(isFilterExpanded ? 270f : 90f);
    }

    public void refreshThemeColors() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
