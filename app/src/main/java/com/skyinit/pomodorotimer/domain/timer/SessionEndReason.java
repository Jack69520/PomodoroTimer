package com.skyinit.pomodorotimer.domain.timer;

/**
 * 学习会话结束原因，供 {@link SessionSettlePolicy} 决策。
 */
public enum SessionEndReason {
    /** 计时自然到点完成。 */
    NATURAL_COMPLETE,
    /** 用户主动结束本轮。 */
    USER_ABORT,
    /** 暂停超时失败。 */
    PAUSE_TIMEOUT,
    /** 切号 / 登出重置。 */
    ACCOUNT_SWITCH,
    /** 强制失败离开（不落库）。 */
    FORCE_FAIL
}
