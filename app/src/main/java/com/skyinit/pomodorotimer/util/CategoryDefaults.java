package com.skyinit.pomodorotimer.util;

import android.content.Context;

import com.skyinit.pomodorotimer.R;

/**
 * 从字符串资源加载的默认分类名，供数据层在无 UI Context 时使用。
 * 在 {@link com.skyinit.pomodorotimer.App#onCreate()} 中初始化。
 */
public final class CategoryDefaults {

    private static String defaultCategory;
    private static String otherCategory;
    private static String workCategory;
    private static String studyCategory;
    private static String lifeCategory;
    private static String sportsCategory;
    private static String entertainmentCategory;

    private CategoryDefaults() {
    }

    public static void init(Context context) {
        Context app = context.getApplicationContext();
        defaultCategory = app.getString(R.string.timer_category_default);
        otherCategory = app.getString(R.string.task_category_other);
        workCategory = app.getString(R.string.task_category_work);
        studyCategory = app.getString(R.string.task_category_study);
        lifeCategory = app.getString(R.string.task_category_life);
        sportsCategory = app.getString(R.string.task_category_sports);
        entertainmentCategory = app.getString(R.string.task_category_entertainment);
    }

    public static String getDefault() {
        return defaultCategory != null ? defaultCategory : "默认";
    }

    public static String getOther() {
        return otherCategory != null ? otherCategory : "其他";
    }

    public static String getWork() {
        return workCategory != null ? workCategory : "工作";
    }

    public static String getStudy() {
        return studyCategory != null ? studyCategory : "学习";
    }

    public static String getLife() {
        return lifeCategory != null ? lifeCategory : "生活";
    }

    public static String getSports() {
        return sportsCategory != null ? sportsCategory : "运动";
    }

    public static String getEntertainment() {
        return entertainmentCategory != null ? entertainmentCategory : "娱乐";
    }

    /** 与 {@link R.array#task_categories} 顺序一致。 */
    public static String[] getTaskCategories(Context context) {
        return context.getApplicationContext().getResources().getStringArray(R.array.task_categories);
    }
}
