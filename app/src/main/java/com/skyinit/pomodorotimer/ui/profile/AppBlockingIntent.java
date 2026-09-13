package com.skyinit.pomodorotimer.ui.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.data.entity.BlockedApp;

/**
 * 管理屏蔽应用页用户意图（轻量 MVI Intent）。
 */
public final class AppBlockingIntent {

    public enum Type {
        SEARCH_DEBOUNCED,
        SEARCH_IMMEDIATE,
        SET_CATEGORY_FILTER,
        SET_STATUS_FILTER,
        SCAN,
        TOGGLE,
        CHECK_AUTO_SCAN,
        RESET_FILTERS
    }

    public enum StatusFilter {
        ALL,
        BLOCKED,
        ALLOWED
    }

    public final Type type;
    @Nullable public final String text;
    @Nullable public final StatusFilter statusFilter;
    @Nullable public final BlockedApp app;
    public final boolean blocked;

    private AppBlockingIntent(Type type,
                              @Nullable String text,
                              @Nullable StatusFilter statusFilter,
                              @Nullable BlockedApp app,
                              boolean blocked) {
        this.type = type;
        this.text = text;
        this.statusFilter = statusFilter;
        this.app = app;
        this.blocked = blocked;
    }

    public static AppBlockingIntent searchDebounced(@Nullable String query) {
        return new AppBlockingIntent(Type.SEARCH_DEBOUNCED, query, null, null, false);
    }

    public static AppBlockingIntent searchImmediate(@Nullable String query) {
        return new AppBlockingIntent(Type.SEARCH_IMMEDIATE, query, null, null, false);
    }

    public static AppBlockingIntent setCategoryFilter(@NonNull String category) {
        return new AppBlockingIntent(Type.SET_CATEGORY_FILTER, category, null, null, false);
    }

    public static AppBlockingIntent setStatusFilter(@NonNull StatusFilter filter) {
        return new AppBlockingIntent(Type.SET_STATUS_FILTER, null, filter, null, false);
    }

    public static AppBlockingIntent scan() {
        return new AppBlockingIntent(Type.SCAN, null, null, null, false);
    }

    public static AppBlockingIntent toggle(@NonNull BlockedApp app, boolean blocked) {
        return new AppBlockingIntent(Type.TOGGLE, null, null, app, blocked);
    }

    public static AppBlockingIntent checkAutoScan() {
        return new AppBlockingIntent(Type.CHECK_AUTO_SCAN, null, null, null, false);
    }

    public static AppBlockingIntent resetFilters() {
        return new AppBlockingIntent(Type.RESET_FILTERS, null, null, null, false);
    }
}
