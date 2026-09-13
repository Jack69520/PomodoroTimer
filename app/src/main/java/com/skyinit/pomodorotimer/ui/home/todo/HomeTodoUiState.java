package com.skyinit.pomodorotimer.ui.home.todo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.model.TodoCollectionSummary;
import com.skyinit.pomodorotimer.domain.todo.TodoFilterQuery;
import com.skyinit.pomodorotimer.domain.todo.TodoSectionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页待办工作台不可变快照。
 * <p>
 * 行类型：分区标题 / 普通待办 / 待办集父卡 / 展开中的子任务行。
 */
public final class HomeTodoUiState {

    public static abstract class DisplayRow {
        public static final int KIND_HEADER = 0;
        public static final int KIND_SIMPLE = 1;
        public static final int KIND_COLLECTION = 2;
        public static final int KIND_SUBTASK = 3;

        public final int kind;

        protected DisplayRow(int kind) {
            this.kind = kind;
        }
    }

    public static final class HeaderRow extends DisplayRow {
        @NonNull
        public final TodoSectionType sectionType;
        public final int itemCount;
        public final boolean expandable;
        public final boolean expanded;

        public HeaderRow(@NonNull TodoSectionType sectionType,
                         int itemCount,
                         boolean expandable,
                         boolean expanded) {
            super(KIND_HEADER);
            this.sectionType = sectionType;
            this.itemCount = itemCount;
            this.expandable = expandable;
            this.expanded = expanded;
        }
    }

    /** 普通待办行。 */
    public static final class SimpleItemRow extends DisplayRow {
        @NonNull
        public final TodoItem todo;
        @NonNull
        public final TodoSectionType sectionType;

        public SimpleItemRow(@NonNull TodoItem todo, @NonNull TodoSectionType sectionType) {
            super(KIND_SIMPLE);
            this.todo = todo;
            this.sectionType = sectionType;
        }
    }

    /** 待办集父卡。 */
    public static final class CollectionHeaderRow extends DisplayRow {
        @NonNull
        public final TodoItem todo;
        @NonNull
        public final TodoSectionType sectionType;
        @Nullable
        public final TodoCollectionSummary summary;
        public final boolean expanded;
        @Nullable
        public final String nextSubtaskTitle;
        /** 是否显示进度条与下一步（由设置控制，默认关）。 */
        public final boolean showProgressDetails;

        public CollectionHeaderRow(@NonNull TodoItem todo,
                                   @NonNull TodoSectionType sectionType,
                                   @Nullable TodoCollectionSummary summary,
                                   boolean expanded,
                                   @Nullable String nextSubtaskTitle,
                                   boolean showProgressDetails) {
            super(KIND_COLLECTION);
            this.todo = todo;
            this.sectionType = sectionType;
            this.summary = summary;
            this.expanded = expanded;
            this.nextSubtaskTitle = nextSubtaskTitle;
            this.showProgressDetails = showProgressDetails;
        }
    }

    /** 展开中的子任务行。 */
    public static final class SubTaskRow extends DisplayRow {
        @NonNull
        public final SubTask subTask;
        @NonNull
        public final TodoItem parent;

        public SubTaskRow(@NonNull SubTask subTask, @NonNull TodoItem parent) {
            super(KIND_SUBTASK);
            this.subTask = subTask;
            this.parent = parent;
        }
    }

    @NonNull
    public final List<DisplayRow> rows;
    public final boolean filterExpanded;
    @NonNull
    public final TodoFilterQuery.PriorityFilter priorityFilter;
    @NonNull
    public final TodoFilterQuery.DueDateFilter dueDateFilter;
    @Nullable
    public final String categoryFilter;
    public final boolean completedExpanded;
    public final boolean loading;
    public final boolean empty;
    public final long startOfToday;
    public final boolean mutating;
    public final int pendingDeleteTodoId;
    public final int pendingSwipePosition;
    @NonNull
    public final Map<Integer, TodoCollectionSummary> collectionSummaries;
    /** 当前展开的待办集 id；0 表示无。 */
    public final int expandedCollectionId;

    public HomeTodoUiState(@NonNull List<DisplayRow> rows,
                           boolean filterExpanded,
                           @NonNull TodoFilterQuery.PriorityFilter priorityFilter,
                           @NonNull TodoFilterQuery.DueDateFilter dueDateFilter,
                           @Nullable String categoryFilter,
                           boolean completedExpanded,
                           boolean loading,
                           boolean empty,
                           long startOfToday,
                           boolean mutating,
                           int pendingDeleteTodoId,
                           int pendingSwipePosition,
                           @Nullable Map<Integer, TodoCollectionSummary> collectionSummaries,
                           int expandedCollectionId) {
        this.rows = rows != null ? rows : Collections.emptyList();
        this.filterExpanded = filterExpanded;
        this.priorityFilter = priorityFilter;
        this.dueDateFilter = dueDateFilter;
        this.categoryFilter = categoryFilter;
        this.completedExpanded = completedExpanded;
        this.loading = loading;
        this.empty = empty;
        this.startOfToday = startOfToday;
        this.mutating = mutating;
        this.pendingDeleteTodoId = pendingDeleteTodoId;
        this.pendingSwipePosition = pendingSwipePosition;
        this.collectionSummaries = collectionSummaries != null
                ? Collections.unmodifiableMap(new HashMap<>(collectionSummaries))
                : Collections.emptyMap();
        this.expandedCollectionId = expandedCollectionId;
    }

    @NonNull
    public static HomeTodoUiState initial(long startOfToday, @Nullable String categoryAllLabel) {
        return new HomeTodoUiState(
                Collections.emptyList(),
                false,
                TodoFilterQuery.PriorityFilter.ALL,
                TodoFilterQuery.DueDateFilter.ALL,
                categoryAllLabel,
                false,
                true,
                true,
                startOfToday,
                false,
                0,
                -1,
                null,
                0);
    }

    @NonNull
    public HomeTodoUiState copyWith(@Nullable List<DisplayRow> rows,
                                    @Nullable Boolean filterExpanded,
                                    @Nullable TodoFilterQuery.PriorityFilter priorityFilter,
                                    @Nullable TodoFilterQuery.DueDateFilter dueDateFilter,
                                    @Nullable String categoryFilter,
                                    boolean clearCategory,
                                    @Nullable Boolean completedExpanded,
                                    @Nullable Boolean loading,
                                    @Nullable Boolean empty,
                                    @Nullable Long startOfToday,
                                    @Nullable Boolean mutating,
                                    @Nullable Integer pendingDeleteTodoId,
                                    @Nullable Integer pendingSwipePosition,
                                    @Nullable Map<Integer, TodoCollectionSummary> collectionSummaries) {
        return copyWith(rows, filterExpanded, priorityFilter, dueDateFilter, categoryFilter,
                clearCategory, completedExpanded, loading, empty, startOfToday, mutating,
                pendingDeleteTodoId, pendingSwipePosition, collectionSummaries, null);
    }

    @NonNull
    public HomeTodoUiState copyWith(@Nullable List<DisplayRow> rows,
                                    @Nullable Boolean filterExpanded,
                                    @Nullable TodoFilterQuery.PriorityFilter priorityFilter,
                                    @Nullable TodoFilterQuery.DueDateFilter dueDateFilter,
                                    @Nullable String categoryFilter,
                                    boolean clearCategory,
                                    @Nullable Boolean completedExpanded,
                                    @Nullable Boolean loading,
                                    @Nullable Boolean empty,
                                    @Nullable Long startOfToday,
                                    @Nullable Boolean mutating,
                                    @Nullable Integer pendingDeleteTodoId,
                                    @Nullable Integer pendingSwipePosition,
                                    @Nullable Map<Integer, TodoCollectionSummary> collectionSummaries,
                                    @Nullable Integer expandedCollectionId) {
        return new HomeTodoUiState(
                rows != null ? rows : this.rows,
                filterExpanded != null ? filterExpanded : this.filterExpanded,
                priorityFilter != null ? priorityFilter : this.priorityFilter,
                dueDateFilter != null ? dueDateFilter : this.dueDateFilter,
                clearCategory ? categoryFilter : (categoryFilter != null ? categoryFilter : this.categoryFilter),
                completedExpanded != null ? completedExpanded : this.completedExpanded,
                loading != null ? loading : this.loading,
                empty != null ? empty : this.empty,
                startOfToday != null ? startOfToday : this.startOfToday,
                mutating != null ? mutating : this.mutating,
                pendingDeleteTodoId != null ? pendingDeleteTodoId : this.pendingDeleteTodoId,
                pendingSwipePosition != null ? pendingSwipePosition : this.pendingSwipePosition,
                collectionSummaries != null ? collectionSummaries : this.collectionSummaries,
                expandedCollectionId != null ? expandedCollectionId : this.expandedCollectionId);
    }

    @NonNull
    public static List<SubTask> copySubtasks(@Nullable List<SubTask> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }
}
