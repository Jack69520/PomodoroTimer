package com.skyinit.pomodorotimer.ui.profile.devlab;

import androidx.annotation.NonNull;

/**
 * 开发实验室用户意图（MVI）。
 */
public final class DevLabIntent {

    public enum Type {
        BOOTSTRAP,
        REFRESH_STORAGE,
        REFRESH_MEMORY,
        REFRESH_RESOURCES,
        REFRESH_LOGS,
        REQUEST_CLEAR_LOGS,
        CONFIRM_CLEAR_LOGS,
        SET_AUTO_UPDATE,
        SET_FILTER_INDEX,
        SET_PAGE_VISIBLE
    }

    public final Type type;
    public final boolean flag;
    public final int index;

    private DevLabIntent(Type type, boolean flag, int index) {
        this.type = type;
        this.flag = flag;
        this.index = index;
    }

    @NonNull
    public static DevLabIntent bootstrap() {
        return new DevLabIntent(Type.BOOTSTRAP, false, 0);
    }

    @NonNull
    public static DevLabIntent refreshStorage() {
        return new DevLabIntent(Type.REFRESH_STORAGE, false, 0);
    }

    @NonNull
    public static DevLabIntent refreshMemory() {
        return new DevLabIntent(Type.REFRESH_MEMORY, false, 0);
    }

    @NonNull
    public static DevLabIntent refreshResources() {
        return new DevLabIntent(Type.REFRESH_RESOURCES, false, 0);
    }

    @NonNull
    public static DevLabIntent refreshLogs() {
        return new DevLabIntent(Type.REFRESH_LOGS, false, 0);
    }

    @NonNull
    public static DevLabIntent requestClearLogs() {
        return new DevLabIntent(Type.REQUEST_CLEAR_LOGS, false, 0);
    }

    @NonNull
    public static DevLabIntent confirmClearLogs() {
        return new DevLabIntent(Type.CONFIRM_CLEAR_LOGS, false, 0);
    }

    @NonNull
    public static DevLabIntent setAutoUpdate(boolean enabled) {
        return new DevLabIntent(Type.SET_AUTO_UPDATE, enabled, 0);
    }

    @NonNull
    public static DevLabIntent setFilterIndex(int index) {
        return new DevLabIntent(Type.SET_FILTER_INDEX, false, index);
    }

    @NonNull
    public static DevLabIntent setPageVisible(boolean visible) {
        return new DevLabIntent(Type.SET_PAGE_VISIBLE, visible, 0);
    }
}
