package com.skyinit.pomodorotimer.domain.timer;

/**
 * 会话结算决策：由 {@link TimerSessionPolicy} 根据快照与当前时刻算出。
 */
public enum SettleDecision {
    /** 仍在计时，无需结算。 */
    NONE,
    /** 运行中会话已到点，应走正常完成。 */
    COMPLETE_RUNNING,
    /** 暂停已超时，应记为失败。 */
    FAIL_PAUSE_TIMEOUT,
    /** Alarm generation 或快照无效，忽略本次触发。 */
    IGNORE
}
