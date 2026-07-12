package com.skyinit.pomodorotimer.ui.calendar;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.data.entity.SessionAppBlockRecord;
import com.skyinit.pomodorotimer.data.repository.SessionBlockRecordRepository;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次番茄计时的应用拦截记录页 ViewModel。
 */
public class SessionBlockRecordsViewModel extends ViewModel {

    public static final class UiState {
        public final long sessionStartTime;
        public final long sessionEndTime;
        public final List<SessionAppBlockRecord> records;
        public final int blockCount;
        public final String encouragementMessage;

        public UiState(long sessionStartTime,
                       long sessionEndTime,
                       List<SessionAppBlockRecord> records,
                       int blockCount,
                       String encouragementMessage) {
            this.sessionStartTime = sessionStartTime;
            this.sessionEndTime = sessionEndTime;
            this.records = records;
            this.blockCount = blockCount;
            this.encouragementMessage = encouragementMessage;
        }
    }

    private final SessionBlockRecordRepository blockRecordRepository;
    private final UserSessionRepository userSessionRepository;
    private final AppExecutors executors = AppExecutors.getInstance();

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> accessDenied = new MutableLiveData<>(false);

    public SessionBlockRecordsViewModel(SessionBlockRecordRepository blockRecordRepository,
                                        UserSessionRepository userSessionRepository) {
        this.blockRecordRepository = blockRecordRepository;
        this.userSessionRepository = userSessionRepository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getAccessDenied() {
        return accessDenied;
    }

    public void load(long startTime, long endTime, String encouragementZero,
                     String encouragementLow, String encouragementHigh) {
        if (startTime <= 0L) {
            accessDenied.setValue(true);
            return;
        }
        executors.diskIo(() -> {
            String userId = userSessionRepository.requireActiveUserId();
            List<SessionAppBlockRecord> records =
                    blockRecordRepository.getRecordsSync(userId, startTime);
            List<SessionAppBlockRecord> safeRecords = records != null ? records : new ArrayList<>();
            int count = safeRecords.size();
            String message;
            if (count == 0) {
                message = encouragementZero;
            } else if (count <= 5) {
                message = encouragementLow;
            } else {
                message = encouragementHigh;
            }
            uiState.postValue(new UiState(startTime, endTime, safeRecords, count, message));
        });
    }
}
