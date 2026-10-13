package com.skyinit.pomodorotimer.domain.timer;

/**
 * 学习会话结算纯策略：时长认真实、完成认规则；严格模式仅影响写入。
 * <p>
 * 门槛与 {@code ActiveSessionStore.SAVE_ELIGIBLE_MS} 保持一致（60 秒）。
 */
public final class SessionSettlePolicy {

    /** 有效专注满此时长才可保存为计入/留痕记录。 */
    public static final long SAVE_ELIGIBLE_MS = 60L * 1000L;

    private SessionSettlePolicy() {
    }

    /**
     * @param reason            结束原因
     * @param strictPomodoro    结算瞬间的严格番茄开关
     * @param activeDurationMs  有效专注毫秒（不含暂停）
     * @param isStudySession    是否学习会话（休息不落 PomodoroSession）
     */
    public static SettleOutcome decide(SessionEndReason reason,
                                       boolean strictPomodoro,
                                       long activeDurationMs,
                                       boolean isStudySession) {
        if (!isStudySession) {
            return SettleOutcome.DISCARD_FORCE;
        }
        if (reason == null) {
            return SettleOutcome.DISCARD_FORCE;
        }
        long active = Math.max(0L, activeDurationMs);
        switch (reason) {
            case NATURAL_COMPLETE:
                return SettleOutcome.COMPLETE;
            case ACCOUNT_SWITCH:
            case FORCE_FAIL:
                return SettleOutcome.DISCARD_FORCE;
            case USER_ABORT:
                if (strictPomodoro) {
                    return SettleOutcome.DISCARD_STRICT;
                }
                if (active < SAVE_ELIGIBLE_MS) {
                    return SettleOutcome.DISCARD_SHORT;
                }
                return SettleOutcome.SAVE_EARLY_END;
            case PAUSE_TIMEOUT:
                if (active < SAVE_ELIGIBLE_MS) {
                    return SettleOutcome.DISCARD_SHORT;
                }
                return strictPomodoro
                        ? SettleOutcome.SAVE_FAILED_RECORD_ONLY
                        : SettleOutcome.SAVE_FAILED_COUNTED;
            default:
                return SettleOutcome.DISCARD_FORCE;
        }
    }

    /**
     * 确认框预览：与 {@link #decide} 同一决策，避免提示与结果不一致。
     */
    public static SettleOutcome previewForConfirm(boolean strictPomodoro,
                                                  long activeDurationMs,
                                                  boolean isStudySession) {
        return decide(SessionEndReason.USER_ABORT, strictPomodoro, activeDurationMs, isStudySession);
    }
}
