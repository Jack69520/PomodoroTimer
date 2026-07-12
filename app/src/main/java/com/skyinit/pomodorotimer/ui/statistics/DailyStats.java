package com.skyinit.pomodorotimer.ui.statistics;

public class DailyStats {
    public String date;
    public int count;
    public long totalDuration;

    public DailyStats(String date, int count, long totalDuration) {
        this.date = date;
        this.count = count;
        this.totalDuration = totalDuration;
    }
}
