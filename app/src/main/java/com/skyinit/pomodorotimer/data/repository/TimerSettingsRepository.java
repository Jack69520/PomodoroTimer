package com.skyinit.pomodorotimer.data.repository;

import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;

/**
 * 用户级计时偏好（学习/休息时长、暂停上限），持久化于 Room {@link UserPomodoroSettings}。
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
        UserPomodoroSettings settings = pomodoroSettingsRepository.getSettings();
        settings.defaultStudyTimeMs = clamped;
        pomodoroSettingsRepository.saveSettings(settings);
        return clamped;
    }

    public long getDefaultBreakTimeMs() {
        return clampBreak(pomodoroSettingsRepository.getSettings().defaultBreakTimeMs);
    }

    public long setDefaultBreakTimeMs(long millis) {
        long clamped = clampBreak(millis);
        UserPomodoroSettings settings = pomodoroSettingsRepository.getSettings();
        settings.defaultBreakTimeMs = clamped;
        pomodoroSettingsRepository.saveSettings(settings);
        return clamped;
    }

    public int getMaxPauseCount() {
        return clampMaxPauseCount(pomodoroSettingsRepository.getSettings().maxPauseCount);
    }

    public int setMaxPauseCount(int count) {
        int clamped = clampMaxPauseCount(count);
        UserPomodoroSettings settings = pomodoroSettingsRepository.getSettings();
        settings.maxPauseCount = clamped;
        pomodoroSettingsRepository.saveSettings(settings);
        return clamped;
    }

    public boolean isDndDuringFocusEnabled() {
        return pomodoroSettingsRepository.getSettings().dndDuringFocusEnabled;
    }

    public void setDndDuringFocusEnabled(boolean enabled) {
        UserPomodoroSettings settings = pomodoroSettingsRepository.getSettings();
        settings.dndDuringFocusEnabled = enabled;
        pomodoroSettingsRepository.saveSettings(settings);
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
