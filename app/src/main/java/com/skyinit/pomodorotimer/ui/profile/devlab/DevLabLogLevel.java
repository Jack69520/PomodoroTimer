package com.skyinit.pomodorotimer.ui.profile.devlab;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.skyinit.pomodorotimer.R;

/**
 * 开发实验室日志级别（与展示文案解耦，避免按本地化字符串匹配）。
 */
public enum DevLabLogLevel {
    ERROR(R.string.dev_lab_log_level_error),
    WARNING(R.string.dev_lab_log_level_warning),
    INFO(R.string.dev_lab_log_level_info),
    DEBUG(R.string.dev_lab_log_level_debug);

    @StringRes
    public final int labelRes;

    DevLabLogLevel(@StringRes int labelRes) {
        this.labelRes = labelRes;
    }

    @NonNull
    public static DevLabLogLevel fromLogcatToken(@NonNull String line) {
        if (line.contains(" E/") || line.contains(" E ")) {
            return ERROR;
        }
        if (line.contains(" W/") || line.contains(" W ")) {
            return WARNING;
        }
        if (line.contains(" D/") || line.contains(" D ")) {
            return DEBUG;
        }
        return INFO;
    }
}
