package com.skyinit.pomodorotimer.ui.settings;

import android.app.Application;
import android.content.Context;
import android.os.Build;
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
import com.skyinit.pomodorotimer.domain.ringtone.RingtoneAccessPolicy;
import com.skyinit.pomodorotimer.domain.ringtone.RingtoneAccessSnapshot;
import com.skyinit.pomodorotimer.ui.account.AccountActivity;
import com.skyinit.pomodorotimer.ui.account.AccountSecurityActivity;
import com.skyinit.pomodorotimer.ui.theme.WallpaperCatalog;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.SettingsPermissionHelper;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;
import com.skyinit.pomodorotimer.util.StudyDurationPickerHelper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 设置主页 ViewModel：Intent → {@link SettingsHubUiState} + {@link SettingsHubEffect}。
 * 异步摘要用 generation 丢弃过期结果；音频授权用 pending + inFlight 防呆。
 */
public class SettingsHubViewModel extends ViewModel {

    private enum AudioPendingAction {
        NONE,
        /** 授权成功后打开铃声选择器。 */
        PICK_AFTER_GRANT
    }

    private final Application application;
    private final SettingsManager settingsManager;
    private final UserPomodoroSettingsRepository pomodoroSettingsRepository;
    private final AppExecutors executors = AppExecutors.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<SettingsHubUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<SettingsHubEffect> effects = new SingleLiveEvent<>();

    private final AtomicInteger refreshGeneration = new AtomicInteger(0);
    private final AtomicBoolean permissionInFlight = new AtomicBoolean(false);
    @NonNull
    private volatile AudioPendingAction audioPending = AudioPendingAction.NONE;
    private volatile boolean awaitingSettingsReturn;

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

    @Nullable
    public SettingsPermissionHelper.Kind getAudioPermissionKind() {
        return RingtoneAccessPolicy.audioReadKind();
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
                clearAudioPending();
                if (intent.ringtoneUri != null && !intent.ringtoneUri.isEmpty()) {
                    settingsManager.setRingtoneUri(intent.ringtoneUri);
                    effects.setValue(SettingsHubEffect.showToast(
                            R.string.settings_toast_custom_ringtone_success, false));
                    refresh();
                }
                break;
            case CUSTOM_RINGTONE_CANCELLED:
                clearAudioPending();
                refresh();
                break;
            case AUDIO_RATIONALE_CONFIRMED:
                emitRequestRuntime();
                break;
            case AUDIO_RATIONALE_CANCELLED:
                clearAudioPending();
                refresh();
                break;
            case AUDIO_SETTINGS_NAVIGATE_FAILED:
                clearAudioPending();
                effects.setValue(SettingsHubEffect.showToast(
                        R.string.settings_permission_open_settings_failed, false));
                refresh();
                break;
            default:
                break;
        }
    }

    /** 从子页返回或 onResume 时刷新摘要；若从系统设置返回且已授权则完成 pending。 */
    @MainThread
    public void onHostResumed() {
        if (awaitingSettingsReturn) {
            awaitingSettingsReturn = false;
            if (RingtoneAccessPolicy.hasAudioReadAccess(application)) {
                completePendingAfterGrant();
                return;
            }
            clearAudioPending();
        }
        refresh();
    }

    /**
     * 运行时权限回调。
     *
     * @param openSettingsFallback true 表示永久拒绝等，需引导去设置
     */
    @MainThread
    public void onAudioPermissionResult(boolean granted, boolean openSettingsFallback) {
        permissionInFlight.set(false);
        if (granted) {
            completePendingAfterGrant();
            return;
        }
        if (openSettingsFallback) {
            effects.setValue(SettingsHubEffect.showAudioPermanentDenyDialog(
                    RingtoneAccessPolicy.audioReadKind()));
            refresh();
            return;
        }
        clearAudioPending();
        effects.setValue(SettingsHubEffect.showToast(
                R.string.ringtone_audio_permission_denied_toast, false));
        refresh();
    }

    /** Activity 判定应直达系统设置时调用，避免 inFlight 卡死。 */
    @MainThread
    public void onAudioPermissionDivertedToSettings() {
        permissionInFlight.set(false);
        awaitingSettingsReturn = true;
    }

    /** 用户确认永久拒绝对话框「去设置」（由 Activity 负责打开设置页）。 */
    @MainThread
    public void onOpenAudioSettingsConfirmed() {
        awaitingSettingsReturn = true;
    }

    /** 永久拒绝对话框取消。 */
    @MainThread
    public void onOpenAudioSettingsCancelled() {
        clearAudioPending();
        refresh();
    }

    private void onRingtoneOptionSelected(int option) {
        switch (option) {
            case 0:
                clearAudioPending();
                settingsManager.setRingtoneUri("default");
                refresh();
                break;
            case 1:
                clearAudioPending();
                settingsManager.setRingtoneUri("silent");
                refresh();
                break;
            case 2:
                beginCustomRingtoneFlow();
                break;
            default:
                break;
        }
    }

    private void beginCustomRingtoneFlow() {
        if (permissionInFlight.get()) {
            return;
        }
        audioPending = AudioPendingAction.PICK_AFTER_GRANT;
        if (!RingtoneAccessPolicy.needsAudioReadPermission(application)) {
            launchPickerForCurrentPreference();
            return;
        }
        effects.setValue(SettingsHubEffect.showAudioAccessRationale(rationaleMessageRes()));
    }

    private void emitRequestRuntime() {
        if (!permissionInFlight.compareAndSet(false, true)) {
            return;
        }
        SettingsPermissionHelper.Kind kind = RingtoneAccessPolicy.audioReadKind();
        String permission = SettingsPermissionHelper.getRuntimePermission(kind);
        if (permission == null) {
            permissionInFlight.set(false);
            awaitingSettingsReturn = true;
            effects.setValue(SettingsHubEffect.openAudioPermissionSettings(kind));
            return;
        }
        if (RingtoneAccessPolicy.hasAudioReadAccess(application)) {
            permissionInFlight.set(false);
            completePendingAfterGrant();
            return;
        }
        effects.setValue(SettingsHubEffect.requestAudioRuntime(permission, kind));
    }

    private void completePendingAfterGrant() {
        AudioPendingAction pending = audioPending;
        audioPending = AudioPendingAction.NONE;
        permissionInFlight.set(false);
        awaitingSettingsReturn = false;
        if (pending == AudioPendingAction.PICK_AFTER_GRANT) {
            launchPickerForCurrentPreference();
            return;
        }
        refresh();
    }

    private void launchPickerForCurrentPreference() {
        String uri = settingsManager.getRingtoneUri();
        String existing = RingtoneAccessPolicy.isCustomPreference(uri) ? uri : null;
        effects.setValue(SettingsHubEffect.launchRingtonePicker(existing));
        refresh();
    }

    private void clearAudioPending() {
        audioPending = AudioPendingAction.NONE;
        permissionInFlight.set(false);
        awaitingSettingsReturn = false;
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

    @NonNull
    private SettingsHubUiState buildSnapshot(@Nullable UserPomodoroSettings pomodoro) {
        Context context = application;
        WallpaperCatalog.WallpaperOption theme =
                WallpaperCatalog.requireOption(context, settingsManager.getWallpaperThemeKey());

        RingtoneAccessSnapshot access = RingtoneAccessPolicy.evaluate(
                context, settingsManager.getRingtoneUri());
        int ringtoneOption = optionToIndex(access.option);
        String ringtoneLabel = resolveRingtoneLabel(context, ringtoneOption);
        String ringtoneHint = access.degraded
                ? context.getString(R.string.ringtone_degraded_hint)
                : "";

        String pomodoroSummary = buildPomodoroSummary(context, pomodoro);

        return new SettingsHubUiState(
                theme.name,
                theme.resId,
                theme.gradient,
                ringtoneLabel,
                ringtoneOption,
                access.degraded,
                ringtoneHint,
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
        return optionToIndex(RingtoneAccessPolicy.resolveOption(uri));
    }

    private static int optionToIndex(@NonNull RingtoneAccessSnapshot.Option option) {
        switch (option) {
            case SILENT:
                return 1;
            case CUSTOM:
                return 2;
            case DEFAULT:
            default:
                return 0;
        }
    }

    @NonNull
    private static String resolveRingtoneLabel(@NonNull Context context, int option) {
        String[] labels = context.getResources().getStringArray(R.array.ringtones);
        if (option >= 0 && option < labels.length) {
            return labels[option];
        }
        return labels[0];
    }

    @androidx.annotation.StringRes
    private static int rationaleMessageRes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return R.string.ringtone_audio_permission_rationale;
        }
        return R.string.ringtone_audio_permission_rationale_legacy;
    }
}
