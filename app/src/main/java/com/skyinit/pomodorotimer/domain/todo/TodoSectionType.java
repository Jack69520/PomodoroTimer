package com.skyinit.pomodorotimer.domain.todo;

/**
 * 主页工作台分组类型（展示顺序由 ordinal 决定，PINNED 单独置顶条）。
 */
public enum TodoSectionType {
    /** 跨组置顶条（最多 3） */
    PINNED,
    /** 过期未完成 */
    OVERDUE,
    /** 今天到期 */
    TODAY,
    /** 今天之后有日期 */
    UPCOMING,
    /** 无截止日期 */
    UNDATED,
    /** 已真正完成（非重复滚动） */
    COMPLETED
}
