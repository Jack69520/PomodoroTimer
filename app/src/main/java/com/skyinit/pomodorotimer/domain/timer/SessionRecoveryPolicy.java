package com.skyinit.pomodorotimer.domain.timer;

/**
 * 启动恢复决策：根据账户边界、结算结果与快照时钟决定 UI/Service 行为。
 * <p>
 * 纯函数，无 Android 依赖。
 */
public final class SessionRecoveryPolicy {

    private SessionRecoveryPolicy() {
    }

    /**
     * @param activeUserId         当前登录用户；Guest 时为 null/空
     * @param lastResult           已结算结果字符串（completed / failed_timeout / break_started）；空表示无
     * @param lastResultSessionId  结果对应会话 id
     * @param hasActiveCheckpoint  是否存在进行中快照
     * @param checkpointUserId     快照归属用户
     * @param running              快照是否运行中
     * @param paused               快照是否暂停
     * @param awaitingPostBreak    休息后待选择
     * @param remainingMs          运行中剩余毫秒（非运行可为 timeLeft）
     * @param pauseElapsedMs       已暂停时长
     * @param pauseTimeoutMs       暂停超时阈值
     * @param sessionId            快照会话 id
     */
    public static RecoveryDecision decide(String activeUserId,
                                          String lastResult,
                                          long lastResultSessionId,
                                          boolean hasActiveCheckpoint,
                                          String checkpointUserId,
                                          boolean running,
                                          boolean paused,
                                          boolean awaitingPostBreak,
                                          long remainingMs,
                                          long pauseElapsedMs,
                                          long pauseTimeoutMs,
                                          long sessionId) {
        boolean guest = activeUserId == null || activeUserId.isEmpty();
        boolean hasResult = lastResult != null && !lastResult.isEmpty();

        if (guest) {
            if (hasActiveCheckpoint || hasResult) {
                return RecoveryDecision.discardForeignOrGuest();
            }
            return RecoveryDecision.none();
        }

        if (hasResult) {
            RecoveryDecision.ResultKind resultKind = mapResult(lastResult);
            if (resultKind != RecoveryDecision.ResultKind.NONE) {
                return RecoveryDecision.showResult(resultKind, lastResultSessionId);
            }
        }

        if (!hasActiveCheckpoint) {
            return RecoveryDecision.none();
        }

        if (checkpointUserId == null || checkpointUserId.isEmpty()
                || !activeUserId.equals(checkpointUserId)) {
            return RecoveryDecision.discardForeignOrGuest();
        }

        if (awaitingPostBreak) {
            return RecoveryDecision.continueActive(sessionId);
        }

        if (running) {
            if (remainingMs > 0L) {
                return RecoveryDecision.continueActive(sessionId);
            }
            return RecoveryDecision.settleThenResult(sessionId);
        }

        if (paused) {
            long timeout = pauseTimeoutMs > 0L
                    ? pauseTimeoutMs
                    : TimerSessionPolicy.DEFAULT_PAUSE_TIMEOUT_MS;
            if (pauseElapsedMs < timeout) {
                return RecoveryDecision.continueActive(sessionId);
            }
            return RecoveryDecision.settleThenResult(sessionId);
        }

        // active 标记存在但阶段不可识别：交 Service 结算/清理
        return RecoveryDecision.settleThenResult(sessionId);
    }

    private static RecoveryDecision.ResultKind mapResult(String lastResult) {
        if ("completed".equals(lastResult)) {
            return RecoveryDecision.ResultKind.COMPLETED;
        }
        if ("failed_timeout".equals(lastResult)) {
            return RecoveryDecision.ResultKind.FAILED_TIMEOUT;
        }
        if ("break_started".equals(lastResult)) {
            return RecoveryDecision.ResultKind.BREAK_STARTED;
        }
        return RecoveryDecision.ResultKind.NONE;
    }
}
