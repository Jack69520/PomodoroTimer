package com.skyinit.pomodorotimer.domain.timer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 暂停原因槽对齐与弹窗判定（纯逻辑，无 Android 依赖）。
 */
public final class PauseReasonPolicy {

    /** 列表中表示尚未标注的占位（编码时用特殊 token）。 */
    public static final String UNSETTLED = "";

    public static final long PAUSE_HINT_URGENT_MS = 60_000L;

    private PauseReasonPolicy() {
    }

    /**
     * 新增一次暂停：追加 unsettled 槽，使 {@code size == pauseCount}。
     */
    public static List<String> appendUnsettledSlot(List<String> reasons, int newPauseCount) {
        List<String> out = copy(reasons);
        while (out.size() < newPauseCount) {
            out.add(UNSETTLED);
        }
        while (out.size() > newPauseCount) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    /**
     * 写入/覆盖当前（最后一次）暂停原因。
     */
    public static List<String> settleCurrentReason(List<String> reasons, int pauseCount, String reason) {
        if (pauseCount <= 0) {
            return copy(reasons);
        }
        String settled = reason == null ? UNSETTLED : reason.trim();
        if (settled.isEmpty()) {
            settled = UNSETTLED;
        }
        List<String> out = copy(reasons);
        while (out.size() < pauseCount) {
            out.add(UNSETTLED);
        }
        out.set(pauseCount - 1, settled);
        return out;
    }

    public static boolean isCurrentReasonSettled(List<String> reasons, int pauseCount) {
        if (pauseCount <= 0) {
            return true;
        }
        if (reasons == null || reasons.size() < pauseCount) {
            return false;
        }
        String last = reasons.get(pauseCount - 1);
        return last != null && !last.trim().isEmpty();
    }

    /**
     * 持久化/完成结算前：将 unsettled 槽规范为「未填写」文案，长度对齐 pauseCount。
     */
    public static List<String> normalizeForPersist(List<String> reasons,
                                                   int pauseCount,
                                                   String unfilledLabel) {
        String label = unfilledLabel == null || unfilledLabel.isEmpty() ? "未填写" : unfilledLabel;
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.max(0, pauseCount); i++) {
            String r = (reasons != null && i < reasons.size()) ? reasons.get(i) : null;
            if (r == null || r.trim().isEmpty()) {
                out.add(label);
            } else {
                out.add(r.trim());
            }
        }
        return out;
    }

    public static boolean shouldAutoPrompt(PauseReasonPromptMode mode,
                                           boolean paused,
                                           boolean currentSettled) {
        return mode != null
                && mode.shouldPrompt()
                && paused
                && !currentSettled;
    }

    public static boolean isPauseHintUrgent(long pauseTimeoutRemainingMs) {
        return pauseTimeoutRemainingMs > 0L && pauseTimeoutRemainingMs <= PAUSE_HINT_URGENT_MS;
    }

    private static List<String> copy(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(reasons);
    }

    public static List<String> immutableCopy(List<String> reasons) {
        return Collections.unmodifiableList(copy(reasons));
    }
}
