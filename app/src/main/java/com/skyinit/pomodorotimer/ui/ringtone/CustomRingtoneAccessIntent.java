package com.skyinit.pomodorotimer.ui.ringtone;

/**
 * 冷启动自定义铃声降级访问意图。
 */
public enum CustomRingtoneAccessIntent {
    EVALUATE_COLD_START,
    PRIMARY_GRANT,
    OPEN_SETTINGS,
    DISMISS,
    RUNTIME_RESULT_GRANTED,
    RUNTIME_RESULT_DENIED,
    RUNTIME_RESULT_PERMANENT_DENY,
    HOST_RESUMED,
    SETTINGS_NAVIGATE_FAILED
}
