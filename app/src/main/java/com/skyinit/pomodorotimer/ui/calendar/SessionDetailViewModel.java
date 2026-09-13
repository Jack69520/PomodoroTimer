package com.skyinit.pomodorotimer.ui.calendar;

import android.app.Application;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.data.repository.SessionRepository;
import com.skyinit.pomodorotimer.data.repository.StatisticsRepository;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.util.SessionPauseUtils;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 记录详情页 ViewModel（MVI）：Intent → 状态归约 → Effect。
 * Room 观察经 Mediator 切换并校验归属；保存用 generation 丢弃过期回调。
 */
public class SessionDetailViewModel extends ViewModel {

    static final int MAX_NOTES_LENGTH = 200;

    private final Application application;
    private final SessionRepository sessionRepository;
    private final StatisticsRepository statisticsRepository;
    private final UserSessionRepository userSessionRepository;

    private final MediatorLiveData<SessionDetailUiState> uiState = new MediatorLiveData<>();
    private final SingleLiveEvent<SessionDetailEffect> effects = new SingleLiveEvent<>();

    private final AtomicInteger saveGeneration = new AtomicInteger(0);
    private final AtomicBoolean navigateBackIssued = new AtomicBoolean(false);

    @Nullable
    private LiveData<PomodoroSession> activeSessionSource;
    private int loadedSessionId = -1;
    @Nullable
    private PomodoroSession currentSession;
    private boolean saving;

    public SessionDetailViewModel(@NonNull Application application,
                                  SessionRepository sessionRepository,
                                  StatisticsRepository statisticsRepository,
                                  UserSessionRepository userSessionRepository) {
        this.application = application;
        this.sessionRepository = sessionRepository;
        this.statisticsRepository = statisticsRepository;
        this.userSessionRepository = userSessionRepository;
        uiState.setValue(SessionDetailUiState.loading());
    }

    @NonNull
    public LiveData<SessionDetailUiState> getUiState() {
        return uiState;
    }

    @NonNull
    public LiveData<SessionDetailEffect> getEffects() {
        return effects;
    }

    @MainThread
    public void dispatch(@NonNull SessionDetailIntent intent) {
        switch (intent.type) {
            case LOAD:
                loadSession(intent.sessionId);
                break;
            case SAVE_NOTES:
                saveNotes(intent.notes);
                break;
            case OPEN_BLOCK_RECORDS:
                openBlockRecords();
                break;
            default:
                break;
        }
    }

    @MainThread
    private void loadSession(int id) {
        if (id <= 0) {
            denyAccess(R.string.session_detail_not_found);
            return;
        }
        if (id == loadedSessionId && activeSessionSource != null) {
            return;
        }
        navigateBackIssued.set(false);
        loadedSessionId = id;
        currentSession = null;
        saving = false;
        uiState.setValue(SessionDetailUiState.loading());

        if (activeSessionSource != null) {
            uiState.removeSource(activeSessionSource);
            activeSessionSource = null;
        }

        LiveData<PomodoroSession> source = sessionRepository.observeSession(id);
        activeSessionSource = source;
        uiState.addSource(source, this::onSessionEmitted);
    }

    @MainThread
    private void onSessionEmitted(@Nullable PomodoroSession loaded) {
        if (navigateBackIssued.get()) {
            return;
        }
        if (loaded == null) {
            denyAccess(R.string.session_detail_not_found);
            return;
        }
        if (loaded.id != loadedSessionId) {
            return;
        }
        String userId;
        try {
            userId = userSessionRepository.requireActiveUserId();
        } catch (RuntimeException e) {
            denyAccess(R.string.session_detail_not_found);
            return;
        }
        if (userId == null || !userId.equals(loaded.userId)) {
            denyAccess(R.string.session_detail_not_found);
            return;
        }
        currentSession = loaded;
        publishUiState();
    }

    @MainThread
    private void saveNotes(@Nullable String rawNotes) {
        PomodoroSession session = currentSession;
        if (session == null || saving) {
            return;
        }
        String notes = rawNotes != null ? rawNotes.trim() : "";
        if (notes.length() > MAX_NOTES_LENGTH) {
            effects.setValue(SessionDetailEffect.showToast(R.string.session_detail_notes_too_long));
            return;
        }
        final int generation = saveGeneration.incrementAndGet();
        final int sessionId = session.id;
        saving = true;
        publishUiState();

        statisticsRepository.updateSessionNotes(sessionId, notes, () -> {
            if (generation != saveGeneration.get()) {
                return;
            }
            saving = false;
            if (currentSession != null && currentSession.id == sessionId) {
                currentSession.notes = notes;
            }
            publishUiState();
            effects.setValue(SessionDetailEffect.showToast(R.string.session_detail_notes_saved));
        });
    }

    @MainThread
    private void openBlockRecords() {
        PomodoroSession session = currentSession;
        if (session == null || session.startTime <= 0L) {
            effects.setValue(SessionDetailEffect.showToast(R.string.session_detail_not_found));
            return;
        }
        effects.setValue(SessionDetailEffect.openBlockRecords(session.startTime, session.endTime));
    }

    @MainThread
    private void publishUiState() {
        PomodoroSession session = currentSession;
        if (session == null) {
            uiState.setValue(SessionDetailUiState.loading());
            return;
        }
        boolean hasPause = SessionPauseUtils.hasPause(
                session.pauseCount, session.pauseReasons, session.pauseReason);
        List<String> reasons = new ArrayList<>(
                SessionPauseUtils.decodeReasons(session.pauseReasons, session.pauseReason));
        int displayPauseCount = session.pauseCount;
        if (displayPauseCount <= 0 && hasPause) {
            displayPauseCount = reasons.size();
        }
        String timeoutLabel = application.getString(R.string.timer_pause_reason_timeout);
        SessionDetailUiState.CompletionStatus status;
        if (session.completed) {
            status = SessionDetailUiState.CompletionStatus.COMPLETED;
        } else if (SessionPauseUtils.isTimeoutFailure(
                session.pauseReason, session.pauseReasons, timeoutLabel)) {
            status = SessionDetailUiState.CompletionStatus.FAILED;
        } else {
            status = SessionDetailUiState.CompletionStatus.INCOMPLETE;
        }
        boolean earlyEnd = session.earlyEnd || !session.completed;
        int blockCount = Math.max(0, session.blockEventCount);
        uiState.setValue(SessionDetailUiState.of(
                session.startTime,
                session.endTime,
                session.duration,
                session.category,
                status,
                hasPause,
                displayPauseCount,
                reasons,
                earlyEnd,
                blockCount,
                encouragementForBlockCount(blockCount),
                session.notes,
                saving));
    }

    @NonNull
    private String encouragementForBlockCount(int count) {
        if (count <= 0) {
            return application.getString(R.string.session_block_encourage_zero);
        }
        if (count <= 5) {
            return application.getString(R.string.session_block_encourage_low);
        }
        return application.getString(R.string.session_block_encourage_high);
    }

    @MainThread
    private void denyAccess(@androidx.annotation.StringRes int toastRes) {
        if (!navigateBackIssued.compareAndSet(false, true)) {
            return;
        }
        currentSession = null;
        saving = false;
        effects.setValue(SessionDetailEffect.navigateBack(toastRes));
    }

    @Override
    protected void onCleared() {
        saveGeneration.incrementAndGet();
        if (activeSessionSource != null) {
            uiState.removeSource(activeSessionSource);
            activeSessionSource = null;
        }
        super.onCleared();
    }
}
