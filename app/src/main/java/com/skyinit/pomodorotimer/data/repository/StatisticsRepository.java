package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.PomodoroSessionDao;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.ui.statistics.DailyStats;
import com.skyinit.pomodorotimer.ui.statistics.HourlyStats;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.SessionPauseUtils;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * 统计数据仓库：所有查询均按当前登录用户过滤。
 */
public class StatisticsRepository {

    private static final int MAX_NOTES_LENGTH = 200;

    private final PomodoroSessionDao sessionDao;
    private final AccountManager accountManager;
    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public StatisticsRepository(Context context, AccountManager accountManager) {
        appContext = context.getApplicationContext();
        AppDatabase database = AppDatabase.getDatabase(appContext);
        sessionDao = database.pomodoroSessionDao();
        this.accountManager = accountManager;
    }

    public void recordSession(String userId, long startTime, long duration,
                              int taskId, String category, String tags) {
        recordSession(userId, startTime, duration, taskId, -1, category, tags, 0, null, false, null);
    }

    public void recordSession(String userId, long startTime, long duration,
                              int taskId, String category, String tags,
                              int pauseCount, List<String> pauseReasonList,
                              boolean earlyEnd, String notes) {
        recordSession(userId, startTime, duration, taskId, -1, category, tags,
                pauseCount, pauseReasonList, earlyEnd, notes);
    }

    public void recordSession(String userId, long startTime, long duration,
                              int taskId, int subTaskId, String category, String tags,
                              int pauseCount, List<String> pauseReasonList,
                              boolean earlyEnd, String notes) {
        AppExecutors.getInstance().diskIo(() -> {
            try {
                PomodoroSession session = new PomodoroSession();
                session.userId = userId;
                session.startTime = startTime;
                session.endTime = startTime + duration;
                session.duration = duration;
                session.completed = true;
                session.taskId = taskId;
                session.subTaskId = subTaskId;
                session.category = category;
                session.tags = tags;
                session.pauseCount = Math.max(0, pauseCount);
                session.pauseReasons = SessionPauseUtils.encodeReasons(pauseReasonList);
                session.pauseReason = session.pauseReasons != null && pauseReasonList != null && !pauseReasonList.isEmpty()
                        ? pauseReasonList.get(pauseReasonList.size() - 1)
                        : null;
                session.earlyEnd = earlyEnd;
                session.notes = notes;
                sessionDao.insert(session);
            } catch (Exception e) {
                AppLog.e("StatisticsRepository", "Failed to record session", e);
            }
        });
    }

    public void recordPausedSession(String userId, long startTime, long duration,
                                    String pauseReason, int taskId, String category) {
        recordPausedSession(userId, startTime, duration, pauseReason, taskId, category, 1,
                pauseReason != null ? Collections.singletonList(pauseReason) : null, true, null);
    }

    public void recordPausedSession(String userId, long startTime, long duration,
                                    String pauseReason, int taskId, String category,
                                    int pauseCount, List<String> pauseReasonList,
                                    boolean earlyEnd, String notes) {
        AppExecutors.getInstance().diskIo(() -> {
            PomodoroSession session = new PomodoroSession();
            session.userId = userId;
            session.startTime = startTime;
            session.endTime = startTime + duration;
            session.duration = duration;
            session.completed = false;
            session.pauseReason = pauseReason;
            session.pauseCount = Math.max(0, pauseCount);
            session.pauseReasons = SessionPauseUtils.encodeReasons(pauseReasonList);
            if (session.pauseReasons == null && pauseReason != null) {
                session.pauseReasons = pauseReason;
            }
            session.earlyEnd = earlyEnd;
            session.notes = notes;
            session.taskId = taskId;
            session.category = category;
            sessionDao.insert(session);
        });
    }

    public void updateSessionNotes(int sessionId, String notes, Runnable onComplete) {
        String sanitizedNotes = notes;
        if (sanitizedNotes != null && sanitizedNotes.length() > MAX_NOTES_LENGTH) {
            sanitizedNotes = sanitizedNotes.substring(0, MAX_NOTES_LENGTH);
        }
        final String notesToSave = sanitizedNotes;
        AppExecutors.getInstance().diskIo(() -> {
            PomodoroSession session = sessionDao.getSessionByIdSync(sessionId);
            if (session != null) {
                session.notes = notesToSave;
                sessionDao.update(session);
            }
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    public void getTodayStats(StatsCallback callback) {
        queryRangeStats(callback, appContext.getString(R.string.statistics_label_today), () -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startOfDay = calendar.getTimeInMillis();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            long endOfDay = calendar.getTimeInMillis();
            return new long[]{startOfDay, endOfDay};
        });
    }

    public void getThisWeekStats(WeeklyStatsCallback callback) {
        String userId = accountManager.requireActiveUserId();
        AppExecutors.getInstance().diskIo(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startOfWeek = calendar.getTimeInMillis();
            calendar.add(Calendar.DAY_OF_WEEK, 7);
            long endOfWeek = calendar.getTimeInMillis();
            int count = sessionDao.getCompletedCountInRangeForUser(userId, startOfWeek, endOfWeek);
            long duration = sessionDao.getTotalDurationInRangeForUser(userId, startOfWeek, endOfWeek);
            postWeekly(callback, new WeeklyStats(count, duration));
        });
    }

    public void getThisMonthStats(MonthlyStatsCallback callback) {
        String userId = accountManager.requireActiveUserId();
        AppExecutors.getInstance().diskIo(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startOfMonth = calendar.getTimeInMillis();
            calendar.add(Calendar.MONTH, 1);
            long endOfMonth = calendar.getTimeInMillis();
            int count = sessionDao.getCompletedCountInRangeForUser(userId, startOfMonth, endOfMonth);
            long duration = sessionDao.getTotalDurationInRangeForUser(userId, startOfMonth, endOfMonth);
            postMonthly(callback, new MonthlyStats(count, duration));
        });
    }

    public void getWeeklyChartData(ChartDataCallback callback) {
        queryChartData(callback, -6, 0);
    }

    public void getMonthlyChartData(ChartDataCallback callback) {
        String userId = accountManager.requireActiveUserId();
        AppExecutors.getInstance().diskIo(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startTime = calendar.getTimeInMillis();

            Calendar end = Calendar.getInstance();
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);
            end.set(Calendar.MILLISECOND, 999);
            long endTime = end.getTimeInMillis();

            List<DailyStats> data = sessionDao.getDailyStatsInRangeSync(userId, startTime, endTime);
            postChart(callback, data);
        });
    }

    public void getMonthlyHourlyDistribution(HourlyDistributionCallback callback) {
        String userId = accountManager.requireActiveUserId();
        AppExecutors.getInstance().diskIo(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startTime = calendar.getTimeInMillis();

            Calendar end = Calendar.getInstance();
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);
            end.set(Calendar.MILLISECOND, 999);
            long endTime = end.getTimeInMillis();

            List<HourlyStats> data = sessionDao.getHourlyStatsInRangeSync(userId, startTime, endTime);
            postHourly(callback, data);
        });
    }

    public void getMonthlyCategoryStats(CategoryStatsCallback callback) {
        String userId = accountManager.requireActiveUserId();
        AppExecutors.getInstance().diskIo(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startTime = calendar.getTimeInMillis();

            Calendar end = Calendar.getInstance();
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);
            end.set(Calendar.MILLISECOND, 999);
            long endTime = end.getTimeInMillis();

            List<com.skyinit.pomodorotimer.ui.statistics.CategoryStats> data =
                    sessionDao.getCategoryStatsSync(userId, startTime, endTime);
            postCategoryStats(callback, data);
        });
    }

    public void getMonthlyPauseReasonStats(PauseReasonStatsCallback callback) {
        String userId = accountManager.requireActiveUserId();
        AppExecutors.getInstance().diskIo(() -> {
            long[] range = monthRange();
            List<com.skyinit.pomodorotimer.ui.statistics.PauseReasonStats> data =
                    sessionDao.getPauseReasonStatsSync(userId, range[0], range[1]);
            postPauseReasonStats(callback, data);
        });
    }

    private long[] monthRange() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();

        Calendar end = Calendar.getInstance();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        return new long[]{startTime, end.getTimeInMillis()};
    }

    public void getSessionsByCategoryThisMonth(String category, SessionsCallback callback) {
        String userId = accountManager.requireActiveUserId();
        AppExecutors.getInstance().diskIo(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startTime = calendar.getTimeInMillis();

            Calendar end = Calendar.getInstance();
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);
            end.set(Calendar.MILLISECOND, 999);
            long endTime = end.getTimeInMillis();

            List<PomodoroSession> sessions = sessionDao.getSessionsByCategoryAndDateRangeSync(
                    userId, startTime, endTime, category);
            postSessions(callback, sessions);
        });
    }

    public void getTotalCompletedCount(TotalCountCallback callback) {
        String userId = accountManager.requireActiveUserId();
        AppExecutors.getInstance().diskIo(() -> {
            int count = sessionDao.getTotalCompletedCountForUser(userId);
            mainHandler.post(() -> callback.onCountReceived(count));
        });
    }

    private interface RangeSupplier {
        long[] getRange();
    }

    private void queryRangeStats(StatsCallback callback, String label, RangeSupplier supplier) {
        String userId = accountManager.requireActiveUserId();
        AppExecutors.getInstance().diskIo(() -> {
            try {
                long[] range = supplier.getRange();
                int count = sessionDao.getCompletedCountInRangeForUser(userId, range[0], range[1]);
                long duration = sessionDao.getTotalDurationInRangeForUser(userId, range[0], range[1]);
                mainHandler.post(() -> callback.onStatsReceived(new DailyStats(label, count, duration)));
            } catch (Exception e) {
                AppLog.e("StatisticsRepository", "Failed to query stats", e);
                mainHandler.post(() -> callback.onStatsReceived(new DailyStats(label, 0, 0)));
            }
        });
    }

    private void queryChartData(ChartDataCallback callback, int dayOffsetStart, int dayOffsetEnd) {
        String userId = accountManager.requireActiveUserId();
        AppExecutors.getInstance().diskIo(() -> {
            Calendar end = Calendar.getInstance();
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);
            end.set(Calendar.MILLISECOND, 999);
            long endTime = end.getTimeInMillis();

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, dayOffsetStart);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startTime = calendar.getTimeInMillis();

            List<DailyStats> data = sessionDao.getDailyStatsInRangeSync(userId, startTime, endTime);
            postChart(callback, data);
        });
    }

    private void postWeekly(WeeklyStatsCallback callback, WeeklyStats stats) {
        if (callback != null) {
            mainHandler.post(() -> callback.onWeeklyStatsReceived(stats));
        }
    }

    private void postMonthly(MonthlyStatsCallback callback, MonthlyStats stats) {
        if (callback != null) {
            mainHandler.post(() -> callback.onMonthlyStatsReceived(stats));
        }
    }

    private void postChart(ChartDataCallback callback, List<DailyStats> data) {
        if (callback != null) {
            mainHandler.post(() -> callback.onDataReceived(data));
        }
    }

    private void postHourly(HourlyDistributionCallback callback, List<HourlyStats> data) {
        if (callback != null) {
            mainHandler.post(() -> callback.onHourlyDataReceived(data));
        }
    }

    private void postCategoryStats(CategoryStatsCallback callback,
                                   List<com.skyinit.pomodorotimer.ui.statistics.CategoryStats> data) {
        if (callback != null) {
            mainHandler.post(() -> callback.onCategoryStatsReceived(data));
        }
    }

    private void postPauseReasonStats(PauseReasonStatsCallback callback,
                                      List<com.skyinit.pomodorotimer.ui.statistics.PauseReasonStats> data) {
        if (callback != null) {
            mainHandler.post(() -> callback.onPauseReasonStatsReceived(data));
        }
    }

    private void postSessions(SessionsCallback callback, List<PomodoroSession> data) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSessionsReceived(data));
        }
    }

    public interface TotalCountCallback {
        void onCountReceived(int count);
    }

    public interface ChartDataCallback {
        void onDataReceived(List<DailyStats> dailyStatsList);
    }

    public interface StatsCallback {
        void onStatsReceived(DailyStats stats);
    }

    public interface WeeklyStatsCallback {
        void onWeeklyStatsReceived(WeeklyStats stats);
    }

    public interface MonthlyStatsCallback {
        void onMonthlyStatsReceived(MonthlyStats stats);
    }

    public interface HourlyDistributionCallback {
        void onHourlyDataReceived(List<HourlyStats> hourlyStatsList);
    }

    public interface CategoryStatsCallback {
        void onCategoryStatsReceived(List<com.skyinit.pomodorotimer.ui.statistics.CategoryStats> stats);
    }

    public interface PauseReasonStatsCallback {
        void onPauseReasonStatsReceived(List<com.skyinit.pomodorotimer.ui.statistics.PauseReasonStats> stats);
    }

    public interface SessionsCallback {
        void onSessionsReceived(List<PomodoroSession> sessions);
    }

    public static class WeeklyStats {
        public final int totalSessions;
        public final long totalDuration;

        public WeeklyStats(int totalSessions, long totalDuration) {
            this.totalSessions = totalSessions;
            this.totalDuration = totalDuration;
        }
    }

    public static class MonthlyStats {
        public final int totalSessions;
        public final long totalDuration;

        public MonthlyStats(int totalSessions, long totalDuration) {
            this.totalSessions = totalSessions;
            this.totalDuration = totalDuration;
        }
    }
}
