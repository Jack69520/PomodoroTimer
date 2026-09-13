package com.skyinit.pomodorotimer.ui.home.recovery;

/**
 * 一次性 UI 副作用。Controller 消费后应 {@link SessionRecoveryViewModel#consumeEffect()}。
 */
public enum SessionRecoveryEffect {
    FORCE_OPEN_TIMER,
    SHOW_RESULT_DIALOG,
    SHOW_DISCARD_TOAST,
    SHOW_RESTORE_FAILED_TOAST,
    SHOW_EXACT_ALARM_HINT
}
