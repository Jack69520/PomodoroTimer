package com.skyinit.pomodorotimer.domain.timer;

/**
 * 暂停原因采集模式（设备级设置）。
 */
public enum PauseReasonPromptMode {
    /** 询问，可跳过（默认）。 */
    ASK_SKIPPABLE(0),
    /** 必须选择原因（或点继续计时）。 */
    REQUIRED(1),
    /** 不询问。 */
    OFF(2);

    public final int storageValue;

    PauseReasonPromptMode(int storageValue) {
        this.storageValue = storageValue;
    }

    public static PauseReasonPromptMode fromStorage(int value) {
        for (PauseReasonPromptMode mode : values()) {
            if (mode.storageValue == value) {
                return mode;
            }
        }
        return ASK_SKIPPABLE;
    }

    public boolean shouldPrompt() {
        return this != OFF;
    }

    public boolean isSkippable() {
        return this == ASK_SKIPPABLE;
    }

    public boolean isRequired() {
        return this == REQUIRED;
    }
}
