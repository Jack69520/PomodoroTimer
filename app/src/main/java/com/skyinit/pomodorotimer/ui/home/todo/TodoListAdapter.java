package com.skyinit.pomodorotimer.ui.home.todo;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.model.TodoCollectionSummary;
import com.skyinit.pomodorotimer.domain.todo.DueDateTime;
import com.skyinit.pomodorotimer.domain.todo.RecurrenceType;
import com.skyinit.pomodorotimer.domain.todo.TodoSectionType;
import com.skyinit.pomodorotimer.util.TaskCategoryStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页分组待办列表：Header / Simple / Collection / SubTask。
 */
public class TodoListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onToggleComplete(@NonNull TodoItem todo, boolean checked);

        void onOpenEdit(@NonNull TodoItem todo);

        void onTogglePin(@NonNull TodoItem todo);

        void onStartTimer(@NonNull TodoItem todo);

        void onDueDateClick(@NonNull TodoItem todo);

        void onRescheduleToToday(@NonNull TodoItem todo);

        void onToggleCompletedSection();

        void onToggleCollectionExpand(@NonNull TodoItem collection);

        void onStartNextSubtask(@NonNull TodoItem collection);

        void onToggleSubtaskComplete(@NonNull SubTask subTask, boolean checked);

        void onStartSubtaskTimer(@NonNull TodoItem parent, @NonNull SubTask subTask);
    }

    /** 滑删目标。 */
    public static final class SwipeTarget {
        public enum Kind { TODO, SUBTASK }

        public final Kind kind;
        @Nullable
        public final TodoItem todo;
        @Nullable
        public final SubTask subTask;

        private SwipeTarget(Kind kind, @Nullable TodoItem todo, @Nullable SubTask subTask) {
            this.kind = kind;
            this.todo = todo;
            this.subTask = subTask;
        }

        @NonNull
        public static SwipeTarget todo(@NonNull TodoItem todo) {
            return new SwipeTarget(Kind.TODO, todo, null);
        }

        @NonNull
        public static SwipeTarget subtask(@NonNull SubTask subTask) {
            return new SwipeTarget(Kind.SUBTASK, null, subTask);
        }
    }

    private final List<HomeTodoUiState.DisplayRow> rows = new ArrayList<>();
    private long startOfToday;
    @Nullable
    private final Listener listener;

    public TodoListAdapter(@Nullable Listener listener) {
        this.listener = listener;
        this.startOfToday = DueDateTime.startOfToday();
    }

    public void submit(@NonNull List<HomeTodoUiState.DisplayRow> newRows, long startOfToday) {
        this.startOfToday = startOfToday > 0 ? startOfToday : DueDateTime.startOfToday();
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @Nullable
    public TodoItem getTodoAt(int position) {
        if (position < 0 || position >= rows.size()) {
            return null;
        }
        HomeTodoUiState.DisplayRow row = rows.get(position);
        if (row instanceof HomeTodoUiState.SimpleItemRow) {
            return ((HomeTodoUiState.SimpleItemRow) row).todo;
        }
        if (row instanceof HomeTodoUiState.CollectionHeaderRow) {
            return ((HomeTodoUiState.CollectionHeaderRow) row).todo;
        }
        return null;
    }

    @Nullable
    public SwipeTarget getSwipeTarget(int position) {
        if (position < 0 || position >= rows.size()) {
            return null;
        }
        HomeTodoUiState.DisplayRow row = rows.get(position);
        if (row instanceof HomeTodoUiState.SimpleItemRow) {
            return SwipeTarget.todo(((HomeTodoUiState.SimpleItemRow) row).todo);
        }
        if (row instanceof HomeTodoUiState.CollectionHeaderRow) {
            return SwipeTarget.todo(((HomeTodoUiState.CollectionHeaderRow) row).todo);
        }
        if (row instanceof HomeTodoUiState.SubTaskRow) {
            return SwipeTarget.subtask(((HomeTodoUiState.SubTaskRow) row).subTask);
        }
        return null;
    }

    public boolean isSwipeable(int position) {
        return getSwipeTarget(position) != null;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).kind;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == HomeTodoUiState.DisplayRow.KIND_HEADER) {
            return new HeaderVH(inflater.inflate(R.layout.item_todo_section_header, parent, false));
        }
        if (viewType == HomeTodoUiState.DisplayRow.KIND_COLLECTION) {
            return new CollectionVH(inflater.inflate(R.layout.item_todo_collection, parent, false));
        }
        if (viewType == HomeTodoUiState.DisplayRow.KIND_SUBTASK) {
            return new SubTaskVH(inflater.inflate(R.layout.item_todo_subtask, parent, false));
        }
        return new SimpleVH(inflater.inflate(R.layout.item_todo, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HomeTodoUiState.DisplayRow row = rows.get(position);
        if (holder instanceof HeaderVH && row instanceof HomeTodoUiState.HeaderRow) {
            ((HeaderVH) holder).bind((HomeTodoUiState.HeaderRow) row, listener);
        } else if (holder instanceof CollectionVH && row instanceof HomeTodoUiState.CollectionHeaderRow) {
            ((CollectionVH) holder).bind((HomeTodoUiState.CollectionHeaderRow) row, startOfToday, listener);
        } else if (holder instanceof SubTaskVH && row instanceof HomeTodoUiState.SubTaskRow) {
            ((SubTaskVH) holder).bind((HomeTodoUiState.SubTaskRow) row, listener);
        } else if (holder instanceof SimpleVH && row instanceof HomeTodoUiState.SimpleItemRow) {
            ((SimpleVH) holder).bind((HomeTodoUiState.SimpleItemRow) row, startOfToday, listener);
        }
    }

    static final class HeaderVH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView count;
        final ImageView chevron;

        HeaderVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.section_title);
            count = itemView.findViewById(R.id.section_count);
            chevron = itemView.findViewById(R.id.section_chevron);
        }

        void bind(@NonNull HomeTodoUiState.HeaderRow row, @Nullable Listener listener) {
            title.setText(sectionTitleRes(row.sectionType));
            if (row.expandable && !row.expanded) {
                title.setText(itemView.getContext().getString(
                        R.string.todo_section_completed_collapsed, row.itemCount));
                count.setVisibility(View.GONE);
            } else {
                count.setVisibility(View.VISIBLE);
                count.setText(String.valueOf(row.itemCount));
            }
            int titleColor = row.sectionType == TodoSectionType.OVERDUE
                    ? ContextCompat.getColor(itemView.getContext(), R.color.todo_overdue_text)
                    : ContextCompat.getColor(itemView.getContext(), R.color.text_secondary);
            title.setTextColor(titleColor);

            if (row.expandable) {
                chevron.setVisibility(View.VISIBLE);
                chevron.animate().rotation(row.expanded ? 270f : 90f).setDuration(160).start();
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onToggleCompletedSection();
                    }
                });
            } else {
                chevron.setVisibility(View.GONE);
                itemView.setOnClickListener(null);
                itemView.setClickable(false);
            }
        }

        private static int sectionTitleRes(@NonNull TodoSectionType type) {
            switch (type) {
                case PINNED:
                    return R.string.todo_section_pinned;
                case OVERDUE:
                    return R.string.todo_section_overdue;
                case TODAY:
                    return R.string.todo_section_today;
                case UPCOMING:
                    return R.string.todo_section_upcoming;
                case UNDATED:
                    return R.string.todo_section_undated;
                case COMPLETED:
                default:
                    return R.string.todo_section_completed;
            }
        }
    }

    static final class SimpleVH extends RecyclerView.ViewHolder {
        final CheckBox checkBox;
        final TextView title;
        final TextView typeBadge;
        final ImageView pinButton;
        final ImageView startButton;
        final TextView priority;
        final TextView category;
        final TextView dueDate;
        final TextView rescheduleToday;
        final TextView recurrenceLabel;
        final TextView subtaskProgress;
        final TextView pomodoroProgress;

        SimpleVH(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.todo_checkbox);
            title = itemView.findViewById(R.id.todo_title);
            typeBadge = itemView.findViewById(R.id.todo_type_badge);
            pinButton = itemView.findViewById(R.id.todo_pin_button);
            startButton = itemView.findViewById(R.id.todo_start_button);
            priority = itemView.findViewById(R.id.todo_priority);
            category = itemView.findViewById(R.id.todo_category);
            dueDate = itemView.findViewById(R.id.todo_due_date);
            rescheduleToday = itemView.findViewById(R.id.todo_reschedule_today);
            recurrenceLabel = itemView.findViewById(R.id.todo_recurrence);
            subtaskProgress = itemView.findViewById(R.id.todo_subtask_progress);
            pomodoroProgress = itemView.findViewById(R.id.todo_pomodoro_progress);
        }

        void bind(@NonNull HomeTodoUiState.SimpleItemRow row,
                  long startOfToday,
                  @Nullable Listener listener) {
            TodoItem item = row.todo;
            title.setText(item.title);
            if (item.completed) {
                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                title.setAlpha(0.55f);
            } else {
                title.setPaintFlags(title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                title.setAlpha(1f);
            }

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(item.completed);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggleComplete(item, isChecked);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOpenEdit(item);
                }
            });

            typeBadge.setVisibility(View.GONE);
            pinButton.setImageResource(item.isPinned
                    ? R.drawable.ic_pin_filled
                    : R.drawable.ic_pin_outline);
            pinButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTogglePin(item);
                }
            });
            startButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStartTimer(item);
                }
            });

            MetaBinder.bindPriority(priority, item.priority);
            MetaBinder.bindCategory(category, item.category);
            MetaBinder.bindDue(dueDate, rescheduleToday, recurrenceLabel,
                    item, row.sectionType, startOfToday, listener);
            subtaskProgress.setVisibility(View.GONE);
            if (item.estimatedPomodoros > 0) {
                pomodoroProgress.setVisibility(View.VISIBLE);
                pomodoroProgress.setText(itemView.getContext().getString(
                        R.string.task_pomodoro_progress,
                        item.completedPomodoros,
                        item.estimatedPomodoros));
            } else {
                pomodoroProgress.setVisibility(View.GONE);
            }
        }
    }

    static final class CollectionVH extends RecyclerView.ViewHolder {
        final ImageView expand;
        final View headerTap;
        final TextView title;
        final TextView progressText;
        final View progressFill;
        final TextView nextLine;
        final ImageView pinButton;
        final ImageView startButton;
        final TextView priority;
        final TextView category;
        final TextView dueDate;
        final TextView rescheduleToday;

        CollectionVH(@NonNull View itemView) {
            super(itemView);
            expand = itemView.findViewById(R.id.collection_expand);
            headerTap = itemView.findViewById(R.id.collection_header_tap);
            title = itemView.findViewById(R.id.todo_title);
            progressText = itemView.findViewById(R.id.collection_progress_text);
            progressFill = itemView.findViewById(R.id.collection_progress_fill);
            nextLine = itemView.findViewById(R.id.collection_next);
            pinButton = itemView.findViewById(R.id.todo_pin_button);
            startButton = itemView.findViewById(R.id.todo_start_button);
            priority = itemView.findViewById(R.id.todo_priority);
            category = itemView.findViewById(R.id.todo_category);
            dueDate = itemView.findViewById(R.id.todo_due_date);
            rescheduleToday = itemView.findViewById(R.id.todo_reschedule_today);
        }

        void bind(@NonNull HomeTodoUiState.CollectionHeaderRow row,
                  long startOfToday,
                  @Nullable Listener listener) {
            TodoItem item = row.todo;
            title.setText(item.title);
            expand.animate().rotation(row.expanded ? 90f : 0f).setDuration(160).start();

            View progressTrack = (View) progressFill.getParent();
            int detailsVisibility = row.showProgressDetails ? View.VISIBLE : View.GONE;
            progressText.setVisibility(detailsVisibility);
            progressTrack.setVisibility(detailsVisibility);
            nextLine.setVisibility(detailsVisibility);

            if (row.showProgressDetails) {
                TodoCollectionSummary summary = row.summary;
                int done = summary != null ? summary.completedSubtasks : 0;
                int total = summary != null ? summary.totalSubtasks : 0;
                progressText.setText(itemView.getContext().getString(
                        R.string.task_subtask_progress, done, total));
                progressFill.post(() -> {
                    int trackW = progressTrack.getWidth();
                    float ratio = total > 0 ? (done * 1f / total) : 0f;
                    ViewGroup.LayoutParams lp = progressFill.getLayoutParams();
                    lp.width = Math.max(0, Math.round(trackW * ratio));
                    progressFill.setLayoutParams(lp);
                });

                if (total <= 0) {
                    nextLine.setText(R.string.todo_collection_next_none);
                } else if (row.nextSubtaskTitle != null && !row.nextSubtaskTitle.isEmpty()) {
                    nextLine.setText(itemView.getContext().getString(
                            R.string.todo_collection_next, row.nextSubtaskTitle));
                } else {
                    nextLine.setText(R.string.todo_collection_next_all_done);
                }
            }

            View.OnClickListener expandClick = v -> {
                if (listener != null) {
                    listener.onToggleCollectionExpand(item);
                }
            };
            expand.setOnClickListener(expandClick);
            headerTap.setOnClickListener(expandClick);
            headerTap.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onOpenEdit(item);
                }
                return true;
            });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onOpenEdit(item);
                }
                return true;
            });

            pinButton.setImageResource(item.isPinned
                    ? R.drawable.ic_pin_filled
                    : R.drawable.ic_pin_outline);
            pinButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTogglePin(item);
                }
            });
            startButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStartNextSubtask(item);
                }
            });

            MetaBinder.bindPriority(priority, item.priority);
            MetaBinder.bindCategory(category, item.category);
            MetaBinder.bindDue(dueDate, rescheduleToday, null,
                    item, row.sectionType, startOfToday, listener);
        }
    }

    static final class SubTaskVH extends RecyclerView.ViewHolder {
        final CheckBox checkBox;
        final TextView title;
        final TextView pomodoro;
        final ImageView startButton;

        SubTaskVH(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.subtask_checkbox);
            title = itemView.findViewById(R.id.subtask_title);
            pomodoro = itemView.findViewById(R.id.subtask_pomodoro);
            startButton = itemView.findViewById(R.id.subtask_start_button);
        }

        void bind(@NonNull HomeTodoUiState.SubTaskRow row, @Nullable Listener listener) {
            SubTask sub = row.subTask;
            title.setText(sub.title);
            if (sub.completed) {
                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                title.setAlpha(0.55f);
            } else {
                title.setPaintFlags(title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                title.setAlpha(1f);
            }
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(sub.completed);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggleSubtaskComplete(sub, isChecked);
                }
            });
            pomodoro.setText(itemView.getContext().getString(
                    R.string.task_pomodoro_progress,
                    Math.max(0, sub.completedPomodoros),
                    Math.max(1, sub.estimatedPomodoros)));
            startButton.setEnabled(!sub.completed);
            startButton.setAlpha(sub.completed ? 0.4f : 1f);
            startButton.setOnClickListener(v -> {
                if (listener != null && !sub.completed) {
                    listener.onStartSubtaskTimer(row.parent, sub);
                }
            });
        }
    }

    /** 普通/集合共用的次要元数据绑定。 */
    static final class MetaBinder {
        static void bindPriority(@NonNull TextView priority, int p) {
            String[] labels = priority.getResources().getStringArray(R.array.task_priorities);
            if (p >= 0 && p < labels.length) {
                priority.setText(labels[p]);
                priority.setVisibility(View.VISIBLE);
                int color;
                switch (p) {
                    case 0:
                        color = ContextCompat.getColor(priority.getContext(), R.color.todo_priority_low);
                        break;
                    case 2:
                        color = ContextCompat.getColor(priority.getContext(), R.color.todo_priority_high);
                        break;
                    case 3:
                        color = ContextCompat.getColor(priority.getContext(), R.color.todo_priority_urgent);
                        break;
                    case 1:
                    default:
                        color = ContextCompat.getColor(priority.getContext(), R.color.todo_priority_medium);
                        break;
                }
                priority.setTextColor(color);
            } else {
                priority.setVisibility(View.GONE);
            }
        }

        static void bindCategory(@NonNull TextView category, @Nullable String cat) {
            if (cat == null || cat.isEmpty()) {
                category.setVisibility(View.GONE);
                return;
            }
            category.setVisibility(View.VISIBLE);
            category.setText(cat);
            TaskCategoryStyle.apply(category, cat);
        }

        static void bindDue(@NonNull TextView dueDate,
                            @NonNull TextView rescheduleToday,
                            @Nullable TextView recurrenceLabel,
                            @NonNull TodoItem item,
                            @NonNull TodoSectionType sectionType,
                            long startOfToday,
                            @Nullable Listener listener) {
            boolean overdue = !item.completed
                    && DueDateTime.isOverdue(item.dueDate, startOfToday);
            DueDateTime.RelativeKind kind = DueDateTime.relativeKind(item.dueDate, startOfToday);

            if (kind == DueDateTime.RelativeKind.NONE) {
                dueDate.setVisibility(View.GONE);
            } else {
                dueDate.setVisibility(View.VISIBLE);
                dueDate.setText(formatDueLabel(dueDate, item.dueDate, startOfToday, kind));
                int colorRes = overdue
                        ? R.color.todo_overdue_text
                        : (kind == DueDateTime.RelativeKind.TODAY
                        ? R.color.todo_due_today
                        : R.color.text_secondary);
                dueDate.setTextColor(ContextCompat.getColor(dueDate.getContext(), colorRes));
                dueDate.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDueDateClick(item);
                    }
                });
            }

            if (overdue || sectionType == TodoSectionType.OVERDUE) {
                rescheduleToday.setVisibility(View.VISIBLE);
                rescheduleToday.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRescheduleToToday(item);
                    }
                });
            } else {
                rescheduleToday.setVisibility(View.GONE);
                rescheduleToday.setOnClickListener(null);
            }

            if (recurrenceLabel != null) {
                if (RecurrenceType.isRecurring(item.recurrenceType)) {
                    recurrenceLabel.setVisibility(View.VISIBLE);
                    recurrenceLabel.setText(recurrenceShort(item.recurrenceType));
                } else {
                    recurrenceLabel.setVisibility(View.GONE);
                }
            }
        }

        private static String formatDueLabel(TextView host, long due, long startOfToday,
                                             DueDateTime.RelativeKind kind) {
            switch (kind) {
                case TODAY:
                    return host.getContext().getString(R.string.todo_due_today);
                case TOMORROW:
                    return host.getContext().getString(R.string.todo_due_tomorrow);
                case OVERDUE:
                    int days = DueDateTime.overdueDays(due, startOfToday);
                    return host.getContext().getString(R.string.todo_due_overdue_days, Math.max(1, days));
                case WEEKDAY_OR_DATE:
                default:
                    return DueDateTime.formatShortDate(due, startOfToday);
            }
        }

        private static int recurrenceShort(int type) {
            switch (type) {
                case RecurrenceType.WEEKLY:
                    return R.string.recurrence_weekly_short;
                case RecurrenceType.MONTHLY:
                    return R.string.recurrence_monthly_short;
                case RecurrenceType.DAILY:
                default:
                    return R.string.recurrence_daily_short;
            }
        }
    }
}
