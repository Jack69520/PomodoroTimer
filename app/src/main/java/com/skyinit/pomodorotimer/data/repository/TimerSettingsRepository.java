package com.skyinit.pomodorotimer.data.repository;

import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;
import com.skyinit.pomodorotimer.domain.timer.PauseReasonPromptMode;

/**
 * 用户级计时偏好（学习/休息时长、暂停上限），持久化于 Room {@link UserPomodoroSettings}。
 * <p>
 * 写路径统一走 {@link UserPomodoroSettingsRepository#applyUpdate}，避免缓存未命中时
 * 用默认值整行覆盖，并与 diskIo 同步写语义兼容。
 */
public class TimerSettingsRepository {

    private static final long MIN_STUDY_TIME_MS = 60_000L;
    private static final long MAX_STUDY_TIME_MS = 180L * 60L * 1000L;
    private static final long MIN_BREAK_TIME_MS = 60_000L;
    private static final long MAX_BREAK_TIME_MS = 30L * 60L * 1000L;

    private final UserPomodoroSettingsRepository pomodoroSettingsRepository;

    public TimerSettingsRepository(UserPomodoroSettingsRepository pomodoroSettingsRepository) {
        this.pomodoroSettingsRepository = pomodoroSettingsRepository;
    }

    public long getDefaultStudyTimeMs() {
        return clampStudy(pomodoroSettingsRepository.getSettings().defaultStudyTimeMs);
    }

    public long setDefaultStudyTimeMs(long millis) {
        long clamped = clampStudy(millis);
        pomodoroSettingsRepository.applyUpdate(settings -> settings.defaultStudyTimeMs = clamped);
        return clamped;
    }

    public long getDefaultBreakTimeMs() {
        return clampBreak(pomodoroSettingsRepository.getSettings().defaultBreakTimeMs);
    }

    public long setDefaultBreakTimeMs(long millis) {
        long clamped = clampBreak(millis);
        pomodoroSettingsRepository.applyUpdate(settings -> settings.defaultBreakTimeMs = clamped);
        return clamped;
    }

    public int getMaxPauseCount() {
        return clampMaxPauseCount(pomodoroSettingsRepository.getSettings().maxPauseCount);
    }

    public int setMaxPauseCount(int count) {
        int clamped = clampMaxPauseCount(count);
        pomodoroSettingsRepository.applyUpdate(settings -> settings.maxPauseCount = clamped);
        return clamped;
    }

    public PauseReasonPromptMode getPauseReasonPromptMode() {
        return PauseReasonPromptMode.fromStorage(
                pomodoroSettingsRepository.getSettings().pauseReasonPromptMode);
    }

    public PauseReasonPromptMode setPauseReasonPromptMode(PauseReasonPromptMode mode) {
        PauseReasonPromptMode resolved = mode != null ? mode : PauseReasonPromptMode.ASK_SKIPPABLE;
        pomodoroSettingsRepository.applyUpdate(
                settings -> settings.pauseReasonPromptMode = resolved.storageValue);
        return resolved;
    }

    public boolean isDndDuringFocusEnabled() {
        return pomodoroSettingsRepository.getSettings().dndDuringFocusEnabled;
    }

    public void setDndDuringFocusEnabled(boolean enabled) {
        pomodoroSettingsRepository.applyUpdate(settings -> settings.dndDuringFocusEnabled = enabled);
    }

    public boolean isAutoBlockDuringPomodoroEnabled() {
        return pomodoroSettingsRepository.getSettings().autoBlockDuringPomodoro;
    }

    public void setAutoBlockDuringPomodoroEnabled(boolean enabled) {
        pomodoroSettingsRepository.applyUpdate(
                settings -> settings.autoBlockDuringPomodoro = enabled);
    }

    public boolean isLockScreenFullscreenEnabled() {
        return pomodoroSettingsRepository.getSettings().lockScreenFullscreenEnabled;
    }

    public void setLockScreenFullscreenEnabled(boolean enabled) {
        pomodoroSettingsRepository.applyUpdate(
                settings -> settings.lockScreenFullscreenEnabled = enabled);
    }

    public long resetToDefault() {
        return setDefaultStudyTimeMs(UserPomodoroSettings.DEFAULT_STUDY_TIME_MS);
    }

    public static long clampStudy(long millis) {
        return Math.max(MIN_STUDY_TIME_MS, Math.min(millis, MAX_STUDY_TIME_MS));
    }

    public static long clampBreak(long millis) {
        return Math.max(MIN_BREAK_TIME_MS, Math.min(millis, MAX_BREAK_TIME_MS));
    }

    public static int clampMaxPauseCount(int count) {
        return Math.max(UserPomodoroSettings.MIN_MAX_PAUSE_COUNT,
                Math.min(count, UserPomodoroSettings.MAX_MAX_PAUSE_COUNT));
    }

    public static long getFactoryDefaultMs() {
        return UserPomodoroSettings.DEFAULT_STUDY_TIME_MS;
    }

    public static long getFactoryDefaultBreakMs() {
        return UserPomodoroSettings.DEFAULT_BREAK_TIME_MS;
    }
}
