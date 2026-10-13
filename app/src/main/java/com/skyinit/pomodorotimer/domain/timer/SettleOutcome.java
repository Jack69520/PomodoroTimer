package com.skyinit.pomodorotimer.domain.timer;

/**
 * 学习会话结算结果（纯领域，不含 Android / 持久化细节）。
 */
public enum SettleOutcome {
    /** 完整完成：计入完成数与专注时长。 */
    COMPLETE,
    /** 提前结束：计入专注时长，不计入完成数。 */
    SAVE_EARLY_END,
    /** 暂停超时失败：计入专注时长。 */
    SAVE_FAILED_COUNTED,
    /** 暂停超时失败：仅日历留痕，不计入专注时长（严格模式）。 */
    SAVE_FAILED_RECORD_ONLY,
    /** 有效专注不足门槛，丢弃。 */
    DISCARD_SHORT,
    /** 严格模式下用户主动结束，丢弃。 */
    DISCARD_STRICT,
    /** 切号 / FORCE_FAIL，强制丢弃。 */
    DISCARD_FORCE;

    public boolean shouldPersist() {
        return this == COMPLETE
                || this == SAVE_EARLY_END
                || this == SAVE_FAILED_COUNTED
                || this == SAVE_FAILED_RECORD_ONLY;
    }

    public boolean countsTowardFocusDuration() {
        return this == COMPLETE
                || this == SAVE_EARLY_END
                || this == SAVE_FAILED_COUNTED;
    }

    public boolean isCompletedTomato() {
        return this == COMPLETE;
    }

    public boolean isFailureShape() {
        return this == SAVE_FAILED_COUNTED || this == SAVE_FAILED_RECORD_ONLY;
    }

    public boolean isEarlyEndShape() {
        return this == SAVE_EARLY_END || isFailureShape();
    }
}
