package com.skyinit.pomodorotimer.ui.home;

import android.app.Application;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.model.TimerUiState;
import com.skyinit.pomodorotimer.data.repository.TimerSettingsRepository;
import com.skyinit.pomodorotimer.data.repository.TimerStateRepository;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.domain.timer.PauseReasonPolicy;
import com.skyinit.pomodorotimer.domain.timer.PauseReasonPromptMode;
import com.skyinit.pomodorotimer.service.TimerService;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

import java.util.Locale;

/**
 * 全屏计时页 ViewModel（MVI）：Intent → Effect；状态来自 {@link TimerStateRepository}。
 */
public class TimerViewModel extends ViewModel {

    private final Application application;
    private final TimerSettingsRepository timerSettings;
    private final UserSessionRepository sessionRepository;
    private final TimerStateRepository timerStateRepository;

    private final MutableLiveData<TimerScreenUiState> screenState = new MutableLiveData<>();
    private final SingleLiveEvent<TimerEffect> effects = new SingleLiveEvent<>();

    private final Observer<TimerUiState> timerObserver = this::onTimerState;

    private int promptedForPauseCount = -1;
    private boolean pendingPromptAfterPause;
    private boolean wasPaused;

    public TimerViewModel(Application application,
                          TimerSettingsRepository timerSettings,
                          UserSessionRepository sessionRepository,
                          TimerStateRepository timerStateRepository) {
        this.application = application;
        this.timerSettings = timerSettings;
        this.sessionRepository = sessionRepository;
        this.timerStateRepository = timerStateRepository;
        timerStateRepository.getState().observeForever(timerObserver);
        TimerUiState initial = timerStateRepository.getCurrentState();
        wasPaused = initial != null && initial.paused;
        onTimerState(initial);
    }

    @NonNull
    public LiveData<TimerUiState> getTimerState() {
        return timerStateRepository.getState();
    }

    @NonNull
    public LiveData<TimerScreenUiState> getScreenState() {
        return screenState;
    }

    @NonNull
    public LiveData<TimerEffect> getEffects() {
        return effects;
    }

    public boolean isLoggedIn() {
        return sessionRepository.isLoggedIn();
    }

    public void syncFromService(TimerService service) {
        timerStateRepository.syncFromService(service);
    }

    @MainThread
    public void dispatch(@NonNull TimerIntent intent) {
        TimerUiState state = timerStateRepository.getCurrentState();
        PauseReasonPromptMode mode = timerSettings.getPauseReasonPromptMode();

        switch (intent.type) {
            case SERVICE_BOUND:
            case SCREEN_RESUMED:
                maybePromptForReason(state, mode);
                break;
            case PRIMARY_CLICKED:
                onPrimaryClicked(state, mode);
                break;
            case SECONDARY_CLICKED:
                onSecondaryClicked(state);
                break;
            case REASON_PICKED:
                if (intent.reason != null && !intent.reason.trim().isEmpty()) {
                    effects.setValue(TimerEffect.deliverSetReason(intent.reason.trim()));
                }
                markPrompted(state);
                break;
            case REASON_SKIPPED:
            case DIALOG_DISMISSED_AS_SKIP:
                effects.setValue(TimerEffect.deliverSetReason(
                        application.getString(R.string.timer_pause_reason_unfilled)));
                markPrompted(state);
                break;
            case REASON_QUICK_RESUME:
                effects.setValue(TimerEffect.deliver(TimerService.ACTION_RESUME_WITH_QUICK_REASON));
                markPrompted(state);
                break;
            case CONFIRM_STOP:
                effects.setValue(TimerEffect.finishSession(
                        TimerService.ACTION_RESET, R.string.timer_toast_timer_ended));
                break;
            case CONFIRM_END_BREAK:
                effects.setValue(TimerEffect.finishSession(
                        TimerService.ACTION_END_BREAK, R.string.timer_toast_break_ended));
                break;
            case CONFIRM_EXIT_RESET:
                effects.setValue(TimerEffect.finishSession(
                        TimerService.ACTION_RESET, R.string.timer_toast_activity_ended));
                break;
            default:
                break;
        }
    }

    private void onPrimaryClicked(@Nullable TimerUiState state, PauseReasonPromptMode mode) {
        if (state == null) {
            return;
        }
        if (state.awaitingPostBreakChoice) {
            effects.setValue(TimerEffect.deliver(TimerService.ACTION_START));
            return;
        }
        if (state.running && state.isStudySession()) {
            if (!state.canPause) {
                effects.setValue(TimerEffect.showToast(R.string.timer_toast_max_pause_reached));
                return;
            }
            pendingPromptAfterPause = mode.shouldPrompt();
            effects.setValue(TimerEffect.deliver(TimerService.ACTION_PAUSE));
            return;
        }
        if (state.paused) {
            effects.setValue(TimerEffect.deliver(TimerService.ACTION_RESUME));
            return;
        }
        if (!state.running && !state.paused && !state.awaitingPostBreakChoice) {
            effects.setValue(TimerEffect.deliver(TimerService.ACTION_START));
        }
    }

    private void onSecondaryClicked(@Nullable TimerUiState state) {
        if (state == null) {
            return;
        }
        if (state.isBreakSession() && state.running) {
            effects.setValue(TimerEffect.showEndBreakConfirm());
            return;
        }
        if (state.awaitingPostBreakChoice) {
            effects.setValue(TimerEffect.finishSession(
                    TimerService.ACTION_RESET, R.string.timer_toast_activity_ended));
            return;
        }
        effects.setValue(TimerEffect.showStopConfirm());
    }

    private void onTimerState(@Nullable TimerUiState state) {
        screenState.postValue(TimerScreenUiState.from(state, this::formatTime, application));
        boolean paused = state != null && state.paused;
        if (wasPaused && !paused) {
            effects.postValue(TimerEffect.dismissPauseReasonDialog());
            promptedForPauseCount = -1;
        }
        wasPaused = paused;
        if (paused && pendingPromptAfterPause) {
            pendingPromptAfterPause = false;
            maybePromptForReason(state, timerSettings.getPauseReasonPromptMode());
        }
    }

    private void maybePromptForReason(@Nullable TimerUiState state, PauseReasonPromptMode mode) {
        if (state == null || !state.paused) {
            return;
        }
        if (!PauseReasonPolicy.shouldAutoPrompt(mode, true, state.pauseReasonSettled)) {
            return;
        }
        if (state.pauseCount > 0 && promptedForPauseCount == state.pauseCount) {
            return;
        }
        promptedForPauseCount = state.pauseCount;
        effects.setValue(TimerEffect.showPauseReasonDialog(mode));
    }

    private void markPrompted(@Nullable TimerUiState state) {
        if (state != null && state.pauseCount > 0) {
            promptedForPauseCount = state.pauseCount;
        }
        pendingPromptAfterPause = false;
    }

    public String formatTime(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        if (totalSeconds >= 3600L) {
            long hours = totalSeconds / 3600L;
            long minutes = (totalSeconds % 3600L) / 60L;
            long seconds = totalSeconds % 60L;
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    public int resolveSessionHintResId(TimerUiState state) {
        if (state == null) {
            return 0;
        }
        if (state.awaitingPostBreakChoice) {
            return R.string.timer_post_break_hint;
        }
        if (state.isBreakSession() && state.running) {
            return state.longBreak
                    ? R.string.timer_long_break_hint
                    : R.string.timer_break_running_hint;
        }
        return 0;
    }

    public boolean shouldShowBreakEndButton(TimerUiState state) {
        return state != null && state.isBreakSession() && state.running;
    }

    public boolean shouldShowPostBreakActions(TimerUiState state) {
        return state != null && state.awaitingPostBreakChoice;
    }

    public boolean shouldShowIdleStudyControls(TimerUiState state) {
        return state != null && !state.running && !state.paused
                && !state.awaitingPostBreakChoice && state.isStudySession();
    }

    public boolean shouldConfirmExit(TimerUiState state) {
        return state != null && state.running && state.isStudySession();
    }

    @Override
    protected void onCleared() {
        timerStateRepository.getState().removeObserver(timerObserver);
        super.onCleared();
    }
}
