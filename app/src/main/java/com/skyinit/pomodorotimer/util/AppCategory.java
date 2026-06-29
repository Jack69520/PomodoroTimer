package com.skyinit.pomodorotimer.util;

import android.content.Context;

import com.skyinit.pomodorotimer.R;

/**
 * 应用屏蔽分类常量，统一管理分类名称与 UI 资源映射。
 * 在 {@link com.skyinit.pomodorotimer.App#onCreate()} 中调用 {@link #init(Context)} 加载本地化名称。
 */
public final class AppCategory {

    public static String FILTER_ALL;

    public static String HEALTH;
    public static String EDUCATION;
    public static String FINANCE;
    public static String TRAVEL;
    public static String JOB;
    public static String OFFICE;
    public static String SOCIAL;
    public static String ENTERTAINMENT;
    public static String SHOPPING;
    public static String TOOL;
    public static String NEWS;
    public static String SYSTEM;
    public static String OTHER;

    /** 可分配给应用的业务分类（不含筛选用的「全部」） */
    public static String[] ASSIGNABLE;

    /** 筛选下拉框选项（含「全部」） */
    public static String[] FILTER_OPTIONS;

    private AppCategory() {
    }

    public static void init(Context context) {
        Context app = context.getApplicationContext();
        FILTER_ALL = app.getString(R.string.common_filter_all);

        String[] categories = app.getResources().getStringArray(R.array.app_category_names);
        FILTER_OPTIONS = categories;
        ASSIGNABLE = new String[categories.length - 1];
        System.arraycopy(categories, 1, ASSIGNABLE, 0, ASSIGNABLE.length);

        HEALTH = ASSIGNABLE[0];
        EDUCATION = ASSIGNABLE[1];
        FINANCE = ASSIGNABLE[2];
        TRAVEL = ASSIGNABLE[3];
        JOB = ASSIGNABLE[4];
        OFFICE = ASSIGNABLE[5];
        SOCIAL = ASSIGNABLE[6];
        ENTERTAINMENT = ASSIGNABLE[7];
        SHOPPING = ASSIGNABLE[8];
        TOOL = ASSIGNABLE[9];
        NEWS = ASSIGNABLE[10];
        SYSTEM = ASSIGNABLE[11];
        OTHER = ASSIGNABLE[12];
    }

    public static boolean isAssignable(String category) {
        if (category == null || ASSIGNABLE == null) {
            return false;
        }
        for (String item : ASSIGNABLE) {
            if (item.equals(category)) {
                return true;
            }
        }
        return false;
    }

    public static int getBackgroundRes(String category) {
        if (category == null) {
            return R.drawable.category_other_background;
        }
        if (category.equals(EDUCATION)) {
            return R.drawable.category_education_background;
        }
        if (category.equals(FINANCE)) {
            return R.drawable.category_finance_background;
        }
        if (category.equals(TRAVEL)) {
            return R.drawable.category_transport_background;
        }
        if (category.equals(HEALTH)) {
            return R.drawable.category_health_background;
        }
        if (category.equals(JOB)) {
            return R.drawable.category_job_background;
        }
        if (category.equals(OFFICE)) {
            return R.drawable.category_office_background;
        }
        if (category.equals(SOCIAL)) {
            return R.drawable.category_life_background;
        }
        if (category.equals(ENTERTAINMENT)) {
            return R.drawable.category_entertainment_background;
        }
        if (category.equals(SHOPPING)) {
            return R.drawable.category_shopping_background;
        }
        if (category.equals(TOOL)) {
            return R.drawable.category_tool_background;
        }
        if (category.equals(NEWS)) {
            return R.drawable.category_news_background;
        }
        if (category.equals(SYSTEM)) {
            return R.drawable.category_system_background;
        }
        return R.drawable.category_other_background;
    }
}
