package com.skyinit.pomodorotimer.ui.home.recovery;

/** 会话恢复用户意图。 */
public enum SessionRecoveryIntent {
    EVALUATE_STARTUP,
    RETRY_EVALUATE,
    SERVICE_READY,
    OPEN_TIMER,
    /** 立即开始新一轮学习倒计时。 */
    START_NEW_ROUND,
    /** 关闭弹窗并回首页，便于长按修改默认时长。 */
    EDIT_NEXT_DURATION,
    DISMISS,
    ACK_DISCARD
}
