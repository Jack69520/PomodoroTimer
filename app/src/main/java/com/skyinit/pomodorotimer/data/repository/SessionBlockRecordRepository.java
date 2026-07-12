package com.skyinit.pomodorotimer.data.repository;

import androidx.lifecycle.LiveData;

import com.skyinit.pomodorotimer.data.dao.SessionAppBlockRecordDao;
import com.skyinit.pomodorotimer.data.entity.SessionAppBlockRecord;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;

import java.util.List;

/**
 * 专注期间应用拦截记录的读写仓库。
 */
public final class SessionBlockRecordRepository {

    private static final String TAG = "SessionBlockRecordRepo";

    private final SessionAppBlockRecordDao dao;

    public SessionBlockRecordRepository(SessionAppBlockRecordDao dao) {
        this.dao = dao;
    }

    /** 仅后台线程调用。 */
    public int getCountSync(String userId, long sessionStartTime) {
        if (userId == null || userId.isEmpty() || sessionStartTime <= 0L) {
            return 0;
        }
        return dao.getCountBySession(userId, sessionStartTime);
    }

    public LiveData<List<SessionAppBlockRecord>> observeRecords(String userId, long sessionStartTime) {
        return dao.observeBySession(userId, sessionStartTime);
    }

    public List<SessionAppBlockRecord> getRecordsSync(String userId, long sessionStartTime) {
        return dao.getBySessionSync(userId, sessionStartTime);
    }

    /** 后台线程调用：记录一次拦截。 */
    public void recordBlockSync(String userId, long sessionStartTime,
                                String packageName, String appName, long blockTimeMillis) {
        if (userId == null || userId.isEmpty() || sessionStartTime <= 0L) {
            return;
        }
        try {
            int nextSequence = dao.getCountBySession(userId, sessionStartTime) + 1;
            SessionAppBlockRecord record = new SessionAppBlockRecord();
            record.userId = userId;
            record.sessionStartTime = sessionStartTime;
            record.sequenceNumber = nextSequence;
            record.appPackageName = packageName != null ? packageName : "";
            record.appName = appName != null ? appName : packageName;
            record.blockTimeMillis = blockTimeMillis;
            dao.insert(record);
        } catch (Exception e) {
            AppLog.e(TAG, "Failed to record block event", e);
        }
    }

    public void deleteSessionRecords(String userId, long sessionStartTime) {
        if (userId == null || userId.isEmpty() || sessionStartTime <= 0L) {
            return;
        }
        AppExecutors.getInstance().diskIo(() -> {
            try {
                dao.deleteBySession(userId, sessionStartTime);
            } catch (Exception e) {
                AppLog.e(TAG, "Failed to delete session block records", e);
            }
        });
    }

    /** 仅后台线程调用。 */
    public void deleteSessionRecordsSync(String userId, long sessionStartTime) {
        if (userId != null && !userId.isEmpty() && sessionStartTime > 0L) {
            dao.deleteBySession(userId, sessionStartTime);
        }
    }
}
