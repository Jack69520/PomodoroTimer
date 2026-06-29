package com.skyinit.pomodorotimer.data.repository;

import androidx.lifecycle.LiveData;

import com.skyinit.pomodorotimer.data.dao.PomodoroSessionDao;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.util.AppExecutors;

import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 专注会话数据访问层，供日历与详情页 ViewModel 使用。
 */
public class SessionRepository {

    public interface HighlightDatesCallback {
        void onDatesLoaded(Set<String> dates);
    }

    private final PomodoroSessionDao sessionDao;
    private final AccountManager accountManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SessionRepository(PomodoroSessionDao sessionDao, AccountManager accountManager) {
        this.sessionDao = sessionDao;
        this.accountManager = accountManager;
    }

    public String getCurrentUserId() {
        return accountManager.requireActiveUserId();
    }

    public boolean hasActiveProfile() {
        return accountManager.hasActiveProfile();
    }

    public LiveData<PomodoroSession> observeSession(int sessionId) {
        return sessionDao.getSessionById(sessionId);
    }

    public LiveData<List<PomodoroSession>> observeSessionsForDay(long dayStartMillis, long dayEndMillis) {
        return sessionDao.getSessionsByDateRange(
                accountManager.requireActiveUserId(), dayStartMillis, dayEndMillis);
    }

    public void loadHighlightedDates(int year, int month, HighlightDatesCallback callback) {
        String userId = accountManager.requireActiveUserId();
        Calendar start = Calendar.getInstance();
        start.set(year, month, 1, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        AppExecutors.getInstance().diskIo(() -> {
            List<Long> startTimes = sessionDao.getSessionStartTimesInRangeSync(
                    userId, start.getTimeInMillis(), end.getTimeInMillis());
            Set<String> dates = new HashSet<>();
            SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            if (startTimes != null) {
                for (Long time : startTimes) {
                    if (time != null) {
                        dates.add(keyFormat.format(new Date(time)));
                    }
                }
            }
            if (callback != null) {
                mainHandler.post(() -> callback.onDatesLoaded(dates));
            }
        });
    }

    public static long[] dayRangeMillis(Calendar date) {
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.setTime(date.getTime());
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = Calendar.getInstance();
        endOfDay.setTime(date.getTime());
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        return new long[]{startOfDay.getTimeInMillis(), endOfDay.getTimeInMillis()};
    }
}
