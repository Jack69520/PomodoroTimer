package com.skyinit.pomodorotimer.ui.ringtone;

import android.app.Application;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.domain.ringtone.RingtoneAccessPolicy;
import com.skyinit.pomodorotimer.domain.ringtone.RingtoneAccessPromptGate;
import com.skyinit.pomodorotimer.util.SettingsPermissionHelper;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 冷启动：自定义铃声软失效提示与快速授权（MVI）。
 * <p>
 * 不处理铃声选择器；授权成功仅解除 degraded（R1）。
 */
public class CustomRingtoneAccessViewModel extends AndroidViewModel {

    private final SettingsManager settingsManager;
    private final SingleLiveEvent<CustomRingtoneAccessEffect> effects = new SingleLiveEvent<>();
    private final MutableLiveData<Boolean> dialogVisible = new MutableLiveData<>(false);

    private final AtomicBoolean permissionInFlight = new AtomicBoolean(false);
    private volatile boolean awaitingSettingsReturn;
    private volatile boolean controllerBound;
    private volatile boolean evaluateScheduled;

    public CustomRingtoneAccessViewModel(@NonNull Application application) {
        super(application);
        this.settingsManager = ((App) application).getContainer().getSettingsManager();
    }

    public CustomRingtoneAccessViewModel(@NonNull Application application,
                                         @NonNull SettingsManager settingsManager) {
        super(application);
        this.settingsManager = settingsManager;
    }

    @NonNull
    public LiveData<CustomRingtoneAccessEffect> getEffects() {
        return effects;
    }

    @NonNull
    public LiveData<Boolean> getDialogVisible() {
        return dialogVisible;
    }

    @NonNull
    public SettingsPermissionHelper.Kind audioKind() {
        return RingtoneAccessPolicy.audioReadKind();
    }

    @Nullable
    public String runtimePermission() {
        return SettingsPermissionHelper.getRuntimePermission(audioKind());
    }

    public boolean markControllerBound() {
        if (controllerBound) {
            return false;
        }
        controllerBound = true;
        return true;
    }

    public void consumeEffect() {
        effects.setValue(null);
    }

    @MainThread
    public void dispatch(@NonNull CustomRingtoneAccessIntent intent) {
        switch (intent) {
            case EVALUATE_COLD_START:
                evaluateColdStart();
                break;
            case PRIMARY_GRANT:
                onPrimaryGrant();
                break;
            case OPEN_SETTINGS:
                awaitingSettingsReturn = true;
                dialogVisible.setValue(false);
                effects.setValue(CustomRingtoneAccessEffect.OPEN_SETTINGS);
                break;
            case DISMISS:
                dialogVisible.setValue(false);
                clearInFlight();
                break;
            case RUNTIME_RESULT_GRANTED:
                clearInFlight();
                awaitingSettingsReturn = false;
                dialogVisible.setValue(false);
                break;
            case RUNTIME_RESULT_DENIED:
                clearInFlight();
                dialogVisible.setValue(false);
                break;
            case RUNTIME_RESULT_PERMANENT_DENY:
                clearInFlight();
                awaitingSettingsReturn = true;
                dialogVisible.setValue(false);
                effects.setValue(CustomRingtoneAccessEffect.OPEN_SETTINGS);
                break;
            case HOST_RESUMED:
                if (awaitingSettingsReturn) {
                    awaitingSettingsReturn = false;
                    clearInFlight();
                    dialogVisible.setValue(false);
                }
                break;
            case SETTINGS_NAVIGATE_FAILED:
                clearInFlight();
                awaitingSettingsReturn = false;
                effects.setValue(CustomRingtoneAccessEffect.SHOW_TOAST_SETTINGS_FAILED);
                break;
            default:
                break;
        }
    }

    private void evaluateColdStart() {
        if (evaluateScheduled) {
            return;
        }
        evaluateScheduled = true;
        if (!RingtoneAccessPolicy.isCustomDegraded(getApplication(), settingsManager.getRingtoneUri())) {
            return;
        }
        if (!RingtoneAccessPromptGate.tryClaimColdStartPrompt()) {
            return;
        }
        dialogVisible.setValue(true);
        effects.setValue(CustomRingtoneAccessEffect.SHOW_DEGRADED_DIALOG);
    }

    private void onPrimaryGrant() {
        if (!permissionInFlight.compareAndSet(false, true)) {
            return;
        }
        dialogVisible.setValue(false);
        if (RingtoneAccessPolicy.hasAudioReadAccess(getApplication())) {
            clearInFlight();
            return;
        }
        String permission = runtimePermission();
        if (permission == null) {
            awaitingSettingsReturn = true;
            effects.setValue(CustomRingtoneAccessEffect.OPEN_SETTINGS);
            return;
        }
        effects.setValue(CustomRingtoneAccessEffect.REQUEST_RUNTIME);
    }

    private void clearInFlight() {
        permissionInFlight.set(false);
    }

    /** Activity 判定应直达设置时调用。 */
    @MainThread
    public void onDivertedToSettings() {
        permissionInFlight.set(false);
        awaitingSettingsReturn = true;
        dialogVisible.setValue(false);
    }
}
