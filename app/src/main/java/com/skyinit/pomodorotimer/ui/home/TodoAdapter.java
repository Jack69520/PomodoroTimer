package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.model.TodoCollectionSummary;
import com.skyinit.pomodorotimer.util.CategoryDefaults;
import com.skyinit.pomodorotimer.util.TodoSortUtils;
import com.skyinit.pomodorotimer.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.ViewHolder> {
    private List<TodoItem> todoList;
    private Map<Integer, TodoCollectionSummary> collectionSummaries = new HashMap<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(TodoItem item);
        void onItemSwiped(TodoItem item); // 滑动删除回调
        void onCheckChanged(TodoItem item, boolean isChecked);
        void onItemEdit(TodoItem item);// 编辑回调接口
        void onItemPin(TodoItem item);// 置顶回调接口
        void onItemStartTimer(TodoItem item);// 新增从待办开始计时
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView typeBadge;
        public CheckBox checkBox;
        public TextView category;
        public TextView tags;
        public TextView priority;
        public TextView dueDate;
        public TextView subtaskProgress;
        public TextView pomodoroProgress;
        public View itemContainer;
        public ImageView pinButton;
        public ImageView startButton;

        public ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.todo_title);
            typeBadge = view.findViewById(R.id.todo_type_badge);
            checkBox = view.findViewById(R.id.todo_checkbox);
            category = view.findViewById(R.id.todo_category);
            tags = view.findViewById(R.id.todo_tags);
            priority = view.findViewById(R.id.todo_priority);
            dueDate = view.findViewById(R.id.todo_due_date);
            subtaskProgress = view.findViewById(R.id.todo_subtask_progress);
            pomodoroProgress = view.findViewById(R.id.todo_pomodoro_progress);
            itemContainer = view.findViewById(R.id.todo_item_container);
            pinButton = view.findViewById(R.id.todo_pin_button);
            startButton = view.findViewById(R.id.todo_start_button);
        }
    }

    public TodoAdapter(List<TodoItem> todoList, OnItemClickListener listener) {
        this.todoList = todoList != null ? todoList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TodoItem item = todoList.get(position);
        TodoSwipeCallback.resetSwipeViews(holder.itemView);
        holder.title.setText(item.title);

        if (holder.typeBadge != null) {
            if (item.isCollection()) {
                holder.typeBadge.setText(R.string.task_type_collection);
                holder.typeBadge.setBackgroundResource(R.drawable.type_collection_todo_background);
                holder.typeBadge.setVisibility(View.VISIBLE);
            } else {
                holder.typeBadge.setText(R.string.task_type_simple);
                holder.typeBadge.setBackgroundResource(R.drawable.type_simple_todo_background);
                holder.typeBadge.setVisibility(View.VISIBLE);
            }
        }

        // 设置分类
        if (holder.category != null) {
            if (item.category != null && !item.category.isEmpty()) {
                holder.category.setText(item.category);
                holder.category.setVisibility(View.VISIBLE);
                
                // 根据分类设置不同的背景
                int backgroundRes = getCategoryBackground(item.category);
                holder.category.setBackgroundResource(backgroundRes);
                
                // 设置文字颜色为白色，确保在渐变背景上清晰可见
                holder.category.setTextColor(0xFFFFFFFF);
            } else {
                holder.category.setVisibility(View.GONE);
            }
        }

        // 设置标签
        if (holder.tags != null) {
            if (item.tags != null && !item.tags.isEmpty()) {
                holder.tags.setText(item.tags);
                holder.tags.setVisibility(View.VISIBLE);
            } else {
                holder.tags.setVisibility(View.GONE);
            }
        }

        // 设置优先级
        if (holder.priority != null) {
            String[] priorityLabels = holder.itemView.getContext().getResources()
                    .getStringArray(R.array.task_priorities);
            String priorityText = item.priority >= 0 && item.priority < priorityLabels.length
                    ? priorityLabels[item.priority] : "";
            int textColor;
            switch (item.priority) {
                case 0:
                    textColor = 0xFF4CAF50; // 绿色
                    break;
                case 1:
                    textColor = 0xFFFF9800; // 橙色
                    break;
                case 2:
                    textColor = 0xFFF44336; // 红色
                    break;
                case 3:
                    textColor = 0xFF9C27B0; // 紫色
                    break;
                default:
                    textColor = 0xFF757575;
                    break;
            }
            holder.priority.setText(priorityText);
            holder.priority.setTextColor(textColor);
            holder.priority.setVisibility(View.VISIBLE);
        }

        // 设置截止日期
        if (holder.dueDate != null) {
            if (item.dueDate > 0) {
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault());
                String dueDateText = dateFormat.format(new java.util.Date(item.dueDate));

                java.util.Calendar startOfToday = java.util.Calendar.getInstance();
                startOfToday.set(java.util.Calendar.HOUR_OF_DAY, 0);
                startOfToday.set(java.util.Calendar.MINUTE, 0);
                startOfToday.set(java.util.Calendar.SECOND, 0);
                startOfToday.set(java.util.Calendar.MILLISECOND, 0);
                long startOfTodayTime = startOfToday.getTimeInMillis();

                if (item.dueDate < startOfTodayTime) {
                    holder.dueDate.setText(holder.itemView.getContext().getString(
                            R.string.task_label_overdue, dueDateText));
                    holder.dueDate.setTextColor(0xFFF44336);
                } else {
                    holder.dueDate.setText(dueDateText);
                    holder.dueDate.setTextColor(0xFF2196F3);
                }
                holder.dueDate.setVisibility(View.VISIBLE);
            } else {
                holder.dueDate.setVisibility(View.GONE);
            }
        }

        if (holder.subtaskProgress != null) {
            if (item.isCollection()) {
                TodoCollectionSummary summary = collectionSummaries.get(item.id);
                if (summary != null && summary.totalSubtasks > 0) {
                    holder.subtaskProgress.setText(holder.itemView.getContext().getString(
                            R.string.task_subtask_progress,
                            summary.completedSubtasks,
                            summary.totalSubtasks));
                    holder.subtaskProgress.setVisibility(View.VISIBLE);
                } else {
                    holder.subtaskProgress.setVisibility(View.GONE);
                }
            } else {
                holder.subtaskProgress.setVisibility(View.GONE);
            }
        }

        if (holder.pomodoroProgress != null) {
            if (item.isCollection()) {
                TodoCollectionSummary summary = collectionSummaries.get(item.id);
                int completed = summary != null ? summary.completedPomodoros : 0;
                int estimated = summary != null ? Math.max(1, summary.estimatedPomodoros) : 0;
                if (estimated > 0) {
                    holder.pomodoroProgress.setText(
                            holder.itemView.getContext().getString(R.string.task_pomodoro_progress, completed, estimated));
                    holder.pomodoroProgress.setVisibility(View.VISIBLE);
                } else {
                    holder.pomodoroProgress.setVisibility(View.GONE);
                }
            } else {
                int estimated = Math.max(1, item.estimatedPomodoros);
                int completed = Math.max(0, item.completedPomodoros);
                holder.pomodoroProgress.setText(
                        holder.itemView.getContext().getString(R.string.task_pomodoro_progress, completed, estimated));
                holder.pomodoroProgress.setVisibility(View.VISIBLE);
            }
        }

        // 设置置顶按钮
        if (holder.pinButton != null) {
            if (item.isPinned) {
                holder.pinButton.setImageResource(R.drawable.ic_pin_filled);
                holder.pinButton.setVisibility(View.VISIBLE);
            } else {
                holder.pinButton.setImageResource(R.drawable.ic_pin_outline);
                holder.pinButton.setVisibility(View.VISIBLE);
            }
            
            // 置顶按钮点击事件
            holder.pinButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemPin(item);
                }
            });
        }

        // 开始计时按钮
        if (holder.startButton != null) {
            holder.startButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemStartTimer(item);
                }
            });
        }

        // 移除之前的监听器以避免递归调用
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(item.completed);

        // 复选框点击事件（只处理选中状态变化）
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onCheckChanged(item, isChecked);
        });

        // 整个待办条的点击事件（除复选框外的所有区域都能进入编辑状态）
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // 通过回调通知Fragment编辑请求
                listener.onItemEdit(item);
            }
        });

        // 为复选框设置点击事件拦截，防止事件传递到父视图
        holder.checkBox.setOnClickListener(v -> {
            // 复选框点击时不触发编辑，只处理选中状态
            // 事件不会传递到父视图
        });
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    public void setTodos(List<TodoItem> todos) {
        this.todoList = todos != null ? todos : new ArrayList<>();
        TodoSortUtils.sortTodosByPriority(this.todoList);
        notifyDataSetChanged();
    }

    public void setCollectionSummaries(Map<Integer, TodoCollectionSummary> summaries) {
        this.collectionSummaries = summaries != null ? summaries : new HashMap<>();
        notifyDataSetChanged();
    }
    
    public List<TodoItem> getTodoList() {
        return todoList;
    }

    public TodoItem getTodoAt(int position) {
        if (position >= 0 && position < todoList.size()) {
            return todoList.get(position);
        }
        return null;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < todoList.size()) {
            todoList.remove(position);
            notifyItemRemoved(position);
        }
    }

    // 新增恢复项目方法（用于取消删除时）
    public void restoreItem(TodoItem item, int position) {
        todoList.add(position, item);
        notifyItemInserted(position);
    }
    
    // 根据分类获取对应的背景资源
    private int getCategoryBackground(String category) {
        if (CategoryDefaults.getWork().equals(category)) {
            return R.drawable.category_work_background;
        } else if (CategoryDefaults.getStudy().equals(category)) {
            return R.drawable.category_study_background;
        } else if (CategoryDefaults.getLife().equals(category)) {
            return R.drawable.category_life_background;
        } else if (CategoryDefaults.getSports().equals(category)) {
            return R.drawable.category_sports_background;
        } else if (CategoryDefaults.getEntertainment().equals(category)) {
            return R.drawable.category_entertainment_background;
        } else {
            return R.drawable.category_other_background;
        }
    }
}
