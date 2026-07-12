package com.skyinit.pomodorotimer.util;

import com.skyinit.pomodorotimer.data.entity.TodoItem;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TodoSortUtils {
    
    /**
     * 按优先级和置顶状态排序待办列表
     * 排序规则：
     * 1. 手动置顶的待办在最前面（按置顶时间倒序）
     * 2. 未置顶的待办按优先级排序：紧急(3) > 高(2) > 中(1) > 低(0)
     * 3. 同优先级按创建时间倒序（最新的在前）
     */
    public static void sortTodosByPriority(List<TodoItem> todos) {
        if (todos == null || todos.isEmpty()) {
            return;
        }
        
        Collections.sort(todos, new Comparator<TodoItem>() {
            @Override
            public int compare(TodoItem item1, TodoItem item2) {
                // 0. 已完成的待办排在未完成的后面
                if (item1.completed && !item2.completed) {
                    return 1;
                }
                if (!item1.completed && item2.completed) {
                    return -1;
                }
                
                // 对同为已完成的项，按完成时间从新到旧排列（最近完成的在前）
                if (item1.completed && item2.completed) {
                    // 若任一完成时间缺失，则回退到创建时间
                    long t1 = item1.completedTime > 0 ? item1.completedTime : item1.createdTime;
                    long t2 = item2.completedTime > 0 ? item2.completedTime : item2.createdTime;
                    return Long.compare(t2, t1);
                }
                
                // 1. 置顶状态比较
                if (item1.isPinned && !item2.isPinned) {
                    return -1; // item1 置顶，排在前面
                }
                if (!item1.isPinned && item2.isPinned) {
                    return 1; // item2 置顶，排在前面
                }
                
                // 2. 如果都置顶，按置顶时间倒序
                if (item1.isPinned && item2.isPinned) {
                    return Long.compare(item2.pinnedTime, item1.pinnedTime);
                }
                
                // 3. 如果都不置顶，按优先级排序
                int priorityCompare = Integer.compare(item2.priority, item1.priority);
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                
                // 4. 同优先级按创建时间倒序
                return Long.compare(item2.createdTime, item1.createdTime);
            }
        });
    }
    
    /**
     * 检查是否可以置顶更多待办
     * @param todos 待办列表
     * @param maxPinned 最大置顶数量
     * @return 当前置顶数量
     */
    public static int getPinnedCount(List<TodoItem> todos) {
        if (todos == null) {
            return 0;
        }
        
        int count = 0;
        for (TodoItem item : todos) {
            if (item.isPinned) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 检查是否可以置顶待办
     * @param todos 待办列表
     * @param maxPinned 最大置顶数量
     * @return true 如果可以置顶
     */
    public static boolean canPinMore(List<TodoItem> todos, int maxPinned) {
        return getPinnedCount(todos) < maxPinned;
    }
}
