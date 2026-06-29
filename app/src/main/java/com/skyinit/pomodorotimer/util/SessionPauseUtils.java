package com.skyinit.pomodorotimer.util;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 专注记录暂停原因的编解码工具。
 */
public final class SessionPauseUtils {

    private static final String REASON_DELIMITER = "||";

    private SessionPauseUtils() {
    }

    public static String encodeReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String reason : reasons) {
            if (TextUtils.isEmpty(reason)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(REASON_DELIMITER);
            }
            builder.append(reason.trim());
        }
        return builder.length() > 0 ? builder.toString() : null;
    }

    public static List<String> decodeReasons(String encoded, String fallbackReason) {
        if (!TextUtils.isEmpty(encoded)) {
            String[] parts = encoded.split("\\Q" + REASON_DELIMITER + "\\E");
            List<String> reasons = new ArrayList<>();
            for (String part : parts) {
                if (!TextUtils.isEmpty(part)) {
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
}
