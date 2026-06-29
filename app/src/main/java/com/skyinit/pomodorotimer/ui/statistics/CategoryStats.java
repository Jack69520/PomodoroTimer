package com.skyinit.pomodorotimer.ui.statistics;

public class CategoryStats {
    public String category;
    public int count;
    public long totalDuration;

    public CategoryStats(String category, int count, long totalDuration) {
        this.category = category;
        this.count = count;
        this.totalDuration = totalDuration;
    }
}
