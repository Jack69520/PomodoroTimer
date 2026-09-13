package com.skyinit.pomodorotimer.ui.profile.devlab;

import androidx.annotation.NonNull;

/**
 * 不可变日志条目。
 */
public final class DevLabLogEntry {

    public final long id;
    @NonNull
    public final String timestamp;
    @NonNull
    public final DevLabLogLevel level;
    @NonNull
    public final String tag;
    @NonNull
    public final String message;

    public DevLabLogEntry(long id,
                          @NonNull String timestamp,
                          @NonNull DevLabLogLevel level,
                          @NonNull String tag,
                          @NonNull String message) {
        this.id = id;
        this.timestamp = timestamp;
        this.level = level;
        this.tag = tag;
        this.message = message;
    }
}
