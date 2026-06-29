package com.skyinit.pomodorotimer.data.repository;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.PomodoroSessionDao;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.ui.statistics.DailyStats;
import com.skyinit.pomodorotimer.ui.statistics.HourlyStats;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.R;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.Calendar;

/**
 * @deprecated 请使用 {@link StatisticsRepository}。
 */
@Deprecated
public class StatisticsManager {
    private final PomodoroSessionDao sessionDao;
    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public StatisticsManager(Context context) {
        this.appContext = context.getApplicationContext();
        AppDatabase database = AppDatabase.getDatabase(appContext);
        sessionDao = database.pomodoroSessionDao();
    }

    public void recordSession(String userId, long startTime, long duration, int taskId, String category, String tags) {
        AppExecutors.getInstance().diskIo(() -> {
            try {
                PomodoroSession session = new PomodoroSession();
                session.userId = userId;
                session.startTime = startTime;
                session.endTime = startTime + duration;
                session.duration = duration;
                session.completed = true;
                session.taskId = taskId;
                session.category = category;
                session.tags = tags;
                sessionDao.insert(session);
                AppLog.d("StatisticsManager", "Session recorded, duration: " + duration);
            } catch (Exception e) {
                AppLog.e("StatisticsManager", "Failed to record session", e);
            }
        });
    }

    public void recordPausedSession(long startTime, long duration, String pauseReason, int taskId, String category) {
        AppExecutors.getInstance().diskIo(() -> {
            PomodoroSession session = new PomodoroSession();
            session.startTime = startTime;
            session.endTime = startTime + duration;
            session.duration = duration;
            session.completed = false;
            session.pauseReason = pauseReason;
            session.taskId = taskId;
            session.category = category;
            sessionDao.insert(session);
        });
    }

    public interface TotalCountCallback {
        void onCountReceived(int count);
    }

    /** 从 Room 聚合累计完成次数（Profile 唯一数据源）。 */
    public void getTotalCompletedCount(String userId, TotalCountCallback callback) {
        if (userId == null) {
            if (callback != null) {
                mainHandler.post(() -> callback.onCountReceived(0));
            }
            return;
        }
        AppExecutors.getInstance().diskIo(() -> {
            int count = sessionDao.getTotalCompletedCountForUser(userId);
            if (callback != null) {
                mainHandler.post(() -> callback.onCountReceived(count));
            }
        });
    }

    public void getTodayStats(StatsCallback callback) {
        AppExecutors.getInstance().diskIo(() -> {
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long startOfDay = calendar.getTimeInMillis();

                calendar.add(Calendar.DAY_OF_MONTH, 1);
                long endOfDay = calendar.getTimeInMillis();

                int count = sessionDao.getCompletedCountInRange(startOfDay, endOfDay);
                long duration = sessionDao.getTotalDurationInRange(startOfDay, endOfDay);
                DailyStats stats = new DailyStats(
                        appContext.getString(R.string.statistics_label_today), count, duration);
                if (callback != null) {
                    mainHandler.post(() -> callback.onStatsReceived(stats));
                }
            } catch (Exception e) {
                AppLog.e("StatisticsManager", "Failed to get today stats", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onStatsReceived(new DailyStats(
                            appContext.getString(R.string.statistics_label_today), 0, 0)));
                }
            }
        });
    }

    public void getThisWeekStats(WeeklyStatsCallback callback) {
        AppExecutors.getInstance().diskIo(() -> {
            String userId = AccountManager.getInstance(appContext).getCurrentUserId();
            if (userId == null) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onWeeklyStatsReceived(new WeeklyStats(0, 0)));
                }
                return;
            }
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
            WeeklyStats stats = new WeeklyStats(count, duration);
            if (callback != null) {
                mainHandler.post(() -> callback.onWeeklyStatsReceived(stats));
            }
        });
    }

    public void getThisMonthStats(MonthlyStatsCallback callback) {
        AppExecutors.getInstance().diskIo(() -> {
            String userId = AccountManager.getInstance(appContext).getCurrentUserId();
            if (userId == null) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onMonthlyStatsReceived(new MonthlyStats(0, 0)));
                }
                return;
            }
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
            MonthlyStats stats = new MonthlyStats(count, duration);
            if (callback != null) {
                mainHandler.post(() -> callback.onMonthlyStatsReceived(stats));
            }
        });
    }

    public void getWeeklyChartData(ChartDataCallback callback) {
        Calendar calendar = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        long endTime = end.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_MONTH, -6);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();

        String userId = AccountManager.getInstance(appContext).getCurrentUserId();
        if (userId == null) {
            if (callback != null) {
                mainHandler.post(() -> callback.onDataReceived(new java.util.ArrayList<>()));
            }
            return;
        }
        sessionDao.getDailyStatsInRange(userId, startTime, endTime).observeForever(dailyStatsList -> {
            if (callback != null) {
                mainHandler.post(() -> callback.onDataReceived(dailyStatsList));
            }
        });
    }

    public void getMonthlyChartData(ChartDataCallback callback) {
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

        String userId = AccountManager.getInstance(appContext).getCurrentUserId();
        if (userId == null) {
            if (callback != null) {
                mainHandler.post(() -> callback.onDataReceived(new java.util.ArrayList<>()));
            }
            return;
        }
        sessionDao.getDailyStatsInRange(userId, startTime, endTime).observeForever(dailyStatsList -> {
            if (callback != null) {
                mainHandler.post(() -> callback.onDataReceived(dailyStatsList));
            }
        });
    }

    public void getMonthlyHourlyDistribution(HourlyDistributionCallback callback) {
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

        String userId = AccountManager.getInstance(appContext).getCurrentUserId();
        if (userId == null) {
            if (callback != null) {
                mainHandler.post(() -> callback.onHourlyDataReceived(new java.util.ArrayList<>()));
            }
            return;
        }

        sessionDao.getHourlyStatsInRange(userId, startTime, endTime).observeForever(hourlyStatsList -> {
            if (callback != null) {
                mainHandler.post(() -> callback.onHourlyDataReceived(hourlyStatsList));
            }
        });
    }

    public interface ChartDataCallback {
        void onDataReceived(java.util.List<DailyStats> dailyStatsList);
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
        void onHourlyDataReceived(java.util.List<HourlyStats> hourlyStatsList);
    }

    public static class WeeklyStats {
        public int totalSessions;
        public long totalDuration;

        public WeeklyStats(int totalSessions, long totalDuration) {
            this.totalSessions = totalSessions;
            this.totalDuration = totalDuration;
        }
    }

    public static class MonthlyStats {
        public int totalSessions;
        public long totalDuration;

        public MonthlyStats(int totalSessions, long totalDuration) {
            this.totalSessions = totalSessions;
            this.totalDuration = totalDuration;
        }
    }
}
