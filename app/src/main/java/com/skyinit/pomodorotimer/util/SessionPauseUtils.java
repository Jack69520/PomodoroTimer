package com.skyinit.pomodorotimer.util;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 专注记录暂停原因的编解码工具。
 * <p>
 * 空槽（尚未标注）编码为 {@link #EMPTY_SLOT_TOKEN}，以保持与 pauseCount 对齐。
 */
public final class SessionPauseUtils {

    private static final String REASON_DELIMITER = "||";
    /** 与 PauseReasonPolicy.UNSETTLED 对应的持久化占位。 */
    static final String EMPTY_SLOT_TOKEN = "\u200B";

    private SessionPauseUtils() {
    }

    public static String encodeReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String reason : reasons) {
            if (builder.length() > 0) {
                builder.append(REASON_DELIMITER);
            }
            if (reason == null || reason.isEmpty()) {
                builder.append(EMPTY_SLOT_TOKEN);
            } else {
                builder.append(reason.trim());
            }
        }
        return builder.length() > 0 ? builder.toString() : null;
    }

    public static List<String> decodeReasons(String encoded, String fallbackReason) {
        if (!TextUtils.isEmpty(encoded)) {
            String[] parts = encoded.split("\\Q" + REASON_DELIMITER + "\\E", -1);
            List<String> reasons = new ArrayList<>();
            for (String part : parts) {
                if (part == null) {
                    reasons.add("");
                } else if (EMPTY_SLOT_TOKEN.equals(part) || part.isEmpty()) {
                    reasons.add("");
                } else {
                    reasons.add(part.trim());
                }
            }
            if (!reasons.isEmpty()) {
                return reasons;
            }
        }
        if (!TextUtils.isEmpty(fallbackReason)) {
            return Collections.singletonList(fallbackReason.trim());
        }
        return Collections.emptyList();
    }

    public static boolean hasPause(int pauseCount, String pauseReasons, String pauseReason) {
        return pauseCount > 0
                || !TextUtils.isEmpty(pauseReasons)
                || !TextUtils.isEmpty(pauseReason);
    }

    public static boolean isTimeoutFailure(String pauseReason, String pauseReasons, String timeoutLabel) {
        if (timeoutLabel == null || timeoutLabel.isEmpty()) {
            return false;
        }
        if (timeoutLabel.equals(pauseReason)) {
            return true;
        }
        return decodeReasons(pauseReasons, pauseReason).contains(timeoutLabel);
    }
}
