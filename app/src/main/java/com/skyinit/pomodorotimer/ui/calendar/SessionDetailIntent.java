package com.skyinit.pomodorotimer.ui.calendar;

import androidx.annotation.Nullable;

/**
 * 记录详情页用户意图。
 */
public final class SessionDetailIntent {

    public enum Type {
        LOAD,
        SAVE_NOTES,
        OPEN_BLOCK_RECORDS
    }

    public final Type type;
    public final int sessionId;
    @Nullable
    public final String notes;

    private SessionDetailIntent(Type type, int sessionId, @Nullable String notes) {
        this.type = type;
        this.sessionId = sessionId;
        this.notes = notes;
    }

    public static SessionDetailIntent load(int sessionId) {
        return new SessionDetailIntent(Type.LOAD, sessionId, null);
    }

    public static SessionDetailIntent saveNotes(@Nullable String notes) {
        return new SessionDetailIntent(Type.SAVE_NOTES, -1, notes);
    }

    public static SessionDetailIntent openBlockRecords() {
        return new SessionDetailIntent(Type.OPEN_BLOCK_RECORDS, -1, null);
    }
}
