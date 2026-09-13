package com.skyinit.pomodorotimer.ui.settings;

import android.app.Application;
import android.content.Context;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;
import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.data.repository.UserPomodoroSettingsRepository;
import com.skyinit.pomodorotimer.ui.account.AccountActivity;
import com.skyinit.pomodorotimer.ui.account.AccountSecurityActivity;
import com.skyinit.pomodorotimer.ui.theme.WallpaperCatalog;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.SettingsPermissionHelper;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;
import com.skyinit.pomodorotimer.util.StudyDurationPickerHelper;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 设置主页 ViewModel：Intent → {@link SettingsHubUiState} + {@link SettingsHubEffect}。
 * 异步摘要用 generation 丢弃过期结果。
 */
public class SettingsHubViewModel extends ViewModel {

    private final Application application;
    private final SettingsManager settingsManager;
    private final UserPomodoroSettingsRepository pomodoroSettingsRepository;
    private final AppExecutors executors = AppExecutors.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<SettingsHubUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<SettingsHubEffect> effects = new SingleLiveEvent<>();

    private final AtomicInteger refreshGeneration = new AtomicInteger(0);

    public SettingsHubViewModel(@NonNull Application application,
                                @NonNull SettingsManager settingsManager,
                                @NonNull UserPomodoroSettingsRepository pomodoroSettingsRepository) {
        this.application = application;
        this.settingsManager = settingsManager;
        this.pomodoroSettingsRepository = pomodoroSettingsRepository;
        uiState.setValue(buildSnapshot(null));
        dispatch(SettingsHubIntent.refresh());
    }

    @NonNull
    public LiveData<SettingsHubUiState> getUiState() {
        return uiState;
    }

    @NonNull
    public LiveData<SettingsHubEffect> getEffects() {
        return effects;
    }

    @MainThread
    public void dispatch(@NonNull SettingsHubIntent intent) {
        switch (intent.type) {
            case REFRESH:
                refresh();
                break;
            case OPEN_ACCOUNT_PROFILE:
                effects.setValue(SettingsHubEffect.startActivity(AccountActivity.class));
                break;
            case OPEN_ACCOUNT_SECURITY:
                effects.setValue(SettingsHubEffect.startActivity(AccountSecurityActivity.class));
                break;
            case OPEN_THEME:
                effects.setValue(SettingsHubEffect.startActivity(ThemeColorSettingsActivity.class));
                break;
            case OPEN_RINGTONE:
                effects.setValue(SettingsHubEffect.showRingtoneChooser(resolveRingtoneOption(
                        settingsManager.getRingtoneUri())));
                break;
            case OPEN_POMODORO:
                effects.setValue(SettingsHubEffect.startActivity(PomodoroSettingsActivity.class));
                break;
            case OPEN_SYSTEM_PERMISSIONS:
                effects.setValue(SettingsHubEffect.startActivity(SystemPermissionsActivity.class));
                break;
            case RINGTONE_OPTION_SELECTED:
                onRingtoneOptionSelected(intent.ringtoneOption);
                break;
            case CUSTOM_RINGTONE_PICKED:
                if (intent.ringtoneUri != null && !intent.ringtoneUri.isEmpty()) {
                    settingsManager.setRingtoneUri(intent.ringtoneUri);
                    effects.setValue(SettingsHubEffect.showToast(
                            R.string.settings_toast_custom_ringtone_success, false));
                    refresh();
                }
                break;
            case CUSTOM_RINGTONE_CANCELLED:
                // 仅刷新标签，保持原选择
                refresh();
                break;
            default:
                break;
        }
    }

    /** 从子页返回或 onResume 时刷新摘要。 */
    @MainThread
    public void onHostResumed() {
        refresh();
    }

    @MainThread
    public void onAudioPermissionResult(boolean granted) {
        if (granted) {
            String uri = settingsManager.getRingtoneUri();
            String existing = isCustomUri(uri) ? uri : null;
            effects.setValue(SettingsHubEffect.launchRingtonePicker(existing));
        } else {
            effects.setValue(SettingsHubEffect.showToast(
                    R.string.settings_toast_storage_permission_denied, false));
            refresh();
        }
    }

    private void refresh() {
        int generation = refreshGeneration.incrementAndGet();
        uiState.setValue(buildSnapshot(null));

        executors.diskIo(() -> {
            UserPomodoroSettings settings = pomodoroSettingsRepository.getSettingsSync();
            if (generation != refreshGeneration.get()) {
                return;
            }
            SettingsHubUiState next = buildSnapshot(settings);
            mainHandler.post(() -> {
                if (generation != refreshGeneration.get()) {
                    return;
                }
                uiState.setValue(next);
            });
        });
    }

    private void onRingtoneOptionSelected(int option) {
        switch (option) {
            case 0:
                settingsManager.setRingtoneUri("default");
                refresh();
                break;
            case 1:
                settingsManager.setRingtoneUri("silent");
                refresh();
                break;
            case 2:
                if (needsAudioPermission()) {
                    effects.setValue(SettingsHubEffect.requestAudioPermission());
                } else {
                    String uri = settingsManager.getRingtoneUri();
                    String existing = isCustomUri(uri) ? uri : null;
                    effects.setValue(SettingsHubEffect.launchRingtonePicker(existing));
                }
                break;
            default:
                break;
        }
    }

    private boolean needsAudioPermission() {
        boolean needMusic = SettingsPermissionHelper.isApplicable(SettingsPermissionHelper.Kind.MUSIC_AUDIO)
                && !SettingsPermissionHelper.isGranted(application, SettingsPermissionHelper.Kind.MUSIC_AUDIO);
        boolean needMedia = SettingsPermissionHelper.isApplicable(SettingsPermissionHelper.Kind.MEDIA_FILES)
                && !SettingsPermissionHelper.isGranted(application, SettingsPermissionHelper.Kind.MEDIA_FILES);
        return needMusic || needMedia;
    }

    @NonNull
    private SettingsHubUiState buildSnapshot(@Nullable UserPomodoroSettings pomodoro) {
        Context context = application;
        WallpaperCatalog.WallpaperOption theme =
                WallpaperCatalog.requireOption(context, settingsManager.getWallpaperThemeKey());
        String themeName = theme.name;
        int themeRes = theme.resId;
        boolean gradient = theme.gradient;

        String ringtoneUri = settingsManager.getRingtoneUri();
        int ringtoneOption = resolveRingtoneOption(ringtoneUri);
        String ringtoneLabel = resolveRingtoneLabel(context, ringtoneOption);

        String pomodoroSummary = buildPomodoroSummary(context, pomodoro);

        return new SettingsHubUiState(
                themeName,
                themeRes,
                gradient,
                ringtoneLabel,
                ringtoneOption,
                pomodoroSummary);
    }

    @NonNull
    private static String buildPomodoroSummary(@NonNull Context context,
                                               @Nullable UserPomodoroSettings settings) {
        if (settings == null) {
            return context.getString(R.string.settings_pomodoro_summary_loading);
        }
        String study = StudyDurationPickerHelper.formatDurationLabel(context, settings.defaultStudyTimeMs);
        int breakMin = (int) (settings.defaultBreakTimeMs / 60_000L);
        return context.getString(R.string.settings_pomodoro_summary_format, study, breakMin);
    }

    private static int resolveRingtoneOption(@Nullable String uri) {
        if (uri == null || "default".equals(uri)
                || RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString().equals(uri)) {
            return 0;
        }
        if ("silent".equals(uri)) {
            return 1;
        }
        return 2;
    }

    @NonNull
    private static String resolveRingtoneLabel(@NonNull Context context, int option) {
        String[] labels = context.getResources().getStringArray(R.array.ringtones);
        if (option >= 0 && option < labels.length) {
            return labels[option];
        }
        return labels[0];
    }

    private static boolean isCustomUri(@Nullable String uri) {
        return uri != null && !"default".equals(uri) && !"silent".equals(uri);
    }
}
