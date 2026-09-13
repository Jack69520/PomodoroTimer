package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.PomodoroSessionDao;
import com.skyinit.pomodorotimer.data.dao.SessionAppBlockRecordDao;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.ui.statistics.CategoryStats;
import com.skyinit.pomodorotimer.ui.statistics.DailyStats;
import com.skyinit.pomodorotimer.ui.statistics.HourlyStats;
import com.skyinit.pomodorotimer.ui.statistics.PauseReasonStats;
import com.skyinit.pomodorotimer.ui.statistics.StatisticsDashboard;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.CategoryDefaults;
import com.skyinit.pomodorotimer.util.SessionPauseUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 统计数据仓库：所有查询均按当前登录用户过滤。
 */
public class StatisticsRepository {

    private static final int MAX_NOTES_LENGTH = 200;

    private final PomodoroSessionDao sessionDao;
    private final SessionAppBlockRecordDao blockRecordDao;
    private final AccountManager accountManager;
    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public StatisticsRepository(Context context, AccountManager accountManager) {
        appContext = context.getApplicationContext();
        AppDatabase database = AppDatabase.getDatabase(appContext);
        sessionDao = database.pomodoroSessionDao();
        blockRecordDao = database.sessionAppBlockRecordDao();
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
                deletePauseSnapshotsSync(userId, startTime);
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
                session.blockEventCount = resolveBlockEventCount(userId, startTime);
                sessionDao.insert(session);
            } catch (Exception e) {
                AppLog.e("StatisticsRepository", "Failed to record session", e);
            }
        });
    }

    private int resolveBlockEventCount(String userId, long sessionStartTime) {
        if (userId == null || userId.isEmpty() || sessionStartTime <= 0L) {
            return 0;
        }
        return blockRecordDao.getCountBySession(userId, sessionStartTime);
    }

    /**
     * 记录终态失败会话（如暂停超时），暂停过程中不落库。
     */
    public void recordFailedSession(String userId, long startTime, long duration,
                                    String pauseReason, int taskId, String category,
                                    int pauseCount, List<String> pauseReasonList,
                                    String notes) {
        AppExecutors.getInstance().diskIo(() -> {
            try {
                deletePauseSnapshotsSync(userId, startTime);
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
                session.earlyEnd = true;
                session.notes = notes;
                session.blockEventCount = resolveBlockEventCount(userId, startTime);
                session.taskId = taskId;
                session.category = category;
                sessionDao.insert(session);
            } catch (Exception e) {
                AppLog.e("StatisticsRepository", "Failed to record failed session", e);
            }
        });
    }

    /** 恢复或完成前清理同一轮专注的暂停快照。 */
    public void deletePauseSnapshotsForSession(String userId, long startTime) {
        if (userId == null || userId.isEmpty() || startTime <= 0L) {
            return;
        }
        AppExecutors.getInstance().diskIo(() -> {
            try {
                sessionDao.deleteIncompleteSessionsByStartTime(userId, startTime);
            } catch (Exception e) {
                AppLog.e("StatisticsRepository", "Failed to delete pause snapshots", e);
            }
        });
    }

    private void deletePauseSnapshotsSync(String userId, long startTime) {
        if (userId != null && !userId.isEmpty() && startTime > 0L) {
            sessionDao.deleteIncompleteSessionsByStartTime(userId, startTime);
        }
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
            List<PomodoroSession> sessions = sessionDao.getCompletedSessionsWithPausesInRangeSync(
                    userId, range[0], range[1]);
            Map<String, Integer> reasonCounts = new HashMap<>();
            if (sessions != null) {
                for (PomodoroSession session : sessions) {
                    List<String> reasons = SessionPauseUtils.decodeReasons(
                            session.pauseReasons, session.pauseReason);
                    for (String reason : reasons) {
                        reasonCounts.merge(reason, 1, Integer::sum);
                    }
                }
            }
            List<PauseReasonStats> data = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : reasonCounts.entrySet()) {
                data.add(new PauseReasonStats(entry.getKey(), entry.getValue()));
            }
            data.sort((a, b) -> Integer.compare(b.count, a.count));
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

    /**
     * 「我的」页迷你统计：同一次 diskIo 内读取累计完成次数与累计时长，保证两项一致。
     * 无活跃档案时回调 (0, 0)。回调投递到主线程。
     */
    public void getProfileSummary(@Nullable ProfileSummaryCallback callback) {
        if (callback == null) {
            return;
        }
        if (!accountManager.hasActiveProfile()) {
            mainHandler.post(() -> callback.onSummaryReceived(0, 0L));
            return;
        }
        final String userId = accountManager.requireActiveUserId();
        AppExecutors.getInstance().diskIo(() -> {
            int count = 0;
            long durationMs = 0L;
            try {
                count = sessionDao.getTotalCompletedCountForUser(userId);
                durationMs = Math.max(0L, sessionDao.getTotalCompletedDurationForUser(userId));
            } catch (Exception e) {
                AppLog.e("StatisticsRepository", "Failed to load profile summary", e);
            }
            final int safeCount = Math.max(0, count);
            final long safeDuration = durationMs;
            mainHandler.post(() -> callback.onSummaryReceived(safeCount, safeDuration));
        });
    }

    /**
     * 在单一 diskIo 任务中构建统计页完整快照，避免多路异步交错。
     * 回调保证投递到主线程；调用方应用 generation 丢弃过期结果。
     */
    public void loadDashboard(DashboardCallback callback) {
        final String userId = accountManager.requireActiveUserId();
        AppExecutors.getInstance().diskIo(() -> {
            try {
                StatisticsDashboard dashboard = buildDashboardSync(userId);
                if (callback != null) {
                    mainHandler.post(() -> callback.onDashboardLoaded(dashboard));
                }
            } catch (Exception e) {
                AppLog.e("StatisticsRepository", "Failed to load dashboard", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onDashboardLoaded(emptyDashboard()));
                }
            }
        });
    }

    private StatisticsDashboard buildDashboardSync(String userId) {
        long[] todayRange = todayRange();
        long[] thisWeekRange = weekRange(0);
        long[] lastWeekRange = weekRange(-1);
        long[] monthRange = monthRange();

        int todayCount = sessionDao.getCompletedCountInRangeForUser(userId, todayRange[0], todayRange[1]);
        long todayDuration = sessionDao.getTotalDurationInRangeForUser(userId, todayRange[0], todayRange[1]);
        DailyStats today = new DailyStats(
                appContext.getString(R.string.statistics_label_today), todayCount, todayDuration);

        WeeklyStats week = new WeeklyStats(
                sessionDao.getCompletedCountInRangeForUser(userId, thisWeekRange[0], thisWeekRange[1]),
                sessionDao.getTotalDurationInRangeForUser(userId, thisWeekRange[0], thisWeekRange[1]));
        WeeklyStats lastWeek = new WeeklyStats(
                sessionDao.getCompletedCountInRangeForUser(userId, lastWeekRange[0], lastWeekRange[1]),
                sessionDao.getTotalDurationInRangeForUser(userId, lastWeekRange[0], lastWeekRange[1]));
        MonthlyStats month = new MonthlyStats(
                sessionDao.getCompletedCountInRangeForUser(userId, monthRange[0], monthRange[1]),
                sessionDao.getTotalDurationInRangeForUser(userId, monthRange[0], monthRange[1]));

        long streakLookbackStart = daysAgoStart(120);
        List<DailyStats> streakSource = sessionDao.getDailyStatsInRangeSync(
                userId, streakLookbackStart, todayRange[1]);
        int currentStreak = computeCurrentStreak(streakSource);

        List<DailyStats> rawWeekly = sessionDao.getDailyStatsInRangeSync(
                userId, daysAgoStart(6), todayRange[1]);
        List<DailyStats> weeklyDays = fillLastNDays(rawWeekly, 7);

        List<DailyStats> rawMonthly = sessionDao.getDailyStatsInRangeSync(
                userId, monthRange[0], monthRange[1]);
        List<DailyStats> monthlyDays = fillMonthDays(rawMonthly);
        int activeDaysThisMonth = 0;
        for (DailyStats day : monthlyDays) {
            if (day.count > 0 || day.totalDuration > 0L) {
                activeDaysThisMonth++;
            }
        }

        List<HourlyStats> hourlyStats = sessionDao.getHourlyStatsInRangeSync(
                userId, monthRange[0], monthRange[1]);
        if (hourlyStats == null) {
            hourlyStats = new ArrayList<>();
        }
        List<CategoryStats> categoryStats = sessionDao.getCategoryStatsSync(
                userId, monthRange[0], monthRange[1]);
        if (categoryStats == null) {
            categoryStats = new ArrayList<>();
        }
        List<PauseReasonStats> pauseReasonStats = buildPauseReasonStatsSync(userId, monthRange);
        int totalCompleted = sessionDao.getTotalCompletedCountForUser(userId);

        return new StatisticsDashboard(
                today,
                week,
                lastWeek,
                month,
                currentStreak,
                activeDaysThisMonth,
                totalCompleted,
                weeklyDays,
                monthlyDays,
                hourlyStats,
                categoryStats,
                pauseReasonStats,
                buildPeakHourInsight(hourlyStats),
                buildTopCategoryInsight(categoryStats));
    }

    private StatisticsDashboard emptyDashboard() {
        DailyStats today = new DailyStats(appContext.getString(R.string.statistics_label_today), 0, 0L);
        WeeklyStats zeroWeek = new WeeklyStats(0, 0L);
        MonthlyStats zeroMonth = new MonthlyStats(0, 0L);
        return new StatisticsDashboard(
                today, zeroWeek, zeroWeek, zeroMonth,
                0, 0, 0,
                fillLastNDays(new ArrayList<>(), 7),
                fillMonthDays(new ArrayList<>()),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                null);
    }

    private List<PauseReasonStats> buildPauseReasonStatsSync(String userId, long[] range) {
        List<PomodoroSession> sessions = sessionDao.getCompletedSessionsWithPausesInRangeSync(
                userId, range[0], range[1]);
        Map<String, Integer> reasonCounts = new HashMap<>();
        if (sessions != null) {
            for (PomodoroSession session : sessions) {
                List<String> reasons = SessionPauseUtils.decodeReasons(
                        session.pauseReasons, session.pauseReason);
                for (String reason : reasons) {
                    reasonCounts.merge(reason, 1, Integer::sum);
                }
            }
        }
        List<PauseReasonStats> data = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : reasonCounts.entrySet()) {
            data.add(new PauseReasonStats(entry.getKey(), entry.getValue()));
        }
        data.sort((a, b) -> Integer.compare(b.count, a.count));
        return data;
    }

    private StatisticsDashboard.PeakHourInsight buildPeakHourInsight(List<HourlyStats> hourlyStats) {
        if (hourlyStats == null || hourlyStats.isEmpty()) {
            return null;
        }
        HourlyStats peak = null;
        for (HourlyStats stats : hourlyStats) {
            if (stats == null || stats.totalDuration <= 0L) {
                continue;
            }
            if (peak == null || stats.totalDuration > peak.totalDuration) {
                peak = stats;
            }
        }
        if (peak == null) {
            return null;
        }
        return new StatisticsDashboard.PeakHourInsight(peak.hour, peak.totalDuration);
    }

    private StatisticsDashboard.TopCategoryInsight buildTopCategoryInsight(List<CategoryStats> categoryStats) {
        if (categoryStats == null || categoryStats.isEmpty()) {
            return null;
        }
        CategoryStats top = null;
        long total = 0L;
        for (CategoryStats stats : categoryStats) {
            if (stats == null) {
                continue;
            }
            total += Math.max(0L, stats.totalDuration);
            if (top == null || stats.totalDuration > top.totalDuration) {
                top = stats;
            }
        }
        if (top == null || top.totalDuration <= 0L || total <= 0L) {
            return null;
        }
        String category = top.category != null && !top.category.isEmpty()
                ? top.category : CategoryDefaults.getDefault();
        float percent = top.totalDuration * 100f / total;
        return new StatisticsDashboard.TopCategoryInsight(category, top.totalDuration, percent);
    }

    private int computeCurrentStreak(List<DailyStats> dailyStats) {
        Set<String> activeDays = new HashSet<>();
        if (dailyStats != null) {
            for (DailyStats stats : dailyStats) {
                if (stats != null && stats.date != null
                        && (stats.count > 0 || stats.totalDuration > 0L)) {
                    activeDays.add(stats.date);
                }
            }
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cursor = Calendar.getInstance();
        // 今天还没有完成时，从昨天开始连续计数，避免「当天未完成就直接断签」。
        String todayKey = format.format(cursor.getTime());
        if (!activeDays.contains(todayKey)) {
            cursor.add(Calendar.DAY_OF_MONTH, -1);
        }
        int streak = 0;
        while (true) {
            String key = format.format(cursor.getTime());
            if (!activeDays.contains(key)) {
                break;
            }
            streak++;
            cursor.add(Calendar.DAY_OF_MONTH, -1);
            if (streak > 400) {
                break;
            }
        }
        return streak;
    }

    private List<DailyStats> fillLastNDays(List<DailyStats> raw, int days) {
        Map<String, DailyStats> byDate = indexByDate(raw);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        List<DailyStats> filled = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            Calendar day = Calendar.getInstance();
            day.add(Calendar.DAY_OF_MONTH, -i);
            String key = format.format(day.getTime());
            DailyStats existing = byDate.get(key);
            if (existing != null) {
                filled.add(existing);
            } else {
                filled.add(new DailyStats(key, 0, 0L));
            }
        }
        return filled;
    }

    private List<DailyStats> fillMonthDays(List<DailyStats> raw) {
        Map<String, DailyStats> byDate = indexByDate(raw);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar calendar = Calendar.getInstance();
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        List<DailyStats> filled = new ArrayList<>(daysInMonth);
        for (int day = 1; day <= daysInMonth; day++) {
            Calendar dayCal = Calendar.getInstance();
            dayCal.set(Calendar.DAY_OF_MONTH, day);
            String key = format.format(dayCal.getTime());
            DailyStats existing = byDate.get(key);
            if (existing != null) {
                filled.add(existing);
            } else {
                filled.add(new DailyStats(key, 0, 0L));
            }
        }
        return filled;
    }

    private Map<String, DailyStats> indexByDate(List<DailyStats> raw) {
        Map<String, DailyStats> map = new HashMap<>();
        if (raw == null) {
            return map;
        }
        for (DailyStats stats : raw) {
            if (stats != null && stats.date != null) {
                map.put(stats.date, stats);
            }
        }
        return map;
    }

    private long[] todayRange() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long start = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return new long[]{start, calendar.getTimeInMillis()};
    }

    /** @param weekOffset 0=本周(周一起)，-1=上周 */
    private long[] weekRange(int weekOffset) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int diffToMonday = (dayOfWeek == Calendar.SUNDAY) ? -6 : (Calendar.MONDAY - dayOfWeek);
        calendar.add(Calendar.DAY_OF_MONTH, diffToMonday + (weekOffset * 7));
        long start = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        return new long[]{start, calendar.getTimeInMillis()};
    }

    private long daysAgoStart(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -daysAgo);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
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

    public interface ProfileSummaryCallback {
        void onSummaryReceived(int totalCompletedCount, long totalFocusDurationMs);
    }

    public interface DashboardCallback {
        void onDashboardLoaded(StatisticsDashboard dashboard);
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
