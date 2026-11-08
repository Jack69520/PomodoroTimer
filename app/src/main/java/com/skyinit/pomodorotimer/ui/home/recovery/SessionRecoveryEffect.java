package com.skyinit.pomodorotimer.ui.home.recovery;

/**
 * 一次性 UI 副作用。Controller 消费后应 {@link SessionRecoveryViewModel#consumeEffect()}。
 */
public enum SessionRecoveryEffect {
    /** 打开计时页（不自动 start）。 */
    FORCE_OPEN_TIMER,
    /** 打开计时页并立即开始新一轮。 */
    START_NEW_ROUND_TIMER,
    /** 回到主页以便修改下一轮默认时长。 */
    OPEN_HOME_FOR_DURATION_EDIT,
    SHOW_RESULT_DIALOG,
    SHOW_DISCARD_TOAST,
    SHOW_RESTORE_FAILED_TOAST,
    /** 无可用休息会话时，避免打开空闲假计时页。 */
    SHOW_BREAK_ENDED_TOAST,
    SHOW_EXACT_ALARM_HINT
}
