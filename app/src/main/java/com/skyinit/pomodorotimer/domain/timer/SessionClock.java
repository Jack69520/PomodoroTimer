package com.skyinit.pomodorotimer.domain.timer;

/**
 * 会话时钟：用 elapsedRealtime + wall-clock 双时钟在进程存活与重启后解析剩余时长。
 * <p>
 * 未重启时优先 elapsed（不受用户改系统时间影响）；检测到重启或 elapsed/wall 严重分歧则回退 wall-clock。
 */
public final class SessionClock {

    private SessionClock() {
    }

    /**
     * 运行态剩余毫秒。
     */
    public static long computeRunningRemainingMs(long endAtElapsedRealtime,
                                                 long endAtWallClockMs,
                                                 long nowElapsedRealtime,
                                                 long nowWallClockMs,
                                                 boolean preferWallClock) {
        if (preferWallClock) {
            if (endAtWallClockMs > 0L) {
                return Math.max(0L, endAtWallClockMs - nowWallClockMs);
            }
            return 0L;
        }
        if (endAtElapsedRealtime > 0L) {
            return Math.max(0L, endAtElapsedRealtime - nowElapsedRealtime);
        }
        if (endAtWallClockMs > 0L) {
            return Math.max(0L, endAtWallClockMs - nowWallClockMs);
        }
        return 0L;
    }

    /**
     * 根据“保存时剩余 + 保存时刻 wall”在重启后重建剩余。
     */
    public static long computeRemainingFromSaved(long remainingAtSaveMs,
                                                 long savedAtWallClockMs,
                                                 long nowWallClockMs) {
        if (remainingAtSaveMs <= 0L) {
            return 0L;
        }
        if (savedAtWallClockMs <= 0L) {
            return remainingAtSaveMs;
        }
        long elapsedWall = Math.max(0L, nowWallClockMs - savedAtWallClockMs);
        return Math.max(0L, remainingAtSaveMs - elapsedWall);
    }

    public static long computePauseElapsedMs(long pauseStartElapsedRealtime,
                                             long pauseStartWallClockMs,
                                             long nowElapsedRealtime,
                                             long nowWallClockMs,
                                             boolean preferWallClock) {
        if (preferWallClock) {
            if (pauseStartWallClockMs > 0L) {
                return Math.max(0L, nowWallClockMs - pauseStartWallClockMs);
            }
            return 0L;
        }
        if (pauseStartElapsedRealtime > 0L) {
            return Math.max(0L, nowElapsedRealtime - pauseStartElapsedRealtime);
        }
        if (pauseStartWallClockMs > 0L) {
            return Math.max(0L, nowWallClockMs - pauseStartWallClockMs);
        }
        return 0L;
    }

    /**
     * elapsed 与 wall 对剩余结论严重分歧，或 elapsed 倒挂 → 优先 wall。
     */
    public static boolean shouldPreferWallClock(long endAtElapsedRealtime,
                                                long endAtWallClockMs,
                                                long nowElapsedRealtime,
                                                long nowWallClockMs) {
        if (endAtElapsedRealtime <= 0L) {
            return endAtWallClockMs > 0L;
        }
        if (endAtWallClockMs <= 0L) {
            return false;
        }
        long byElapsed = endAtElapsedRealtime - nowElapsedRealtime;
        long byWall = endAtWallClockMs - nowWallClockMs;
        if (byElapsed > 6L * 60L * 60L * 1000L && byWall < byElapsed - 60L * 60L * 1000L) {
            return true;
        }
        return byElapsed < -60_000L;
    }

    public static boolean shouldPreferWallForPause(long pauseStartElapsedRealtime,
                                                   long pauseStartWallClockMs,
                                                   long nowElapsedRealtime,
                                                   long nowWallClockMs) {
        if (pauseStartElapsedRealtime <= 0L) {
            return pauseStartWallClockMs > 0L;
        }
        if (pauseStartElapsedRealtime > nowElapsedRealtime + 60_000L) {
            return pauseStartWallClockMs > 0L;
        }
        long byElapsed = nowElapsedRealtime - pauseStartElapsedRealtime;
        return byElapsed < -60_000L && pauseStartWallClockMs > 0L;
    }
}
